package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType
import org.junit.Assert.*
import org.junit.Test

class IntervalDeckTest {

    // ========== UNLOCK_ORDER structure tests ==========

    @Test
    fun `UNLOCK_ORDER has 12 intervals`() {
        assertEquals(12, IntervalDeck.UNLOCK_ORDER.size)
    }

    @Test
    fun `UNLOCK_ORDER contains all interval types`() {
        val unlockIntervals = IntervalDeck.UNLOCK_ORDER.toSet()
        val allIntervals = IntervalType.entries.toSet()
        assertEquals(allIntervals, unlockIntervals)
    }

    @Test
    fun `UNLOCK_ORDER has no duplicates`() {
        val uniqueIntervals = IntervalDeck.UNLOCK_ORDER.toSet()
        assertEquals(IntervalDeck.UNLOCK_ORDER.size, uniqueIntervals.size)
    }

    // ========== Difficulty ordering tests ==========

    @Test
    fun `first interval is Perfect 5th (easiest)`() {
        assertEquals(IntervalType.PERFECT_5TH, IntervalDeck.UNLOCK_ORDER[0])
    }

    @Test
    fun `last interval is Tritone (hardest)`() {
        assertEquals(IntervalType.TRITONE, IntervalDeck.UNLOCK_ORDER.last())
    }

    @Test
    fun `octave is second to unlock`() {
        assertEquals(IntervalType.OCTAVE, IntervalDeck.UNLOCK_ORDER[1])
    }

    @Test
    fun `thirds unlock early (groups 2-3)`() {
        assertEquals(IntervalType.MAJOR_3RD, IntervalDeck.UNLOCK_ORDER[2])
        assertEquals(IntervalType.MINOR_3RD, IntervalDeck.UNLOCK_ORDER[3])
    }

    @Test
    fun `seconds unlock mid-range (groups 5-6)`() {
        assertEquals(IntervalType.MAJOR_2ND, IntervalDeck.UNLOCK_ORDER[5])
        assertEquals(IntervalType.MINOR_2ND, IntervalDeck.UNLOCK_ORDER[6])
    }

    @Test
    fun `sevenths unlock late (groups 9-10)`() {
        assertEquals(IntervalType.MINOR_7TH, IntervalDeck.UNLOCK_ORDER[9])
        assertEquals(IntervalType.MAJOR_7TH, IntervalDeck.UNLOCK_ORDER[10])
    }

    // ========== Card count tests ==========

    @Test
    fun `TOTAL_CARDS equals 108`() {
        // 12 intervals × 3 octaves × 3 directions = 108
        assertEquals(108, IntervalDeck.TOTAL_CARDS)
    }

    @Test
    fun `CARDS_PER_GROUP equals 9`() {
        // 1 interval × 3 octaves × 3 directions = 9
        assertEquals(9, IntervalDeck.CARDS_PER_GROUP)
    }

    @Test
    fun `MAX_UNLOCK_LEVEL equals 11`() {
        // 0-indexed, 12 groups total
        assertEquals(11, IntervalDeck.MAX_UNLOCK_LEVEL)
    }

    // ========== getAllCards tests ==========

    @Test
    fun `getAllCards returns 108 cards`() {
        assertEquals(108, IntervalDeck.getAllCards().size)
    }

    @Test
    fun `getAllCards covers all intervals`() {
        val allCards = IntervalDeck.getAllCards()
        val intervalsUsed = allCards.map { it.interval }.toSet()
        assertEquals(IntervalType.entries.toSet(), intervalsUsed)
    }

    @Test
    fun `getAllCards covers all octaves`() {
        val allCards = IntervalDeck.getAllCards()
        val octavesUsed = allCards.map { it.octave }.toSet()
        assertEquals(setOf(3, 4, 5), octavesUsed)
    }

    @Test
    fun `getAllCards covers all directions`() {
        val allCards = IntervalDeck.getAllCards()
        val directionsUsed = allCards.map { it.direction }.toSet()
        assertEquals(IntervalDirection.entries.toSet(), directionsUsed)
    }

    @Test
    fun `getAllCards has unique card ids`() {
        val allCards = IntervalDeck.getAllCards()
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(allCards.size, uniqueIds.size)
    }

    @Test
    fun `each interval appears 9 times (3 octaves x 3 directions)`() {
        val allCards = IntervalDeck.getAllCards()
        val cardsByInterval = allCards.groupBy { it.interval }

        IntervalType.entries.forEach { interval ->
            assertEquals(
                "Interval $interval should appear 9 times",
                9,
                cardsByInterval[interval]?.size
            )
        }
    }

    // ========== getStartingCards tests ==========

    @Test
    fun `getStartingCards returns 3 cards`() {
        assertEquals(3, IntervalDeck.getStartingCards().size)
    }

    @Test
    fun `starting cards are Perfect 5th at octave 4`() {
        val startingCards = IntervalDeck.getStartingCards()
        startingCards.forEach { card ->
            assertEquals(IntervalType.PERFECT_5TH, card.interval)
            assertEquals(4, card.octave)
        }
    }

    @Test
    fun `starting cards cover all 3 directions`() {
        val startingCards = IntervalDeck.getStartingCards()
        val directions = startingCards.map { it.direction }.toSet()
        assertEquals(IntervalDirection.entries.toSet(), directions)
    }

    @Test
    fun `getStartingCardIds matches getStartingCards ids`() {
        val startingCards = IntervalDeck.getStartingCards()
        val startingIds = IntervalDeck.getStartingCardIds()

        assertEquals(startingCards.map { it.id }.toSet(), startingIds)
    }

    // ========== getGroupIndex tests ==========

    @Test
    fun `getGroupIndex returns 0 for Perfect 5th cards`() {
        val p5Cards = IntervalDeck.getAllCards().filter { it.interval == IntervalType.PERFECT_5TH }
        p5Cards.forEach { card ->
            assertEquals(0, IntervalDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns 1 for Octave cards`() {
        val octaveCards = IntervalDeck.getAllCards().filter { it.interval == IntervalType.OCTAVE }
        octaveCards.forEach { card ->
            assertEquals(1, IntervalDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns 11 for Tritone cards`() {
        val tritoneCards = IntervalDeck.getAllCards().filter { it.interval == IntervalType.TRITONE }
        tritoneCards.forEach { card ->
            assertEquals(11, IntervalDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns correct index for all cards`() {
        val allCards = IntervalDeck.getAllCards()
        allCards.forEach { card ->
            val expectedIndex = IntervalDeck.UNLOCK_ORDER.indexOf(card.interval)
            assertEquals(
                "Card ${card.id} should be in group $expectedIndex",
                expectedIndex,
                IntervalDeck.getGroupIndex(card)
            )
        }
    }

    // ========== getGroupName tests ==========

    @Test
    fun `getGroupName returns interval name for valid indices`() {
        assertEquals("Perfect 5th", IntervalDeck.getGroupName(0))
        assertEquals("Octave", IntervalDeck.getGroupName(1))
        assertEquals("Major 3rd", IntervalDeck.getGroupName(2))
        assertEquals("Tritone", IntervalDeck.getGroupName(11))
    }

    @Test
    fun `getGroupName returns Unknown for invalid indices`() {
        assertEquals("Unknown", IntervalDeck.getGroupName(-1))
        assertEquals("Unknown", IntervalDeck.getGroupName(12))
        assertEquals("Unknown", IntervalDeck.getGroupName(100))
    }

    // ========== Unlock progression tests ==========

    @Test
    fun `consonant intervals unlock before dissonant intervals`() {
        // Perfect 5th and Octave (most consonant) unlock first
        val p5Index = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.PERFECT_5TH)
        val octaveIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.OCTAVE)
        val tritoneIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.TRITONE)
        val minor2ndIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.MINOR_2ND)

        assertTrue("Perfect 5th should unlock before Tritone", p5Index < tritoneIndex)
        assertTrue("Octave should unlock before Minor 2nd", octaveIndex < minor2ndIndex)
    }

    @Test
    fun `major and minor thirds unlock before sixths`() {
        val major3rdIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.MAJOR_3RD)
        val minor3rdIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.MINOR_3RD)
        val major6thIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.MAJOR_6TH)
        val minor6thIndex = IntervalDeck.UNLOCK_ORDER.indexOf(IntervalType.MINOR_6TH)

        assertTrue("Major 3rd should unlock before Major 6th", major3rdIndex < major6thIndex)
        assertTrue("Minor 3rd should unlock before Minor 6th", minor3rdIndex < minor6thIndex)
    }
}
