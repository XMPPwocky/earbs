package net.xmppwocky.earbs.data.repository

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.data.db.CardDao
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
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.Deck
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.FunctionDeck
import net.xmppwocky.earbs.model.KeyQuality
import java.time.LocalDateTime

private const val TAG = "EarbsRepository"
private const val PREF_KEY_UNLOCK_LEVEL = "unlock_level"
private const val PREF_KEY_FUNCTION_UNLOCK_LEVEL = "function_unlock_level"
private const val PREF_KEY_SESSION_SIZE = "session_size"
private const val PREF_KEY_TARGET_RETENTION = "target_retention"
private const val DEFAULT_SESSION_SIZE = 20
private const val DEFAULT_TARGET_RETENTION = 0.9f

class EarbsRepository(
    private val cardDao: CardDao,
    private val functionCardDao: FunctionCardDao,
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

    // ========== Chord Type Game (Game 1) ==========

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
            val cardId = "${chordType.name}_${startingGroup.octave}_${startingGroup.playbackMode.name}"
            CardEntity(
                id = cardId,
                chordType = chordType.name,
                octave = startingGroup.octave,
                playbackMode = startingGroup.playbackMode.name
            )
        }

        // Create FSRS state for each card
        val fsrsStates = startingCards.map { card ->
            FsrsStateEntity(
                cardId = card.id,
                gameType = GameType.CHORD_TYPE.name,
                dueDate = now  // Immediately due
            )
        }

        cardDao.insertAll(startingCards)
        fsrsStateDao.insertAll(fsrsStates)
        prefs.edit().putInt(PREF_KEY_UNLOCK_LEVEL, 0).apply()
        Log.i(TAG, "Inserted ${startingCards.size} starting cards with FSRS state, unlock level set to 0")
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
            val cardId = "${chordType.name}_${nextGroup.octave}_${nextGroup.playbackMode.name}"
            CardEntity(
                id = cardId,
                chordType = chordType.name,
                octave = nextGroup.octave,
                playbackMode = nextGroup.playbackMode.name
            )
        }

        val fsrsStates = newCards.map { card ->
            FsrsStateEntity(
                cardId = card.id,
                gameType = GameType.CHORD_TYPE.name,
                dueDate = now  // Due immediately
            )
        }

        cardDao.insertAll(newCards)
        fsrsStateDao.insertAll(fsrsStates)
        prefs.edit().putInt(PREF_KEY_UNLOCK_LEVEL, nextLevel).apply()
        Log.i(TAG, "Unlocked ${newCards.size} new cards with FSRS state, unlock level now $nextLevel")
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
     * Select cards for a review session, preferring same-group cards:
     * 1. Get all due cards
     * 2. Group by (octave, playbackMode)
     * 3. Pick the group with most due cards (tie-break by most overdue)
     * 4. If group has >= sessionSize due, take sessionSize from that group
     * 5. If group has < sessionSize due:
     *    a. Take all due from that group
     *    b. Pad with non-due from SAME group first
     *    c. If still < sessionSize, pad with cards from OTHER groups
     * 6. Shuffle and return
     */
    suspend fun selectCardsForReview(): List<Card> {
        val sessionSize = getSessionSize()
        val now = System.currentTimeMillis()
        Log.i(TAG, "Selecting $sessionSize cards for review (now=$now)")

        val allDue = cardDao.getDueCards(now)
        Log.i(TAG, "Found ${allDue.size} total due cards")

        if (allDue.isEmpty()) {
            // No due cards - pick from any unlocked, prefer a single group
            Log.i(TAG, "No due cards, selecting from all unlocked cards")
            return selectFromAllUnlocked(sessionSize)
        }

        // Group due cards by (octave, playbackMode)
        val grouped = allDue.groupBy { it.octave to it.playbackMode }
        Log.i(TAG, "Due cards grouped into ${grouped.size} groups: ${grouped.map { (k, v) -> "${k.first}/${k.second}=${v.size}" }}")

        // Pick group with most due cards (tie-break by most overdue = lowest dueDate)
        val (bestGroup, bestDue) = grouped.maxByOrNull { (_, cards) ->
            // Primary: card count (multiplied to dominate)
            // Secondary: negative of min dueDate (lower = more overdue = higher priority)
            cards.size * 1_000_000_000L - cards.minOf { it.dueDate }
        }!!

        val (octave, mode) = bestGroup
        Log.i(TAG, "Selected best group: octave=$octave, mode=$mode with ${bestDue.size} due cards")

        val selected = mutableListOf<CardWithFsrs>()
        selected.addAll(bestDue.sortedBy { it.dueDate }.take(sessionSize))
        Log.i(TAG, "Added ${selected.size} due cards from best group")

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { it.toCard() }.also { logSelection(it) }
        }

        // Pad with non-due from same group
        val nonDueSameGroup = cardDao.getNonDueCardsByGroup(now, octave, mode, sessionSize - selected.size)
        Log.i(TAG, "Padding with ${nonDueSameGroup.size} non-due cards from same group")
        selected.addAll(nonDueSameGroup)

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { it.toCard() }.also { logSelection(it) }
        }

        // Still not enough - pad with due cards from other groups
        val selectedIds = selected.map { it.id }.toSet()
        val otherDueCards = allDue.filter { it.id !in selectedIds }.sortedBy { it.dueDate }
        val toAddFromOther = otherDueCards.take(sessionSize - selected.size)
        Log.i(TAG, "Padding with ${toAddFromOther.size} due cards from other groups")
        selected.addAll(toAddFromOther)

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { it.toCard() }.also { logSelection(it) }
        }

        // If STILL not enough, get non-due from any group
        val stillSelectedIds = selected.map { it.id }.toSet()
        val moreNonDue = cardDao.getNonDueCards(now, sessionSize - selected.size)
            .filter { it.id !in stillSelectedIds }
        Log.i(TAG, "Padding with ${moreNonDue.size} non-due cards from any group")
        selected.addAll(moreNonDue)

        // Repeat cards to fill session if still not enough unique cards
        val cards = selected.take(sessionSize).map { it.toCard() }
        val filled = repeatToFill(cards, sessionSize)
        return filled.shuffled().also { logSelection(it) }
    }

    /**
     * Select cards when no cards are due. Prefers a single group.
     */
    private suspend fun selectFromAllUnlocked(sessionSize: Int): List<Card> {
        val allCards = cardDao.getAllUnlockedWithFsrs()
        if (allCards.isEmpty()) {
            Log.w(TAG, "No unlocked cards available")
            return emptyList()
        }

        // Group by (octave, playbackMode) and pick the largest group
        val grouped = allCards.groupBy { it.octave to it.playbackMode }
        val (bestGroup, bestCards) = grouped.maxByOrNull { (_, cards) ->
            // Prefer groups with more cards, tie-break by earliest due date
            cards.size * 1_000_000_000L - cards.minOf { it.dueDate }
        }!!

        Log.i(TAG, "No due cards - selected group: octave=${bestGroup.first}, mode=${bestGroup.second} with ${bestCards.size} cards")

        val selected = mutableListOf<CardWithFsrs>()
        selected.addAll(bestCards.sortedBy { it.dueDate }.take(sessionSize))

        if (selected.size < sessionSize) {
            // Pad from other groups
            val selectedIds = selected.map { it.id }.toSet()
            val otherCards = allCards.filter { it.id !in selectedIds }.sortedBy { it.dueDate }
            selected.addAll(otherCards.take(sessionSize - selected.size))
        }

        // Repeat cards to fill session if still not enough unique cards
        val cards = selected.take(sessionSize).map { it.toCard() }
        val filled = repeatToFill(cards, sessionSize)
        return filled.shuffled().also { logSelection(it) }
    }

    /**
     * Log the final selection for debugging.
     */
    private fun logSelection(cards: List<Card>) {
        Log.i(TAG, "Final selection of ${cards.size} cards:")
        cards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.chordType.name}@${card.octave}/${card.playbackMode.name}")
        }
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
     */
    suspend fun getDueCount(): Int {
        val count = fsrsStateDao.countDueByGameType(GameType.CHORD_TYPE.name, System.currentTimeMillis())
        Log.d(TAG, "Chord type due count: $count")
        return count
    }

    fun getDueCountFlow(): Flow<Int> {
        return fsrsStateDao.countDueByGameTypeFlow(GameType.CHORD_TYPE.name, System.currentTimeMillis())
    }

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
     * Initialize the function game starting deck on first launch.
     * Creates 3 cards: IV, V, vi @ major key, octave 4, arpeggiated.
     */
    suspend fun initializeFunctionStartingDeck() {
        val existingCount = functionCardDao.count()
        if (existingCount > 0) {
            Log.i(TAG, "Database already has $existingCount function cards, skipping initialization")
            return
        }

        Log.i(TAG, "Initializing function starting deck with 3 primary major functions @ octave 4")
        val now = System.currentTimeMillis()

        // Starting deck is group 0: IV, V, vi @ major key, octave 4, arpeggiated
        val startingGroup = FunctionDeck.UNLOCK_ORDER[0]
        val startingCards = startingGroup.functions.map { function ->
            val cardId = "${function.name}_${startingGroup.keyQuality.name}_${startingGroup.octave}_${startingGroup.playbackMode.name}"
            FunctionCardEntity(
                id = cardId,
                function = function.name,
                keyQuality = startingGroup.keyQuality.name,
                octave = startingGroup.octave,
                playbackMode = startingGroup.playbackMode.name
            )
        }

        // Create FSRS state for each card
        val fsrsStates = startingCards.map { card ->
            FsrsStateEntity(
                cardId = card.id,
                gameType = GameType.CHORD_FUNCTION.name,
                dueDate = now  // Immediately due
            )
        }

        functionCardDao.insertAll(startingCards)
        fsrsStateDao.insertAll(fsrsStates)
        prefs.edit().putInt(PREF_KEY_FUNCTION_UNLOCK_LEVEL, 0).apply()
        Log.i(TAG, "Inserted ${startingCards.size} function starting cards with FSRS state, unlock level set to 0")
    }

    /**
     * Get the current function unlock level (0-indexed).
     */
    fun getFunctionUnlockLevel(): Int {
        return prefs.getInt(PREF_KEY_FUNCTION_UNLOCK_LEVEL, 0)
    }

    /**
     * Check if there are more function cards to unlock.
     */
    fun canUnlockMoreFunctions(): Boolean {
        return getFunctionUnlockLevel() < FunctionDeck.MAX_UNLOCK_LEVEL
    }

    /**
     * Unlock the next group of 3 function cards.
     * @return true if unlock succeeded, false if already at max level
     */
    suspend fun unlockNextFunctionGroup(): Boolean {
        val currentLevel = getFunctionUnlockLevel()
        if (currentLevel >= FunctionDeck.MAX_UNLOCK_LEVEL) {
            Log.i(TAG, "Already at max function unlock level ($currentLevel), cannot unlock more")
            return false
        }

        val nextLevel = currentLevel + 1
        val nextGroup = FunctionDeck.UNLOCK_ORDER[nextLevel]
        val now = System.currentTimeMillis()

        Log.i(TAG, "Unlocking function group $nextLevel: ${nextGroup.functions.map { it.displayName }} @ ${nextGroup.keyQuality} key, octave ${nextGroup.octave}, ${nextGroup.playbackMode}")

        val newCards = nextGroup.functions.map { function ->
            val cardId = "${function.name}_${nextGroup.keyQuality.name}_${nextGroup.octave}_${nextGroup.playbackMode.name}"
            FunctionCardEntity(
                id = cardId,
                function = function.name,
                keyQuality = nextGroup.keyQuality.name,
                octave = nextGroup.octave,
                playbackMode = nextGroup.playbackMode.name
            )
        }

        val fsrsStates = newCards.map { card ->
            FsrsStateEntity(
                cardId = card.id,
                gameType = GameType.CHORD_FUNCTION.name,
                dueDate = now  // Due immediately
            )
        }

        functionCardDao.insertAll(newCards)
        fsrsStateDao.insertAll(fsrsStates)
        prefs.edit().putInt(PREF_KEY_FUNCTION_UNLOCK_LEVEL, nextLevel).apply()
        Log.i(TAG, "Unlocked ${newCards.size} new function cards with FSRS state, unlock level now $nextLevel")
        return true
    }

    /**
     * Get the number of unlocked function cards.
     */
    suspend fun getFunctionUnlockedCount(): Int {
        return functionCardDao.countUnlocked()
    }

    /**
     * Observe the number of unlocked function cards.
     */
    fun getFunctionUnlockedCountFlow(): Flow<Int> {
        return functionCardDao.countUnlockedFlow()
    }

    /**
     * Select function cards for a review session.
     * Similar logic to chord type game but groups by (keyQuality, octave, playbackMode).
     */
    suspend fun selectFunctionCardsForReview(): List<FunctionCard> {
        val sessionSize = getSessionSize()
        val now = System.currentTimeMillis()
        Log.i(TAG, "Selecting $sessionSize function cards for review (now=$now)")

        val allDue = functionCardDao.getDueCards(now)
        Log.i(TAG, "Found ${allDue.size} total due function cards")

        if (allDue.isEmpty()) {
            Log.i(TAG, "No due function cards, selecting from all unlocked")
            return selectFunctionCardsFromAllUnlocked(sessionSize)
        }

        // Group due cards by (keyQuality, octave, playbackMode)
        val grouped = allDue.groupBy { Triple(it.keyQuality, it.octave, it.playbackMode) }
        Log.i(TAG, "Due function cards grouped into ${grouped.size} groups")

        // Pick group with most due cards
        val (bestGroup, bestDue) = grouped.maxByOrNull { (_, cards) ->
            cards.size * 1_000_000_000L - cards.minOf { it.dueDate }
        }!!

        val (keyQuality, octave, mode) = bestGroup
        Log.i(TAG, "Selected best group: $keyQuality/$octave/$mode with ${bestDue.size} due cards")

        val selected = mutableListOf<FunctionCardWithFsrs>()
        selected.addAll(bestDue.sortedBy { it.dueDate }.take(sessionSize))
        Log.i(TAG, "Added ${selected.size} due function cards from best group")

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { it.toFunctionCard() }.also { logFunctionSelection(it) }
        }

        // Pad with non-due from same group
        val nonDueSameGroup = functionCardDao.getNonDueCardsByGroup(now, keyQuality, octave, mode, sessionSize - selected.size)
        Log.i(TAG, "Padding with ${nonDueSameGroup.size} non-due function cards from same group")
        selected.addAll(nonDueSameGroup)

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { it.toFunctionCard() }.also { logFunctionSelection(it) }
        }

        // Pad with due cards from other groups
        val selectedIds = selected.map { it.id }.toSet()
        val otherDueCards = allDue.filter { it.id !in selectedIds }.sortedBy { it.dueDate }
        val toAddFromOther = otherDueCards.take(sessionSize - selected.size)
        Log.i(TAG, "Padding with ${toAddFromOther.size} due function cards from other groups")
        selected.addAll(toAddFromOther)

        if (selected.size >= sessionSize) {
            return selected.shuffled().map { it.toFunctionCard() }.also { logFunctionSelection(it) }
        }

        // If still not enough, get non-due from any group
        val stillSelectedIds = selected.map { it.id }.toSet()
        val moreNonDue = functionCardDao.getNonDueCards(now, sessionSize - selected.size)
            .filter { it.id !in stillSelectedIds }
        Log.i(TAG, "Padding with ${moreNonDue.size} non-due function cards from any group")
        selected.addAll(moreNonDue)

        // Repeat cards to fill session if still not enough unique cards
        val cards = selected.take(sessionSize).map { it.toFunctionCard() }
        val filled = repeatToFill(cards, sessionSize)
        return filled.shuffled().also { logFunctionSelection(it) }
    }

    /**
     * Select function cards when no cards are due.
     */
    private suspend fun selectFunctionCardsFromAllUnlocked(sessionSize: Int): List<FunctionCard> {
        val allCards = functionCardDao.getAllUnlockedWithFsrs()
        if (allCards.isEmpty()) {
            Log.w(TAG, "No unlocked function cards available")
            return emptyList()
        }

        // Group by (keyQuality, octave, playbackMode) and pick the largest group
        val grouped = allCards.groupBy { Triple(it.keyQuality, it.octave, it.playbackMode) }
        val (bestGroup, bestCards) = grouped.maxByOrNull { (_, cards) ->
            cards.size * 1_000_000_000L - cards.minOf { it.dueDate }
        }!!

        Log.i(TAG, "No due function cards - selected group: ${bestGroup.first}/${bestGroup.second}/${bestGroup.third} with ${bestCards.size} cards")

        val selected = mutableListOf<FunctionCardWithFsrs>()
        selected.addAll(bestCards.sortedBy { it.dueDate }.take(sessionSize))

        if (selected.size < sessionSize) {
            val selectedIds = selected.map { it.id }.toSet()
            val otherCards = allCards.filter { it.id !in selectedIds }.sortedBy { it.dueDate }
            selected.addAll(otherCards.take(sessionSize - selected.size))
        }

        // Repeat cards to fill session if still not enough unique cards
        val cards = selected.take(sessionSize).map { it.toFunctionCard() }
        val filled = repeatToFill(cards, sessionSize)
        return filled.shuffled().also { logFunctionSelection(it) }
    }

    /**
     * Log the final function card selection for debugging.
     */
    private fun logFunctionSelection(cards: List<FunctionCard>) {
        Log.i(TAG, "Final function selection of ${cards.size} cards:")
        cards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.function.displayName} in ${card.keyQuality.name}@${card.octave}/${card.playbackMode.name}")
        }
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
     */
    suspend fun getFunctionDueCount(): Int {
        val count = fsrsStateDao.countDueByGameType(GameType.CHORD_FUNCTION.name, System.currentTimeMillis())
        Log.d(TAG, "Function due count: $count")
        return count
    }

    fun getFunctionDueCountFlow(): Flow<Int> {
        return fsrsStateDao.countDueByGameTypeFlow(GameType.CHORD_FUNCTION.name, System.currentTimeMillis())
    }

    /**
     * Get all function cards with FSRS flow for UI.
     */
    fun getAllFunctionCardsWithFsrsFlow(): Flow<List<FunctionCardWithFsrs>> {
        return functionCardDao.getAllUnlockedWithFsrsFlow()
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
