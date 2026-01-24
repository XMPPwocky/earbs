package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity

/**
 * View data class for card statistics aggregated from trials.
 */
data class CardStatsView(
    val cardId: String,
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
     * Get per-card lifetime statistics.
     */
    @Query("""
        SELECT cardId,
               COUNT(*) as totalTrials,
               SUM(CASE WHEN wasCorrect THEN 1 ELSE 0 END) as correctTrials
        FROM trials
        GROUP BY cardId
    """)
    fun getCardStats(): Flow<List<CardStatsView>>

    /**
     * Get session overview with trial counts.
     */
    @Query("""
        SELECT
            s.id,
            s.startedAt,
            s.completedAt,
            s.octave,
            COUNT(t.id) as totalTrials,
            SUM(CASE WHEN t.wasCorrect THEN 1 ELSE 0 END) as correctTrials
        FROM review_sessions s
        LEFT JOIN trials t ON s.id = t.sessionId
        GROUP BY s.id
        ORDER BY s.startedAt DESC
    """)
    fun getSessionOverviews(): Flow<List<SessionOverview>>
}
