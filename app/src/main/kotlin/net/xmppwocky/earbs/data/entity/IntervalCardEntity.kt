package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card for the interval recognition game.
 * A card is a (interval, octave, direction) tuple.
 *
 * FSRS state is stored separately in the fsrs_state table.
 *
 * Card ID format: "{INTERVAL_TYPE}_{octave}_{DIRECTION}"
 * Example: "PERFECT_5TH_4_ASCENDING", "MAJOR_3RD_3_HARMONIC"
 */
@Entity(tableName = "interval_cards")
data class IntervalCardEntity(
    @PrimaryKey val id: String,             // e.g., "PERFECT_5TH_4_ASCENDING"
    val interval: String,                   // PERFECT_5TH, MAJOR_3RD, etc.
    val octave: Int,                        // 3, 4, or 5
    val direction: String,                  // ASCENDING, DESCENDING, or HARMONIC
    val unlocked: Boolean = true,
    val deprecated: Boolean = false         // App-level deprecation (cards excluded from reviews)
)
