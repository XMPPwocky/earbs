package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity

/**
 * View data class for confusion matrix entries.
 * Each entry represents how many times 'actual' was answered as 'answered'.
 */
data class ConfusionEntry(
    val actual: String,
    val answered: String,
    val count: Int
)

/**
 * View data class for card statistics aggregated from trials.
 */
data class CardStatsView(
    val cardId: String,
    val gameType: String,
    val totalTrials: Int,
    val correctTrials: Int
) {
    val accuracy: Float get() = if (totalTrials > 0) correctTrials.toFloat() / totalTrials else 0f
}

/**
 * View data class for session overview with computed stats.
 */
data class SessionOverview(
    val id: Long,
    val startedAt: Long,
    val completedAt: Long?,
    val gameType: String,
    val octave: Int,
    val totalTrials: Int,
    val correctTrials: Int
) {
    val accuracy: Float get() = if (totalTrials > 0) correctTrials.toFloat() / totalTrials else 0f
}

@Dao
interface HistoryDao {
    /**
     * Get all sessions ordered by most recent first.
     */
    @Query("SELECT * FROM review_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<ReviewSessionEntity>>

    /**
     * Get all sessions for a specific game type ordered by most recent first.
     */
    @Query("SELECT * FROM review_sessions WHERE gameType = :gameType ORDER BY startedAt DESC")
    fun getSessionsByGameType(gameType: String): Flow<List<ReviewSessionEntity>>

    /**
     * Get per-card lifetime statistics.
     */
    @Query("""
        SELECT cardId,
               gameType,
               COUNT(*) as totalTrials,
               SUM(CASE WHEN wasCorrect THEN 1 ELSE 0 END) as correctTrials
        FROM trials
        GROUP BY cardId, gameType
    """)
    fun getCardStats(): Flow<List<CardStatsView>>

    /**
     * Get per-card lifetime statistics for a specific game type.
     */
    @Query("""
        SELECT cardId,
               gameType,
               COUNT(*) as totalTrials,
               SUM(CASE WHEN wasCorrect THEN 1 ELSE 0 END) as correctTrials
        FROM trials
        WHERE gameType = :gameType
        GROUP BY cardId
    """)
    fun getCardStatsByGameType(gameType: String): Flow<List<CardStatsView>>

    /**
     * Get session overview with trial counts.
     */
    @Query("""
        SELECT
            s.id,
            s.startedAt,
            s.completedAt,
            s.gameType,
            s.octave,
            COUNT(t.id) as totalTrials,
            SUM(CASE WHEN t.wasCorrect THEN 1 ELSE 0 END) as correctTrials
        FROM review_sessions s
        LEFT JOIN trials t ON s.id = t.sessionId
        GROUP BY s.id
        ORDER BY s.startedAt DESC
    """)
    fun getSessionOverviews(): Flow<List<SessionOverview>>

    /**
     * Get session overview for a specific game type.
     */
    @Query("""
        SELECT
            s.id,
            s.startedAt,
            s.completedAt,
            s.gameType,
            s.octave,
            COUNT(t.id) as totalTrials,
            SUM(CASE WHEN t.wasCorrect THEN 1 ELSE 0 END) as correctTrials
        FROM review_sessions s
        LEFT JOIN trials t ON s.id = t.sessionId
        WHERE s.gameType = :gameType
        GROUP BY s.id
        ORDER BY s.startedAt DESC
    """)
    fun getSessionOverviewsByGameType(gameType: String): Flow<List<SessionOverview>>

    /**
     * Get confusion data for chord type game, optionally filtered by octave.
     * cardId format: "MAJOR_4_ARPEGGIATED" -> chord type is first segment
     *
     * For correct answers, actual == answered (the chord type).
     * For wrong answers, actual is parsed from cardId, answered is from answeredChordType.
     *
     * Note: Excludes wrong answers with null answeredChordType (legacy data from before migration 4->5).
     */
    @Query("""
        SELECT
            SUBSTR(cardId, 1, INSTR(cardId, '_') - 1) as actual,
            CASE
                WHEN wasCorrect THEN SUBSTR(cardId, 1, INSTR(cardId, '_') - 1)
                ELSE answeredChordType
            END as answered,
            COUNT(*) as count
        FROM trials
        WHERE gameType = 'CHORD_TYPE'
          AND (:octave IS NULL OR SUBSTR(cardId, INSTR(cardId, '_') + 1, 1) = CAST(:octave AS TEXT))
          AND (wasCorrect = 1 OR answeredChordType IS NOT NULL)
        GROUP BY actual, answered
    """)
    suspend fun getChordTypeConfusionData(octave: Int?): List<ConfusionEntry>

    /**
     * Get confusion data for function game, filtered by key quality.
     * cardId format: "IV_MAJOR_4_ARPEGGIATED" -> function is first segment, key quality is second
     *
     * For correct answers, actual == answered (the function).
     * For wrong answers, actual is parsed from cardId, answered is from answeredFunction.
     *
     * Note: Excludes wrong answers with null answeredFunction (legacy data from before migration 4->5).
     */
    @Query("""
        SELECT
            SUBSTR(cardId, 1, INSTR(cardId, '_') - 1) as actual,
            CASE
                WHEN wasCorrect THEN SUBSTR(cardId, 1, INSTR(cardId, '_') - 1)
                ELSE answeredFunction
            END as answered,
            COUNT(*) as count
        FROM trials
        WHERE gameType = 'CHORD_FUNCTION'
          AND SUBSTR(cardId, INSTR(cardId, '_') + 1,
              INSTR(SUBSTR(cardId, INSTR(cardId, '_') + 1), '_') - 1) = :keyQuality
          AND (wasCorrect = 1 OR answeredFunction IS NOT NULL)
        GROUP BY actual, answered
    """)
    suspend fun getFunctionConfusionData(keyQuality: String): List<ConfusionEntry>
}
