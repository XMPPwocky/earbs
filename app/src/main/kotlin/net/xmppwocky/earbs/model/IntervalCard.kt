package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType
import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * Card for the interval recognition game.
 * Tuple: (interval, octave, direction)
 */
data class IntervalCard(
    val interval: IntervalType,
    override val octave: Int,
    val direction: IntervalDirection
) : GameCard {
    override val id: String
        get() = "${interval.name}_${octave}_${direction.name}"

    // Intervals don't use playback mode in the traditional sense,
    // but we need to satisfy the interface. Direction determines playback style.
    override val playbackMode: PlaybackMode
        get() = if (direction == IntervalDirection.HARMONIC) PlaybackMode.BLOCK else PlaybackMode.ARPEGGIATED

    override val displayName: String
        get() = interval.displayName

    companion object {
        /**
         * Parse an IntervalCard from its string ID.
         * Format: {INTERVAL_TYPE}_{octave}_{DIRECTION}
         * Example: "MAJOR_3RD_4_ASCENDING", "PERFECT_5TH_3_HARMONIC"
         */
        fun fromId(id: String): IntervalCard {
            val parts = id.split("_")
            // Direction is always the last part
            val direction = IntervalDirection.valueOf(parts.last())
            // Octave is always second to last
            val octave = parts[parts.size - 2].toInt()
            // Interval name is everything before octave (may contain underscores)
            val intervalName = parts.dropLast(2).joinToString("_")
            val interval = IntervalType.valueOf(intervalName)
            return IntervalCard(interval, octave, direction)
        }
    }
}
