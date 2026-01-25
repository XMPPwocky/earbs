package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * Represents a group of function cards that unlock together.
 * All cards in a group share the same key quality, octave, and playback mode.
 */
data class FunctionUnlockGroup(
    val functions: List<ChordFunction>,
    val keyQuality: KeyQuality,
    val octave: Int,
    val playbackMode: PlaybackMode
) {
    init {
        require(functions.size == 3) { "FunctionUnlockGroup must have exactly 3 functions, got ${functions.size}" }
        require(functions.all { it.keyQuality == keyQuality }) {
            "All functions must match the group's key quality"
        }
    }

    /**
     * Generate the 3 cards for this unlock group.
     */
    fun toCards(): List<FunctionCard> = functions.map { function ->
        FunctionCard(function, keyQuality, octave, playbackMode)
    }
}

/**
 * Defines the function card deck and unlock progression.
 *
 * Card model: (function, key_quality, octave, playback_mode)
 *
 * Major key functions: ii, iii, IV, V, vi, vii° (skip I - tonic vs tonic is trivial)
 * Minor key functions: ii°, III, iv, v, VI, VII (skip i)
 *
 * Total cards: 6 functions × 3 octaves × 2 modes × 2 key qualities = 72 cards
 * Unlocked in 24 groups of 3 cards each.
 *
 * Unlock order:
 * - Major key first (most familiar for beginners)
 * - Start with common functions (IV, V, vi)
 * - Then less common (ii, iii, vii°)
 * - Minor key unlocks after all major groups are complete
 */
object FunctionDeck {

    /**
     * Primary major functions (most common in pop/rock).
     */
    private val MAJOR_PRIMARY = listOf(ChordFunction.IV, ChordFunction.V, ChordFunction.vi)

    /**
     * Secondary major functions (less common but still important).
     */
    private val MAJOR_SECONDARY = listOf(ChordFunction.ii, ChordFunction.iii, ChordFunction.vii_dim)

    /**
     * Primary minor functions.
     */
    private val MINOR_PRIMARY = listOf(ChordFunction.iv, ChordFunction.v, ChordFunction.VI)

    /**
     * Secondary minor functions.
     */
    private val MINOR_SECONDARY = listOf(ChordFunction.ii_dim, ChordFunction.III, ChordFunction.VII)

    /**
     * Full unlock order: 24 groups of 3 cards each.
     *
     * Order:
     * 0-11: Major key (all octaves and modes)
     * 12-23: Minor key (all octaves and modes)
     */
    val UNLOCK_ORDER: List<FunctionUnlockGroup> = buildList {
        // Major key groups (0-11)
        // Octave 4 first (middle register, easiest to hear)
        add(FunctionUnlockGroup(MAJOR_PRIMARY, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED))     // 0: Starting deck
        add(FunctionUnlockGroup(MAJOR_SECONDARY, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED))  // 1
        add(FunctionUnlockGroup(MAJOR_PRIMARY, KeyQuality.MAJOR, 4, PlaybackMode.BLOCK))          // 2
        add(FunctionUnlockGroup(MAJOR_SECONDARY, KeyQuality.MAJOR, 4, PlaybackMode.BLOCK))        // 3

        // Octave 3 (lower register)
        add(FunctionUnlockGroup(MAJOR_PRIMARY, KeyQuality.MAJOR, 3, PlaybackMode.ARPEGGIATED))    // 4
        add(FunctionUnlockGroup(MAJOR_SECONDARY, KeyQuality.MAJOR, 3, PlaybackMode.ARPEGGIATED))  // 5
        add(FunctionUnlockGroup(MAJOR_PRIMARY, KeyQuality.MAJOR, 3, PlaybackMode.BLOCK))          // 6
        add(FunctionUnlockGroup(MAJOR_SECONDARY, KeyQuality.MAJOR, 3, PlaybackMode.BLOCK))        // 7

        // Octave 5 (higher register)
        add(FunctionUnlockGroup(MAJOR_PRIMARY, KeyQuality.MAJOR, 5, PlaybackMode.ARPEGGIATED))    // 8
        add(FunctionUnlockGroup(MAJOR_SECONDARY, KeyQuality.MAJOR, 5, PlaybackMode.ARPEGGIATED))  // 9
        add(FunctionUnlockGroup(MAJOR_PRIMARY, KeyQuality.MAJOR, 5, PlaybackMode.BLOCK))          // 10
        add(FunctionUnlockGroup(MAJOR_SECONDARY, KeyQuality.MAJOR, 5, PlaybackMode.BLOCK))        // 11

        // Minor key groups (12-23) - unlocks after major is complete
        add(FunctionUnlockGroup(MINOR_PRIMARY, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED))    // 12
        add(FunctionUnlockGroup(MINOR_SECONDARY, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED))  // 13
        add(FunctionUnlockGroup(MINOR_PRIMARY, KeyQuality.MINOR, 4, PlaybackMode.BLOCK))          // 14
        add(FunctionUnlockGroup(MINOR_SECONDARY, KeyQuality.MINOR, 4, PlaybackMode.BLOCK))        // 15

        add(FunctionUnlockGroup(MINOR_PRIMARY, KeyQuality.MINOR, 3, PlaybackMode.ARPEGGIATED))    // 16
        add(FunctionUnlockGroup(MINOR_SECONDARY, KeyQuality.MINOR, 3, PlaybackMode.ARPEGGIATED))  // 17
        add(FunctionUnlockGroup(MINOR_PRIMARY, KeyQuality.MINOR, 3, PlaybackMode.BLOCK))          // 18
        add(FunctionUnlockGroup(MINOR_SECONDARY, KeyQuality.MINOR, 3, PlaybackMode.BLOCK))        // 19

        add(FunctionUnlockGroup(MINOR_PRIMARY, KeyQuality.MINOR, 5, PlaybackMode.ARPEGGIATED))    // 20
        add(FunctionUnlockGroup(MINOR_SECONDARY, KeyQuality.MINOR, 5, PlaybackMode.ARPEGGIATED))  // 21
        add(FunctionUnlockGroup(MINOR_PRIMARY, KeyQuality.MINOR, 5, PlaybackMode.BLOCK))          // 22
        add(FunctionUnlockGroup(MINOR_SECONDARY, KeyQuality.MINOR, 5, PlaybackMode.BLOCK))        // 23: Full deck
    }

    /**
     * Total number of cards when fully unlocked.
     */
    const val TOTAL_CARDS = 72  // 6 functions × 3 octaves × 2 modes × 2 key qualities

    /**
     * Maximum unlock level (0-indexed).
     */
    val MAX_UNLOCK_LEVEL = UNLOCK_ORDER.size - 1

    /**
     * Number of cards per unlock group.
     */
    const val CARDS_PER_GROUP = 3
}
