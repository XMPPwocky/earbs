package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.IntervalCardEntity

/**
 * Combined view of an interval card with its FSRS state.
 */
data class IntervalCardWithFsrs(
    val id: String,
    val interval: String,
    val octave: Int,
    val direction: String,
    val unlocked: Boolean,
    val deprecated: Boolean,
    val stability: Double,
    val difficulty: Double,
    val interval_: Int,  // FSRS interval (days), renamed to avoid conflict
    val dueDate: Long,
    val reviewCount: Int,
    val lastReview: Long?,
    val phase: Int,
    val lapses: Int
)

@Dao
interface IntervalCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: IntervalCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<IntervalCardEntity>)

    @Query("SELECT * FROM interval_cards WHERE id = :id")
    suspend fun getById(id: String): IntervalCardEntity?

    @Query("SELECT * FROM interval_cards WHERE unlocked = 1 AND deprecated = 0")
    suspend fun getAllUnlocked(): List<IntervalCardEntity>

    @Query("SELECT * FROM interval_cards WHERE unlocked = 1 AND deprecated = 0")
    fun getAllUnlockedFlow(): Flow<List<IntervalCardEntity>>

    @Query("SELECT * FROM interval_cards WHERE unlocked = 1 AND deprecated = 0 AND octave = :octave AND direction = :direction")
    suspend fun getByGroup(octave: Int, direction: String): List<IntervalCardEntity>

    @Query("SELECT COUNT(*) FROM interval_cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM interval_cards WHERE unlocked = 1 AND deprecated = 0")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM interval_cards WHERE unlocked = 1 AND deprecated = 0")
    fun countUnlockedFlow(): Flow<Int>

    // ========== Queries with FSRS state (JOIN with fsrs_state) ==========

    /**
     * Get all unlocked, non-deprecated interval cards with their FSRS state, ordered by due date.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               f.stability, f.difficulty, f.interval as interval_, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0
        ORDER BY f.dueDate ASC
    """)
    suspend fun getAllUnlockedWithFsrs(): List<IntervalCardWithFsrs>

    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               f.stability, f.difficulty, f.interval as interval_, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0
        ORDER BY f.dueDate ASC
    """)
    fun getAllUnlockedWithFsrsFlow(): Flow<List<IntervalCardWithFsrs>>

    /**
     * Get interval card with its FSRS state (includes deprecated cards for history display).
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               f.stability, f.difficulty, f.interval as interval_, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.id = :id
    """)
    suspend fun getByIdWithFsrs(id: String): IntervalCardWithFsrs?

    /**
     * Get all due interval cards (dueDate <= now) ordered by due date.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               f.stability, f.difficulty, f.interval as interval_, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0 AND f.dueDate <= :now
        ORDER BY f.dueDate ASC
    """)
    suspend fun getDueCards(now: Long): List<IntervalCardWithFsrs>

    /**
     * Get non-due interval cards for a specific (interval, octave, direction) group with limit.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               f.stability, f.difficulty, f.interval as interval_, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0 AND ic.interval = :interval AND ic.octave = :octave AND ic.direction = :direction AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCardsByGroup(now: Long, interval: String, octave: Int, direction: String, limit: Int): List<IntervalCardWithFsrs>

    /**
     * Get non-due interval cards (reviewing early) to pad session.
     * Excludes deprecated cards.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               f.stability, f.difficulty, f.interval as interval_, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0 AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCards(now: Long, limit: Int): List<IntervalCardWithFsrs>

    /**
     * Count of due interval cards (excludes deprecated).
     */
    @Query("""
        SELECT COUNT(*) FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0 AND f.dueDate <= :now
    """)
    suspend fun countDue(now: Long): Int

    @Query("""
        SELECT COUNT(*) FROM interval_cards ic
        INNER JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.unlocked = 1 AND ic.deprecated = 0 AND f.dueDate <= :now
    """)
    fun countDueFlow(now: Long): Flow<Int>

    // ========== Unlock screen methods ==========

    /**
     * Get all non-deprecated interval cards (locked and unlocked) ordered by interval, octave, direction.
     * Used for the unlock management screen.
     */
    @Query("""
        SELECT * FROM interval_cards
        WHERE deprecated = 0
        ORDER BY interval ASC, octave ASC, direction ASC
    """)
    suspend fun getAllCardsOrdered(): List<IntervalCardEntity>

    /**
     * Get all non-deprecated interval cards with their FSRS state (or null if no FSRS state exists).
     * Used for the unlock management screen to show FSRS info for unlocked cards.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval_,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM interval_cards ic
        LEFT JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.deprecated = 0
        ORDER BY ic.interval ASC, ic.octave ASC, ic.direction ASC
    """)
    suspend fun getAllCardsWithFsrsOrdered(): List<IntervalCardWithFsrs>

    /**
     * Get all non-deprecated interval cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval_,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM interval_cards ic
        LEFT JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.deprecated = 0
        ORDER BY ic.interval ASC, ic.octave ASC, ic.direction ASC
    """)
    fun getAllCardsWithFsrsOrderedFlow(): Flow<List<IntervalCardWithFsrs>>

    /**
     * Get all interval card IDs. Used to check which cards already exist.
     */
    @Query("SELECT id FROM interval_cards")
    suspend fun getAllIds(): List<String>

    /**
     * Set the unlocked status for an interval card.
     */
    @Query("UPDATE interval_cards SET unlocked = :unlocked WHERE id = :id")
    suspend fun setUnlocked(id: String, unlocked: Boolean)

    /**
     * Set the deprecated status for an interval card.
     */
    @Query("UPDATE interval_cards SET deprecated = :deprecated WHERE id = :id")
    suspend fun setDeprecated(id: String, deprecated: Boolean)

    // ========== Deprecated cards (for history/archive) ==========

    /**
     * Get all deprecated interval cards with their FSRS state.
     * Used for the "Archived Cards" section in history screen.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval_,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM interval_cards ic
        LEFT JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.deprecated = 1
        ORDER BY ic.interval ASC, ic.octave ASC, ic.direction ASC
    """)
    suspend fun getDeprecatedCardsWithFsrs(): List<IntervalCardWithFsrs>

    /**
     * Get all deprecated interval cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT ic.id, ic.interval, ic.octave, ic.direction, ic.unlocked, ic.deprecated,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval_,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM interval_cards ic
        LEFT JOIN fsrs_state f ON ic.id = f.cardId
        WHERE ic.deprecated = 1
        ORDER BY ic.interval ASC, ic.octave ASC, ic.direction ASC
    """)
    fun getDeprecatedCardsWithFsrsFlow(): Flow<List<IntervalCardWithFsrs>>

    /**
     * Count of deprecated interval cards.
     */
    @Query("SELECT COUNT(*) FROM interval_cards WHERE deprecated = 1")
    suspend fun countDeprecated(): Int
}
