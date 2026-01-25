package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class FunctionCardTest {

    @Test
    fun `card id has correct format`() {
        // Test various ID formats
        assertEquals("IV_MAJOR_4_ARPEGGIATED",
            FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED).id)
        assertEquals("iv_MINOR_3_BLOCK",
            FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 3, PlaybackMode.BLOCK).id)
        assertEquals("vii_dim_MAJOR_5_ARPEGGIATED",
            FunctionCard(ChordFunction.vii_dim, KeyQuality.MAJOR, 5, PlaybackMode.ARPEGGIATED).id)
    }

    @Test
    fun `fromId parses valid ids correctly`() {
        val testCases = listOf(
            "IV_MAJOR_4_ARPEGGIATED" to FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            "iv_MINOR_3_BLOCK" to FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 3, PlaybackMode.BLOCK),
            "vii_dim_MAJOR_5_ARPEGGIATED" to FunctionCard(ChordFunction.vii_dim, KeyQuality.MAJOR, 5, PlaybackMode.ARPEGGIATED),
        )

        testCases.forEach { (id, expected) ->
            val parsed = FunctionCard.fromId(id)
            assertNotNull("Failed to parse: $id", parsed)
            assertEquals(expected.function, parsed!!.function)
            assertEquals(expected.keyQuality, parsed.keyQuality)
            assertEquals(expected.octave, parsed.octave)
            assertEquals(expected.playbackMode, parsed.playbackMode)
        }
    }

    @Test
    fun `fromId returns null for invalid input`() {
        val invalidInputs = listOf(
            "invalid",
            "IV_MAJOR",              // incomplete
            "IV_MAJOR_4_ARPEGGIATED_EXTRA",  // too many parts
            "INVALID_MAJOR_4_ARPEGGIATED",   // invalid function
            "IV_INVALID_4_ARPEGGIATED",      // invalid key quality
            "IV_MAJOR_X_ARPEGGIATED",        // invalid octave
            "IV_MAJOR_4_INVALID",            // invalid playback mode
            "",                              // empty string
        )

        invalidInputs.forEach { input ->
            assertNull("Should return null for: '$input'", FunctionCard.fromId(input))
        }
    }

    @Test
    fun `all function cards round-trip correctly`() {
        val allCards = FunctionDeck.UNLOCK_ORDER.flatMap { it.toCards() }

        allCards.forEach { original ->
            val parsed = FunctionCard.fromId(original.id)
            assertNotNull("Failed to parse: ${original.id}", parsed)
            assertEquals(original, parsed)
        }
    }

    @Test
    fun `cards with same properties are equal`() {
        val card1 = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different properties are not equal`() {
        val base = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val differentFunction = FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val differentKeyQuality = FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)

        assertNotEquals(base, differentFunction)
        assertNotEquals(base, differentKeyQuality)
    }
}
