package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class CardTest {

    @Test
    fun `card id has correct format`() {
        // Test various ID formats across chord types, octaves, and modes
        assertEquals("MAJOR_4_ARPEGGIATED", Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED).id)
        assertEquals("MIN7_3_BLOCK", Card(ChordType.MIN7, 3, PlaybackMode.BLOCK).id)
        assertEquals("SUS2_3_ARPEGGIATED", Card(ChordType.SUS2, 3, PlaybackMode.ARPEGGIATED).id)
        assertEquals("SUS2_4_ARPEGGIATED", Card(ChordType.SUS2, 4, PlaybackMode.ARPEGGIATED).id)
        assertEquals("SUS2_5_ARPEGGIATED", Card(ChordType.SUS2, 5, PlaybackMode.ARPEGGIATED).id)
    }

    @Test
    fun `cards with same properties have same id`() {
        val card1 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val card2 = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        assertEquals(card1, card2)
        assertEquals(card1.id, card2.id)
    }

    @Test
    fun `cards with different properties have different ids`() {
        val base = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val differentChordType = Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED)
        val differentOctave = Card(ChordType.MAJOR, 5, PlaybackMode.ARPEGGIATED)
        val differentMode = Card(ChordType.MAJOR, 4, PlaybackMode.BLOCK)

        assertNotEquals(base, differentChordType)
        assertNotEquals(base.id, differentChordType.id)
        assertNotEquals(base, differentOctave)
        assertNotEquals(base.id, differentOctave.id)
        assertNotEquals(base, differentMode)
        assertNotEquals(base.id, differentMode.id)
    }
}
