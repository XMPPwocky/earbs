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
        require(progressions.size == 2) { "ProgressionUnlockGroup must have exactly 2 progressions, got ${progressions.size}" }
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
 * Note: key quality is NOT part of the card - it's randomized at playback time
 *
 * 8 progression types:
 * - 3-chord: I-IV-I, I-V-I
 * - 4-chord resolving: I-IV-V-I, I-ii-V-I
 * - 5-chord: I-vi-ii-V-I, I-vi-IV-V-I
 * - Loops: I-V-vi-IV, I-vi-IV-V
 *
 * Total cards: 8 progressions × 3 octaves × 2 modes = 48 cards
 * Unlocked in 24 groups of 2 cards each.
 *
 * Unlock order:
 * - 3-chord progressions first (simplest)
 * - Then 4-chord resolving
 * - Then 5-chord
 * - Loops last (most complex pattern recognition)
 * - Within each complexity: octave 4 first, then 3, then 5
 * - Within each octave: arpeggiated first, then block
 */
object ProgressionDeck {

    /**
     * 3-chord progressions (simplest).
     */
    private val THREE_CHORD = listOf(ProgressionType.I_IV_I, ProgressionType.I_V_I)

    /**
     * 4-chord resolving progressions.
     */
    private val FOUR_CHORD_RESOLVING = listOf(ProgressionType.I_IV_V_I, ProgressionType.I_ii_V_I)

    /**
     * 5-chord progressions.
     */
    private val FIVE_CHORD = listOf(ProgressionType.I_vi_ii_V_I, ProgressionType.I_vi_IV_V_I)

    /**
     * Loop progressions (most complex).
     */
    private val LOOPS = listOf(ProgressionType.I_V_vi_IV, ProgressionType.I_vi_IV_V)

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
     * Full unlock order: 24 groups of 2 cards each.
     *
     * Order:
     * 0-5: 3-chord progressions (all octaves and modes)
     * 6-11: 4-chord resolving progressions
     * 12-17: 5-chord progressions
     * 18-23: Loop progressions
     */
    val UNLOCK_ORDER: List<ProgressionUnlockGroup> = buildList {
        // 3-chord (groups 0-5)
        addAll(generateGroups(THREE_CHORD))

        // 4-chord resolving (groups 6-11)
        addAll(generateGroups(FOUR_CHORD_RESOLVING))

        // 5-chord (groups 12-17)
        addAll(generateGroups(FIVE_CHORD))

        // Loops (groups 18-23)
        addAll(generateGroups(LOOPS))
    }

    /**
     * Total number of cards when fully unlocked.
     */
    const val TOTAL_CARDS = 48  // 8 progressions × 3 octaves × 2 modes

    /**
     * Maximum unlock level (0-indexed).
     */
    val MAX_UNLOCK_LEVEL = UNLOCK_ORDER.size - 1

    /**
     * Number of cards per unlock group.
     */
    const val CARDS_PER_GROUP = 2

    /**
     * Generate all possible progression cards in the deck (48 cards).
     * Used for pre-creating all cards in the database.
     */
    fun getAllCards(): List<ProgressionCard> {
        return UNLOCK_ORDER.flatMap { group -> group.toCards() }
    }

    /**
     * Get the unlock group index for a card.
     * Returns the group index (0-23) or -1 if not found.
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
            group.progressions.all { it in THREE_CHORD } -> "3-chord"
            group.progressions.all { it in FOUR_CHORD_RESOLVING } -> "4-chord"
            group.progressions.all { it in FIVE_CHORD } -> "5-chord"
            group.progressions.all { it in LOOPS } -> "Loops"
            else -> "Mixed"
        }

        val modeName = group.playbackMode.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$complexity @ Octave ${group.octave}, $modeName"
    }
}
