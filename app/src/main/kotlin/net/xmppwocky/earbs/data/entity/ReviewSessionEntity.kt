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
    val octave: Int = 0,                    // deprecated: sessions now have mixed cards
    val playbackMode: String = "MIXED"      // deprecated: sessions now have mixed cards
)
