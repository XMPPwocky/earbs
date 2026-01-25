package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Every individual trial in a review session.
 * Full audit trail of all user answers across all game types.
 *
 * Note: cardId references cards.id, function_cards.id, or progression_cards.id depending on gameType.
 * The FK constraint was removed to support multiple card tables.
 */
@Entity(
    tableName = "trials",
    foreignKeys = [
        ForeignKey(
            entity = ReviewSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("cardId"),
        Index("gameType")
    ]
)
data class TrialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cardId: String,
    val timestamp: Long,                    // epoch millis
    val wasCorrect: Boolean,
    val gameType: String = "CHORD_TYPE",    // CHORD_TYPE, CHORD_FUNCTION, or CHORD_PROGRESSION
    val answeredChordType: String? = null,  // What user answered for chord type game (null = correct or N/A)
    val answeredFunction: String? = null,   // What user answered for function game (null = correct or N/A)
    val answeredProgression: String? = null // What user answered for progression game (null = correct or N/A)
)
