package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class FunctionCardTest {

    // ========== ID format tests ==========

    @Test
    fun `card id has correct format`() {
        val card = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", card.id)
    }

    @Test
    fun `card id with minor key`() {
        val card = FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 3, PlaybackMode.BLOCK)
        assertEquals("iv_MINOR_3_BLOCK", card.id)
    }

    @Test
    fun `card id with diminished function`() {
        val card = FunctionCard(ChordFunction.vii_dim, KeyQuality.MAJOR, 5, PlaybackMode.ARPEGGIATED)
        assertEquals("vii_dim_MAJOR_5_ARPEGGIATED", card.id)
    }

    // ========== fromId parsing tests ==========

    @Test
    fun `fromId parses IV_MAJOR_4_ARPEGGIATED correctly`() {
        val card = FunctionCard.fromId("IV_MAJOR_4_ARPEGGIATED")

        assertNotNull(card)
        assertEquals(ChordFunction.IV, card!!.function)
        assertEquals(KeyQuality.MAJOR, card.keyQuality)
        assertEquals(4, card.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, card.playbackMode)
    }

    @Test
    fun `fromId parses minor key card correctly`() {
        val card = FunctionCard.fromId("iv_MINOR_3_BLOCK")

        assertNotNull(card)
        assertEquals(ChordFunction.iv, card!!.function)
        assertEquals(KeyQuality.MINOR, card.keyQuality)
        assertEquals(3, card.octave)
        assertEquals(PlaybackMode.BLOCK, card.playbackMode)
    }

    @Test
    fun `fromId parses diminished function correctly`() {
        val card = FunctionCard.fromId("vii_dim_MAJOR_5_ARPEGGIATED")

        assertNotNull(card)
        assertEquals(ChordFunction.vii_dim, card!!.function)
        assertEquals(KeyQuality.MAJOR, card.keyQuality)
        assertEquals(5, card.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, card.playbackMode)
    }

    @Test
    fun `fromId returns null for invalid string`() {
        assertNull(FunctionCard.fromId("invalid"))
    }

    @Test
    fun `fromId returns null for incomplete id`() {
        assertNull(FunctionCard.fromId("IV_MAJOR"))
    }

    @Test
    fun `fromId returns null for too many parts`() {
        assertNull(FunctionCard.fromId("IV_MAJOR_4_ARPEGGIATED_EXTRA"))
    }

    @Test
    fun `fromId returns null for invalid function`() {
        assertNull(FunctionCard.fromId("INVALID_MAJOR_4_ARPEGGIATED"))
    }

    @Test
    fun `fromId returns null for invalid key quality`() {
        assertNull(FunctionCard.fromId("IV_INVALID_4_ARPEGGIATED"))
    }

    @Test
    fun `fromId returns null for invalid octave`() {
        assertNull(FunctionCard.fromId("IV_MAJOR_X_ARPEGGIATED"))
    }

    @Test
    fun `fromId returns null for invalid playback mode`() {
        assertNull(FunctionCard.fromId("IV_MAJOR_4_INVALID"))
    }

    @Test
    fun `fromId returns null for empty string`() {
        assertNull(FunctionCard.fromId(""))
    }

    // ========== ID round-trip tests ==========

    @Test
    fun `card id round-trips correctly`() {
        val original = FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.BLOCK)
        val parsed = FunctionCard.fromId(original.id)

        assertNotNull(parsed)
        assertEquals(original, parsed)
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

    // ========== Display name tests ==========

    @Test
    fun `display name includes function display name`() {
        val card = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("IV"))
    }

    @Test
    fun `display name includes key quality lowercase`() {
        val card = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("major"))
    }

    @Test
    fun `display name includes octave`() {
        val card = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("4"))
    }

    @Test
    fun `display name includes playback mode lowercase`() {
        val card = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("arpeggiated"))
    }

    // ========== Equality tests ==========

    @Test
    fun `cards with same properties are equal`() {
        val card1 = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different functions are not equal`() {
        val card1 = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertNotEquals(card1, card2)
    }

    @Test
    fun `cards with different key qualities are not equal`() {
        val card1 = FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertNotEquals(card1, card2)
    }
}
