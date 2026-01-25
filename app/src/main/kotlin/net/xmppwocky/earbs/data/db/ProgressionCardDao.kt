package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.ProgressionCardEntity

/**
 * Combined view of a progression card with its FSRS state.
 */
data class ProgressionCardWithFsrs(
    val id: String,
    val progression: String,
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
interface ProgressionCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: ProgressionCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<ProgressionCardEntity>)

    @Query("SELECT * FROM progression_cards WHERE id = :id")
    suspend fun getById(id: String): ProgressionCardEntity?

    @Query("SELECT * FROM progression_cards WHERE unlocked = 1")
    suspend fun getAllUnlocked(): List<ProgressionCardEntity>

    @Query("SELECT * FROM progression_cards WHERE unlocked = 1")
    fun getAllUnlockedFlow(): Flow<List<ProgressionCardEntity>>

    @Query("SELECT * FROM progression_cards WHERE unlocked = 1 AND octave = :octave AND playbackMode = :mode")
    suspend fun getByGroup(octave: Int, mode: String): List<ProgressionCardEntity>

    @Query("SELECT COUNT(*) FROM progression_cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM progression_cards WHERE unlocked = 1")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM progression_cards WHERE unlocked = 1")
    fun countUnlockedFlow(): Flow<Int>

    // ========== Queries with FSRS state (JOIN with fsrs_state) ==========

    /**
     * Get all unlocked progression cards with their FSRS state, ordered by due date.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1
        ORDER BY f.dueDate ASC
    """)
    suspend fun getAllUnlockedWithFsrs(): List<ProgressionCardWithFsrs>

    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1
        ORDER BY f.dueDate ASC
    """)
    fun getAllUnlockedWithFsrsFlow(): Flow<List<ProgressionCardWithFsrs>>

    /**
     * Get progression card with its FSRS state.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.id = :id
    """)
    suspend fun getByIdWithFsrs(id: String): ProgressionCardWithFsrs?

    /**
     * Get all due progression cards (dueDate <= now) ordered by due date.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1 AND f.dueDate <= :now
        ORDER BY f.dueDate ASC
    """)
    suspend fun getDueCards(now: Long): List<ProgressionCardWithFsrs>

    /**
     * Get non-due progression cards for a specific (octave, playbackMode) group with limit.
     * Note: Progressions don't have keyQuality, so grouping is by octave + mode only.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1 AND pc.octave = :octave AND pc.playbackMode = :mode AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCardsByGroup(now: Long, octave: Int, mode: String, limit: Int): List<ProgressionCardWithFsrs>

    /**
     * Get non-due progression cards (reviewing early) to pad session.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1 AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCards(now: Long, limit: Int): List<ProgressionCardWithFsrs>

    /**
     * Count of due progression cards.
     */
    @Query("""
        SELECT COUNT(*) FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1 AND f.dueDate <= :now
    """)
    suspend fun countDue(now: Long): Int

    @Query("""
        SELECT COUNT(*) FROM progression_cards pc
        INNER JOIN fsrs_state f ON pc.id = f.cardId
        WHERE pc.unlocked = 1 AND f.dueDate <= :now
    """)
    fun countDueFlow(now: Long): Flow<Int>

    // ========== Unlock screen methods ==========

    /**
     * Get all progression cards (locked and unlocked) ordered by octave, playbackMode, progression.
     * Used for the unlock management screen.
     */
    @Query("""
        SELECT * FROM progression_cards
        ORDER BY octave ASC, playbackMode ASC, progression ASC
    """)
    suspend fun getAllCardsOrdered(): List<ProgressionCardEntity>

    /**
     * Get all progression cards with their FSRS state (or null if no FSRS state exists).
     * Used for the unlock management screen to show FSRS info for unlocked cards.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM progression_cards pc
        LEFT JOIN fsrs_state f ON pc.id = f.cardId
        ORDER BY octave ASC, playbackMode ASC, progression ASC
    """)
    suspend fun getAllCardsWithFsrsOrdered(): List<ProgressionCardWithFsrs>

    /**
     * Get all progression cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT pc.id, pc.progression, pc.octave, pc.playbackMode, pc.unlocked,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM progression_cards pc
        LEFT JOIN fsrs_state f ON pc.id = f.cardId
        ORDER BY octave ASC, playbackMode ASC, progression ASC
    """)
    fun getAllCardsWithFsrsOrderedFlow(): Flow<List<ProgressionCardWithFsrs>>

    /**
     * Get all progression card IDs. Used to check which cards already exist.
     */
    @Query("SELECT id FROM progression_cards")
    suspend fun getAllIds(): List<String>

    /**
     * Set the unlocked status for a progression card.
     */
    @Query("UPDATE progression_cards SET unlocked = :unlocked WHERE id = :id")
    suspend fun setUnlocked(id: String, unlocked: Boolean)
}
