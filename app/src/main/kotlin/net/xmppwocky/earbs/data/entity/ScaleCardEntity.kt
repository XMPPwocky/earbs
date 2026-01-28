package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card for the scale recognition game.
 * A card is a (scale, octave, direction) tuple.
 *
 * FSRS state is stored separately in the fsrs_state table.
 *
 * Card ID format: "{SCALE_TYPE}_{octave}_{DIRECTION}"
 * Example: "MAJOR_4_ASCENDING", "MINOR_PENTATONIC_3_BOTH"
 */
@Entity(tableName = "scale_cards")
data class ScaleCardEntity(
    @PrimaryKey val id: String,             // e.g., "MAJOR_4_ASCENDING"
    val scale: String,                      // MAJOR, NATURAL_MINOR, DORIAN, etc.
    val octave: Int,                        // 3, 4, or 5
    val direction: String,                  // ASCENDING, DESCENDING, or BOTH
    val unlocked: Boolean = true,
    val deprecated: Boolean = false         // App-level deprecation (cards excluded from reviews)
)
