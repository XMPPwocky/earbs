package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class ProgressionCardTest {

    @Test
    fun `card id has correct format`() {
        assertEquals(
            "I_IV_I_4_ARPEGGIATED",
            ProgressionCard(ProgressionType.I_IV_I, 4, PlaybackMode.ARPEGGIATED).id
        )
        assertEquals(
            "I_V_vi_IV_3_BLOCK",
            ProgressionCard(ProgressionType.I_V_vi_IV, 3, PlaybackMode.BLOCK).id
        )
        assertEquals(
            "I_vi_ii_V_I_5_ARPEGGIATED",
            ProgressionCard(ProgressionType.I_vi_ii_V_I, 5, PlaybackMode.ARPEGGIATED).id
        )
    }

    @Test
    fun `fromId parses valid ids correctly`() {
        val testCases = listOf(
            "I_IV_I_4_ARPEGGIATED" to ProgressionCard(ProgressionType.I_IV_I, 4, PlaybackMode.ARPEGGIATED),
            "I_V_I_3_BLOCK" to ProgressionCard(ProgressionType.I_V_I, 3, PlaybackMode.BLOCK),
            "I_V_vi_IV_5_ARPEGGIATED" to ProgressionCard(ProgressionType.I_V_vi_IV, 5, PlaybackMode.ARPEGGIATED),
            "I_vi_ii_V_I_4_BLOCK" to ProgressionCard(ProgressionType.I_vi_ii_V_I, 4, PlaybackMode.BLOCK),
        )

        testCases.forEach { (id, expected) ->
            val parsed = ProgressionCard.fromId(id)
            assertNotNull("Failed to parse: $id", parsed)
            assertEquals(expected.progression, parsed!!.progression)
            assertEquals(expected.octave, parsed.octave)
            assertEquals(expected.playbackMode, parsed.playbackMode)
        }
    }

    @Test
    fun `fromId returns null for invalid input`() {
        val invalidInputs = listOf(
            "invalid",
            "I_IV",                              // incomplete
            "INVALID_4_ARPEGGIATED",             // invalid progression
            "I_IV_I_X_ARPEGGIATED",              // invalid octave
            "I_IV_I_4_INVALID",                  // invalid playback mode
            "",                                  // empty string
        )

        invalidInputs.forEach { input ->
            assertNull("Should return null for: '$input'", ProgressionCard.fromId(input))
        }
    }

    @Test
    fun `all progression cards round-trip correctly`() {
        val allCards = ProgressionDeck.getAllCards()

        allCards.forEach { original ->
            val parsed = ProgressionCard.fromId(original.id)
            assertNotNull("Failed to parse: ${original.id}", parsed)
            assertEquals(original, parsed)
        }
    }

    @Test
    fun `cards with same properties are equal`() {
        val card1 = ProgressionCard(ProgressionType.I_IV_V_I, 4, PlaybackMode.ARPEGGIATED)
        val card2 = ProgressionCard(ProgressionType.I_IV_V_I, 4, PlaybackMode.ARPEGGIATED)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different properties are not equal`() {
        val base = ProgressionCard(ProgressionType.I_IV_V_I, 4, PlaybackMode.ARPEGGIATED)
        val differentProgression = ProgressionCard(ProgressionType.I_V_I, 4, PlaybackMode.ARPEGGIATED)
        val differentOctave = ProgressionCard(ProgressionType.I_IV_V_I, 3, PlaybackMode.ARPEGGIATED)
        val differentMode = ProgressionCard(ProgressionType.I_IV_V_I, 4, PlaybackMode.BLOCK)

        assertNotEquals(base, differentProgression)
        assertNotEquals(base, differentOctave)
        assertNotEquals(base, differentMode)
    }

    @Test
    fun `displayName contains progression and octave`() {
        val card = ProgressionCard(ProgressionType.I_IV_V_I, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("I - IV - V - I"))
        assertTrue(card.displayName.contains("4"))
        assertTrue(card.displayName.contains("arpeggiated"))
    }

    @Test
    fun `all cards have unique ids`() {
        val allCards = ProgressionDeck.getAllCards()
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(allCards.size, uniqueIds.size)
    }
}
