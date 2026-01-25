package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class CardTest {

    @Test
    fun `card id has correct format`() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals("MAJOR_4_ARPEGGIATED", card.id)
    }

    @Test
    fun `card id with different chord type`() {
        val card = Card(ChordType.MIN7, 3, PlaybackMode.BLOCK)
        assertEquals("MIN7_3_BLOCK", card.id)
    }

    @Test
    fun `card id with all octaves`() {
        val card3 = Card(ChordType.SUS2, 3, PlaybackMode.ARPEGGIATED)
        val card4 = Card(ChordType.SUS2, 4, PlaybackMode.ARPEGGIATED)
        val card5 = Card(ChordType.SUS2, 5, PlaybackMode.ARPEGGIATED)

        assertEquals("SUS2_3_ARPEGGIATED", card3.id)
        assertEquals("SUS2_4_ARPEGGIATED", card4.id)
        assertEquals("SUS2_5_ARPEGGIATED", card5.id)
    }

    @Test
    fun `card display name includes chord type display name`() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("Major"))
    }

    @Test
    fun `card display name includes octave`() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("4"))
    }

    @Test
    fun `card display name includes playback mode lowercase`() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertTrue(card.displayName.contains("arpeggiated"))
    }

    @Test
    fun `card display name for block mode`() {
        val card = Card(ChordType.MINOR, 5, PlaybackMode.BLOCK)
        assertTrue(card.displayName.contains("block"))
    }

    @Test
    fun `cards with same properties are equal`() {
        val card1 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different chord types are not equal`() {
        val card1 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different octaves are not equal`() {
        val card1 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = Card(ChordType.MAJOR, 5, PlaybackMode.ARPEGGIATED)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different playback modes are not equal`() {
        val card1 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = Card(ChordType.MAJOR, 4, PlaybackMode.BLOCK)
        assertNotEquals(card1, card2)
        assertNotEquals(card1.id, card2.id)
    }
}
