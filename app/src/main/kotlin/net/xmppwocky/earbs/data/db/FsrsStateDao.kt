package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.FsrsStateEntity

@Dao
interface FsrsStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: FsrsStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(states: List<FsrsStateEntity>)

    @Query("SELECT * FROM fsrs_state WHERE cardId = :cardId")
    suspend fun getByCardId(cardId: String): FsrsStateEntity?

    @Query("SELECT * FROM fsrs_state WHERE gameType = :gameType")
    suspend fun getByGameType(gameType: String): List<FsrsStateEntity>

    @Query("SELECT * FROM fsrs_state WHERE gameType = :gameType AND dueDate <= :now")
    suspend fun getDueByGameType(gameType: String, now: Long): List<FsrsStateEntity>

    @Query("SELECT * FROM fsrs_state WHERE dueDate <= :now")
    suspend fun getAllDue(now: Long): List<FsrsStateEntity>

    @Query("SELECT COUNT(*) FROM fsrs_state WHERE gameType = :gameType AND dueDate <= :now")
    suspend fun countDueByGameType(gameType: String, now: Long): Int

    @Query("SELECT COUNT(*) FROM fsrs_state WHERE gameType = :gameType AND dueDate <= :now")
    fun countDueByGameTypeFlow(gameType: String, now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM fsrs_state WHERE dueDate <= :now")
    suspend fun countAllDue(now: Long): Int

    @Query("SELECT COUNT(*) FROM fsrs_state WHERE gameType = :gameType")
    suspend fun countByGameType(gameType: String): Int

    @Query("SELECT COUNT(*) FROM fsrs_state WHERE gameType = :gameType")
    fun countByGameTypeFlow(gameType: String): Flow<Int>

    @Query("""
        UPDATE fsrs_state SET
            stability = :stability,
            difficulty = :difficulty,
            interval = :interval,
            dueDate = :dueDate,
            reviewCount = :reviewCount,
            lastReview = :lastReview,
            phase = :phase,
            lapses = :lapses
        WHERE cardId = :cardId
    """)
    suspend fun updateFsrsState(
        cardId: String,
        stability: Double,
        difficulty: Double,
        interval: Int,
        dueDate: Long,
        reviewCount: Int,
        lastReview: Long?,
        phase: Int,
        lapses: Int
    )

    @Query("DELETE FROM fsrs_state WHERE cardId = :cardId")
    suspend fun delete(cardId: String)
}
