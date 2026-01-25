package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card for the chord type identification game.
 * A card is a (chord_type, octave, playback_mode) tuple.
 *
 * FSRS state is stored separately in the fsrs_state table.
 *
 * Card ID format: "CHORDTYPE_OCTAVE_PLAYBACKMODE" (e.g., "MAJOR_4_ARPEGGIATED")
 */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,            // e.g., "MAJOR_4_ARPEGGIATED"
    val chordType: String,                  // MAJOR, MINOR, SUS2, SUS4, DOM7, MAJ7, MIN7, DIM7
    val octave: Int,                        // 3, 4, or 5
    val playbackMode: String,               // ARPEGGIATED or BLOCK
    val unlocked: Boolean = true
)
