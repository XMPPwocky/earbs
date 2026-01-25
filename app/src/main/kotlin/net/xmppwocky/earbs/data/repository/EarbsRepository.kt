package net.xmppwocky.earbs.data.repository

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.data.db.CardDao
import net.xmppwocky.earbs.data.db.CardSessionAccuracy
import net.xmppwocky.earbs.data.db.SessionCardStats
import net.xmppwocky.earbs.data.db.CardStatsView
import net.xmppwocky.earbs.data.db.ConfusionEntry
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.FsrsStateDao
import net.xmppwocky.earbs.data.db.FunctionCardDao
import net.xmppwocky.earbs.data.db.HistoryDao
import net.xmppwocky.earbs.data.db.ReviewSessionDao
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.db.TrialDao
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity
import net.xmppwocky.earbs.fsrs.CardPhase
import net.xmppwocky.earbs.fsrs.DEFAULT_PARAMS
import net.xmppwocky.earbs.fsrs.FSRS
import net.xmppwocky.earbs.fsrs.FlashCard
import net.xmppwocky.earbs.fsrs.Rating
import net.xmppwocky.earbs.data.db.FunctionCardWithFsrs
import net.xmppwocky.earbs.data.db.ProgressionCardDao
import net.xmppwocky.earbs.data.db.ProgressionCardWithFsrs
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.Deck
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.FunctionDeck
import net.xmppwocky.earbs.model.GameCard
import net.xmppwocky.earbs.model.KeyQuality
import net.xmppwocky.earbs.model.ProgressionCard
import net.xmppwocky.earbs.model.ProgressionDeck
import net.xmppwocky.earbs.model.ProgressionType
import java.time.LocalDateTime

private const val TAG = "EarbsRepository"
private const val PREF_KEY_SESSION_SIZE = "session_size"
private const val PREF_KEY_TARGET_RETENTION = "target_retention"
private const val DEFAULT_SESSION_SIZE = 20
private const val DEFAULT_TARGET_RETENTION = 0.9f

class EarbsRepository(
    private val cardDao: CardDao,
    private val functionCardDao: FunctionCardDao,
    private val progressionCardDao: ProgressionCardDao,
    private val fsrsStateDao: FsrsStateDao,
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
     * Repeat cards cyclically to fill the target size.
     * Example: [A, B] with targetSize=5 → [A, B, A, B, A]
     */
    private fun <T> repeatToFill(cards: List<T>, targetSize: Int): List<T> {
        if (cards.isEmpty() || cards.size >= targetSize) return cards
        val result = mutableListOf<T>()
        while (result.size < targetSize) {
            result.addAll(cards.take(targetSize - result.size))
        }
        return result
    }

    // ========== Generic Card Operations (Adapters) ==========

    /** Adapter for chord type card operations */
    private val chordTypeOps by lazy { ChordTypeCardOperations(cardDao) }

    /** Adapter for function card operations */
    private val functionOps by lazy { FunctionCardOperations(functionCardDao) }

    /** Adapter for progression card operations */
    private val progressionOps by lazy { ProgressionCardOperations(progressionCardDao) }

    // ========== Generic Methods with GameType Dispatch ==========

    /**
     * Initialize deck for a game type.
     * Ensures all cards have FSRS state initialized.
     */
    suspend fun initializeDeck(gameType: GameType) {
        when (gameType) {
            GameType.CHORD_TYPE -> initializeStartingDeck()
            GameType.CHORD_FUNCTION -> initializeFunctionStartingDeck()
            GameType.CHORD_PROGRESSION -> initializeProgressionStartingDeck()
        }
    }

    /**
     * Get due count for a game type.
     */
    suspend fun getDueCount(gameType: GameType): Int {
        val count = fsrsStateDao.countDueByGameType(gameType.name, System.currentTimeMillis())
        Log.d(TAG, "${gameType.name} due count: $count")
        return count
    }

    /**
     * Get due count flow for a game type.
     */
    fun getDueCountFlow(gameType: GameType): Flow<Int> {
        return fsrsStateDao.countDueByGameTypeFlow(gameType.name, System.currentTimeMillis())
    }

    /**
     * Get unlocked count for a game type.
     */
    suspend fun getUnlockedCount(gameType: GameType): Int {
        return when (gameType) {
            GameType.CHORD_TYPE -> cardDao.countUnlocked()
            GameType.CHORD_FUNCTION -> functionCardDao.countUnlocked()
            GameType.CHORD_PROGRESSION -> progressionCardDao.countUnlocked()
        }
    }

    /**
     * Get unlocked count flow for a game type.
     */
    fun getUnlockedCountFlow(gameType: GameType): Flow<Int> {
        return when (gameType) {
            GameType.CHORD_TYPE -> cardDao.countUnlockedFlow()
            GameType.CHORD_FUNCTION -> functionCardDao.countUnlockedFlow()
            GameType.CHORD_PROGRESSION -> progressionCardDao.countUnlockedFlow()
        }
    }

    /**
     * Set card unlock status for any game type.
     */
    suspend fun setCardUnlocked(gameType: GameType, cardId: String, unlocked: Boolean) {
        Log.i(TAG, "Setting ${gameType.name} card $cardId unlocked=$unlocked")
        when (gameType) {
            GameType.CHORD_TYPE -> cardDao.setUnlocked(cardId, unlocked)
            GameType.CHORD_FUNCTION -> functionCardDao.setUnlocked(cardId, unlocked)
            GameType.CHORD_PROGRESSION -> progressionCardDao.setUnlocked(cardId, unlocked)
        }
    }

    // ========== Generic Card Selection Algorithm ==========

    /**
     * Generic card selection algorithm that works with any game type.
     * Selects cards for a review session, preferring same-group cards:
     * 1. Get all due cards
     * 2. Group by game-specific key (octave/mode for chord type, keyQuality/octave/mode for function)
     * 3. Pick the group with most due cards (tie-break by most overdue)
     * 4. If group has >= sessionSize due, take sessionSize from that group
     * 5. If group has < sessionSize due:
     *    a. Take all due from that group
     *    b. Pad with non-due from SAME group first
     *    c. If still < sessionSize, pad with cards from OTHER groups
     * 6. Shuffle and return
     */
    private suspend fun <C : GameCard> selectCardsGeneric(
        ops: GameCardOperations<C>,
        gameName: String
    ): List<C> {
        val sessionSize = getSessionSize()
        val now = System.currentTimeMillis()
        Log.i(TAG, "Selecting $sessionSize $gameName cards for review (now=$now)")

        val allDue = ops.getDueCards(now)
        Log.i(TAG, "Found ${allDue.size} total due $gameName cards")

        if (allDue.isEmpty()) {
            Log.i(TAG, "No due $gameName cards, selecting from all unlocked")
            return selectFromAllUnlockedGeneric(ops, sessionSize, gameName)
        }

        // Group due cards by their grouping key
        val grouped = allDue.groupBy { ops.getGroupingKey(it) }
        Log.i(TAG, "Due $gameName cards grouped into ${grouped.size} groups")

        // Pick group with most due cards (tie-break by most overdue = lowest dueDate)
        val (bestGroup, bestDue) = grouped.maxByOrNull { (_, cards) ->
            cards.size * 1_000_000_000L - cards.minOf { it.dueDate }
        }!!

        val (groupKey, octave, mode) = bestGroup
        Log.i(TAG, "Selected best group: groupKey=$groupKey, octave=$octave, mode=$mode with ${bestDue.size} due cards")

        val selected = mutableListOf<CardWithFsrsData>()
        selected.addAll(bestDue.sortedBy { it.dueDate }.take(sessionSize))
        Log.i(TAG, "Added ${selected.size} due $gameName cards from best group")

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { ops.toDomainCard(it) }.also { logGenericSelection(it, gameName) }
        }

        // Pad with non-due from same group
        val nonDueSameGroup = ops.getNonDueCardsByGroup(now, groupKey, octave, mode, sessionSize - selected.size)
        Log.i(TAG, "Padding with ${nonDueSameGroup.size} non-due $gameName cards from same group")
        selected.addAll(nonDueSameGroup)

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { ops.toDomainCard(it) }.also { logGenericSelection(it, gameName) }
        }

        // Pad with due cards from other groups
        val selectedIds = selected.map { it.id }.toSet()
        val otherDueCards = allDue.filter { it.id !in selectedIds }.sortedBy { it.dueDate }
        val toAddFromOther = otherDueCards.take(sessionSize - selected.size)
        Log.i(TAG, "Padding with ${toAddFromOther.size} due $gameName cards from other groups")
        selected.addAll(toAddFromOther)

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { ops.toDomainCard(it) }.also { logGenericSelection(it, gameName) }
        }

        // If still not enough, get non-due from any group
        val stillSelectedIds = selected.map { it.id }.toSet()
        val moreNonDue = ops.getNonDueCards(now, sessionSize - selected.size)
            .filter { it.id !in stillSelectedIds }
        Log.i(TAG, "Padding with ${moreNonDue.size} non-due $gameName cards from any group")
        selected.addAll(moreNonDue)

        // Repeat cards to fill session if still not enough unique cards
        val cards = selected.take(sessionSize).map { ops.toDomainCard(it) }
        val filled = repeatToFill(cards, sessionSize)
        return filled.shuffled().also { logGenericSelection(it, gameName) }
    }

    /**
     * Select cards when no cards are due. Prefers a single group.
     */
    private suspend fun <C : GameCard> selectFromAllUnlockedGeneric(
        ops: GameCardOperations<C>,
        sessionSize: Int,
        gameName: String
    ): List<C> {
        val allCards = ops.getAllUnlockedWithFsrs()
        if (allCards.isEmpty()) {
            Log.w(TAG, "No unlocked $gameName cards available")
            return emptyList()
        }

        // Group by grouping key and pick the largest group
        val grouped = allCards.groupBy { ops.getGroupingKey(it) }
        val (bestGroup, bestCards) = grouped.maxByOrNull { (_, cards) ->
            cards.size * 1_000_000_000L - cards.minOf { it.dueDate }
        }!!

        Log.i(TAG, "No due $gameName cards - selected group: $bestGroup with ${bestCards.size} cards")

        val selected = mutableListOf<CardWithFsrsData>()
        selected.addAll(bestCards.sortedBy { it.dueDate }.take(sessionSize))

        if (selected.size < sessionSize) {
            val selectedIds = selected.map { it.id }.toSet()
            val otherCards = allCards.filter { it.id !in selectedIds }.sortedBy { it.dueDate }
            selected.addAll(otherCards.take(sessionSize - selected.size))
        }

        // Repeat cards to fill session if still not enough unique cards
        val cards = selected.take(sessionSize).map { ops.toDomainCard(it) }
        val filled = repeatToFill(cards, sessionSize)
        return filled.shuffled().also { logGenericSelection(it, gameName) }
    }

    /**
     * Log the final selection for debugging.
     */
    private fun <C : GameCard> logGenericSelection(cards: List<C>, gameName: String) {
        Log.i(TAG, "Final $gameName selection of ${cards.size} cards:")
        cards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.displayName}")
        }
    }

    // ========== Chord Type Game (Game 1) ==========

    /**
     * Ensure chord type cards are properly initialized.
     * Cards are pre-created by the database migration (v7).
     * This method is kept for safety in case FSRS state is missing.
     */
    suspend fun initializeStartingDeck() {
        val cardCount = cardDao.count()
        Log.i(TAG, "Chord type cards in database: $cardCount")

        // Cards should be pre-created by migration. Just verify FSRS state exists.
        if (cardCount > 0) {
            val now = System.currentTimeMillis()
            val cards = cardDao.getAllCardsOrdered()
            var createdCount = 0
            for (card in cards) {
                val fsrsState = fsrsStateDao.getByCardId(card.id)
                if (fsrsState == null) {
                    Log.w(TAG, "Creating missing FSRS state for ${card.id}")
                    fsrsStateDao.insert(FsrsStateEntity(
                        cardId = card.id,
                        gameType = GameType.CHORD_TYPE.name,
                        dueDate = now
                    ))
                    createdCount++
                }
            }
            if (createdCount > 0) {
                Log.i(TAG, "Created $createdCount missing FSRS states for chord type cards")
            }
        }
    }

    // ========== Card Unlock Management ==========

    /**
     * Set the unlock status for a chord type card.
     * FSRS state is preserved when locking/unlocking.
     * @see setCardUnlocked(GameType, String, Boolean) for generic version
     */
    suspend fun setCardUnlocked(cardId: String, unlocked: Boolean) =
        setCardUnlocked(GameType.CHORD_TYPE, cardId, unlocked)

    /**
     * Get all chord type cards for the unlock management screen.
     * Includes both locked and unlocked cards with their FSRS state.
     */
    fun getAllCardsForUnlockScreen(): Flow<List<CardWithFsrs>> {
        return cardDao.getAllCardsWithFsrsOrderedFlow()
    }

    /**
     * Get all chord type cards for the unlock management screen (suspend version).
     */
    suspend fun getAllCardsForUnlockScreenSuspend(): List<CardWithFsrs> {
        return cardDao.getAllCardsWithFsrsOrdered()
    }

    /**
     * Get the number of unlocked chord type cards.
     * @see getUnlockedCount(GameType) for generic version
     */
    suspend fun getUnlockedCount(): Int = getUnlockedCount(GameType.CHORD_TYPE)

    /**
     * Observe the number of unlocked chord type cards.
     * @see getUnlockedCountFlow(GameType) for generic version
     */
    fun getUnlockedCountFlow(): Flow<Int> = getUnlockedCountFlow(GameType.CHORD_TYPE)

    /**
     * Select cards for a review session.
     * Uses the generic selection algorithm with chord type card operations.
     */
    suspend fun selectCardsForReview(): List<Card> {
        return selectCardsGeneric(chordTypeOps, "chord type")
    }

    /**
     * Start a new review session and return its database ID.
     */
    suspend fun startSession(gameType: GameType = GameType.CHORD_TYPE): Long {
        val session = ReviewSessionEntity(
            startedAt = System.currentTimeMillis(),
            gameType = gameType.name,
            octave = 0,  // Mixed cards - not meaningful
            playbackMode = "MIXED"
        )
        val sessionId = reviewSessionDao.insert(session)
        Log.i(TAG, "Started ${gameType.name} session id=$sessionId")
        return sessionId
    }

    /**
     * Record a single trial and immediately update FSRS state.
     * Called after each answer in a session.
     *
     * @param answeredChordType The chord type the user selected (for tracking wrong answers)
     * @return the new due date for the card
     */
    suspend fun recordTrialAndUpdateFsrs(
        sessionId: Long,
        card: Card,
        wasCorrect: Boolean,
        answeredChordType: ChordType
    ): Long {
        val timestamp = System.currentTimeMillis()
        val cardId = card.toCardId()

        Log.i(TAG, "Recording trial for $cardId: ${if (wasCorrect) "CORRECT" else "WRONG (answered ${answeredChordType.name})"}")

        // 1. Insert trial record (only store answeredChordType for wrong answers)
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = cardId,
            timestamp = timestamp,
            wasCorrect = wasCorrect,
            gameType = GameType.CHORD_TYPE.name,
            answeredChordType = if (wasCorrect) null else answeredChordType.name
        ))

        // 2. Get FSRS state
        val fsrsState = fsrsStateDao.getByCardId(cardId)
        if (fsrsState == null) {
            Log.e(TAG, "FSRS state not found: $cardId")
            return timestamp
        }

        // 3. Calculate rating: correct=Good, wrong=Again
        val rating = if (wasCorrect) Rating.Good else Rating.Again

        // 4. Apply FSRS update immediately
        return applyFsrsUpdate(fsrsState, rating)
    }

    /**
     * Apply FSRS update for a card with the given rating.
     * @return the new due date
     */
    private suspend fun applyFsrsUpdate(fsrsState: FsrsStateEntity, rating: Rating): Long {
        val now = System.currentTimeMillis()
        Log.i(TAG, "Updating FSRS state for ${fsrsState.cardId} with rating ${rating.name}")

        // Calculate actual elapsed days since last review (for retrievability calculation)
        // FSRS needs actual elapsed time, not the scheduled interval
        val elapsedDays = if (fsrsState.lastReview != null) {
            val elapsedMs = now - fsrsState.lastReview
            (elapsedMs / (24.0 * 60 * 60 * 1000)).coerceAtLeast(0.0).toInt()
        } else {
            // First review after Added phase - use scheduled interval as fallback
            fsrsState.interval
        }
        Log.d(TAG, "  Actual elapsed days: $elapsedDays (scheduled was ${fsrsState.interval})")

        // Convert to FlashCard for FSRS calculation
        val flashCard = FlashCard(
            id = 0,  // Not used
            stability = fsrsState.stability,
            difficulty = fsrsState.difficulty,
            interval = elapsedDays,  // Use actual elapsed time, not scheduled interval
            dueDate = LocalDateTime.now(),
            reviewCount = fsrsState.reviewCount,
            lastReview = LocalDateTime.now(),
            phase = fsrsState.phase
        )

        // Calculate new FSRS state using current target retention setting
        val fsrs = getFsrs()
        val grades = fsrs.calculate(flashCard)
        val chosenGrade = grades.first { it.choice == rating }

        val newDueDate = now + chosenGrade.durationMillis

        // Determine new phase
        val newPhase = when {
            rating == Rating.Again -> CardPhase.ReLearning.value
            fsrsState.phase == CardPhase.Added.value -> CardPhase.Review.value
            else -> CardPhase.Review.value
        }

        // Determine lapses
        val newLapses = if (rating == Rating.Again) fsrsState.lapses + 1 else fsrsState.lapses

        // For Added phase, FSRS returns 0.0 for stability/difficulty (uses fixed intervals).
        // Keep the card's existing values to avoid NaN crashes on subsequent reviews.
        val newStability = if (fsrsState.phase == CardPhase.Added.value || chosenGrade.stability == 0.0) {
            fsrsState.stability
        } else {
            chosenGrade.stability
        }
        val newDifficulty = if (fsrsState.phase == CardPhase.Added.value || chosenGrade.difficulty == 0.0) {
            fsrsState.difficulty
        } else {
            chosenGrade.difficulty
        }

        Log.i(TAG, "  FSRS update: stability=$newStability, difficulty=$newDifficulty")
        Log.i(TAG, "  interval=${chosenGrade.interval}d, dueIn=${chosenGrade.durationMillis / 1000 / 60}min")

        fsrsStateDao.updateFsrsState(
            cardId = fsrsState.cardId,
            stability = newStability,
            difficulty = newDifficulty,
            interval = chosenGrade.interval,
            dueDate = newDueDate,
            reviewCount = fsrsState.reviewCount + 1,
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
     * Get count of due cards for chord type game.
     * @see getDueCount(GameType) for generic version
     */
    suspend fun getDueCount(): Int = getDueCount(GameType.CHORD_TYPE)

    /**
     * Observe due count for chord type game.
     * @see getDueCountFlow(GameType) for generic version
     */
    fun getDueCountFlow(): Flow<Int> = getDueCountFlow(GameType.CHORD_TYPE)

    /**
     * Get all cards with FSRS flow for UI.
     */
    fun getAllCardsWithFsrsFlow(): Flow<List<CardWithFsrs>> {
        return cardDao.getAllUnlockedWithFsrsFlow()
    }

    /**
     * Get session overview for history screen.
     */
    fun getSessionOverviews(): Flow<List<SessionOverview>> {
        return historyDao.getSessionOverviews()
    }

    /**
     * Get session overview for a specific game type.
     */
    fun getSessionOverviewsByGameType(gameType: GameType): Flow<List<SessionOverview>> {
        return historyDao.getSessionOverviewsByGameType(gameType.name)
    }

    /**
     * Get card statistics for history screen.
     */
    fun getCardStats(): Flow<List<CardStatsView>> {
        return historyDao.getCardStats()
    }

    /**
     * Get card statistics for a specific game type.
     */
    fun getCardStatsByGameType(gameType: GameType): Flow<List<CardStatsView>> {
        return historyDao.getCardStatsByGameType(gameType.name)
    }

    /**
     * Get all sessions for history screen.
     */
    fun getAllSessions(): Flow<List<ReviewSessionEntity>> {
        return historyDao.getAllSessions()
    }

    /**
     * Get trials for a specific session.
     */
    suspend fun getTrialsForSession(sessionId: Long): List<TrialEntity> {
        return trialDao.getTrialsForSession(sessionId)
    }

    /**
     * Get confusion data for chord type game, optionally filtered by octave.
     */
    suspend fun getChordTypeConfusionData(octave: Int? = null): List<ConfusionEntry> {
        return historyDao.getChordTypeConfusionData(octave)
    }

    /**
     * Get confusion data for function game, filtered by key quality.
     */
    suspend fun getFunctionConfusionData(keyQuality: String): List<ConfusionEntry> {
        return historyDao.getFunctionConfusionData(keyQuality)
    }

    // ========== Chord Function Game (Game 2) ==========

    /**
     * Ensure function cards are properly initialized.
     * Cards are pre-created by the database migration (v7).
     * This method is kept for safety in case FSRS state is missing.
     */
    suspend fun initializeFunctionStartingDeck() {
        val cardCount = functionCardDao.count()
        Log.i(TAG, "Function cards in database: $cardCount")

        // Cards should be pre-created by migration. Just verify FSRS state exists.
        if (cardCount > 0) {
            val now = System.currentTimeMillis()
            val cards = functionCardDao.getAllCardsOrdered()
            var createdCount = 0
            for (card in cards) {
                val fsrsState = fsrsStateDao.getByCardId(card.id)
                if (fsrsState == null) {
                    Log.w(TAG, "Creating missing FSRS state for ${card.id}")
                    fsrsStateDao.insert(FsrsStateEntity(
                        cardId = card.id,
                        gameType = GameType.CHORD_FUNCTION.name,
                        dueDate = now
                    ))
                    createdCount++
                }
            }
            if (createdCount > 0) {
                Log.i(TAG, "Created $createdCount missing FSRS states for function cards")
            }
        }
    }

    // ========== Function Card Unlock Management ==========

    /**
     * Set the unlock status for a function card.
     * FSRS state is preserved when locking/unlocking.
     * @see setCardUnlocked(GameType, String, Boolean) for generic version
     */
    suspend fun setFunctionCardUnlocked(cardId: String, unlocked: Boolean) =
        setCardUnlocked(GameType.CHORD_FUNCTION, cardId, unlocked)

    /**
     * Get all function cards for the unlock management screen.
     * Includes both locked and unlocked cards with their FSRS state.
     */
    fun getAllFunctionCardsForUnlockScreen(): Flow<List<FunctionCardWithFsrs>> {
        return functionCardDao.getAllCardsWithFsrsOrderedFlow()
    }

    /**
     * Get all function cards for the unlock management screen (suspend version).
     */
    suspend fun getAllFunctionCardsForUnlockScreenSuspend(): List<FunctionCardWithFsrs> {
        return functionCardDao.getAllCardsWithFsrsOrdered()
    }

    /**
     * Get the number of unlocked function cards.
     * @see getUnlockedCount(GameType) for generic version
     */
    suspend fun getFunctionUnlockedCount(): Int = getUnlockedCount(GameType.CHORD_FUNCTION)

    /**
     * Observe the number of unlocked function cards.
     * @see getUnlockedCountFlow(GameType) for generic version
     */
    fun getFunctionUnlockedCountFlow(): Flow<Int> = getUnlockedCountFlow(GameType.CHORD_FUNCTION)

    /**
     * Select function cards for a review session.
     * Uses the generic selection algorithm with function card operations.
     */
    suspend fun selectFunctionCardsForReview(): List<FunctionCard> {
        return selectCardsGeneric(functionOps, "function")
    }

    /**
     * Record a function game trial and update FSRS state.
     *
     * @param answeredFunction The function the user selected
     * @return the new due date for the card
     */
    suspend fun recordFunctionTrialAndUpdateFsrs(
        sessionId: Long,
        card: FunctionCard,
        wasCorrect: Boolean,
        answeredFunction: ChordFunction
    ): Long {
        val timestamp = System.currentTimeMillis()
        val cardId = card.id

        Log.i(TAG, "Recording function trial for $cardId: ${if (wasCorrect) "CORRECT" else "WRONG (answered ${answeredFunction.displayName})"}")

        // 1. Insert trial record
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = cardId,
            timestamp = timestamp,
            wasCorrect = wasCorrect,
            gameType = GameType.CHORD_FUNCTION.name,
            answeredFunction = if (wasCorrect) null else answeredFunction.name
        ))

        // 2. Get FSRS state
        val fsrsState = fsrsStateDao.getByCardId(cardId)
        if (fsrsState == null) {
            Log.e(TAG, "FSRS state not found: $cardId")
            return timestamp
        }

        // 3. Calculate rating: correct=Good, wrong=Again
        val rating = if (wasCorrect) Rating.Good else Rating.Again

        // 4. Apply FSRS update
        return applyFsrsUpdate(fsrsState, rating)
    }

    /**
     * Get count of due function cards.
     * @see getDueCount(GameType) for generic version
     */
    suspend fun getFunctionDueCount(): Int = getDueCount(GameType.CHORD_FUNCTION)

    /**
     * Observe due count for function game.
     * @see getDueCountFlow(GameType) for generic version
     */
    fun getFunctionDueCountFlow(): Flow<Int> = getDueCountFlow(GameType.CHORD_FUNCTION)

    /**
     * Get all function cards with FSRS flow for UI.
     */
    fun getAllFunctionCardsWithFsrsFlow(): Flow<List<FunctionCardWithFsrs>> {
        return functionCardDao.getAllUnlockedWithFsrsFlow()
    }

    // ========== Chord Progression Game (Game 3) ==========

    /**
     * Ensure progression cards are properly initialized.
     * Cards are pre-created by the database migration (v8).
     * This method is kept for safety in case FSRS state is missing.
     */
    suspend fun initializeProgressionStartingDeck() {
        val cardCount = progressionCardDao.count()
        Log.i(TAG, "Progression cards in database: $cardCount")

        // Cards should be pre-created by migration. Just verify FSRS state exists.
        if (cardCount > 0) {
            val now = System.currentTimeMillis()
            val cards = progressionCardDao.getAllCardsOrdered()
            var createdCount = 0
            for (card in cards) {
                val fsrsState = fsrsStateDao.getByCardId(card.id)
                if (fsrsState == null) {
                    Log.w(TAG, "Creating missing FSRS state for ${card.id}")
                    fsrsStateDao.insert(FsrsStateEntity(
                        cardId = card.id,
                        gameType = GameType.CHORD_PROGRESSION.name,
                        dueDate = now
                    ))
                    createdCount++
                }
            }
            if (createdCount > 0) {
                Log.i(TAG, "Created $createdCount missing FSRS states for progression cards")
            }
        }
    }

    // ========== Progression Card Unlock Management ==========

    /**
     * Set the unlock status for a progression card.
     * FSRS state is preserved when locking/unlocking.
     * @see setCardUnlocked(GameType, String, Boolean) for generic version
     */
    suspend fun setProgressionCardUnlocked(cardId: String, unlocked: Boolean) =
        setCardUnlocked(GameType.CHORD_PROGRESSION, cardId, unlocked)

    /**
     * Get all progression cards for the unlock management screen.
     * Includes both locked and unlocked cards with their FSRS state.
     */
    fun getAllProgressionCardsForUnlockScreen(): Flow<List<ProgressionCardWithFsrs>> {
        return progressionCardDao.getAllCardsWithFsrsOrderedFlow()
    }

    /**
     * Get all progression cards for the unlock management screen (suspend version).
     */
    suspend fun getAllProgressionCardsForUnlockScreenSuspend(): List<ProgressionCardWithFsrs> {
        return progressionCardDao.getAllCardsWithFsrsOrdered()
    }

    /**
     * Get the number of unlocked progression cards.
     * @see getUnlockedCount(GameType) for generic version
     */
    suspend fun getProgressionUnlockedCount(): Int = getUnlockedCount(GameType.CHORD_PROGRESSION)

    /**
     * Observe the number of unlocked progression cards.
     * @see getUnlockedCountFlow(GameType) for generic version
     */
    fun getProgressionUnlockedCountFlow(): Flow<Int> = getUnlockedCountFlow(GameType.CHORD_PROGRESSION)

    /**
     * Select progression cards for a review session.
     * Uses the generic selection algorithm with progression card operations.
     */
    suspend fun selectProgressionCardsForReview(): List<ProgressionCard> {
        return selectCardsGeneric(progressionOps, "progression")
    }

    /**
     * Record a progression game trial and update FSRS state.
     *
     * @param answeredProgression The progression the user selected
     * @return the new due date for the card
     */
    suspend fun recordProgressionTrialAndUpdateFsrs(
        sessionId: Long,
        card: ProgressionCard,
        wasCorrect: Boolean,
        answeredProgression: ProgressionType
    ): Long {
        val timestamp = System.currentTimeMillis()
        val cardId = card.id

        Log.i(TAG, "Recording progression trial for $cardId: ${if (wasCorrect) "CORRECT" else "WRONG (answered ${answeredProgression.displayName})"}")

        // 1. Insert trial record
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = cardId,
            timestamp = timestamp,
            wasCorrect = wasCorrect,
            gameType = GameType.CHORD_PROGRESSION.name,
            answeredProgression = if (wasCorrect) null else answeredProgression.name
        ))

        // 2. Get FSRS state
        val fsrsState = fsrsStateDao.getByCardId(cardId)
        if (fsrsState == null) {
            Log.e(TAG, "FSRS state not found: $cardId")
            return timestamp
        }

        // 3. Calculate rating: correct=Good, wrong=Again
        val rating = if (wasCorrect) Rating.Good else Rating.Again

        // 4. Apply FSRS update
        return applyFsrsUpdate(fsrsState, rating)
    }

    /**
     * Get count of due progression cards.
     * @see getDueCount(GameType) for generic version
     */
    suspend fun getProgressionDueCount(): Int = getDueCount(GameType.CHORD_PROGRESSION)

    /**
     * Observe due count for progression game.
     * @see getDueCountFlow(GameType) for generic version
     */
    fun getProgressionDueCountFlow(): Flow<Int> = getDueCountFlow(GameType.CHORD_PROGRESSION)

    /**
     * Get all progression cards with FSRS flow for UI.
     */
    fun getAllProgressionCardsWithFsrsFlow(): Flow<List<ProgressionCardWithFsrs>> {
        return progressionCardDao.getAllUnlockedWithFsrsFlow()
    }

    /**
     * Reset FSRS state for a card to initial values.
     * Review history is preserved.
     */
    suspend fun resetFsrsState(cardId: String, gameType: GameType) {
        val now = System.currentTimeMillis()
        fsrsStateDao.updateFsrsState(
            cardId = cardId,
            stability = 2.5,
            difficulty = 2.5,
            interval = 0,
            dueDate = now,
            reviewCount = 0,
            lastReview = null,
            phase = 0,
            lapses = 0
        )
        Log.i(TAG, "Reset FSRS state for card $cardId (gameType=$gameType)")
    }

    /**
     * Get per-session accuracy data for a specific card (for graphing).
     */
    suspend fun getCardSessionAccuracy(cardId: String): List<CardSessionAccuracy> {
        return trialDao.getCardSessionAccuracy(cardId)
    }

    /**
     * Get card by ID with FSRS state.
     */
    suspend fun getCardWithFsrs(cardId: String): CardWithFsrs? {
        return cardDao.getByIdWithFsrs(cardId)
    }

    /**
     * Get lifetime stats for a specific card.
     */
    suspend fun getCardLifetimeStats(cardId: String): Pair<Int, Int> {
        val total = trialDao.countTrialsForCard(cardId)
        val correct = trialDao.countCorrectTrialsForCard(cardId)
        return Pair(total, correct)
    }

    /**
     * Get last session stats for a specific card.
     */
    suspend fun getCardLastSessionStats(cardId: String): CardSessionAccuracy? {
        return trialDao.getCardSessionAccuracy(cardId).lastOrNull()
    }

    /**
     * Get per-card stats for a specific session (for results screen breakdown).
     * Reusable by ResultsScreen and History->Sessions tab.
     */
    suspend fun getSessionCardStats(sessionId: Long): List<SessionCardStats> {
        return trialDao.getSessionCardStats(sessionId)
    }
}

/**
 * Convert CardWithFsrs to domain Card.
 */
private fun CardWithFsrs.toCard(): Card {
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
 * Convert FunctionCardWithFsrs to domain FunctionCard.
 */
private fun FunctionCardWithFsrs.toFunctionCard(): FunctionCard {
    return FunctionCard(
        function = ChordFunction.valueOf(function),
        keyQuality = KeyQuality.valueOf(keyQuality),
        octave = octave,
        playbackMode = PlaybackMode.valueOf(playbackMode)
    )
}

/**
 * Convert ProgressionCardWithFsrs to domain ProgressionCard.
 */
private fun ProgressionCardWithFsrs.toProgressionCard(): ProgressionCard {
    return ProgressionCard(
        progression = ProgressionType.valueOf(progression),
        octave = octave,
        playbackMode = PlaybackMode.valueOf(playbackMode)
    )
}
