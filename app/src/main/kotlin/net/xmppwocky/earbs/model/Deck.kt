package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * Represents a group of 4 cards that unlock together.
 * All cards in a group share the same octave and playback mode.
 */
data class UnlockGroup(
    val chordTypes: List<ChordType>,
    val octave: Int,
    val playbackMode: PlaybackMode
) {
    init {
        require(chordTypes.size == 4) { "UnlockGroup must have exactly 4 chord types, got ${chordTypes.size}" }
    }

    /**
     * Generate the 4 cards for this unlock group.
     */
    fun toCards(): List<Card> = chordTypes.map { chordType ->
        Card(chordType, octave, playbackMode)
    }
}

/**
 * Defines the card deck and unlock progression.
 *
 * Card model: (chord_type, octave, playback_mode)
 * Session constraint: All 4 cards must share the same (octave, playback_mode)
 *
 * Total cards: 8 types × 3 octaves × 2 modes = 48 cards
 * Unlocked in 12 groups of 4 cards each.
 */
object Deck {

    /**
     * The starting deck: 4 triad types at octave 4, arpeggiated.
     * These are the cards available from the beginning.
     */
    val STARTING_CARDS = listOf(
        Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED),
        Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED),
        Card(ChordType.SUS2, 4, PlaybackMode.ARPEGGIATED),
        Card(ChordType.SUS4, 4, PlaybackMode.ARPEGGIATED)
    )

    /**
     * Full unlock order: 12 groups of 4 cards each.
     *
     * Each unlock adds all 4 chord types of a category (triads or 7ths)
     * for a specific (octave, playback_mode) combination.
     *
     * Order:
     * 1. Triads @ octave 4, arpeggiated (starting deck)
     * 2. Triads @ octave 4, block
     * 3. Triads @ octave 3, arpeggiated
     * 4. Triads @ octave 3, block
     * 5. Triads @ octave 5, arpeggiated
     * 6. Triads @ octave 5, block
     * 7. 7ths @ octave 4, arpeggiated
     * 8. 7ths @ octave 4, block
     * 9. 7ths @ octave 3, arpeggiated
     * 10. 7ths @ octave 3, block
     * 11. 7ths @ octave 5, arpeggiated
     * 12. 7ths @ octave 5, block
     */
    val UNLOCK_ORDER: List<UnlockGroup> = listOf(
        // Triads
        UnlockGroup(ChordType.TRIADS, 4, PlaybackMode.ARPEGGIATED), // 0: Starting deck
        UnlockGroup(ChordType.TRIADS, 4, PlaybackMode.BLOCK),       // 1
        UnlockGroup(ChordType.TRIADS, 3, PlaybackMode.ARPEGGIATED), // 2
        UnlockGroup(ChordType.TRIADS, 3, PlaybackMode.BLOCK),       // 3
        UnlockGroup(ChordType.TRIADS, 5, PlaybackMode.ARPEGGIATED), // 4
        UnlockGroup(ChordType.TRIADS, 5, PlaybackMode.BLOCK),       // 5
        // 7ths
        UnlockGroup(ChordType.SEVENTHS, 4, PlaybackMode.ARPEGGIATED), // 6
        UnlockGroup(ChordType.SEVENTHS, 4, PlaybackMode.BLOCK),       // 7
        UnlockGroup(ChordType.SEVENTHS, 3, PlaybackMode.ARPEGGIATED), // 8
        UnlockGroup(ChordType.SEVENTHS, 3, PlaybackMode.BLOCK),       // 9
        UnlockGroup(ChordType.SEVENTHS, 5, PlaybackMode.ARPEGGIATED), // 10
        UnlockGroup(ChordType.SEVENTHS, 5, PlaybackMode.BLOCK),       // 11: Full deck
    )

    /**
     * Total number of cards when fully unlocked.
     */
    const val TOTAL_CARDS = 48  // 8 types × 3 octaves × 2 modes

    /**
     * Maximum unlock level (0-indexed).
     */
    val MAX_UNLOCK_LEVEL = UNLOCK_ORDER.size - 1
}
