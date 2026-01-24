package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.SessionCardSummaryEntity

@Dao
interface SessionCardSummaryDao {
    @Insert
    suspend fun insert(summary: SessionCardSummaryEntity)

    @Insert
    suspend fun insertAll(summaries: List<SessionCardSummaryEntity>)

    @Query("SELECT * FROM session_card_summaries WHERE sessionId = :sessionId")
    fun getSummariesForSession(sessionId: Long): Flow<List<SessionCardSummaryEntity>>

    @Query("SELECT * FROM session_card_summaries WHERE sessionId = :sessionId")
    suspend fun getSummariesForSessionSync(sessionId: Long): List<SessionCardSummaryEntity>
}
