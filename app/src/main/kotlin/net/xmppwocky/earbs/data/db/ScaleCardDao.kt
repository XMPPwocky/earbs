package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.ScaleCardEntity

/**
 * Combined view of a scale card with its FSRS state.
 */
data class ScaleCardWithFsrs(
    val id: String,
    val scale: String,
    val octave: Int,
    val direction: String,
    val unlocked: Boolean,
    val deprecated: Boolean,
    val stability: Double,
    val difficulty: Double,
    val interval: Int,  // FSRS interval (days)
    val dueDate: Long,
    val reviewCount: Int,
    val lastReview: Long?,
    val phase: Int,
    val lapses: Int
)

@Dao
interface ScaleCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: ScaleCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<ScaleCardEntity>)

    @Query("SELECT * FROM scale_cards WHERE id = :id")
    suspend fun getById(id: String): ScaleCardEntity?

    @Query("SELECT * FROM scale_cards WHERE unlocked = 1 AND deprecated = 0")
    suspend fun getAllUnlocked(): List<ScaleCardEntity>

    @Query("SELECT * FROM scale_cards WHERE unlocked = 1 AND deprecated = 0")
    fun getAllUnlockedFlow(): Flow<List<ScaleCardEntity>>

    @Query("SELECT * FROM scale_cards WHERE unlocked = 1 AND deprecated = 0 AND octave = :octave AND direction = :direction")
    suspend fun getByGroup(octave: Int, direction: String): List<ScaleCardEntity>

    @Query("SELECT COUNT(*) FROM scale_cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM scale_cards WHERE unlocked = 1 AND deprecated = 0")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM scale_cards WHERE unlocked = 1 AND deprecated = 0")
    fun countUnlockedFlow(): Flow<Int>

    // ========== Queries with FSRS state (JOIN with fsrs_state) ==========

    /**
     * Get all unlocked, non-deprecated scale cards with their FSRS state, ordered by due date.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0
        ORDER BY f.dueDate ASC
    """)
    suspend fun getAllUnlockedWithFsrs(): List<ScaleCardWithFsrs>

    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0
        ORDER BY f.dueDate ASC
    """)
    fun getAllUnlockedWithFsrsFlow(): Flow<List<ScaleCardWithFsrs>>

    /**
     * Get scale card with its FSRS state (includes deprecated cards for history display).
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.id = :id
    """)
    suspend fun getByIdWithFsrs(id: String): ScaleCardWithFsrs?

    /**
     * Get all due scale cards (dueDate <= now) ordered by due date.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0 AND f.dueDate <= :now
        ORDER BY f.dueDate ASC
    """)
    suspend fun getDueCards(now: Long): List<ScaleCardWithFsrs>

    /**
     * Get non-due scale cards for a specific (scale, octave, direction) group with limit.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0 AND sc.scale = :scale AND sc.octave = :octave AND sc.direction = :direction AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCardsByGroup(now: Long, scale: String, octave: Int, direction: String, limit: Int): List<ScaleCardWithFsrs>

    /**
     * Get non-due scale cards (reviewing early) to pad session.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0 AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCards(now: Long, limit: Int): List<ScaleCardWithFsrs>

    /**
     * Count of due scale cards (excludes deprecated).
     */
    @Query("""
        SELECT COUNT(*) FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0 AND f.dueDate <= :now
    """)
    suspend fun countDue(now: Long): Int

    @Query("""
        SELECT COUNT(*) FROM scale_cards sc
        INNER JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.unlocked = 1 AND sc.deprecated = 0 AND f.dueDate <= :now
    """)
    fun countDueFlow(now: Long): Flow<Int>

    // ========== Unlock screen methods ==========

    /**
     * Get all non-deprecated scale cards (locked and unlocked) ordered by scale, octave, direction.
     * Used for the unlock management screen.
     */
    @Query("""
        SELECT * FROM scale_cards
        WHERE deprecated = 0
        ORDER BY scale ASC, octave ASC, direction ASC
    """)
    suspend fun getAllCardsOrdered(): List<ScaleCardEntity>

    /**
     * Get all non-deprecated scale cards with their FSRS state (or null if no FSRS state exists).
     * Used for the unlock management screen to show FSRS info for unlocked cards.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM scale_cards sc
        LEFT JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.deprecated = 0
        ORDER BY sc.scale ASC, sc.octave ASC, sc.direction ASC
    """)
    suspend fun getAllCardsWithFsrsOrdered(): List<ScaleCardWithFsrs>

    /**
     * Get all non-deprecated scale cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM scale_cards sc
        LEFT JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.deprecated = 0
        ORDER BY sc.scale ASC, sc.octave ASC, sc.direction ASC
    """)
    fun getAllCardsWithFsrsOrderedFlow(): Flow<List<ScaleCardWithFsrs>>

    /**
     * Get all scale card IDs. Used to check which cards already exist.
     */
    @Query("SELECT id FROM scale_cards")
    suspend fun getAllIds(): List<String>

    /**
     * Set the unlocked status for a scale card.
     */
    @Query("UPDATE scale_cards SET unlocked = :unlocked WHERE id = :id")
    suspend fun setUnlocked(id: String, unlocked: Boolean)

    /**
     * Set the deprecated status for a scale card.
     */
    @Query("UPDATE scale_cards SET deprecated = :deprecated WHERE id = :id")
    suspend fun setDeprecated(id: String, deprecated: Boolean)

    // ========== Deprecated cards (for history/archive) ==========

    /**
     * Get all deprecated scale cards with their FSRS state.
     * Used for the "Archived Cards" section in history screen.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM scale_cards sc
        LEFT JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.deprecated = 1
        ORDER BY sc.scale ASC, sc.octave ASC, sc.direction ASC
    """)
    suspend fun getDeprecatedCardsWithFsrs(): List<ScaleCardWithFsrs>

    /**
     * Get all deprecated scale cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT sc.id, sc.scale, sc.octave, sc.direction, sc.unlocked, sc.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM scale_cards sc
        LEFT JOIN fsrs_state f ON sc.id = f.cardId
        WHERE sc.deprecated = 1
        ORDER BY sc.scale ASC, sc.octave ASC, sc.direction ASC
    """)
    fun getDeprecatedCardsWithFsrsFlow(): Flow<List<ScaleCardWithFsrs>>

    /**
     * Count of deprecated scale cards.
     */
    @Query("SELECT COUNT(*) FROM scale_cards WHERE deprecated = 1")
    suspend fun countDeprecated(): Int
}
