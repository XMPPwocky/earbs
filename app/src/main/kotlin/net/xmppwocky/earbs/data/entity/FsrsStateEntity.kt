package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Game type for distinguishing cards across different games.
 */
enum class GameType {
    CHORD_TYPE,
    CHORD_FUNCTION,
    CHORD_PROGRESSION,
    INTERVAL,
    SCALE
}

/**
 * User-facing display name for each game type.
 * Using an exhaustive when expression ensures compile error if new game type is added.
 */
val GameType.displayName: String get() = when (this) {
    GameType.CHORD_TYPE -> "Chord Type"
    GameType.CHORD_FUNCTION -> "Function"
    GameType.CHORD_PROGRESSION -> "Progression"
    GameType.INTERVAL -> "Interval"
    GameType.SCALE -> "Scale"
}

/**
 * FSRS spaced repetition state for any card across all games.
 * This table stores the scheduling state separately from game-specific card data.
 *
 * cardId references either cards.id or function_cards.id depending on gameType.
 */
@Entity(
    tableName = "fsrs_state",
    indices = [
        Index("gameType"),
        Index("dueDate")
    ]
)
data class FsrsStateEntity(
    @PrimaryKey val cardId: String,        // References cards.id or function_cards.id
    val gameType: String,                   // CHORD_TYPE or CHORD_FUNCTION
    val stability: Double = 2.5,
    val difficulty: Double = 2.5,
    val interval: Int = 0,
    val dueDate: Long,                      // epoch millis
    val reviewCount: Int = 0,
    val lastReview: Long? = null,           // epoch millis
    val phase: Int = 0,                     // 0=Added, 1=ReLearning, 2=Review
    val lapses: Int = 0
)
