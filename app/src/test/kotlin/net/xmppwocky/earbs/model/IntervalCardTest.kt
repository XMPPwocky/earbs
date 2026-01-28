package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType
import org.junit.Assert.*
import org.junit.Test

class IntervalCardTest {

    // ========== Card ID format tests ==========

    @Test
    fun `card id has correct format for ascending intervals`() {
        assertEquals(
            "PERFECT_5TH_4_ASCENDING",
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING).id
        )
        assertEquals(
            "MAJOR_3RD_3_ASCENDING",
            IntervalCard(IntervalType.MAJOR_3RD, 3, IntervalDirection.ASCENDING).id
        )
        assertEquals(
            "MINOR_2ND_5_ASCENDING",
            IntervalCard(IntervalType.MINOR_2ND, 5, IntervalDirection.ASCENDING).id
        )
    }

    @Test
    fun `card id has correct format for descending intervals`() {
        assertEquals(
            "PERFECT_5TH_4_DESCENDING",
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.DESCENDING).id
        )
        assertEquals(
            "OCTAVE_3_DESCENDING",
            IntervalCard(IntervalType.OCTAVE, 3, IntervalDirection.DESCENDING).id
        )
        assertEquals(
            "TRITONE_5_DESCENDING",
            IntervalCard(IntervalType.TRITONE, 5, IntervalDirection.DESCENDING).id
        )
    }

    @Test
    fun `card id has correct format for harmonic intervals`() {
        assertEquals(
            "PERFECT_5TH_4_HARMONIC",
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.HARMONIC).id
        )
        assertEquals(
            "MINOR_7TH_3_HARMONIC",
            IntervalCard(IntervalType.MINOR_7TH, 3, IntervalDirection.HARMONIC).id
        )
        assertEquals(
            "MAJOR_6TH_5_HARMONIC",
            IntervalCard(IntervalType.MAJOR_6TH, 5, IntervalDirection.HARMONIC).id
        )
    }

    // ========== fromId parsing tests ==========

    @Test
    fun `fromId parses valid ascending interval ids correctly`() {
        val testCases = listOf(
            "PERFECT_5TH_4_ASCENDING" to IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING),
            "MAJOR_3RD_3_ASCENDING" to IntervalCard(IntervalType.MAJOR_3RD, 3, IntervalDirection.ASCENDING),
            "MINOR_2ND_5_ASCENDING" to IntervalCard(IntervalType.MINOR_2ND, 5, IntervalDirection.ASCENDING),
            "OCTAVE_4_ASCENDING" to IntervalCard(IntervalType.OCTAVE, 4, IntervalDirection.ASCENDING),
        )

        testCases.forEach { (id, expected) ->
            val parsed = IntervalCard.fromId(id)
            assertEquals("Failed to parse: $id - interval", expected.interval, parsed.interval)
            assertEquals("Failed to parse: $id - octave", expected.octave, parsed.octave)
            assertEquals("Failed to parse: $id - direction", expected.direction, parsed.direction)
        }
    }

    @Test
    fun `fromId parses valid descending interval ids correctly`() {
        val testCases = listOf(
            "PERFECT_5TH_4_DESCENDING" to IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.DESCENDING),
            "TRITONE_3_DESCENDING" to IntervalCard(IntervalType.TRITONE, 3, IntervalDirection.DESCENDING),
            "MAJOR_7TH_5_DESCENDING" to IntervalCard(IntervalType.MAJOR_7TH, 5, IntervalDirection.DESCENDING),
        )

        testCases.forEach { (id, expected) ->
            val parsed = IntervalCard.fromId(id)
            assertEquals("Failed to parse: $id - interval", expected.interval, parsed.interval)
            assertEquals("Failed to parse: $id - octave", expected.octave, parsed.octave)
            assertEquals("Failed to parse: $id - direction", expected.direction, parsed.direction)
        }
    }

    @Test
    fun `fromId parses valid harmonic interval ids correctly`() {
        val testCases = listOf(
            "PERFECT_5TH_4_HARMONIC" to IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.HARMONIC),
            "MINOR_6TH_3_HARMONIC" to IntervalCard(IntervalType.MINOR_6TH, 3, IntervalDirection.HARMONIC),
            "PERFECT_4TH_5_HARMONIC" to IntervalCard(IntervalType.PERFECT_4TH, 5, IntervalDirection.HARMONIC),
        )

        testCases.forEach { (id, expected) ->
            val parsed = IntervalCard.fromId(id)
            assertEquals("Failed to parse: $id - interval", expected.interval, parsed.interval)
            assertEquals("Failed to parse: $id - octave", expected.octave, parsed.octave)
            assertEquals("Failed to parse: $id - direction", expected.direction, parsed.direction)
        }
    }

    @Test
    fun `fromId throws for invalid input`() {
        val invalidInputs = listOf(
            "invalid",
            "PERFECT_5TH",                         // incomplete
            "INVALID_INTERVAL_4_ASCENDING",        // invalid interval
            "PERFECT_5TH_X_ASCENDING",             // invalid octave
            "PERFECT_5TH_4_INVALID",               // invalid direction
            "",                                     // empty string
            "PERFECT_5TH_4",                       // missing direction
            "4_ASCENDING",                         // missing interval
        )

        invalidInputs.forEach { input ->
            try {
                IntervalCard.fromId(input)
                fail("Should throw for: '$input'")
            } catch (e: Exception) {
                // Expected - either IllegalArgumentException or NumberFormatException
            }
        }
    }

    @Test
    fun `all interval cards round-trip correctly`() {
        val allCards = IntervalDeck.getAllCards()

        allCards.forEach { original ->
            val parsed = IntervalCard.fromId(original.id)
            assertEquals(original.interval, parsed.interval)
            assertEquals(original.octave, parsed.octave)
            assertEquals(original.direction, parsed.direction)
            assertEquals(original.id, parsed.id)
        }
    }

    // ========== Equality tests ==========

    @Test
    fun `cards with same properties are equal`() {
        val card1 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        val card2 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different intervals are not equal`() {
        val card1 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        val card2 = IntervalCard(IntervalType.PERFECT_4TH, 4, IntervalDirection.ASCENDING)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different octaves are not equal`() {
        val card1 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        val card2 = IntervalCard(IntervalType.PERFECT_5TH, 3, IntervalDirection.ASCENDING)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different directions are not equal`() {
        val card1 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        val card2 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.DESCENDING)
        val card3 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.HARMONIC)
        assertNotEquals(card1, card2)
        assertNotEquals(card1, card3)
        assertNotEquals(card2, card3)
    }

    // ========== displayName tests ==========

    @Test
    fun `displayName is the interval display name`() {
        val card = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        assertEquals("Perfect 5th", card.displayName)
    }

    @Test
    fun `displayName varies by interval type`() {
        val card1 = IntervalCard(IntervalType.MAJOR_3RD, 4, IntervalDirection.ASCENDING)
        val card2 = IntervalCard(IntervalType.MINOR_7TH, 4, IntervalDirection.ASCENDING)

        assertEquals("Major 3rd", card1.displayName)
        assertEquals("Minor 7th", card2.displayName)
    }

    @Test
    fun `displayName is same regardless of direction`() {
        val ascending = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        val descending = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.DESCENDING)
        val harmonic = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.HARMONIC)

        assertEquals(ascending.displayName, descending.displayName)
        assertEquals(ascending.displayName, harmonic.displayName)
    }

    // ========== GameCard interface tests ==========

    @Test
    fun `id property returns correct format`() {
        val card = IntervalCard(IntervalType.MAJOR_3RD, 4, IntervalDirection.ASCENDING)
        assertEquals("MAJOR_3RD_4_ASCENDING", card.id)
    }

    @Test
    fun `octave property returns correct value`() {
        val card3 = IntervalCard(IntervalType.PERFECT_5TH, 3, IntervalDirection.ASCENDING)
        val card4 = IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING)
        val card5 = IntervalCard(IntervalType.PERFECT_5TH, 5, IntervalDirection.ASCENDING)

        assertEquals(3, card3.octave)
        assertEquals(4, card4.octave)
        assertEquals(5, card5.octave)
    }

    // ========== All cards unique test ==========

    @Test
    fun `all generated cards have unique ids`() {
        val allCards = IntervalDeck.getAllCards()
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(allCards.size, uniqueIds.size)
    }

    // ========== Interval type coverage ==========

    @Test
    fun `cards exist for all 12 intervals`() {
        val allCards = IntervalDeck.getAllCards()
        val intervalsUsed = allCards.map { it.interval }.toSet()
        assertEquals(12, intervalsUsed.size)
        assertEquals(IntervalType.entries.toSet(), intervalsUsed)
    }
}
