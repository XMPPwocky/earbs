package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * FSRS state for a card (chord_type + octave combination).
 * Each card tracks its spaced repetition state independently.
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,           // e.g., "MAJOR_4"
    val chordType: String,                 // MAJOR, MINOR, SUS2, etc.
    val octave: Int,                       // 3, 4, or 5
    val stability: Double = 2.5,
    val difficulty: Double = 2.5,
    val interval: Int = 0,
    val dueDate: Long,                     // epoch millis
    val reviewCount: Int = 0,
    val lastReview: Long? = null,          // epoch millis
    val phase: Int = 0,                    // 0=Added, 1=ReLearning, 2=Review
    val lapses: Int = 0,
    val unlocked: Boolean = true
)
