package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * A card represents a (chord_type, octave, playback_mode) tuple for spaced repetition.
 * User answers chord type only; octave and playback mode affect how the chord sounds.
 *
 * Playback mode is now a property of the card (not a user toggle). This prevents
 * users from "cheesing" by identifying the playback mode instead of the chord quality.
 */
data class Card(
    val chordType: ChordType,
    override val octave: Int,
    override val playbackMode: PlaybackMode
) : GameCard {
    override val id: String get() = "${chordType.name}_${octave}_${playbackMode.name}"
    override val displayName: String get() = "${chordType.displayName} @ Oct $octave (${playbackMode.name.lowercase()})"
}
