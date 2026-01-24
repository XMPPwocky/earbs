package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity

@Dao
interface ReviewSessionDao {
    @Insert
    suspend fun insert(session: ReviewSessionEntity): Long

    @Query("SELECT * FROM review_sessions WHERE id = :id")
    suspend fun getById(id: Long): ReviewSessionEntity?

    @Query("SELECT * FROM review_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<ReviewSessionEntity>>

    @Query("SELECT * FROM review_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ReviewSessionEntity>>

    @Query("UPDATE review_sessions SET completedAt = :completedAt WHERE id = :id")
    suspend fun markComplete(id: Long, completedAt: Long)
}
