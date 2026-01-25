package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class ProgressionCardTest {

    @Test
    fun `card id has correct format for major progressions`() {
        assertEquals(
            "I_IV_I_MAJOR_4_ARPEGGIATED",
            ProgressionCard(ProgressionType.I_IV_I_MAJOR, 4, PlaybackMode.ARPEGGIATED).id
        )
        assertEquals(
            "I_V_vi_IV_MAJOR_3_BLOCK",
            ProgressionCard(ProgressionType.I_V_vi_IV_MAJOR, 3, PlaybackMode.BLOCK).id
        )
        assertEquals(
            "I_vi_ii_V_I_MAJOR_5_ARPEGGIATED",
            ProgressionCard(ProgressionType.I_vi_ii_V_I_MAJOR, 5, PlaybackMode.ARPEGGIATED).id
        )
    }

    @Test
    fun `card id has correct format for minor progressions`() {
        assertEquals(
            "i_iv_i_MINOR_4_ARPEGGIATED",
            ProgressionCard(ProgressionType.i_iv_i_MINOR, 4, PlaybackMode.ARPEGGIATED).id
        )
        assertEquals(
            "i_v_VI_iv_MINOR_3_BLOCK",
            ProgressionCard(ProgressionType.i_v_VI_iv_MINOR, 3, PlaybackMode.BLOCK).id
        )
        assertEquals(
            "i_VI_iio_v_i_MINOR_5_ARPEGGIATED",
            ProgressionCard(ProgressionType.i_VI_iio_v_i_MINOR, 5, PlaybackMode.ARPEGGIATED).id
        )
    }

    @Test
    fun `fromId parses valid major progression ids correctly`() {
        val testCases = listOf(
            "I_IV_I_MAJOR_4_ARPEGGIATED" to ProgressionCard(ProgressionType.I_IV_I_MAJOR, 4, PlaybackMode.ARPEGGIATED),
            "I_V_I_MAJOR_3_BLOCK" to ProgressionCard(ProgressionType.I_V_I_MAJOR, 3, PlaybackMode.BLOCK),
            "I_V_vi_IV_MAJOR_5_ARPEGGIATED" to ProgressionCard(ProgressionType.I_V_vi_IV_MAJOR, 5, PlaybackMode.ARPEGGIATED),
            "I_vi_ii_V_I_MAJOR_4_BLOCK" to ProgressionCard(ProgressionType.I_vi_ii_V_I_MAJOR, 4, PlaybackMode.BLOCK),
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
    fun `fromId parses valid minor progression ids correctly`() {
        val testCases = listOf(
            "i_iv_i_MINOR_4_ARPEGGIATED" to ProgressionCard(ProgressionType.i_iv_i_MINOR, 4, PlaybackMode.ARPEGGIATED),
            "i_v_i_MINOR_3_BLOCK" to ProgressionCard(ProgressionType.i_v_i_MINOR, 3, PlaybackMode.BLOCK),
            "i_v_VI_iv_MINOR_5_ARPEGGIATED" to ProgressionCard(ProgressionType.i_v_VI_iv_MINOR, 5, PlaybackMode.ARPEGGIATED),
            "i_VI_iio_v_i_MINOR_4_BLOCK" to ProgressionCard(ProgressionType.i_VI_iio_v_i_MINOR, 4, PlaybackMode.BLOCK),
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
            "I_IV",                                   // incomplete
            "INVALID_MAJOR_4_ARPEGGIATED",            // invalid progression
            "I_IV_I_MAJOR_X_ARPEGGIATED",             // invalid octave
            "I_IV_I_MAJOR_4_INVALID",                 // invalid playback mode
            "",                                       // empty string
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
        val card1 = ProgressionCard(ProgressionType.I_IV_V_I_MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = ProgressionCard(ProgressionType.I_IV_V_I_MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different properties are not equal`() {
        val base = ProgressionCard(ProgressionType.I_IV_V_I_MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val differentProgression = ProgressionCard(ProgressionType.I_V_I_MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val differentOctave = ProgressionCard(ProgressionType.I_IV_V_I_MAJOR, 3, PlaybackMode.ARPEGGIATED)
        val differentMode = ProgressionCard(ProgressionType.I_IV_V_I_MAJOR, 4, PlaybackMode.BLOCK)

        assertNotEquals(base, differentProgression)
        assertNotEquals(base, differentOctave)
        assertNotEquals(base, differentMode)
    }

    @Test
    fun `displayName contains progression and octave`() {
        val card = ProgressionCard(ProgressionType.I_IV_V_I_MAJOR, 4, PlaybackMode.ARPEGGIATED)
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

    @Test
    fun `major and minor cards with same pattern are different`() {
        val major = ProgressionCard(ProgressionType.I_IV_I_MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val minor = ProgressionCard(ProgressionType.i_iv_i_MINOR, 4, PlaybackMode.ARPEGGIATED)

        assertNotEquals(major, minor)
        assertNotEquals(major.id, minor.id)
    }
}
