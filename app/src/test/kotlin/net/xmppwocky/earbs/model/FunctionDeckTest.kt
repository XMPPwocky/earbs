package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class FunctionDeckTest {

    // ========== UNLOCK_ORDER structure tests ==========

    @Test
    fun `UNLOCK_ORDER has 24 groups`() {
        assertEquals(24, FunctionDeck.UNLOCK_ORDER.size)
    }

    @Test
    fun `each unlock group has exactly 3 cards`() {
        FunctionDeck.UNLOCK_ORDER.forEach { group ->
            assertEquals(3, group.functions.size)
        }
    }

    @Test
    fun `CARDS_PER_GROUP is 3`() {
        assertEquals(3, FunctionDeck.CARDS_PER_GROUP)
    }

    @Test
    fun `first group is IV, V, vi at major key octave 4 arpeggiated`() {
        val firstGroup = FunctionDeck.UNLOCK_ORDER[0]
        assertEquals(KeyQuality.MAJOR, firstGroup.keyQuality)
        assertEquals(4, firstGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, firstGroup.playbackMode)

        val functions = firstGroup.functions
        assertTrue(functions.contains(ChordFunction.IV))
        assertTrue(functions.contains(ChordFunction.V))
        assertTrue(functions.contains(ChordFunction.vi))
    }

    @Test
    fun `second group is ii, iii, vii_dim at major key octave 4 arpeggiated`() {
        val secondGroup = FunctionDeck.UNLOCK_ORDER[1]
        assertEquals(KeyQuality.MAJOR, secondGroup.keyQuality)
        assertEquals(4, secondGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, secondGroup.playbackMode)

        val functions = secondGroup.functions
        assertTrue(functions.contains(ChordFunction.ii))
        assertTrue(functions.contains(ChordFunction.iii))
        assertTrue(functions.contains(ChordFunction.vii_dim))
    }

    @Test
    fun `groups 0-11 are major key`() {
        for (i in 0..11) {
            assertEquals(KeyQuality.MAJOR, FunctionDeck.UNLOCK_ORDER[i].keyQuality)
        }
    }

    @Test
    fun `groups 12-23 are minor key`() {
        for (i in 12..23) {
            assertEquals(KeyQuality.MINOR, FunctionDeck.UNLOCK_ORDER[i].keyQuality)
        }
    }

    @Test
    fun `first minor group is iv, v, VI at minor key octave 4 arpeggiated`() {
        val firstMinorGroup = FunctionDeck.UNLOCK_ORDER[12]
        assertEquals(KeyQuality.MINOR, firstMinorGroup.keyQuality)
        assertEquals(4, firstMinorGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, firstMinorGroup.playbackMode)

        val functions = firstMinorGroup.functions
        assertTrue(functions.contains(ChordFunction.iv))
        assertTrue(functions.contains(ChordFunction.v))
        assertTrue(functions.contains(ChordFunction.VI))
    }

    @Test
    fun `last group is minor secondary at octave 5 block`() {
        val lastGroup = FunctionDeck.UNLOCK_ORDER[23]
        assertEquals(KeyQuality.MINOR, lastGroup.keyQuality)
        assertEquals(5, lastGroup.octave)
        assertEquals(PlaybackMode.BLOCK, lastGroup.playbackMode)
    }

    // ========== TOTAL_CARDS test ==========

    @Test
    fun `TOTAL_CARDS equals 72`() {
        assertEquals(72, FunctionDeck.TOTAL_CARDS)
    }

    @Test
    fun `TOTAL_CARDS equals sum of all group cards`() {
        val totalFromGroups = FunctionDeck.UNLOCK_ORDER.sumOf { it.functions.size }
        assertEquals(FunctionDeck.TOTAL_CARDS, totalFromGroups)
    }

    // ========== MAX_UNLOCK_LEVEL test ==========

    @Test
    fun `MAX_UNLOCK_LEVEL is 23`() {
        assertEquals(23, FunctionDeck.MAX_UNLOCK_LEVEL)
    }

    // ========== FunctionUnlockGroup.toCards tests ==========

    @Test
    fun `toCards generates correct FunctionCard objects`() {
        val group = FunctionDeck.UNLOCK_ORDER[0]
        val cards = group.toCards()

        assertEquals(3, cards.size)
        cards.forEach { card ->
            assertEquals(4, card.octave)
            assertEquals(PlaybackMode.ARPEGGIATED, card.playbackMode)
            assertEquals(KeyQuality.MAJOR, card.keyQuality)
        }
    }

    @Test
    fun `toCards preserves function order`() {
        val group = FunctionDeck.UNLOCK_ORDER[0]
        val cards = group.toCards()

        assertEquals(ChordFunction.IV, cards[0].function)
        assertEquals(ChordFunction.V, cards[1].function)
        assertEquals(ChordFunction.vi, cards[2].function)
    }

    // ========== FunctionUnlockGroup validation tests ==========

    @Test
    fun `FunctionUnlockGroup requires exactly 3 functions`() {
        assertThrows(IllegalArgumentException::class.java) {
            FunctionUnlockGroup(
                listOf(ChordFunction.IV, ChordFunction.V),
                KeyQuality.MAJOR,
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    @Test
    fun `FunctionUnlockGroup with 4 functions throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            FunctionUnlockGroup(
                listOf(ChordFunction.IV, ChordFunction.V, ChordFunction.vi, ChordFunction.ii),
                KeyQuality.MAJOR,
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    @Test
    fun `FunctionUnlockGroup requires matching key quality`() {
        assertThrows(IllegalArgumentException::class.java) {
            // Minor functions with MAJOR key quality should fail
            FunctionUnlockGroup(
                listOf(ChordFunction.iv, ChordFunction.v, ChordFunction.VI),
                KeyQuality.MAJOR,  // Wrong - these are minor functions
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    // ========== Octave coverage tests ==========

    @Test
    fun `all unlock groups cover octaves 3, 4, and 5`() {
        val octaves = FunctionDeck.UNLOCK_ORDER.map { it.octave }.toSet()
        assertEquals(setOf(3, 4, 5), octaves)
    }

    @Test
    fun `both playback modes are used`() {
        val modes = FunctionDeck.UNLOCK_ORDER.map { it.playbackMode }.toSet()
        assertEquals(setOf(PlaybackMode.ARPEGGIATED, PlaybackMode.BLOCK), modes)
    }

    // ========== Card uniqueness test ==========

    @Test
    fun `all generated cards are unique`() {
        val allCards = FunctionDeck.UNLOCK_ORDER.flatMap { it.toCards() }
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(72, uniqueIds.size)
    }

    // ========== Key quality alternation tests ==========

    @Test
    fun `major key has 12 groups`() {
        val majorGroups = FunctionDeck.UNLOCK_ORDER.filter { it.keyQuality == KeyQuality.MAJOR }
        assertEquals(12, majorGroups.size)
    }

    @Test
    fun `minor key has 12 groups`() {
        val minorGroups = FunctionDeck.UNLOCK_ORDER.filter { it.keyQuality == KeyQuality.MINOR }
        assertEquals(12, minorGroups.size)
    }

    @Test
    fun `each function appears in multiple octaves and modes for major key`() {
        val majorCards = FunctionDeck.UNLOCK_ORDER
            .filter { it.keyQuality == KeyQuality.MAJOR }
            .flatMap { it.toCards() }

        // Each function should appear 6 times (3 octaves * 2 modes)
        val functionCounts = majorCards.groupBy { it.function }.mapValues { it.value.size }
        ChordFunction.MAJOR_FUNCTIONS.forEach { function ->
            assertEquals("$function should appear 6 times", 6, functionCounts[function])
        }
    }

    @Test
    fun `each function appears in multiple octaves and modes for minor key`() {
        val minorCards = FunctionDeck.UNLOCK_ORDER
            .filter { it.keyQuality == KeyQuality.MINOR }
            .flatMap { it.toCards() }

        // Each function should appear 6 times (3 octaves * 2 modes)
        val functionCounts = minorCards.groupBy { it.function }.mapValues { it.value.size }
        ChordFunction.MINOR_FUNCTIONS.forEach { function ->
            assertEquals("$function should appear 6 times", 6, functionCounts[function])
        }
    }
}
