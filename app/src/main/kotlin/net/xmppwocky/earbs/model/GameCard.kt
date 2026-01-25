package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * Common interface for all game card types.
 * Each game type has its own card implementation with game-specific properties,
 * but all share these common properties for UI and session management.
 */
interface GameCard {
    /** Unique identifier for this card (used for database storage) */
    val id: String

    /** Octave for audio playback (3, 4, or 5) */
    val octave: Int

    /** How the chord is played (block or arpeggiated) */
    val playbackMode: PlaybackMode

    /** Human-readable description of this card */
    val displayName: String
}
