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
    val deprecated: Boolean,
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

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND deprecated = 0")
    suspend fun getAllUnlocked(): List<FunctionCardEntity>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND deprecated = 0")
    fun getAllUnlockedFlow(): Flow<List<FunctionCardEntity>>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND deprecated = 0 AND keyQuality = :keyQuality")
    suspend fun getByKeyQuality(keyQuality: String): List<FunctionCardEntity>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND deprecated = 0 AND octave = :octave AND playbackMode = :mode")
    suspend fun getByGroup(octave: Int, mode: String): List<FunctionCardEntity>

    @Query("SELECT COUNT(*) FROM function_cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM function_cards WHERE unlocked = 1 AND deprecated = 0")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM function_cards WHERE unlocked = 1 AND deprecated = 0")
    fun countUnlockedFlow(): Flow<Int>

    // ========== Queries with FSRS state (JOIN with fsrs_state) ==========

    /**
     * Get all unlocked, non-deprecated function cards with their FSRS state, ordered by due date.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0
        ORDER BY f.dueDate ASC
    """)
    suspend fun getAllUnlockedWithFsrs(): List<FunctionCardWithFsrs>

    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0
        ORDER BY f.dueDate ASC
    """)
    fun getAllUnlockedWithFsrsFlow(): Flow<List<FunctionCardWithFsrs>>

    /**
     * Get function card with its FSRS state (includes deprecated cards for history display).
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.id = :id
    """)
    suspend fun getByIdWithFsrs(id: String): FunctionCardWithFsrs?

    /**
     * Get all due function cards (dueDate <= now) ordered by due date.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0 AND f.dueDate <= :now
        ORDER BY f.dueDate ASC
    """)
    suspend fun getDueCards(now: Long): List<FunctionCardWithFsrs>

    /**
     * Get non-due function cards for a specific (keyQuality, octave, playbackMode) group with limit.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0 AND fc.keyQuality = :keyQuality AND fc.octave = :octave AND fc.playbackMode = :mode AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCardsByGroup(now: Long, keyQuality: String, octave: Int, mode: String, limit: Int): List<FunctionCardWithFsrs>

    /**
     * Get non-due function cards (reviewing early) to pad session.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0 AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCards(now: Long, limit: Int): List<FunctionCardWithFsrs>

    /**
     * Count of due function cards (excludes deprecated).
     */
    @Query("""
        SELECT COUNT(*) FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0 AND f.dueDate <= :now
    """)
    suspend fun countDue(now: Long): Int

    @Query("""
        SELECT COUNT(*) FROM function_cards fc
        INNER JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.unlocked = 1 AND fc.deprecated = 0 AND f.dueDate <= :now
    """)
    fun countDueFlow(now: Long): Flow<Int>

    // ========== Unlock screen methods ==========

    /**
     * Get all non-deprecated function cards (locked and unlocked) ordered by keyQuality, octave, playbackMode, function.
     * Used for the unlock management screen.
     */
    @Query("""
        SELECT * FROM function_cards
        WHERE deprecated = 0
        ORDER BY keyQuality ASC, octave ASC, playbackMode ASC, function ASC
    """)
    suspend fun getAllCardsOrdered(): List<FunctionCardEntity>

    /**
     * Get all non-deprecated function cards with their FSRS state (or null if no FSRS state exists).
     * Used for the unlock management screen to show FSRS info for unlocked cards.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM function_cards fc
        LEFT JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.deprecated = 0
        ORDER BY keyQuality ASC, octave ASC, playbackMode ASC, function ASC
    """)
    suspend fun getAllCardsWithFsrsOrdered(): List<FunctionCardWithFsrs>

    /**
     * Get all non-deprecated function cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM function_cards fc
        LEFT JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.deprecated = 0
        ORDER BY keyQuality ASC, octave ASC, playbackMode ASC, function ASC
    """)
    fun getAllCardsWithFsrsOrderedFlow(): Flow<List<FunctionCardWithFsrs>>

    /**
     * Get all function card IDs. Used to check which cards already exist.
     */
    @Query("SELECT id FROM function_cards")
    suspend fun getAllIds(): List<String>

    /**
     * Set the unlocked status for a function card.
     */
    @Query("UPDATE function_cards SET unlocked = :unlocked WHERE id = :id")
    suspend fun setUnlocked(id: String, unlocked: Boolean)

    /**
     * Set the deprecated status for a function card.
     */
    @Query("UPDATE function_cards SET deprecated = :deprecated WHERE id = :id")
    suspend fun setDeprecated(id: String, deprecated: Boolean)

    // ========== Deprecated cards (for history/archive) ==========

    /**
     * Get all deprecated function cards with their FSRS state.
     * Used for the "Archived Cards" section in history screen.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM function_cards fc
        LEFT JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.deprecated = 1
        ORDER BY fc.keyQuality ASC, fc.function ASC, fc.octave ASC, fc.playbackMode ASC
    """)
    suspend fun getDeprecatedCardsWithFsrs(): List<FunctionCardWithFsrs>

    /**
     * Get all deprecated function cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT fc.id, fc.function, fc.keyQuality, fc.octave, fc.playbackMode, fc.unlocked, fc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM function_cards fc
        LEFT JOIN fsrs_state f ON fc.id = f.cardId
        WHERE fc.deprecated = 1
        ORDER BY fc.keyQuality ASC, fc.function ASC, fc.octave ASC, fc.playbackMode ASC
    """)
    fun getDeprecatedCardsWithFsrsFlow(): Flow<List<FunctionCardWithFsrs>>

    /**
     * Count of deprecated function cards.
     */
    @Query("SELECT COUNT(*) FROM function_cards WHERE deprecated = 1")
    suspend fun countDeprecated(): Int
}
