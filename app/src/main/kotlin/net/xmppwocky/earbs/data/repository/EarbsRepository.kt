package net.xmppwocky.earbs.data.repository

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.data.db.CardDao
import net.xmppwocky.earbs.data.db.CardStatsView
import net.xmppwocky.earbs.data.db.HistoryDao
import net.xmppwocky.earbs.data.db.ReviewSessionDao
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.db.TrialDao
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity
import net.xmppwocky.earbs.fsrs.CardPhase
import net.xmppwocky.earbs.fsrs.DEFAULT_PARAMS
import net.xmppwocky.earbs.fsrs.FSRS
import net.xmppwocky.earbs.fsrs.FlashCard
import net.xmppwocky.earbs.fsrs.Rating
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.Deck
import java.time.LocalDateTime

private const val TAG = "EarbsRepository"
private const val PREF_KEY_UNLOCK_LEVEL = "unlock_level"
private const val PREF_KEY_SESSION_SIZE = "session_size"
private const val PREF_KEY_TARGET_RETENTION = "target_retention"
private const val DEFAULT_SESSION_SIZE = 20
private const val DEFAULT_TARGET_RETENTION = 0.9f

class EarbsRepository(
    private val cardDao: CardDao,
    private val reviewSessionDao: ReviewSessionDao,
    private val trialDao: TrialDao,
    private val historyDao: HistoryDao,
    private val prefs: SharedPreferences
) {
    /**
     * Get FSRS instance with current target retention setting.
     */
    private fun getFsrs(): FSRS {
        val targetRetention = prefs.getFloat(PREF_KEY_TARGET_RETENTION, DEFAULT_TARGET_RETENTION).toDouble()
        Log.d(TAG, "Creating FSRS with target retention: $targetRetention")
        return FSRS(requestRetention = targetRetention, params = DEFAULT_PARAMS)
    }

    /**
     * Get configured session size from settings.
     */
    fun getSessionSize(): Int {
        return prefs.getInt(PREF_KEY_SESSION_SIZE, DEFAULT_SESSION_SIZE)
    }

    /**
     * Initialize the starting deck on first launch.
     * Creates 4 cards: MAJOR, MINOR, SUS2, SUS4 @ octave 4, arpeggiated.
     */
    suspend fun initializeStartingDeck() {
        val existingCount = cardDao.count()
        if (existingCount > 0) {
            Log.i(TAG, "Database already has $existingCount cards, skipping initialization")
            return
        }

        Log.i(TAG, "Initializing starting deck with 4 arpeggiated triads @ octave 4")
        val now = System.currentTimeMillis()

        // Starting deck is group 0: triads @ octave 4, arpeggiated
        val startingGroup = Deck.UNLOCK_ORDER[0]
        val startingCards = startingGroup.chordTypes.map { chordType ->
            CardEntity(
                id = "${chordType.name}_${startingGroup.octave}_${startingGroup.playbackMode.name}",
                chordType = chordType.name,
                octave = startingGroup.octave,
                playbackMode = startingGroup.playbackMode.name,
                dueDate = now  // Immediately due
            )
        }

        cardDao.insertAll(startingCards)
        prefs.edit().putInt(PREF_KEY_UNLOCK_LEVEL, 0).apply()
        Log.i(TAG, "Inserted ${startingCards.size} starting cards, unlock level set to 0")
    }

    /**
     * Get the current unlock level (0-indexed).
     * Level 0 = starting deck (group 0 unlocked)
     * Level 11 = full deck (all 12 groups unlocked)
     */
    fun getUnlockLevel(): Int {
        return prefs.getInt(PREF_KEY_UNLOCK_LEVEL, 0)
    }

    /**
     * Check if there are more cards to unlock.
     */
    fun canUnlockMore(): Boolean {
        return getUnlockLevel() < Deck.MAX_UNLOCK_LEVEL
    }

    /**
     * Unlock the next group of 4 cards.
     * @return true if unlock succeeded, false if already at max level
     */
    suspend fun unlockNextGroup(): Boolean {
        val currentLevel = getUnlockLevel()
        if (currentLevel >= Deck.MAX_UNLOCK_LEVEL) {
            Log.i(TAG, "Already at max unlock level ($currentLevel), cannot unlock more")
            return false
        }

        val nextLevel = currentLevel + 1
        val nextGroup = Deck.UNLOCK_ORDER[nextLevel]
        val now = System.currentTimeMillis()

        Log.i(TAG, "Unlocking group $nextLevel: ${nextGroup.chordTypes.map { it.displayName }} @ octave ${nextGroup.octave}, ${nextGroup.playbackMode}")

        val newCards = nextGroup.chordTypes.map { chordType ->
            CardEntity(
                id = "${chordType.name}_${nextGroup.octave}_${nextGroup.playbackMode.name}",
                chordType = chordType.name,
                octave = nextGroup.octave,
                playbackMode = nextGroup.playbackMode.name,
                dueDate = now  // Due immediately
            )
        }

        cardDao.insertAll(newCards)
        prefs.edit().putInt(PREF_KEY_UNLOCK_LEVEL, nextLevel).apply()
        Log.i(TAG, "Unlocked ${newCards.size} new cards, unlock level now $nextLevel")
        return true
    }

    /**
     * Get the number of unlocked cards.
     */
    suspend fun getUnlockedCount(): Int {
        return cardDao.countUnlocked()
    }

    /**
     * Observe the number of unlocked cards.
     */
    fun getUnlockedCountFlow(): Flow<Int> {
        return cardDao.countUnlockedFlow()
    }

    /**
     * Select cards for a review session:
     * 1. Get due cards (dueDate <= now)
     * 2. If fewer than session size, pad with non-due cards (reviewing early)
     * 3. Shuffle randomly
     */
    suspend fun selectCardsForReview(): List<Card> {
        val sessionSize = getSessionSize()
        val now = System.currentTimeMillis()
        Log.i(TAG, "Selecting $sessionSize cards for review (now=$now)")

        val dueCards = cardDao.getDueCards(now)
        Log.i(TAG, "Found ${dueCards.size} due cards")

        val selectedCards = if (dueCards.size >= sessionSize) {
            // More than enough due cards - take the most overdue
            dueCards.take(sessionSize)
        } else if (dueCards.isEmpty()) {
            // No due cards - select from all unlocked cards
            Log.i(TAG, "No due cards, selecting from all unlocked cards")
            val allCards = cardDao.getAllUnlocked()
            allCards.take(sessionSize)
        } else {
            // Pad with non-due cards (reviewing early)
            val needed = sessionSize - dueCards.size
            val nonDue = cardDao.getNonDueCards(now, needed)
            Log.i(TAG, "Padding with ${nonDue.size} non-due cards")
            dueCards + nonDue
        }

        // Shuffle randomly
        val shuffledCards = selectedCards.shuffled()

        Log.i(TAG, "Selected ${shuffledCards.size} cards for review:")
        shuffledCards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.id} (due=${card.dueDate <= now})")
        }

        return shuffledCards.map { it.toCard() }
    }

    /**
     * Start a new review session and return its database ID.
     * Sessions now contain mixed cards, so octave/mode are set to defaults.
     */
    suspend fun startSession(): Long {
        val session = ReviewSessionEntity(
            startedAt = System.currentTimeMillis(),
            octave = 0,  // Mixed cards - not meaningful
            playbackMode = "MIXED"
        )
        val sessionId = reviewSessionDao.insert(session)
        Log.i(TAG, "Started session id=$sessionId")
        return sessionId
    }

    /**
     * Record a single trial and immediately update FSRS state.
     * Called after each answer in a session.
     *
     * @return the new due date for the card
     */
    suspend fun recordTrialAndUpdateFsrs(
        sessionId: Long,
        card: Card,
        wasCorrect: Boolean
    ): Long {
        val timestamp = System.currentTimeMillis()
        val cardId = card.toCardId()

        Log.i(TAG, "Recording trial for $cardId: ${if (wasCorrect) "CORRECT" else "WRONG"}")

        // 1. Insert trial record
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = cardId,
            timestamp = timestamp,
            wasCorrect = wasCorrect
        ))

        // 2. Get card entity
        val cardEntity = cardDao.getById(cardId)
        if (cardEntity == null) {
            Log.e(TAG, "Card not found: $cardId")
            return timestamp
        }

        // 3. Calculate rating: correct=Good, wrong=Again
        val rating = if (wasCorrect) Rating.Good else Rating.Again

        // 4. Apply FSRS update immediately
        return applyFsrsUpdate(cardEntity, rating)
    }

    /**
     * Apply FSRS update for a card with the given rating.
     * @return the new due date
     */
    private suspend fun applyFsrsUpdate(cardEntity: CardEntity, rating: Rating): Long {
        Log.i(TAG, "Updating FSRS state for ${cardEntity.id} with rating ${rating.name}")

        // Convert CardEntity to FlashCard for FSRS calculation
        val flashCard = FlashCard(
            id = 0,  // Not used
            stability = cardEntity.stability,
            difficulty = cardEntity.difficulty,
            interval = cardEntity.interval,
            dueDate = LocalDateTime.now(),
            reviewCount = cardEntity.reviewCount,
            lastReview = LocalDateTime.now(),
            phase = cardEntity.phase
        )

        // Calculate new FSRS state using current target retention setting
        val fsrs = getFsrs()
        val grades = fsrs.calculate(flashCard)
        val chosenGrade = grades.first { it.choice == rating }

        val now = System.currentTimeMillis()
        val newDueDate = now + chosenGrade.durationMillis

        // Determine new phase
        val newPhase = when {
            rating == Rating.Again -> CardPhase.ReLearning.value
            cardEntity.phase == CardPhase.Added.value -> CardPhase.Review.value
            else -> CardPhase.Review.value
        }

        // Determine lapses
        val newLapses = if (rating == Rating.Again) cardEntity.lapses + 1 else cardEntity.lapses

        // For Added phase, FSRS returns 0.0 for stability/difficulty (uses fixed intervals).
        // Keep the card's existing values to avoid NaN crashes on subsequent reviews.
        val newStability = if (cardEntity.phase == CardPhase.Added.value || chosenGrade.stability == 0.0) {
            cardEntity.stability
        } else {
            chosenGrade.stability
        }
        val newDifficulty = if (cardEntity.phase == CardPhase.Added.value || chosenGrade.difficulty == 0.0) {
            cardEntity.difficulty
        } else {
            chosenGrade.difficulty
        }

        Log.i(TAG, "  FSRS update: stability=$newStability, difficulty=$newDifficulty")
        Log.i(TAG, "  interval=${chosenGrade.interval}d, dueIn=${chosenGrade.durationMillis / 1000 / 60}min")

        cardDao.updateFsrsState(
            id = cardEntity.id,
            stability = newStability,
            difficulty = newDifficulty,
            interval = chosenGrade.interval,
            dueDate = newDueDate,
            reviewCount = cardEntity.reviewCount + 1,
            lastReview = now,
            phase = newPhase,
            lapses = newLapses
        )

        return newDueDate
    }

    /**
     * Complete a session by marking it as finished.
     * FSRS updates are done per-trial, so this just marks completion time.
     */
    suspend fun completeSession(sessionId: Long) {
        val now = System.currentTimeMillis()
        Log.i(TAG, "Completing session $sessionId")
        reviewSessionDao.markComplete(sessionId, now)
        Log.i(TAG, "Session $sessionId complete")
    }

    /**
     * Get count of due cards.
     */
    suspend fun getDueCount(): Int {
        val count = cardDao.countDue(System.currentTimeMillis())
        Log.d(TAG, "Due count: $count")
        return count
    }

    fun getDueCountFlow(): Flow<Int> {
        return cardDao.countDueFlow(System.currentTimeMillis())
    }

    /**
     * Get all cards flow for UI.
     */
    fun getAllCardsFlow(): Flow<List<CardEntity>> {
        return cardDao.getAllUnlockedFlow()
    }

    /**
     * Get session overview for history screen.
     */
    fun getSessionOverviews(): Flow<List<SessionOverview>> {
        return historyDao.getSessionOverviews()
    }

    /**
     * Get card statistics for history screen.
     */
    fun getCardStats(): Flow<List<CardStatsView>> {
        return historyDao.getCardStats()
    }

    /**
     * Get all sessions for history screen.
     */
    fun getAllSessions(): Flow<List<ReviewSessionEntity>> {
        return historyDao.getAllSessions()
    }
}

/**
 * Convert CardEntity to domain Card.
 */
private fun CardEntity.toCard(): Card {
    return Card(
        chordType = ChordType.valueOf(chordType),
        octave = octave,
        playbackMode = PlaybackMode.valueOf(playbackMode)
    )
}

/**
 * Convert domain Card to CardEntity ID format.
 */
private fun Card.toCardId(): String {
    return "${chordType.name}_${octave}_${playbackMode.name}"
}
