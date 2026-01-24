package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for each 40-trial review session.
 */
@Entity(tableName = "review_sessions")
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,                   // epoch millis
    val completedAt: Long? = null,         // epoch millis
    val octave: Int                        // which octave this session was for
)
