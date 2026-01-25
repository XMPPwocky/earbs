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
         */
        fun fromId(id: String): FunctionCard? {
            val parts = id.split("_")
            if (parts.size != 4) return null
            return try {
                FunctionCard(
                    function = ChordFunction.valueOf(parts[0]),
                    keyQuality = KeyQuality.valueOf(parts[1]),
                    octave = parts[2].toInt(),
                    playbackMode = PlaybackMode.valueOf(parts[3])
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
