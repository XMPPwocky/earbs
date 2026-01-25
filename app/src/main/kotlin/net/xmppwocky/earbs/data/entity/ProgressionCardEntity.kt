package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Card for the chord progression identification game.
 * A card is a (progression, octave, playback_mode) tuple.
 *
 * Note: Unlike FunctionCardEntity, there is NO keyQuality field.
 * Key quality is randomized at playback time and hidden from the user.
 *
 * FSRS state is stored separately in the fsrs_state table.
 *
 * Card ID format: "PROGRESSION_OCTAVE_PLAYBACKMODE" (e.g., "I_IV_V_I_4_ARPEGGIATED")
 */
@Entity(tableName = "progression_cards")
data class ProgressionCardEntity(
    @PrimaryKey val id: String,            // e.g., "I_IV_V_I_4_ARPEGGIATED"
    val progression: String,                // I_IV_I, I_V_vi_IV, etc.
    val octave: Int,                        // 3, 4, or 5
    val playbackMode: String,               // ARPEGGIATED or BLOCK
    val unlocked: Boolean = true
)
