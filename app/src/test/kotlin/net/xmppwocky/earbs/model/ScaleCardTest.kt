package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType
import org.junit.Assert.*
import org.junit.Test

class ScaleCardTest {

    // ========== Card ID format tests ==========

    @Test
    fun `card id has correct format for ascending scales`() {
        assertEquals(
            "MAJOR_4_ASCENDING",
            ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING).id
        )
        assertEquals(
            "DORIAN_3_ASCENDING",
            ScaleCard(ScaleType.DORIAN, 3, ScaleDirection.ASCENDING).id
        )
        assertEquals(
            "MAJOR_PENTATONIC_5_ASCENDING",
            ScaleCard(ScaleType.MAJOR_PENTATONIC, 5, ScaleDirection.ASCENDING).id
        )
    }

    @Test
    fun `card id has correct format for descending scales`() {
        assertEquals(
            "MAJOR_4_DESCENDING",
            ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.DESCENDING).id
        )
        assertEquals(
            "NATURAL_MINOR_3_DESCENDING",
            ScaleCard(ScaleType.NATURAL_MINOR, 3, ScaleDirection.DESCENDING).id
        )
        assertEquals(
            "MINOR_PENTATONIC_5_DESCENDING",
            ScaleCard(ScaleType.MINOR_PENTATONIC, 5, ScaleDirection.DESCENDING).id
        )
    }

    @Test
    fun `card id has correct format for both direction`() {
        assertEquals(
            "MAJOR_4_BOTH",
            ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.BOTH).id
        )
        assertEquals(
            "HARMONIC_MINOR_3_BOTH",
            ScaleCard(ScaleType.HARMONIC_MINOR, 3, ScaleDirection.BOTH).id
        )
        assertEquals(
            "LYDIAN_5_BOTH",
            ScaleCard(ScaleType.LYDIAN, 5, ScaleDirection.BOTH).id
        )
    }

    // ========== fromId parsing tests ==========

    @Test
    fun `fromId parses valid ascending scale ids correctly`() {
        val testCases = listOf(
            "MAJOR_4_ASCENDING" to ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING),
            "DORIAN_3_ASCENDING" to ScaleCard(ScaleType.DORIAN, 3, ScaleDirection.ASCENDING),
            "MAJOR_PENTATONIC_5_ASCENDING" to ScaleCard(ScaleType.MAJOR_PENTATONIC, 5, ScaleDirection.ASCENDING),
            "NATURAL_MINOR_4_ASCENDING" to ScaleCard(ScaleType.NATURAL_MINOR, 4, ScaleDirection.ASCENDING),
        )

        testCases.forEach { (id, expected) ->
            val parsed = ScaleCard.fromId(id)
            assertEquals("Failed to parse: $id - scale", expected.scale, parsed.scale)
            assertEquals("Failed to parse: $id - octave", expected.octave, parsed.octave)
            assertEquals("Failed to parse: $id - direction", expected.direction, parsed.direction)
        }
    }

    @Test
    fun `fromId parses valid descending scale ids correctly`() {
        val testCases = listOf(
            "MAJOR_4_DESCENDING" to ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.DESCENDING),
            "HARMONIC_MINOR_3_DESCENDING" to ScaleCard(ScaleType.HARMONIC_MINOR, 3, ScaleDirection.DESCENDING),
            "MINOR_PENTATONIC_5_DESCENDING" to ScaleCard(ScaleType.MINOR_PENTATONIC, 5, ScaleDirection.DESCENDING),
        )

        testCases.forEach { (id, expected) ->
            val parsed = ScaleCard.fromId(id)
            assertEquals("Failed to parse: $id - scale", expected.scale, parsed.scale)
            assertEquals("Failed to parse: $id - octave", expected.octave, parsed.octave)
            assertEquals("Failed to parse: $id - direction", expected.direction, parsed.direction)
        }
    }

    @Test
    fun `fromId parses valid both direction scale ids correctly`() {
        val testCases = listOf(
            "MAJOR_4_BOTH" to ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.BOTH),
            "MIXOLYDIAN_3_BOTH" to ScaleCard(ScaleType.MIXOLYDIAN, 3, ScaleDirection.BOTH),
            "MELODIC_MINOR_5_BOTH" to ScaleCard(ScaleType.MELODIC_MINOR, 5, ScaleDirection.BOTH),
        )

        testCases.forEach { (id, expected) ->
            val parsed = ScaleCard.fromId(id)
            assertEquals("Failed to parse: $id - scale", expected.scale, parsed.scale)
            assertEquals("Failed to parse: $id - octave", expected.octave, parsed.octave)
            assertEquals("Failed to parse: $id - direction", expected.direction, parsed.direction)
        }
    }

    @Test
    fun `fromId throws for invalid input`() {
        val invalidInputs = listOf(
            "invalid",
            "MAJOR",                               // incomplete
            "INVALID_SCALE_4_ASCENDING",           // invalid scale
            "MAJOR_X_ASCENDING",                   // invalid octave
            "MAJOR_4_INVALID",                     // invalid direction
            "",                                     // empty string
            "MAJOR_4",                             // missing direction
            "4_ASCENDING",                         // missing scale
        )

        invalidInputs.forEach { input ->
            try {
                ScaleCard.fromId(input)
                fail("Should throw for: '$input'")
            } catch (e: Exception) {
                // Expected - either IllegalArgumentException or NumberFormatException
            }
        }
    }

    @Test
    fun `all scale cards round-trip correctly`() {
        val allCards = ScaleDeck.getAllCards()

        allCards.forEach { original ->
            val parsed = ScaleCard.fromId(original.id)
            assertEquals(original.scale, parsed.scale)
            assertEquals(original.octave, parsed.octave)
            assertEquals(original.direction, parsed.direction)
            assertEquals(original.id, parsed.id)
        }
    }

    // ========== Equality tests ==========

    @Test
    fun `cards with same properties are equal`() {
        val card1 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        val card2 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different scales are not equal`() {
        val card1 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        val card2 = ScaleCard(ScaleType.NATURAL_MINOR, 4, ScaleDirection.ASCENDING)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different octaves are not equal`() {
        val card1 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        val card2 = ScaleCard(ScaleType.MAJOR, 3, ScaleDirection.ASCENDING)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different directions are not equal`() {
        val card1 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        val card2 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.DESCENDING)
        val card3 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.BOTH)
        assertNotEquals(card1, card2)
        assertNotEquals(card1, card3)
        assertNotEquals(card2, card3)
    }

    // ========== displayName tests ==========

    @Test
    fun `displayName is the scale display name`() {
        val card = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        assertEquals("Major", card.displayName)
    }

    @Test
    fun `displayName varies by scale type`() {
        val card1 = ScaleCard(ScaleType.DORIAN, 4, ScaleDirection.ASCENDING)
        val card2 = ScaleCard(ScaleType.MINOR_PENTATONIC, 4, ScaleDirection.ASCENDING)

        assertEquals("Dorian", card1.displayName)
        assertEquals("Minor Pentatonic", card2.displayName)
    }

    @Test
    fun `displayName is same regardless of direction`() {
        val ascending = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        val descending = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.DESCENDING)
        val both = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.BOTH)

        assertEquals(ascending.displayName, descending.displayName)
        assertEquals(ascending.displayName, both.displayName)
    }

    // ========== GameCard interface tests ==========

    @Test
    fun `id property returns correct format`() {
        val card = ScaleCard(ScaleType.DORIAN, 4, ScaleDirection.ASCENDING)
        assertEquals("DORIAN_4_ASCENDING", card.id)
    }

    @Test
    fun `octave property returns correct value`() {
        val card3 = ScaleCard(ScaleType.MAJOR, 3, ScaleDirection.ASCENDING)
        val card4 = ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING)
        val card5 = ScaleCard(ScaleType.MAJOR, 5, ScaleDirection.ASCENDING)

        assertEquals(3, card3.octave)
        assertEquals(4, card4.octave)
        assertEquals(5, card5.octave)
    }

    // ========== All cards unique test ==========

    @Test
    fun `all generated cards have unique ids`() {
        val allCards = ScaleDeck.getAllCards()
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(allCards.size, uniqueIds.size)
    }

    // ========== Scale type coverage ==========

    @Test
    fun `cards exist for all 10 scales`() {
        val allCards = ScaleDeck.getAllCards()
        val scalesUsed = allCards.map { it.scale }.toSet()
        assertEquals(10, scalesUsed.size)
        assertEquals(ScaleType.entries.toSet(), scalesUsed)
    }
}
