package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class DeckTest {

    // ========== UNLOCK_ORDER structure tests ==========

    @Test
    fun `UNLOCK_ORDER has 12 groups`() {
        assertEquals(12, Deck.UNLOCK_ORDER.size)
    }

    @Test
    fun `each unlock group has exactly 4 chord types`() {
        Deck.UNLOCK_ORDER.forEach { group ->
            assertEquals(4, group.chordTypes.size)
        }
    }

    @Test
    fun `first group is triads at octave 4 arpeggiated`() {
        val firstGroup = Deck.UNLOCK_ORDER[0]
        assertEquals(ChordType.TRIADS, firstGroup.chordTypes)
        assertEquals(4, firstGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, firstGroup.playbackMode)
    }

    @Test
    fun `first group contains Major, Minor, Sus2, Sus4`() {
        val firstGroup = Deck.UNLOCK_ORDER[0]
        assertTrue(firstGroup.chordTypes.contains(ChordType.MAJOR))
        assertTrue(firstGroup.chordTypes.contains(ChordType.MINOR))
        assertTrue(firstGroup.chordTypes.contains(ChordType.SUS2))
        assertTrue(firstGroup.chordTypes.contains(ChordType.SUS4))
    }

    @Test
    fun `second group is triads at octave 4 block`() {
        val secondGroup = Deck.UNLOCK_ORDER[1]
        assertEquals(ChordType.TRIADS, secondGroup.chordTypes)
        assertEquals(4, secondGroup.octave)
        assertEquals(PlaybackMode.BLOCK, secondGroup.playbackMode)
    }

    @Test
    fun `seventh group is sevenths at octave 4 arpeggiated`() {
        val seventhGroup = Deck.UNLOCK_ORDER[6]
        assertEquals(ChordType.SEVENTHS, seventhGroup.chordTypes)
        assertEquals(4, seventhGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, seventhGroup.playbackMode)
    }

    @Test
    fun `last group is sevenths at octave 5 block`() {
        val lastGroup = Deck.UNLOCK_ORDER[11]
        assertEquals(ChordType.SEVENTHS, lastGroup.chordTypes)
        assertEquals(5, lastGroup.octave)
        assertEquals(PlaybackMode.BLOCK, lastGroup.playbackMode)
    }

    // ========== TOTAL_CARDS test ==========

    @Test
    fun `TOTAL_CARDS equals 48`() {
        assertEquals(48, Deck.TOTAL_CARDS)
    }

    @Test
    fun `TOTAL_CARDS equals sum of all group cards`() {
        val totalFromGroups = Deck.UNLOCK_ORDER.sumOf { it.chordTypes.size }
        assertEquals(Deck.TOTAL_CARDS, totalFromGroups)
    }

    // ========== MAX_UNLOCK_LEVEL test ==========

    @Test
    fun `MAX_UNLOCK_LEVEL is 11`() {
        assertEquals(11, Deck.MAX_UNLOCK_LEVEL)
    }

    // ========== STARTING_CARDS tests ==========

    @Test
    fun `STARTING_CARDS has 4 cards`() {
        assertEquals(4, Deck.STARTING_CARDS.size)
    }

    @Test
    fun `STARTING_CARDS are all at octave 4`() {
        Deck.STARTING_CARDS.forEach { card ->
            assertEquals(4, card.octave)
        }
    }

    @Test
    fun `STARTING_CARDS are all arpeggiated`() {
        Deck.STARTING_CARDS.forEach { card ->
            assertEquals(PlaybackMode.ARPEGGIATED, card.playbackMode)
        }
    }

    @Test
    fun `STARTING_CARDS contain triads only`() {
        val startingTypes = Deck.STARTING_CARDS.map { it.chordType }
        assertTrue(startingTypes.contains(ChordType.MAJOR))
        assertTrue(startingTypes.contains(ChordType.MINOR))
        assertTrue(startingTypes.contains(ChordType.SUS2))
        assertTrue(startingTypes.contains(ChordType.SUS4))
    }

    @Test
    fun `STARTING_CARDS match first unlock group cards`() {
        val firstGroupCards = Deck.UNLOCK_ORDER[0].toCards()
        assertEquals(Deck.STARTING_CARDS, firstGroupCards)
    }

    // ========== UnlockGroup.toCards tests ==========

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

    @Test
    fun `toCards preserves chord type order`() {
        val group = Deck.UNLOCK_ORDER[0]
        val cards = group.toCards()

        assertEquals(ChordType.MAJOR, cards[0].chordType)
        assertEquals(ChordType.MINOR, cards[1].chordType)
        assertEquals(ChordType.SUS2, cards[2].chordType)
        assertEquals(ChordType.SUS4, cards[3].chordType)
    }

    // ========== UnlockGroup validation tests ==========

    @Test
    fun `UnlockGroup requires exactly 4 chord types`() {
        assertThrows(IllegalArgumentException::class.java) {
            UnlockGroup(listOf(ChordType.MAJOR, ChordType.MINOR), 4, PlaybackMode.ARPEGGIATED)
        }
    }

    @Test
    fun `UnlockGroup with 5 chord types throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            UnlockGroup(
                listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4, ChordType.DOM7),
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    // ========== Octave coverage tests ==========

    @Test
    fun `all unlock groups cover octaves 3, 4, and 5`() {
        val octaves = Deck.UNLOCK_ORDER.map { it.octave }.toSet()
        assertEquals(setOf(3, 4, 5), octaves)
    }

    @Test
    fun `both playback modes are used`() {
        val modes = Deck.UNLOCK_ORDER.map { it.playbackMode }.toSet()
        assertEquals(setOf(PlaybackMode.ARPEGGIATED, PlaybackMode.BLOCK), modes)
    }

    // ========== Card uniqueness test ==========

    @Test
    fun `all generated cards are unique`() {
        val allCards = Deck.UNLOCK_ORDER.flatMap { it.toCards() }
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(48, uniqueIds.size)
    }
}
