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
}
