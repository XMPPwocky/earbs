package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class DeckTest {

    @Test
    fun `UNLOCK_ORDER generates 48 unique cards`() {
        val allCards = Deck.UNLOCK_ORDER.flatMap { it.toCards() }
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(48, allCards.size)
        assertEquals(48, uniqueIds.size)
    }

    @Test
    fun `all octaves and playback modes are covered`() {
        val octaves = Deck.UNLOCK_ORDER.map { it.octave }.toSet()
        val modes = Deck.UNLOCK_ORDER.map { it.playbackMode }.toSet()

        assertEquals(setOf(3, 4, 5), octaves)
        assertEquals(setOf(PlaybackMode.ARPEGGIATED, PlaybackMode.BLOCK), modes)
    }

    @Test
    fun `first unlock group is starting deck`() {
        val firstGroup = Deck.UNLOCK_ORDER[0]
        assertEquals(ChordType.TRIADS, firstGroup.chordTypes)
        assertEquals(4, firstGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, firstGroup.playbackMode)

        // STARTING_CARDS should match first group
        assertEquals(Deck.STARTING_CARDS, firstGroup.toCards())
    }

    @Test
    fun `UnlockGroup requires exactly 4 chord types`() {
        assertThrows(IllegalArgumentException::class.java) {
            UnlockGroup(listOf(ChordType.MAJOR, ChordType.MINOR), 4, PlaybackMode.ARPEGGIATED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            UnlockGroup(
                listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4, ChordType.DOM7),
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    @Test
    fun `TOTAL_CARDS equals sum of all group cards`() {
        val totalFromGroups = Deck.UNLOCK_ORDER.sumOf { it.chordTypes.size }
        assertEquals(Deck.TOTAL_CARDS, totalFromGroups)
    }

    @Test
    fun `toCards generates correct Card objects`() {
        val group = UnlockGroup(ChordType.TRIADS, 4, PlaybackMode.ARPEGGIATED)
        val cards = group.toCards()

        assertEquals(4, cards.size)
        cards.forEach { card ->
            assertEquals(4, card.octave)
            assertEquals(PlaybackMode.ARPEGGIATED, card.playbackMode)
            assertTrue(ChordType.TRIADS.contains(card.chordType))
        }
    }
}
