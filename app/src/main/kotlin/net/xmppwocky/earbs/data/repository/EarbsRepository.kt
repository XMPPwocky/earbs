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
import net.xmppwocky.earbs.data.db.SessionCardSummaryDao
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.db.TrialDao
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.SessionCardSummaryEntity
import net.xmppwocky.earbs.data.entity.TrialEntity
import net.xmppwocky.earbs.fsrs.CardPhase
import net.xmppwocky.earbs.fsrs.DEFAULT_PARAMS
import net.xmppwocky.earbs.fsrs.FSRS
import net.xmppwocky.earbs.fsrs.FlashCard
import net.xmppwocky.earbs.fsrs.Rating
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.CardScore
import net.xmppwocky.earbs.model.Deck
import net.xmppwocky.earbs.model.Grade
import java.time.LocalDateTime

private const val TAG = "EarbsRepository"

/**
 * Record of a single trial during a review session.
 */
data class TrialRecord(
    val cardId: String,
    val timestamp: Long,
    val wasCorrect: Boolean
)

private const val PREF_KEY_UNLOCK_LEVEL = "unlock_level"

class EarbsRepository(
    private val cardDao: CardDao,
    private val reviewSessionDao: ReviewSessionDao,
    private val trialDao: TrialDao,
    private val sessionCardSummaryDao: SessionCardSummaryDao,
    private val historyDao: HistoryDao,
    private val prefs: SharedPreferences
) {
    private val fsrs = FSRS(requestRetention = 0.9, params = DEFAULT_PARAMS)

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
     * Select 4 cards for a review session using the card selection algorithm:
     * 1. Get due cards (dueDate <= now)
     * 2. Group by (octave, playbackMode), pick group with most due
     * 3. If <4 due, pad with non-due cards from same group
     *
     * Session constraint: All 4 cards must share the same (octave, playbackMode)
     */
    suspend fun selectCardsForReview(): List<Card> {
        val now = System.currentTimeMillis()
        Log.i(TAG, "Selecting cards for review (now=$now)")

        val dueCards = cardDao.getDueCards(now)
        Log.i(TAG, "Found ${dueCards.size} due cards")

        if (dueCards.isEmpty()) {
            // No due cards - fall back to first available group (octave 4, arpeggiated)
            Log.i(TAG, "No due cards, selecting from octave 4 arpeggiated")
            val fallbackCards = cardDao.getCardsForGroup(4, PlaybackMode.ARPEGGIATED.name)
            return fallbackCards.take(4).map { it.toCard() }
        }

        // Group by (octave, playbackMode), pick group with most due
        val byGroup = dueCards.groupBy { it.octave to it.playbackMode }
        val bestGroup = byGroup.maxByOrNull { it.value.size }?.key
            ?: (4 to PlaybackMode.ARPEGGIATED.name)
        val (bestOctave, bestMode) = bestGroup

        Log.i(TAG, "Best group: octave $bestOctave, mode $bestMode (${byGroup[bestGroup]?.size ?: 0} due)")

        val groupDueCards = byGroup[bestGroup] ?: emptyList()

        val selectedCards = if (groupDueCards.size >= 4) {
            groupDueCards.take(4)
        } else {
            // Pad with non-due cards from same group
            val nonDue = cardDao.getNonDueCardsForGroup(bestOctave, bestMode, now)
            Log.i(TAG, "Padding with ${nonDue.size} non-due cards from octave $bestOctave, mode $bestMode")
            (groupDueCards + nonDue).take(4)
        }

        Log.i(TAG, "Selected ${selectedCards.size} cards for review:")
        selectedCards.forEach { card ->
            Log.i(TAG, "  - ${card.id} (due=${card.dueDate <= now})")
        }

        return selectedCards.map { it.toCard() }
    }

    /**
     * Start a new review session and return its database ID.
     */
    suspend fun startSession(octave: Int, playbackMode: PlaybackMode): Long {
        val session = ReviewSessionEntity(
            startedAt = System.currentTimeMillis(),
            octave = octave,
            playbackMode = playbackMode.name
        )
        val sessionId = reviewSessionDao.insert(session)
        Log.i(TAG, "Started session id=$sessionId for octave $octave, mode $playbackMode")
        return sessionId
    }

    /**
     * Complete a session: persist trials, summaries, and update FSRS state.
     */
    suspend fun completeSession(
        sessionId: Long,
        trialRecords: List<TrialRecord>,
        cardScores: List<CardScore>
    ) {
        val now = System.currentTimeMillis()
        Log.i(TAG, "Completing session $sessionId with ${trialRecords.size} trials")

        // 1. Mark session complete
        reviewSessionDao.markComplete(sessionId, now)

        // 2. Insert all trials
        val trialEntities = trialRecords.map { trial ->
            TrialEntity(
                sessionId = sessionId,
                cardId = trial.cardId,
                timestamp = trial.timestamp,
                wasCorrect = trial.wasCorrect
            )
        }
        trialDao.insertAll(trialEntities)
        Log.i(TAG, "Inserted ${trialEntities.size} trial records")

        // 3. Insert summaries and update FSRS state for each card
        for (cardScore in cardScores) {
            val cardId = cardScore.card.toCardId()

            // Insert summary
            sessionCardSummaryDao.insert(
                SessionCardSummaryEntity(
                    sessionId = sessionId,
                    cardId = cardId,
                    trialsCount = cardScore.total,
                    correctCount = cardScore.correct,
                    grade = cardScore.grade.name
                )
            )

            // Update FSRS state
            val cardEntity = cardDao.getById(cardId) ?: continue
            updateFsrsState(cardEntity, cardScore.grade)
        }

        Log.i(TAG, "Session $sessionId complete")
    }

    private suspend fun updateFsrsState(cardEntity: CardEntity, grade: Grade) {
        Log.i(TAG, "Updating FSRS state for ${cardEntity.id} with grade ${grade.name}")

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

        // Calculate new FSRS state
        val grades = fsrs.calculate(flashCard)
        val rating = grade.toRating()
        val chosenGrade = grades.first { it.choice == rating }

        val now = System.currentTimeMillis()
        val newDueDate = now + chosenGrade.durationMillis

        // Determine new phase
        val newPhase = when {
            rating == Rating.Again -> CardPhase.ReLearning.value
            cardEntity.phase == CardPhase.Added.value && rating != Rating.Again -> CardPhase.Review.value
            else -> CardPhase.Review.value
        }

        // Determine lapses
        val newLapses = if (rating == Rating.Again) cardEntity.lapses + 1 else cardEntity.lapses

        Log.i(TAG, "  FSRS update: stability=${chosenGrade.stability}, difficulty=${chosenGrade.difficulty}")
        Log.i(TAG, "  interval=${chosenGrade.interval}d, dueIn=${chosenGrade.durationMillis / 1000 / 60}min")

        cardDao.updateFsrsState(
            id = cardEntity.id,
            stability = chosenGrade.stability,
            difficulty = chosenGrade.difficulty,
            interval = chosenGrade.interval,
            dueDate = newDueDate,
            reviewCount = cardEntity.reviewCount + 1,
            lastReview = now,
            phase = newPhase,
            lapses = newLapses
        )
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

    /**
     * Get summaries for a specific session.
     */
    fun getSummariesForSession(sessionId: Long) =
        sessionCardSummaryDao.getSummariesForSession(sessionId)
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

/**
 * Convert app Grade to FSRS Rating.
 */
private fun Grade.toRating(): Rating = when (this) {
    Grade.EASY -> Rating.Easy
    Grade.GOOD -> Rating.Good
    Grade.HARD -> Rating.Hard
    Grade.AGAIN -> Rating.Again
}
