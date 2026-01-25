package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * A card for the chord progression game.
 * Represents a (progression, octave, playback_mode) tuple for spaced repetition.
 *
 * Note: Unlike FunctionCard, ProgressionCard does NOT include keyQuality as a property.
 * Key quality is randomized at playback time and hidden from the user.
 *
 * User hears: a sequence of chords forming a progression
 * User answers: the progression name (I-IV-I, I-V-vi-IV, etc.)
 */
data class ProgressionCard(
    val progression: ProgressionType,
    override val octave: Int,
    override val playbackMode: PlaybackMode
) : GameCard {
    override val id: String get() = "${progression.name}_${octave}_${playbackMode.name}"

    override val displayName: String get() = "${progression.displayName} @ Oct $octave (${playbackMode.name.lowercase()})"

    companion object {
        /**
         * Parse a ProgressionCard from its ID string.
         * ID format: {progression}_{octave}_{playbackMode}
         * Note: progression names may contain underscores, so we parse from the right.
         */
        fun fromId(id: String): ProgressionCard? {
            val parts = id.split("_")
            if (parts.size < 3) return null
            return try {
                // Parse from the right: playbackMode, octave are last 2 parts
                // Everything before that is the progression name (which contains underscores)
                val playbackMode = PlaybackMode.valueOf(parts.last())
                val octave = parts[parts.size - 2].toInt()
                val progressionName = parts.dropLast(2).joinToString("_")

                ProgressionCard(
                    progression = ProgressionType.valueOf(progressionName),
                    octave = octave,
                    playbackMode = playbackMode
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
