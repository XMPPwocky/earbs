package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType

/**
 * Deck configuration for the scale recognition game.
 * 10 scales × 3 octaves × 3 directions = 90 cards total.
 */
object ScaleDeck {
    /** Total number of scale cards in the deck */
    const val TOTAL_CARDS = 90  // 10 scales × 3 octaves × 3 directions

    /** Cards per unlock group (1 scale × 3 octaves × 3 directions) */
    const val CARDS_PER_GROUP = 9

    /** Maximum unlock level (0-indexed) */
    const val MAX_UNLOCK_LEVEL = 9  // 10 groups total

    /**
     * Unlock order: scales ordered from most familiar to more exotic.
     * Each scale is unlocked as a group (all octaves and directions together).
     */
    val UNLOCK_ORDER = listOf(
        ScaleType.MAJOR,            // Group 0: The reference scale
        ScaleType.NATURAL_MINOR,    // Group 1: Most common minor
        ScaleType.MAJOR_PENTATONIC, // Group 2: Simple, distinctive (5 notes)
        ScaleType.MINOR_PENTATONIC, // Group 3: Blues/rock foundation
        ScaleType.DORIAN,           // Group 4: Jazz/rock minor variant
        ScaleType.MIXOLYDIAN,       // Group 5: Dominant/rock sound
        ScaleType.HARMONIC_MINOR,   // Group 6: Distinctive augmented 2nd
        ScaleType.MELODIC_MINOR,    // Group 7: Jazz minor
        ScaleType.PHRYGIAN,         // Group 8: Spanish/exotic flavor
        ScaleType.LYDIAN            // Group 9: Bright, dreamy, raised 4th
    )

    /**
     * Get all possible scale cards.
     */
    fun getAllCards(): List<ScaleCard> {
        return ScaleType.entries.flatMap { scale ->
            listOf(3, 4, 5).flatMap { octave ->
                ScaleDirection.entries.map { direction ->
                    ScaleCard(scale, octave, direction)
                }
            }
        }
    }

    /**
     * Get the starting cards for a new deck.
     * Major and Natural Minor scales at octave 4, all 3 directions.
     * This ensures at least 2 scale types from the first review.
     */
    fun getStartingCards(): List<ScaleCard> {
        val startingScales = listOf(ScaleType.MAJOR, ScaleType.NATURAL_MINOR)
        return startingScales.flatMap { scale ->
            ScaleDirection.entries.map { direction ->
                ScaleCard(scale, 4, direction)
            }
        }
    }

    /**
     * Get the starting card IDs for a new deck.
     */
    fun getStartingCardIds(): Set<String> {
        return getStartingCards().map { it.id }.toSet()
    }

    /**
     * Get the unlock group index for a card.
     * Cards are grouped by scale type.
     */
    fun getGroupIndex(card: ScaleCard): Int {
        return UNLOCK_ORDER.indexOf(card.scale)
    }

    /**
     * Get the name of an unlock group.
     */
    fun getGroupName(groupIndex: Int): String {
        return UNLOCK_ORDER.getOrNull(groupIndex)?.displayName ?: "Unknown"
    }
}
