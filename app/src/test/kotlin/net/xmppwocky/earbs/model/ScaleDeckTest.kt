package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType
import org.junit.Assert.*
import org.junit.Test

class ScaleDeckTest {

    // ========== UNLOCK_ORDER structure tests ==========

    @Test
    fun `UNLOCK_ORDER has 10 scales`() {
        assertEquals(10, ScaleDeck.UNLOCK_ORDER.size)
    }

    @Test
    fun `UNLOCK_ORDER contains all scale types`() {
        val unlockScales = ScaleDeck.UNLOCK_ORDER.toSet()
        val allScales = ScaleType.entries.toSet()
        assertEquals(allScales, unlockScales)
    }

    @Test
    fun `UNLOCK_ORDER has no duplicates`() {
        val uniqueScales = ScaleDeck.UNLOCK_ORDER.toSet()
        assertEquals(ScaleDeck.UNLOCK_ORDER.size, uniqueScales.size)
    }

    // ========== Difficulty ordering tests ==========

    @Test
    fun `first scale is Major (easiest)`() {
        assertEquals(ScaleType.MAJOR, ScaleDeck.UNLOCK_ORDER[0])
    }

    @Test
    fun `last scale is Lydian (hardest)`() {
        assertEquals(ScaleType.LYDIAN, ScaleDeck.UNLOCK_ORDER.last())
    }

    @Test
    fun `Natural Minor is second to unlock`() {
        assertEquals(ScaleType.NATURAL_MINOR, ScaleDeck.UNLOCK_ORDER[1])
    }

    @Test
    fun `pentatonic scales unlock in middle range`() {
        val majorPentaIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.MAJOR_PENTATONIC)
        val minorPentaIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.MINOR_PENTATONIC)

        assertTrue("Major Pentatonic should be in middle range", majorPentaIndex >= 2 && majorPentaIndex <= 5)
        assertTrue("Minor Pentatonic should be in middle range", minorPentaIndex >= 2 && minorPentaIndex <= 5)
    }

    @Test
    fun `modes unlock later`() {
        val dorianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.DORIAN)
        val mixolydianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.MIXOLYDIAN)
        val phrygianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.PHRYGIAN)
        val lydianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.LYDIAN)

        // Modes should unlock after basic scales
        assertTrue("Dorian should unlock after Natural Minor", dorianIndex > 1)
        assertTrue("Lydian should be last", lydianIndex == 9)
    }

    // ========== Card count tests ==========

    @Test
    fun `TOTAL_CARDS equals 90`() {
        // 10 scales × 3 octaves × 3 directions = 90
        assertEquals(90, ScaleDeck.TOTAL_CARDS)
    }

    @Test
    fun `CARDS_PER_GROUP equals 9`() {
        // 1 scale × 3 octaves × 3 directions = 9
        assertEquals(9, ScaleDeck.CARDS_PER_GROUP)
    }

    @Test
    fun `MAX_UNLOCK_LEVEL equals 9`() {
        // 0-indexed, 10 groups total
        assertEquals(9, ScaleDeck.MAX_UNLOCK_LEVEL)
    }

    // ========== getAllCards tests ==========

    @Test
    fun `getAllCards returns 90 cards`() {
        assertEquals(90, ScaleDeck.getAllCards().size)
    }

    @Test
    fun `getAllCards covers all scales`() {
        val allCards = ScaleDeck.getAllCards()
        val scalesUsed = allCards.map { it.scale }.toSet()
        assertEquals(ScaleType.entries.toSet(), scalesUsed)
    }

    @Test
    fun `getAllCards covers all octaves`() {
        val allCards = ScaleDeck.getAllCards()
        val octavesUsed = allCards.map { it.octave }.toSet()
        assertEquals(setOf(3, 4, 5), octavesUsed)
    }

    @Test
    fun `getAllCards covers all directions`() {
        val allCards = ScaleDeck.getAllCards()
        val directionsUsed = allCards.map { it.direction }.toSet()
        assertEquals(ScaleDirection.entries.toSet(), directionsUsed)
    }

    @Test
    fun `getAllCards has unique card ids`() {
        val allCards = ScaleDeck.getAllCards()
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(allCards.size, uniqueIds.size)
    }

    @Test
    fun `each scale appears 9 times (3 octaves x 3 directions)`() {
        val allCards = ScaleDeck.getAllCards()
        val cardsByScale = allCards.groupBy { it.scale }

        ScaleType.entries.forEach { scale ->
            assertEquals(
                "Scale $scale should appear 9 times",
                9,
                cardsByScale[scale]?.size
            )
        }
    }

    // ========== getStartingCards tests ==========

    @Test
    fun `getStartingCards returns 6 cards`() {
        assertEquals(6, ScaleDeck.getStartingCards().size)
    }

    @Test
    fun `starting cards are Major and Natural Minor at octave 4`() {
        val startingCards = ScaleDeck.getStartingCards()
        val scales = startingCards.map { it.scale }.toSet()
        assertEquals(setOf(ScaleType.MAJOR, ScaleType.NATURAL_MINOR), scales)
        startingCards.forEach { card ->
            assertEquals(4, card.octave)
        }
    }

    @Test
    fun `starting cards cover all 3 directions for each scale`() {
        val startingCards = ScaleDeck.getStartingCards()
        val majorCards = startingCards.filter { it.scale == ScaleType.MAJOR }
        val minorCards = startingCards.filter { it.scale == ScaleType.NATURAL_MINOR }
        assertEquals(ScaleDirection.entries.toSet(), majorCards.map { it.direction }.toSet())
        assertEquals(ScaleDirection.entries.toSet(), minorCards.map { it.direction }.toSet())
    }

    @Test
    fun `getStartingCardIds matches getStartingCards ids`() {
        val startingCards = ScaleDeck.getStartingCards()
        val startingIds = ScaleDeck.getStartingCardIds()

        assertEquals(startingCards.map { it.id }.toSet(), startingIds)
    }

    // ========== getGroupIndex tests ==========

    @Test
    fun `getGroupIndex returns 0 for Major cards`() {
        val majorCards = ScaleDeck.getAllCards().filter { it.scale == ScaleType.MAJOR }
        majorCards.forEach { card ->
            assertEquals(0, ScaleDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns 1 for Natural Minor cards`() {
        val naturalMinorCards = ScaleDeck.getAllCards().filter { it.scale == ScaleType.NATURAL_MINOR }
        naturalMinorCards.forEach { card ->
            assertEquals(1, ScaleDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns 9 for Lydian cards`() {
        val lydianCards = ScaleDeck.getAllCards().filter { it.scale == ScaleType.LYDIAN }
        lydianCards.forEach { card ->
            assertEquals(9, ScaleDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns correct index for all cards`() {
        val allCards = ScaleDeck.getAllCards()
        allCards.forEach { card ->
            val expectedIndex = ScaleDeck.UNLOCK_ORDER.indexOf(card.scale)
            assertEquals(
                "Card ${card.id} should be in group $expectedIndex",
                expectedIndex,
                ScaleDeck.getGroupIndex(card)
            )
        }
    }

    // ========== getGroupName tests ==========

    @Test
    fun `getGroupName returns scale name for valid indices`() {
        assertEquals("Major", ScaleDeck.getGroupName(0))
        assertEquals("Natural Minor", ScaleDeck.getGroupName(1))
        assertEquals("Lydian", ScaleDeck.getGroupName(9))
    }

    @Test
    fun `getGroupName returns Unknown for invalid indices`() {
        assertEquals("Unknown", ScaleDeck.getGroupName(-1))
        assertEquals("Unknown", ScaleDeck.getGroupName(10))
        assertEquals("Unknown", ScaleDeck.getGroupName(100))
    }

    // ========== Unlock progression tests ==========

    @Test
    fun `familiar scales unlock before exotic modes`() {
        val majorIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.MAJOR)
        val naturalMinorIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.NATURAL_MINOR)
        val phrygianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.PHRYGIAN)
        val lydianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.LYDIAN)

        assertTrue("Major should unlock before Phrygian", majorIndex < phrygianIndex)
        assertTrue("Natural Minor should unlock before Lydian", naturalMinorIndex < lydianIndex)
    }

    @Test
    fun `basic minor scales unlock before modes`() {
        val naturalMinorIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.NATURAL_MINOR)
        val harmonicMinorIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.HARMONIC_MINOR)
        val dorianIndex = ScaleDeck.UNLOCK_ORDER.indexOf(ScaleType.DORIAN)

        assertTrue("Natural Minor should unlock before Dorian", naturalMinorIndex < dorianIndex)
        assertTrue("Natural Minor should unlock before Harmonic Minor", naturalMinorIndex < harmonicMinorIndex)
    }
}
