package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.CardEntity

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE unlocked = 1")
    suspend fun getAllUnlocked(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE unlocked = 1 ORDER BY dueDate ASC")
    fun getAllUnlockedFlow(): Flow<List<CardEntity>>

    /**
     * Get all due cards (dueDate <= now) ordered by due date.
     */
    @Query("SELECT * FROM cards WHERE unlocked = 1 AND dueDate <= :now ORDER BY dueDate ASC")
    suspend fun getDueCards(now: Long): List<CardEntity>

    /**
     * Get non-due cards for a specific octave.
     * Used to pad sessions when fewer than 4 cards are due.
     */
    @Query("SELECT * FROM cards WHERE unlocked = 1 AND octave = :octave AND dueDate > :now ORDER BY dueDate ASC")
    suspend fun getNonDueCardsForOctave(octave: Int, now: Long): List<CardEntity>

    /**
     * Get all cards for a specific octave.
     */
    @Query("SELECT * FROM cards WHERE unlocked = 1 AND octave = :octave ORDER BY dueDate ASC")
    suspend fun getCardsForOctave(octave: Int): List<CardEntity>

    /**
     * Count of due cards.
     */
    @Query("SELECT COUNT(*) FROM cards WHERE unlocked = 1 AND dueDate <= :now")
    suspend fun countDue(now: Long): Int

    @Query("SELECT COUNT(*) FROM cards WHERE unlocked = 1 AND dueDate <= :now")
    fun countDueFlow(now: Long): Flow<Int>

    /**
     * Update FSRS state after a review session.
     */
    @Query("""
        UPDATE cards SET
            stability = :stability,
            difficulty = :difficulty,
            interval = :interval,
            dueDate = :dueDate,
            reviewCount = :reviewCount,
            lastReview = :lastReview,
            phase = :phase,
            lapses = :lapses
        WHERE id = :id
    """)
    suspend fun updateFsrsState(
        id: String,
        stability: Double,
        difficulty: Double,
        interval: Int,
        dueDate: Long,
        reviewCount: Int,
        lastReview: Long,
        phase: Int,
        lapses: Int
    )

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int
}
