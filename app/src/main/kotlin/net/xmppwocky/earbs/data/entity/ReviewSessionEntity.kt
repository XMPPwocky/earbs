package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for each review session.
 * Sessions can belong to different game types (chord type identification vs chord function).
 */
@Entity(tableName = "review_sessions")
data class ReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,                    // epoch millis
    val completedAt: Long? = null,          // epoch millis
    val gameType: String = "CHORD_TYPE",    // CHORD_TYPE or CHORD_FUNCTION
    @Deprecated("Sessions now have mixed cards with varying octaves")
    val octave: Int = 0,
    @Deprecated("Sessions now have mixed cards with varying playback modes")
    val playbackMode: String = "MIXED"
)
