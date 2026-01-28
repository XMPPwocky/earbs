package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * Represents a group of progression cards that unlock together.
 * All cards in a group share the same octave and playback mode.
 */
data class ProgressionUnlockGroup(
    val progressions: List<ProgressionType>,
    val octave: Int,
    val playbackMode: PlaybackMode
) {
    init {
        require(progressions.size in 1..2) { "ProgressionUnlockGroup must have 1-2 progressions, got ${progressions.size}" }
    }

    /**
     * Generate the 2 cards for this unlock group.
     */
    fun toCards(): List<ProgressionCard> = progressions.map { progression ->
        ProgressionCard(progression, octave, playbackMode)
    }
}

/**
 * Defines the progression card deck and unlock progression.
 *
 * Card model: (progression, octave, playback_mode)
 * Each progression has a fixed key quality (major or minor).
 *
 * 14 active progression types:
 * - 3-chord: I-IV-I, I-V-I (major); i-iv-i, i-v-i (minor)
 * - 4-chord resolving: I-IV-V-I, I-ii-V-I (major); i-iv-v-i, i-ii°-v-i (minor)
 * - New 4-chord: I-vi-ii-V (major); i-VI-ii°-v (minor) - replaced 5-chord versions
 * - Loops: I-V-vi-IV, I-vi-IV-V (major); i-v-VI-iv, i-VI-iv-v (minor)
 *
 * Total active cards: 14 progressions × 3 octaves × 2 modes = 84 cards
 * (Plus 24 deprecated 5-chord cards preserved for historical data)
 * Unlocked in 48 groups.
 *
 * Unlock order:
 * - 3-chord major, then 3-chord minor
 * - 4-chord resolving major, then 4-chord resolving minor
 * - New 4-chord major, then new 4-chord minor
 * - Loops major, then loops minor
 * - Within each: octave 4 first, then 3, then 5
 * - Within each octave: arpeggiated first, then block
 */
object ProgressionDeck {

    /**
     * 3-chord major progressions.
     */
    private val THREE_CHORD_MAJOR = listOf(
        ProgressionType.I_IV_I_MAJOR,
        ProgressionType.I_V_I_MAJOR
    )

    /**
     * 3-chord minor progressions.
     */
    private val THREE_CHORD_MINOR = listOf(
        ProgressionType.i_iv_i_MINOR,
        ProgressionType.i_v_i_MINOR
    )

    /**
     * 4-chord resolving major progressions.
     */
    private val FOUR_CHORD_MAJOR = listOf(
        ProgressionType.I_IV_V_I_MAJOR,
        ProgressionType.I_ii_V_I_MAJOR
    )

    /**
     * 4-chord resolving minor progressions.
     */
    private val FOUR_CHORD_MINOR = listOf(
        ProgressionType.i_iv_v_i_MINOR,
        ProgressionType.i_iio_v_i_MINOR
    )

    /**
     * New 4-chord major progressions (replace deprecated 5-chord).
     * Only I_vi_ii_V_MAJOR since I_vi_IV_V_MAJOR already exists in LOOPS_MAJOR.
     */
    private val NEW_FOUR_CHORD_MAJOR = listOf(
        ProgressionType.I_vi_ii_V_MAJOR
    )

    /**
     * New 4-chord minor progressions (replace deprecated 5-chord).
     * Only i_VI_iio_v_MINOR since i_VI_iv_v_MINOR already exists in LOOPS_MINOR.
     */
    private val NEW_FOUR_CHORD_MINOR = listOf(
        ProgressionType.i_VI_iio_v_MINOR
    )

    /**
     * Loop major progressions.
     */
    private val LOOPS_MAJOR = listOf(
        ProgressionType.I_V_vi_IV_MAJOR,
        ProgressionType.I_vi_IV_V_MAJOR
    )

    /**
     * Loop minor progressions.
     */
    private val LOOPS_MINOR = listOf(
        ProgressionType.i_v_VI_iv_MINOR,
        ProgressionType.i_VI_iv_v_MINOR
    )

    /**
     * Generate unlock groups for a set of progressions across all octaves and modes.
     * Order: oct 4 arp, oct 4 block, oct 3 arp, oct 3 block, oct 5 arp, oct 5 block
     */
    private fun generateGroups(progressions: List<ProgressionType>): List<ProgressionUnlockGroup> = buildList {
        // Octave 4 first (middle register, easiest to hear)
        add(ProgressionUnlockGroup(progressions, 4, PlaybackMode.ARPEGGIATED))
        add(ProgressionUnlockGroup(progressions, 4, PlaybackMode.BLOCK))

        // Octave 3 (lower register)
        add(ProgressionUnlockGroup(progressions, 3, PlaybackMode.ARPEGGIATED))
        add(ProgressionUnlockGroup(progressions, 3, PlaybackMode.BLOCK))

        // Octave 5 (higher register)
        add(ProgressionUnlockGroup(progressions, 5, PlaybackMode.ARPEGGIATED))
        add(ProgressionUnlockGroup(progressions, 5, PlaybackMode.BLOCK))
    }

    /**
     * Full unlock order: 42 groups (36 with 2 cards + 6 with 1 card = 84 total cards).
     *
     * Order by complexity, then key quality:
     * 0-5: 3-chord major (2 each)
     * 6-11: 3-chord minor (2 each)
     * 12-17: 4-chord major resolving (2 each)
     * 18-23: 4-chord minor resolving (2 each)
     * 24-29: New 4-chord major (1 each) - replaces deprecated 5-chord
     * 30-35: New 4-chord minor (1 each) - replaces deprecated 5-chord
     * 36-41: Loops major (2 each)
     * 42-47: Loops minor (2 each)
     */
    val UNLOCK_ORDER: List<ProgressionUnlockGroup> = buildList {
        // 3-chord progressions
        addAll(generateGroups(THREE_CHORD_MAJOR))   // groups 0-5
        addAll(generateGroups(THREE_CHORD_MINOR))   // groups 6-11

        // 4-chord resolving progressions
        addAll(generateGroups(FOUR_CHORD_MAJOR))    // groups 12-17
        addAll(generateGroups(FOUR_CHORD_MINOR))    // groups 18-23

        // New 4-chord progressions (replacing deprecated 5-chord)
        addAll(generateGroups(NEW_FOUR_CHORD_MAJOR))  // groups 24-29
        addAll(generateGroups(NEW_FOUR_CHORD_MINOR))  // groups 30-35

        // Loop progressions
        addAll(generateGroups(LOOPS_MAJOR))         // groups 36-41
        addAll(generateGroups(LOOPS_MINOR))         // groups 42-47
    }

    /**
     * Total number of active cards when fully unlocked.
     * 14 active progressions × 3 octaves × 2 modes = 84 cards
     * (4 deprecated 5-chord progressions × 6 variants = 24 deprecated cards not counted)
     */
    const val TOTAL_CARDS = 84

    /**
     * Maximum unlock level (0-indexed).
     */
    val MAX_UNLOCK_LEVEL = UNLOCK_ORDER.size - 1

    /**
     * Number of cards per unlock group.
     */
    const val CARDS_PER_GROUP = 2

    /**
     * Generate all possible progression cards in the deck (96 cards).
     * Used for pre-creating all cards in the database.
     */
    fun getAllCards(): List<ProgressionCard> {
        return UNLOCK_ORDER.flatMap { group -> group.toCards() }
    }

    /**
     * Get the unlock group index for a card.
     * Returns the group index (0-47) or -1 if not found.
     */
    fun getGroupIndex(card: ProgressionCard): Int {
        return UNLOCK_ORDER.indexOfFirst { group ->
            group.octave == card.octave &&
            group.playbackMode == card.playbackMode &&
            card.progression in group.progressions
        }
    }

    /**
     * Get a human-readable name for an unlock group.
     */
    fun getGroupName(groupIndex: Int): String {
        if (groupIndex < 0 || groupIndex >= UNLOCK_ORDER.size) return "Unknown Group"
        val group = UNLOCK_ORDER[groupIndex]

        val complexity = when {
            group.progressions.all { it in THREE_CHORD_MAJOR } -> "3-chord Major"
            group.progressions.all { it in THREE_CHORD_MINOR } -> "3-chord Minor"
            group.progressions.all { it in FOUR_CHORD_MAJOR } -> "4-chord Major"
            group.progressions.all { it in FOUR_CHORD_MINOR } -> "4-chord Minor"
            group.progressions.all { it in NEW_FOUR_CHORD_MAJOR } -> "4-chord Major (vi-ii-V)"
            group.progressions.all { it in NEW_FOUR_CHORD_MINOR } -> "4-chord Minor (VI-ii°-v)"
            group.progressions.all { it in LOOPS_MAJOR } -> "Loops Major"
            group.progressions.all { it in LOOPS_MINOR } -> "Loops Minor"
            else -> "Mixed"
        }

        val modeName = group.playbackMode.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$complexity @ Octave ${group.octave}, $modeName"
    }
}
