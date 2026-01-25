package net.xmppwocky.earbs.model

import org.junit.Assert.*
import org.junit.Test

class ProgressionTypeTest {

    // ========== Progression count tests ==========

    @Test
    fun `there are 8 progression types total`() {
        assertEquals(8, ProgressionType.entries.size)
    }

    @Test
    fun `there are 6 resolving progressions`() {
        val resolving = ProgressionType.entries.filter { it.category == ProgressionCategory.RESOLVING }
        assertEquals(6, resolving.size)
    }

    @Test
    fun `there are 2 loop progressions`() {
        val loops = ProgressionType.entries.filter { it.category == ProgressionCategory.LOOP }
        assertEquals(2, loops.size)
    }

    // ========== Resolving progressions end on I ==========

    @Test
    fun `all resolving progressions end on I (semitone 0)`() {
        val resolving = ProgressionType.entries.filter { it.category == ProgressionCategory.RESOLVING }
        resolving.forEach { prog ->
            assertEquals(
                "Resolving progression ${prog.name} should end on I (semitone 0)",
                0,
                prog.semitoneOffsets.last()
            )
        }
    }

    // ========== Chord count tests ==========

    @Test
    fun `3-chord progressions have 3 chords`() {
        assertEquals(3, ProgressionType.I_IV_I.semitoneOffsets.size)
        assertEquals(3, ProgressionType.I_V_I.semitoneOffsets.size)
    }

    @Test
    fun `4-chord resolving progressions have 4 chords`() {
        assertEquals(4, ProgressionType.I_IV_V_I.semitoneOffsets.size)
        assertEquals(4, ProgressionType.I_ii_V_I.semitoneOffsets.size)
    }

    @Test
    fun `5-chord progressions have 5 chords`() {
        assertEquals(5, ProgressionType.I_vi_ii_V_I.semitoneOffsets.size)
        assertEquals(5, ProgressionType.I_vi_IV_V_I.semitoneOffsets.size)
    }

    @Test
    fun `loop progressions have 4 chords`() {
        assertEquals(4, ProgressionType.I_V_vi_IV.semitoneOffsets.size)
        assertEquals(4, ProgressionType.I_vi_IV_V.semitoneOffsets.size)
    }

    // ========== Semitone offset tests ==========

    @Test
    fun `I_IV_I has correct semitone offsets`() {
        assertEquals(listOf(0, 5, 0), ProgressionType.I_IV_I.semitoneOffsets)
    }

    @Test
    fun `I_V_I has correct semitone offsets`() {
        assertEquals(listOf(0, 7, 0), ProgressionType.I_V_I.semitoneOffsets)
    }

    @Test
    fun `I_IV_V_I has correct semitone offsets`() {
        assertEquals(listOf(0, 5, 7, 0), ProgressionType.I_IV_V_I.semitoneOffsets)
    }

    @Test
    fun `I_ii_V_I has correct semitone offsets`() {
        assertEquals(listOf(0, 2, 7, 0), ProgressionType.I_ii_V_I.semitoneOffsets)
    }

    @Test
    fun `I_vi_ii_V_I has correct semitone offsets`() {
        assertEquals(listOf(0, 9, 2, 7, 0), ProgressionType.I_vi_ii_V_I.semitoneOffsets)
    }

    @Test
    fun `I_vi_IV_V_I has correct semitone offsets`() {
        assertEquals(listOf(0, 9, 5, 7, 0), ProgressionType.I_vi_IV_V_I.semitoneOffsets)
    }

    @Test
    fun `I_V_vi_IV has correct semitone offsets`() {
        assertEquals(listOf(0, 7, 9, 5), ProgressionType.I_V_vi_IV.semitoneOffsets)
    }

    @Test
    fun `I_vi_IV_V has correct semitone offsets`() {
        assertEquals(listOf(0, 9, 5, 7), ProgressionType.I_vi_IV_V.semitoneOffsets)
    }

    // ========== Major key chord quality tests ==========

    @Test
    fun `I_IV_V_I major key qualities are all MAJOR`() {
        val qualities = ProgressionType.I_IV_V_I.chordQualities(KeyQuality.MAJOR)
        assertEquals(
            listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR),
            qualities
        )
    }

    @Test
    fun `I_ii_V_I major key has ii as MINOR`() {
        val qualities = ProgressionType.I_ii_V_I.chordQualities(KeyQuality.MAJOR)
        assertEquals(ChordQuality.MINOR, qualities[1]) // ii is second chord
    }

    @Test
    fun `I_vi_IV_V major key has vi as MINOR`() {
        val qualities = ProgressionType.I_vi_IV_V.chordQualities(KeyQuality.MAJOR)
        assertEquals(
            listOf(ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MAJOR),
            qualities
        )
    }

    @Test
    fun `I_V_vi_IV major key has vi as MINOR`() {
        val qualities = ProgressionType.I_V_vi_IV.chordQualities(KeyQuality.MAJOR)
        assertEquals(
            listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR),
            qualities
        )
    }

    @Test
    fun `I_vi_ii_V_I major key has correct qualities`() {
        val qualities = ProgressionType.I_vi_ii_V_I.chordQualities(KeyQuality.MAJOR)
        assertEquals(
            listOf(
                ChordQuality.MAJOR,  // I
                ChordQuality.MINOR,  // vi
                ChordQuality.MINOR,  // ii
                ChordQuality.MAJOR,  // V
                ChordQuality.MAJOR   // I
            ),
            qualities
        )
    }

    // ========== Minor key chord quality tests ==========

    @Test
    fun `I_IV_V_I minor key qualities are all MINOR`() {
        val qualities = ProgressionType.I_IV_V_I.chordQualities(KeyQuality.MINOR)
        assertEquals(
            listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR),
            qualities
        )
    }

    @Test
    fun `I_ii_V_I minor key has ii as DIMINISHED`() {
        val qualities = ProgressionType.I_ii_V_I.chordQualities(KeyQuality.MINOR)
        assertEquals(ChordQuality.DIMINISHED, qualities[1]) // ii° is second chord
    }

    @Test
    fun `I_vi_IV_V minor key has VI as MAJOR`() {
        val qualities = ProgressionType.I_vi_IV_V.chordQualities(KeyQuality.MINOR)
        assertEquals(
            listOf(ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR),
            qualities
        )
    }

    @Test
    fun `I_V_vi_IV minor key has VI as MAJOR`() {
        val qualities = ProgressionType.I_V_vi_IV.chordQualities(KeyQuality.MINOR)
        assertEquals(
            listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR),
            qualities
        )
    }

    @Test
    fun `I_vi_ii_V_I minor key has correct qualities`() {
        val qualities = ProgressionType.I_vi_ii_V_I.chordQualities(KeyQuality.MINOR)
        assertEquals(
            listOf(
                ChordQuality.MINOR,      // i
                ChordQuality.MAJOR,      // VI
                ChordQuality.DIMINISHED, // ii°
                ChordQuality.MINOR,      // v
                ChordQuality.MINOR       // i
            ),
            qualities
        )
    }

    // ========== Quality count matches chord count ==========

    @Test
    fun `chord qualities list matches semitone offsets count for all progressions`() {
        ProgressionType.entries.forEach { prog ->
            val majorQualities = prog.chordQualities(KeyQuality.MAJOR)
            val minorQualities = prog.chordQualities(KeyQuality.MINOR)

            assertEquals(
                "${prog.name} major qualities count should match chord count",
                prog.semitoneOffsets.size,
                majorQualities.size
            )
            assertEquals(
                "${prog.name} minor qualities count should match chord count",
                prog.semitoneOffsets.size,
                minorQualities.size
            )
        }
    }

    // ========== Companion object lists ==========

    @Test
    fun `THREE_CHORD contains correct progressions`() {
        assertEquals(2, ProgressionType.THREE_CHORD.size)
        assertTrue(ProgressionType.THREE_CHORD.contains(ProgressionType.I_IV_I))
        assertTrue(ProgressionType.THREE_CHORD.contains(ProgressionType.I_V_I))
    }

    @Test
    fun `FOUR_CHORD_RESOLVING contains correct progressions`() {
        assertEquals(2, ProgressionType.FOUR_CHORD_RESOLVING.size)
        assertTrue(ProgressionType.FOUR_CHORD_RESOLVING.contains(ProgressionType.I_IV_V_I))
        assertTrue(ProgressionType.FOUR_CHORD_RESOLVING.contains(ProgressionType.I_ii_V_I))
    }

    @Test
    fun `FIVE_CHORD contains correct progressions`() {
        assertEquals(2, ProgressionType.FIVE_CHORD.size)
        assertTrue(ProgressionType.FIVE_CHORD.contains(ProgressionType.I_vi_ii_V_I))
        assertTrue(ProgressionType.FIVE_CHORD.contains(ProgressionType.I_vi_IV_V_I))
    }

    @Test
    fun `LOOPS contains correct progressions`() {
        assertEquals(2, ProgressionType.LOOPS.size)
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.I_V_vi_IV))
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.I_vi_IV_V))
    }

    // ========== Display name tests ==========

    @Test
    fun `display names contain correct chord numerals`() {
        assertEquals("I - IV - I", ProgressionType.I_IV_I.displayName)
        assertEquals("I - V - I", ProgressionType.I_V_I.displayName)
        assertEquals("I - IV - V - I", ProgressionType.I_IV_V_I.displayName)
        assertEquals("I - ii - V - I", ProgressionType.I_ii_V_I.displayName)
        assertEquals("I - vi - ii - V - I", ProgressionType.I_vi_ii_V_I.displayName)
        assertEquals("I - vi - IV - V - I", ProgressionType.I_vi_IV_V_I.displayName)
        assertEquals("I - V - vi - IV", ProgressionType.I_V_vi_IV.displayName)
        assertEquals("I - vi - IV - V", ProgressionType.I_vi_IV_V.displayName)
    }
}
