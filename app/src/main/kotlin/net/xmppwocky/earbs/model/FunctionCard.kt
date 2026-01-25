package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * A card for the chord function game.
 * Represents a (function, key_quality, octave, playback_mode) tuple for spaced repetition.
 *
 * User hears: tonic chord (I or i), then target chord
 * User answers: the function (roman numeral) of the target chord
 */
data class FunctionCard(
    val function: ChordFunction,
    val keyQuality: KeyQuality,
    val octave: Int,
    val playbackMode: PlaybackMode
) {
    val id: String get() = "${function.name}_${keyQuality.name}_${octave}_${playbackMode.name}"

    val displayName: String get() = "${function.displayName} in ${keyQuality.name.lowercase()} @ Oct $octave (${playbackMode.name.lowercase()})"

    companion object {
        /**
         * Parse a FunctionCard from its ID string.
         * ID format: {function}_{keyQuality}_{octave}_{playbackMode}
         * Note: function names may contain underscores (e.g., vii_dim), so we parse from the right.
         */
        fun fromId(id: String): FunctionCard? {
            val parts = id.split("_")
            if (parts.size < 4) return null
            return try {
                // Parse from the right: playbackMode, octave, keyQuality are last 3 parts
                // Everything before that is the function name (which may contain underscores)
                val playbackMode = PlaybackMode.valueOf(parts.last())
                val octave = parts[parts.size - 2].toInt()
                val keyQuality = KeyQuality.valueOf(parts[parts.size - 3])
                val functionName = parts.dropLast(3).joinToString("_")

                FunctionCard(
                    function = ChordFunction.valueOf(functionName),
                    keyQuality = keyQuality,
                    octave = octave,
                    playbackMode = playbackMode
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
