package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType

/**
 * Card for the scale recognition game.
 * Tuple: (scale, octave, direction)
 */
data class ScaleCard(
    val scale: ScaleType,
    override val octave: Int,
    val direction: ScaleDirection
) : GameCard {
    override val id: String
        get() = "${scale.name}_${octave}_${direction.name}"

    // Scales are always played as sequential notes
    override val playbackMode: PlaybackMode
        get() = PlaybackMode.ARPEGGIATED

    override val displayName: String
        get() = scale.displayName

    companion object {
        /**
         * Parse a ScaleCard from its string ID.
         * Format: {SCALE_TYPE}_{octave}_{DIRECTION}
         * Example: "MAJOR_4_ASCENDING", "MINOR_PENTATONIC_3_BOTH"
         */
        fun fromId(id: String): ScaleCard {
            val parts = id.split("_")
            // Direction is always the last part
            val direction = ScaleDirection.valueOf(parts.last())
            // Octave is always second to last
            val octave = parts[parts.size - 2].toInt()
            // Scale name is everything before octave (may contain underscores)
            val scaleName = parts.dropLast(2).joinToString("_")
            val scale = ScaleType.valueOf(scaleName)
            return ScaleCard(scale, octave, direction)
        }
    }
}
