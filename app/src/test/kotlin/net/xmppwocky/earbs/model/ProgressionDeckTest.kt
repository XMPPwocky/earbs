package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Test

class ProgressionDeckTest {

    // ========== UNLOCK_ORDER structure tests ==========

    @Test
    fun `UNLOCK_ORDER has 48 groups`() {
        assertEquals(48, ProgressionDeck.UNLOCK_ORDER.size)
    }

    @Test
    fun `each unlock group has exactly 2 progressions`() {
        ProgressionDeck.UNLOCK_ORDER.forEach { group ->
            assertEquals(2, group.progressions.size)
        }
    }

    @Test
    fun `CARDS_PER_GROUP is 2`() {
        assertEquals(2, ProgressionDeck.CARDS_PER_GROUP)
    }

    // ========== Starting deck tests ==========

    @Test
    fun `first group is 3-chord major at octave 4 arpeggiated`() {
        val firstGroup = ProgressionDeck.UNLOCK_ORDER[0]
        assertEquals(4, firstGroup.octave)
        assertEquals(PlaybackMode.ARPEGGIATED, firstGroup.playbackMode)

        val progressions = firstGroup.progressions
        assertTrue(progressions.contains(ProgressionType.I_IV_I_MAJOR))
        assertTrue(progressions.contains(ProgressionType.I_V_I_MAJOR))
    }

    @Test
    fun `starting deck has 3-chord major progressions only`() {
        val firstGroup = ProgressionDeck.UNLOCK_ORDER[0]
        firstGroup.progressions.forEach { prog ->
            assertEquals(3, prog.semitoneOffsets.size)
            assertEquals(KeyQuality.MAJOR, prog.keyQuality)
        }
    }

    // ========== Complexity order tests (Major then Minor for each complexity) ==========

    @Test
    fun `groups 0-5 are 3-chord major progressions`() {
        for (i in 0..5) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have 3-chord progressions", 3, prog.semitoneOffsets.size)
                assertEquals("Group $i should have major progressions", KeyQuality.MAJOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 6-11 are 3-chord minor progressions`() {
        for (i in 6..11) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have 3-chord progressions", 3, prog.semitoneOffsets.size)
                assertEquals("Group $i should have minor progressions", KeyQuality.MINOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 12-17 are 4-chord major resolving progressions`() {
        for (i in 12..17) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have 4-chord progressions", 4, prog.semitoneOffsets.size)
                assertEquals("Group $i should have resolving progressions", ProgressionCategory.RESOLVING, prog.category)
                assertEquals("Group $i should have major progressions", KeyQuality.MAJOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 18-23 are 4-chord minor resolving progressions`() {
        for (i in 18..23) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have 4-chord progressions", 4, prog.semitoneOffsets.size)
                assertEquals("Group $i should have resolving progressions", ProgressionCategory.RESOLVING, prog.category)
                assertEquals("Group $i should have minor progressions", KeyQuality.MINOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 24-29 are 5-chord major progressions`() {
        for (i in 24..29) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have 5-chord progressions", 5, prog.semitoneOffsets.size)
                assertEquals("Group $i should have major progressions", KeyQuality.MAJOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 30-35 are 5-chord minor progressions`() {
        for (i in 30..35) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have 5-chord progressions", 5, prog.semitoneOffsets.size)
                assertEquals("Group $i should have minor progressions", KeyQuality.MINOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 36-41 are loop major progressions`() {
        for (i in 36..41) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have loop progressions", ProgressionCategory.LOOP, prog.category)
                assertEquals("Group $i should have major progressions", KeyQuality.MAJOR, prog.keyQuality)
            }
        }
    }

    @Test
    fun `groups 42-47 are loop minor progressions`() {
        for (i in 42..47) {
            val group = ProgressionDeck.UNLOCK_ORDER[i]
            group.progressions.forEach { prog ->
                assertEquals("Group $i should have loop progressions", ProgressionCategory.LOOP, prog.category)
                assertEquals("Group $i should have minor progressions", KeyQuality.MINOR, prog.keyQuality)
            }
        }
    }

    // ========== TOTAL_CARDS tests ==========

    @Test
    fun `TOTAL_CARDS equals 96`() {
        assertEquals(96, ProgressionDeck.TOTAL_CARDS)
    }

    @Test
    fun `TOTAL_CARDS equals sum of all group cards`() {
        val totalFromGroups = ProgressionDeck.UNLOCK_ORDER.sumOf { it.progressions.size }
        assertEquals(ProgressionDeck.TOTAL_CARDS, totalFromGroups)
    }

    @Test
    fun `getAllCards returns 96 cards`() {
        assertEquals(96, ProgressionDeck.getAllCards().size)
    }

    // ========== MAX_UNLOCK_LEVEL test ==========

    @Test
    fun `MAX_UNLOCK_LEVEL is 47`() {
        assertEquals(47, ProgressionDeck.MAX_UNLOCK_LEVEL)
    }

    // ========== ProgressionUnlockGroup.toCards tests ==========

    @Test
    fun `toCards generates correct ProgressionCard objects`() {
        val group = ProgressionDeck.UNLOCK_ORDER[0]
        val cards = group.toCards()

        assertEquals(2, cards.size)
        cards.forEach { card ->
            assertEquals(4, card.octave)
            assertEquals(PlaybackMode.ARPEGGIATED, card.playbackMode)
        }
    }

    @Test
    fun `toCards preserves progression order`() {
        val group = ProgressionDeck.UNLOCK_ORDER[0]
        val cards = group.toCards()

        assertEquals(ProgressionType.I_IV_I_MAJOR, cards[0].progression)
        assertEquals(ProgressionType.I_V_I_MAJOR, cards[1].progression)
    }

    // ========== ProgressionUnlockGroup validation tests ==========

    @Test
    fun `ProgressionUnlockGroup requires exactly 2 progressions`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProgressionUnlockGroup(
                listOf(ProgressionType.I_IV_I_MAJOR),
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    @Test
    fun `ProgressionUnlockGroup with 3 progressions throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            ProgressionUnlockGroup(
                listOf(ProgressionType.I_IV_I_MAJOR, ProgressionType.I_V_I_MAJOR, ProgressionType.I_IV_V_I_MAJOR),
                4,
                PlaybackMode.ARPEGGIATED
            )
        }
    }

    // ========== Octave coverage tests ==========

    @Test
    fun `all unlock groups cover octaves 3, 4, and 5`() {
        val octaves = ProgressionDeck.UNLOCK_ORDER.map { it.octave }.toSet()
        assertEquals(setOf(3, 4, 5), octaves)
    }

    @Test
    fun `both playback modes are used`() {
        val modes = ProgressionDeck.UNLOCK_ORDER.map { it.playbackMode }.toSet()
        assertEquals(setOf(PlaybackMode.ARPEGGIATED, PlaybackMode.BLOCK), modes)
    }

    // ========== Card uniqueness test ==========

    @Test
    fun `all generated cards are unique`() {
        val allCards = ProgressionDeck.getAllCards()
        val uniqueIds = allCards.map { it.id }.toSet()
        assertEquals(96, uniqueIds.size)
    }

    // ========== Each progression appears in all octaves and modes ==========

    @Test
    fun `each progression appears in multiple octaves and modes`() {
        val allCards = ProgressionDeck.getAllCards()

        // Each progression should appear 6 times (3 octaves * 2 modes)
        val progressionCounts = allCards.groupBy { it.progression }.mapValues { it.value.size }
        ProgressionType.entries.forEach { progression ->
            assertEquals(
                "$progression should appear 6 times",
                6,
                progressionCounts[progression]
            )
        }
    }

    // ========== getGroupIndex tests ==========

    @Test
    fun `getGroupIndex returns correct index for first group cards`() {
        val firstGroupCards = ProgressionDeck.UNLOCK_ORDER[0].toCards()
        firstGroupCards.forEach { card ->
            assertEquals(0, ProgressionDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns correct index for last group cards`() {
        val lastGroupCards = ProgressionDeck.UNLOCK_ORDER[47].toCards()
        lastGroupCards.forEach { card ->
            assertEquals(47, ProgressionDeck.getGroupIndex(card))
        }
    }

    @Test
    fun `getGroupIndex returns correct index for all cards`() {
        ProgressionDeck.UNLOCK_ORDER.forEachIndexed { index, group ->
            group.toCards().forEach { card ->
                assertEquals(
                    "Card ${card.id} should be in group $index",
                    index,
                    ProgressionDeck.getGroupIndex(card)
                )
            }
        }
    }

    // ========== getGroupName tests ==========

    @Test
    fun `getGroupName returns valid name for first group`() {
        val name = ProgressionDeck.getGroupName(0)
        assertTrue(name.contains("3-chord Major"))
        assertTrue(name.contains("Octave 4"))
        assertTrue(name.contains("Arpeggiated"))
    }

    @Test
    fun `getGroupName returns valid name for minor group`() {
        val name = ProgressionDeck.getGroupName(6)
        assertTrue(name.contains("3-chord Minor"))
    }

    @Test
    fun `getGroupName returns valid name for loop major group`() {
        val name = ProgressionDeck.getGroupName(36)
        assertTrue(name.contains("Loops Major"))
    }

    @Test
    fun `getGroupName returns valid name for loop minor group`() {
        val name = ProgressionDeck.getGroupName(42)
        assertTrue(name.contains("Loops Minor"))
    }

    @Test
    fun `getGroupName returns Unknown Group for invalid index`() {
        assertEquals("Unknown Group", ProgressionDeck.getGroupName(-1))
        assertEquals("Unknown Group", ProgressionDeck.getGroupName(48))
        assertEquals("Unknown Group", ProgressionDeck.getGroupName(100))
    }

    // ========== Octave pattern within complexity groups ==========

    @Test
    fun `within each complexity level octave 4 comes first`() {
        // Check 3-chord major groups (0-5)
        assertEquals(4, ProgressionDeck.UNLOCK_ORDER[0].octave)
        assertEquals(4, ProgressionDeck.UNLOCK_ORDER[1].octave)
        assertEquals(3, ProgressionDeck.UNLOCK_ORDER[2].octave)
        assertEquals(3, ProgressionDeck.UNLOCK_ORDER[3].octave)
        assertEquals(5, ProgressionDeck.UNLOCK_ORDER[4].octave)
        assertEquals(5, ProgressionDeck.UNLOCK_ORDER[5].octave)
    }

    @Test
    fun `within each octave arpeggiated comes before block`() {
        // Check pattern for each complexity/key-quality section
        // Each section has 6 groups: oct4-arp, oct4-block, oct3-arp, oct3-block, oct5-arp, oct5-block
        val sectionStarts = listOf(0, 6, 12, 18, 24, 30, 36, 42)
        sectionStarts.forEach { start ->
            // Oct 4: arp, block
            assertEquals(PlaybackMode.ARPEGGIATED, ProgressionDeck.UNLOCK_ORDER[start].playbackMode)
            assertEquals(PlaybackMode.BLOCK, ProgressionDeck.UNLOCK_ORDER[start + 1].playbackMode)
            // Oct 3: arp, block
            assertEquals(PlaybackMode.ARPEGGIATED, ProgressionDeck.UNLOCK_ORDER[start + 2].playbackMode)
            assertEquals(PlaybackMode.BLOCK, ProgressionDeck.UNLOCK_ORDER[start + 3].playbackMode)
            // Oct 5: arp, block
            assertEquals(PlaybackMode.ARPEGGIATED, ProgressionDeck.UNLOCK_ORDER[start + 4].playbackMode)
            assertEquals(PlaybackMode.BLOCK, ProgressionDeck.UNLOCK_ORDER[start + 5].playbackMode)
        }
    }
}
