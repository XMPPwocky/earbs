package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType

/**
 * Defines the unlock progression for interval recognition cards.
 * 12 groups, one per interval, ordered by difficulty.
 * Each group has 9 cards (3 octaves × 3 directions).
 */
object IntervalDeck {
    const val TOTAL_CARDS = 108  // 12 intervals × 3 octaves × 3 directions
    const val CARDS_PER_GROUP = 9  // 1 interval × 3 octaves × 3 directions
    const val MAX_UNLOCK_LEVEL = 11  // 0-indexed, 12 groups total

    /**
     * Unlock order from easiest to hardest intervals.
     */
    val UNLOCK_ORDER = listOf(
        IntervalType.PERFECT_5TH,   // Group 0: Most consonant, easiest to hear
        IntervalType.OCTAVE,        // Group 1: Very distinctive
        IntervalType.MAJOR_3RD,     // Group 2: Major chord foundation
        IntervalType.MINOR_3RD,     // Group 3: Minor chord foundation
        IntervalType.PERFECT_4TH,   // Group 4: Common melodic interval
        IntervalType.MAJOR_2ND,     // Group 5: Whole step
        IntervalType.MINOR_2ND,     // Group 6: Half step
        IntervalType.MAJOR_6TH,     // Group 7: Wider, sweet sound
        IntervalType.MINOR_6TH,     // Group 8: Wider, darker sound
        IntervalType.MINOR_7TH,     // Group 9: Dominant 7th sound
        IntervalType.MAJOR_7TH,     // Group 10: Leading tone tension
        IntervalType.TRITONE        // Group 11: Most dissonant, hardest
    )

    /**
     * Get the unlock group index for a card.
     * Cards are grouped by interval type.
     */
    fun getGroupIndex(card: IntervalCard): Int {
        return UNLOCK_ORDER.indexOf(card.interval)
    }

    /**
     * Get a human-readable name for an unlock group.
     */
    fun getGroupName(groupIndex: Int): String {
        return UNLOCK_ORDER.getOrNull(groupIndex)?.displayName ?: "Unknown"
    }

    /**
     * Generate all possible interval cards.
     */
    fun getAllCards(): List<IntervalCard> {
        return IntervalType.entries.flatMap { interval ->
            listOf(3, 4, 5).flatMap { octave ->
                IntervalDirection.entries.map { direction ->
                    IntervalCard(interval, octave, direction)
                }
            }
        }
    }

    /**
     * Get the starting deck (cards unlocked by default).
     * Perfect 5th at octave 4, all 3 directions.
     */
    fun getStartingCards(): List<IntervalCard> {
        return listOf(
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING),
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.DESCENDING),
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.HARMONIC)
        )
    }

    /**
     * Get the starting card IDs.
     */
    fun getStartingCardIds(): Set<String> {
        return getStartingCards().map { it.id }.toSet()
    }
}
