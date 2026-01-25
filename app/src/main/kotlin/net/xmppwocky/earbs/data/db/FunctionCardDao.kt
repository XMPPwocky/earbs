package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.FunctionCardEntity

/**
 * Combined view of a function card with its FSRS state.
 */
data class FunctionCardWithFsrs(
    val id: String,
    val function: String,
    val keyQuality: String,
    val octave: Int,
    val playbackMode: String,
    val unlocked: Boolean,
    val stability: Double,
    val difficulty: Double,
    val interval: Int,
    val dueDate: Long,
    val reviewCount: Int,
    val lastReview: Long?,
    val phase: Int,
    val lapses: Int
)

@Dao
interface FunctionCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: FunctionCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<FunctionCardEntity>)

    @Query("SELECT * FROM function_cards WHERE id = :id")
    suspend fun getById(id: String): FunctionCardEntity?

    @Query("SELECT * FROM function_cards WHERE unlocked = 1")
    suspend fun getAllUnlocked(): List<FunctionCardEntity>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1")
    fun getAllUnlockedFlow(): Flow<List<FunctionCardEntity>>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND keyQuality = :keyQuality")
    suspend fun getByKeyQuality(keyQuality: String): List<FunctionCardEntity>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND octave = :octave AND playbackMode = :mode")
    suspend fun getByGroup(octave: Int, mode: String): List<FunctionCardEntity>

    @Query("SELECT COUNT(*) FROM function_cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM function_cards WHERE unlocked = 1")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM function_cards WHERE unlocked = 1")
    fun countUnlockedFlow(): Flow<Int>

    // ========== Queries with FSRS state (JOIN with fsrs_state) ==========

    /**
     * Get all unlocked function cards with their FSRS state, ordered by due date.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1
        ORDER BY f.dueDate ASC
    """)
    suspend fun getAllUnlockedWithFsrs(): List<FunctionCardWithFsrs>

    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1
        ORDER BY f.dueDate ASC
    """)
    fun getAllUnlockedWithFsrsFlow(): Flow<List<FunctionCardWithFsrs>>

    /**
     * Get function card with its FSRS state.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.id = :id
    """)
    suspend fun getByIdWithFsrs(id: String): FunctionCardWithFsrs?

    /**
     * Get all due function cards (dueDate <= now) ordered by due date.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND f.dueDate <= :now
        ORDER BY f.dueDate ASC
    """)
    suspend fun getDueCards(now: Long): List<FunctionCardWithFsrs>

    /**
     * Get non-due function cards for a specific (keyQuality, octave, playbackMode) group with limit.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.keyQuality = :keyQuality AND fc.octave = :octave AND fc.playbackMode = :mode AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCardsByGroup(now: Long, keyQuality: String, octave: Int, mode: String, limit: Int): List<FunctionCardWithFsrs>

    /**
     * Get non-due function cards (reviewing early) to pad session.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCards(now: Long, limit: Int): List<FunctionCardWithFsrs>

    /**
     * Count of due function cards.
     */
    @Query("""
        SELECT COUNT(*) FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND f.dueDate <= :now
    """)
    suspend fun countDue(now: Long): Int

    @Query("""
        SELECT COUNT(*) FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND f.dueDate <= :now
    """)
    fun countDueFlow(now: Long): Flow<Int>
}
