package net.xmppwocky.earbs.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.audio.ChordType
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

class EarbsRepository(
    private val cardDao: CardDao,
    private val reviewSessionDao: ReviewSessionDao,
    private val trialDao: TrialDao,
    private val sessionCardSummaryDao: SessionCardSummaryDao,
    private val historyDao: HistoryDao
) {
    private val fsrs = FSRS(requestRetention = 0.9, params = DEFAULT_PARAMS)

    /**
     * Initialize the starting deck on first launch.
     * Creates 4 cards: MAJOR, MINOR, SUS2, SUS4 @ octave 4.
     */
    suspend fun initializeStartingDeck() {
        val existingCount = cardDao.count()
        if (existingCount > 0) {
            Log.i(TAG, "Database already has $existingCount cards, skipping initialization")
            return
        }

        Log.i(TAG, "Initializing starting deck with 4 cards")
        val now = System.currentTimeMillis()

        val startingCards = listOf(
            ChordType.MAJOR,
            ChordType.MINOR,
            ChordType.SUS2,
            ChordType.SUS4
        ).map { chordType ->
            CardEntity(
                id = "${chordType.name}_4",
                chordType = chordType.name,
                octave = 4,
                dueDate = now  // Immediately due
            )
        }

        cardDao.insertAll(startingCards)
        Log.i(TAG, "Inserted ${startingCards.size} starting cards")
    }

    /**
     * Select 4 cards for a review session using the card selection algorithm:
     * 1. Get due cards (dueDate <= now)
     * 2. Group by octave, pick octave with most due
     * 3. If <4 due, pad with non-due cards from same octave
     */
    suspend fun selectCardsForReview(): List<Card> {
        val now = System.currentTimeMillis()
        Log.i(TAG, "Selecting cards for review (now=$now)")

        val dueCards = cardDao.getDueCards(now)
        Log.i(TAG, "Found ${dueCards.size} due cards")

        if (dueCards.isEmpty()) {
            // No due cards - fall back to all unlocked cards from octave 4
            Log.i(TAG, "No due cards, selecting from octave 4")
            val octave4Cards = cardDao.getCardsForOctave(4)
            return octave4Cards.take(4).map { it.toCard() }
        }

        // Group by octave, pick octave with most due
        val byOctave = dueCards.groupBy { it.octave }
        val bestOctave = byOctave.maxByOrNull { it.value.size }?.key ?: 4
        Log.i(TAG, "Best octave: $bestOctave (${byOctave[bestOctave]?.size ?: 0} due)")

        val octaveDueCards = byOctave[bestOctave] ?: emptyList()

        val selectedCards = if (octaveDueCards.size >= 4) {
            octaveDueCards.take(4)
        } else {
            // Pad with non-due cards from same octave
            val nonDue = cardDao.getNonDueCardsForOctave(bestOctave, now)
            Log.i(TAG, "Padding with ${nonDue.size} non-due cards from octave $bestOctave")
            (octaveDueCards + nonDue).take(4)
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
    suspend fun startSession(octave: Int): Long {
        val session = ReviewSessionEntity(
            startedAt = System.currentTimeMillis(),
            octave = octave
        )
        val sessionId = reviewSessionDao.insert(session)
        Log.i(TAG, "Started session id=$sessionId for octave $octave")
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
        octave = octave
    )
}

/**
 * Convert domain Card to CardEntity ID format.
 */
private fun Card.toCardId(): String {
    return "${chordType.name}_$octave"
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
