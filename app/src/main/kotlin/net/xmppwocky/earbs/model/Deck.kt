package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType

/**
 * Defines the card deck and unlock progression.
 */
object Deck {

    /**
     * The starting deck: 4 basic chord types at octave 4.
     * These are the cards available from the beginning.
     */
    val STARTING_CARDS = listOf(
        Card(ChordType.MAJOR, 4),
        Card(ChordType.MINOR, 4),
        Card(ChordType.SUS2, 4),
        Card(ChordType.SUS4, 4)
    )

    /**
     * Full unlock order for future implementation:
     * 1. Major, Minor, Sus2, Sus4 @ octave 4 (starting deck)
     * 2. Major, Minor @ octave 3
     * 3. Sus2, Sus4 @ octave 3
     * 4. Major, Minor @ octave 5
     * 5. Sus2, Sus4 @ octave 5
     * 6. Dom7, Maj7 @ octave 4
     * 7. Min7 @ octave 4, Dom7 @ octave 3
     * ... (continues interleaving new types with octave expansion)
     */
    val UNLOCK_ORDER = listOf(
        // Level 1: Starting deck (unlocked by default)
        listOf(
            Card(ChordType.MAJOR, 4),
            Card(ChordType.MINOR, 4),
            Card(ChordType.SUS2, 4),
            Card(ChordType.SUS4, 4)
        ),
        // Level 2
        listOf(
            Card(ChordType.MAJOR, 3),
            Card(ChordType.MINOR, 3)
        ),
        // Level 3
        listOf(
            Card(ChordType.SUS2, 3),
            Card(ChordType.SUS4, 3)
        ),
        // Level 4
        listOf(
            Card(ChordType.MAJOR, 5),
            Card(ChordType.MINOR, 5)
        ),
        // Level 5
        listOf(
            Card(ChordType.SUS2, 5),
            Card(ChordType.SUS4, 5)
        ),
        // Level 6
        listOf(
            Card(ChordType.DOM7, 4),
            Card(ChordType.MAJ7, 4)
        ),
        // Level 7
        listOf(
            Card(ChordType.MIN7, 4),
            Card(ChordType.DOM7, 3)
        )
    )
}
