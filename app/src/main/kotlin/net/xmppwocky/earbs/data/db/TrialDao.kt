package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.TrialEntity

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
}
