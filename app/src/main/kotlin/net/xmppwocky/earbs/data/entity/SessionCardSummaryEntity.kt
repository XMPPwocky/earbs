package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Aggregated results per card per session.
 * Used for quick grade lookup and FSRS state updates.
 */
@Entity(
    tableName = "session_card_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ReviewSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("cardId")
    ]
)
data class SessionCardSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cardId: String,
    val trialsCount: Int,
    val correctCount: Int,
    val grade: String                      // EASY, GOOD, HARD, AGAIN
)
