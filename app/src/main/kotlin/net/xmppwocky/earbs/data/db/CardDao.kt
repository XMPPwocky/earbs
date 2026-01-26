package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.CardEntity

/**
 * Combined view of a card with its FSRS state.
 */
data class CardWithFsrs(
    val id: String,
    val chordType: String,
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

/**
 * Generic card with FSRS state that can represent any game type.
 * Used by CardDetailsScreen to display cards regardless of game type.
 */
data class GenericCardWithFsrs(
    val id: String,
    val displayName: String,  // "Major", "V (major)", "I-IV-V-I (major)", etc.
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
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE unlocked = 1")
    suspend fun getAllUnlocked(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE unlocked = 1")
    fun getAllUnlockedFlow(): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE unlocked = 1")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE unlocked = 1")
    fun countUnlockedFlow(): Flow<Int>

    // ========== Queries with FSRS state (JOIN with fsrs_state) ==========

    /**
     * Get all unlocked cards with their FSRS state, ordered by due date.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1
        ORDER BY f.dueDate ASC
    """)
    suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrs>

    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1
        ORDER BY f.dueDate ASC
    """)
    fun getAllUnlockedWithFsrsFlow(): Flow<List<CardWithFsrs>>

    /**
     * Get card with its FSRS state.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.id = :id
    """)
    suspend fun getByIdWithFsrs(id: String): CardWithFsrs?

    /**
     * Get all due cards (dueDate <= now) ordered by due date.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1 AND f.dueDate <= :now
        ORDER BY f.dueDate ASC
    """)
    suspend fun getDueCards(now: Long): List<CardWithFsrs>

    /**
     * Get non-due cards for a specific (octave, playbackMode) group with limit.
     * Used to pad sessions preferring same-group cards.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1 AND c.octave = :octave AND c.playbackMode = :mode AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCardsByGroup(now: Long, octave: Int, mode: String, limit: Int): List<CardWithFsrs>

    /**
     * Get non-due cards (reviewing early) to pad session.
     * Ordered by due date so cards closest to being due are selected first.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               f.stability, f.difficulty, f.interval, f.dueDate,
               f.reviewCount, f.lastReview, f.phase, f.lapses
        FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1 AND f.dueDate > :now
        ORDER BY f.dueDate ASC
        LIMIT :limit
    """)
    suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrs>

    /**
     * Count of due cards.
     */
    @Query("""
        SELECT COUNT(*) FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1 AND f.dueDate <= :now
    """)
    suspend fun countDue(now: Long): Int

    @Query("""
        SELECT COUNT(*) FROM cards c
        INNER JOIN fsrs_state f ON c.id = f.cardId
        WHERE c.unlocked = 1 AND f.dueDate <= :now
    """)
    fun countDueFlow(now: Long): Flow<Int>

    // ========== Unlock screen methods ==========

    /**
     * Get all cards (locked and unlocked) ordered by chordType, octave, playbackMode.
     * Used for the unlock management screen.
     */
    @Query("""
        SELECT * FROM cards
        ORDER BY octave ASC, playbackMode ASC, chordType ASC
    """)
    suspend fun getAllCardsOrdered(): List<CardEntity>

    /**
     * Get all cards with their FSRS state (or null if no FSRS state exists).
     * Used for the unlock management screen to show FSRS info for unlocked cards.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM cards c
        LEFT JOIN fsrs_state f ON c.id = f.cardId
        ORDER BY octave ASC, playbackMode ASC, chordType ASC
    """)
    suspend fun getAllCardsWithFsrsOrdered(): List<CardWithFsrs>

    /**
     * Get all cards with their FSRS state as a Flow.
     */
    @Query("""
        SELECT c.id, c.chordType, c.octave, c.playbackMode, c.unlocked,
               COALESCE(f.stability, 2.5) as stability,
               COALESCE(f.difficulty, 2.5) as difficulty,
               COALESCE(f.interval, 0) as interval,
               COALESCE(f.dueDate, 0) as dueDate,
               COALESCE(f.reviewCount, 0) as reviewCount,
               f.lastReview as lastReview,
               COALESCE(f.phase, 0) as phase,
               COALESCE(f.lapses, 0) as lapses
        FROM cards c
        LEFT JOIN fsrs_state f ON c.id = f.cardId
        ORDER BY octave ASC, playbackMode ASC, chordType ASC
    """)
    fun getAllCardsWithFsrsOrderedFlow(): Flow<List<CardWithFsrs>>

    /**
     * Get all card IDs. Used to check which cards already exist.
     */
    @Query("SELECT id FROM cards")
    suspend fun getAllIds(): List<String>

    /**
     * Set the unlocked status for a card.
     */
    @Query("UPDATE cards SET unlocked = :unlocked WHERE id = :id")
    suspend fun setUnlocked(id: String, unlocked: Boolean)
}
