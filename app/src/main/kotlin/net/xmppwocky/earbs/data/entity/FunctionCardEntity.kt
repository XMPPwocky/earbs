package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card for the chord function identification game.
 * A card is a (function, key_quality, octave, playback_mode) tuple.
 *
 * FSRS state is stored separately in the fsrs_state table.
 *
 * Card ID format: "FUNCTION_KEYQUALITY_OCTAVE_PLAYBACKMODE" (e.g., "IV_MAJOR_4_ARPEGGIATED")
 */
@Entity(tableName = "function_cards")
data class FunctionCardEntity(
    @PrimaryKey val id: String,            // e.g., "IV_MAJOR_4_ARPEGGIATED"
    val function: String,                   // IV, V, vi, ii, iii, vii_dim, etc.
    val keyQuality: String,                 // MAJOR or MINOR
    val octave: Int,                        // 3, 4, or 5
    val playbackMode: String,               // ARPEGGIATED or BLOCK
    val unlocked: Boolean = true,
    val deprecated: Boolean = false         // App-level deprecation (cards excluded from reviews)
)
