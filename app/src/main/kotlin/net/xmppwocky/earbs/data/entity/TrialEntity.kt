package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Every individual trial (40 per session).
 * Full audit trail of all user answers.
 */
@Entity(
    tableName = "trials",
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
data class TrialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cardId: String,
    val timestamp: Long,                   // epoch millis
    val wasCorrect: Boolean,
    val answeredChordType: String? = null  // What user answered (null = correct or legacy data)
)
