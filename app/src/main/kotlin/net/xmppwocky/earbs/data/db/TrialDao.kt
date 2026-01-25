package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.TrialEntity

/**
 * Per-session accuracy for a specific card, used for graphing.
 */
data class CardSessionAccuracy(
    val sessionId: Long,
    val sessionDate: Long,
    val trialsInSession: Int,
    val correctInSession: Int
)

/**
 * Per-card stats within a session, used for session results breakdown.
 * Reusable by both ResultsScreen and History->Sessions tab.
 */
data class SessionCardStats(
    val cardId: String,
    val gameType: String,
    val trialsInSession: Int,
    val correctInSession: Int
)

@Dao
interface TrialDao {
    @Insert
    suspend fun insert(trial: TrialEntity)

    @Insert
    suspend fun insertAll(trials: List<TrialEntity>)

    @Query("SELECT * FROM trials WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getTrialsForSession(sessionId: Long): List<TrialEntity>

    @Query("SELECT * FROM trials ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTrials(limit: Int = 100): Flow<List<TrialEntity>>

    @Query("SELECT COUNT(*) FROM trials WHERE cardId = :cardId")
    suspend fun countTrialsForCard(cardId: String): Int

    @Query("SELECT COUNT(*) FROM trials WHERE cardId = :cardId AND wasCorrect = 1")
    suspend fun countCorrectTrialsForCard(cardId: String): Int

    /**
     * Get per-session accuracy for a specific card, used for graphing accuracy over time.
     */
    @Query("""
        SELECT t.sessionId, rs.startedAt as sessionDate,
               COUNT(*) as trialsInSession,
               SUM(CASE WHEN t.wasCorrect THEN 1 ELSE 0 END) as correctInSession
        FROM trials t
        INNER JOIN review_sessions rs ON t.sessionId = rs.id
        WHERE t.cardId = :cardId
        GROUP BY t.sessionId
        ORDER BY rs.startedAt ASC
    """)
    suspend fun getCardSessionAccuracy(cardId: String): List<CardSessionAccuracy>

    /**
     * Get per-card stats for a specific session, used for session results breakdown.
     * Returns cards ordered by number of trials (descending).
     */
    @Query("""
        SELECT cardId, gameType,
               COUNT(*) as trialsInSession,
               SUM(CASE WHEN wasCorrect THEN 1 ELSE 0 END) as correctInSession
        FROM trials
        WHERE sessionId = :sessionId
        GROUP BY cardId
        ORDER BY trialsInSession DESC
    """)
    suspend fun getSessionCardStats(sessionId: Long): List<SessionCardStats>
}
