package net.xmppwocky.earbs.model

import org.junit.Assert.*
import org.junit.Test

class ProgressionTypeTest {

    // ========== Progression count tests ==========

    @Test
    fun `there are 18 progression types total`() {
        // 14 active + 4 deprecated 5-chord = 18 total
        assertEquals(18, ProgressionType.entries.size)
    }

    @Test
    fun `there are 9 major progressions`() {
        // 8 original + 1 new (I_vi_ii_V_MAJOR)
        assertEquals(9, ProgressionType.MAJOR_PROGRESSIONS.size)
    }

    @Test
    fun `there are 9 minor progressions`() {
        // 8 original + 1 new (i_VI_iio_v_MINOR)
        assertEquals(9, ProgressionType.MINOR_PROGRESSIONS.size)
    }

    @Test
    fun `there are 12 resolving progressions (6 major + 6 minor)`() {
        // Resolving progressions end on I (tonic)
        val resolving = ProgressionType.entries.filter { it.category == ProgressionCategory.RESOLVING }
        assertEquals(12, resolving.size)
    }

    @Test
    fun `there are 6 loop progressions (3 major + 3 minor)`() {
        // 4 original + 2 new (I_vi_ii_V_MAJOR, i_VI_iio_v_MINOR)
        val loops = ProgressionType.entries.filter { it.category == ProgressionCategory.LOOP }
        assertEquals(6, loops.size)
    }

    // ========== Key quality tests ==========

    @Test
    fun `all major progressions have MAJOR keyQuality`() {
        ProgressionType.MAJOR_PROGRESSIONS.forEach { prog ->
            assertEquals("${prog.name} should have MAJOR keyQuality", KeyQuality.MAJOR, prog.keyQuality)
        }
    }

    @Test
    fun `all minor progressions have MINOR keyQuality`() {
        ProgressionType.MINOR_PROGRESSIONS.forEach { prog ->
            assertEquals("${prog.name} should have MINOR keyQuality", KeyQuality.MINOR, prog.keyQuality)
        }
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
        assertEquals(3, ProgressionType.I_IV_I_MAJOR.semitoneOffsets.size)
        assertEquals(3, ProgressionType.I_V_I_MAJOR.semitoneOffsets.size)
        assertEquals(3, ProgressionType.i_iv_i_MINOR.semitoneOffsets.size)
        assertEquals(3, ProgressionType.i_v_i_MINOR.semitoneOffsets.size)
    }

    @Test
    fun `4-chord resolving progressions have 4 chords`() {
        assertEquals(4, ProgressionType.I_IV_V_I_MAJOR.semitoneOffsets.size)
        assertEquals(4, ProgressionType.I_ii_V_I_MAJOR.semitoneOffsets.size)
        assertEquals(4, ProgressionType.i_iv_v_i_MINOR.semitoneOffsets.size)
        assertEquals(4, ProgressionType.i_iio_v_i_MINOR.semitoneOffsets.size)
    }

    @Test
    fun `5-chord progressions have 5 chords`() {
        assertEquals(5, ProgressionType.I_vi_ii_V_I_MAJOR.semitoneOffsets.size)
        assertEquals(5, ProgressionType.I_vi_IV_V_I_MAJOR.semitoneOffsets.size)
        assertEquals(5, ProgressionType.i_VI_iio_v_i_MINOR.semitoneOffsets.size)
        assertEquals(5, ProgressionType.i_VI_iv_v_i_MINOR.semitoneOffsets.size)
    }

    @Test
    fun `loop progressions have 4 chords`() {
        assertEquals(4, ProgressionType.I_V_vi_IV_MAJOR.semitoneOffsets.size)
        assertEquals(4, ProgressionType.I_vi_IV_V_MAJOR.semitoneOffsets.size)
        assertEquals(4, ProgressionType.i_v_VI_iv_MINOR.semitoneOffsets.size)
        assertEquals(4, ProgressionType.i_VI_iv_v_MINOR.semitoneOffsets.size)
    }

    // ========== Major semitone offset tests ==========

    @Test
    fun `I_IV_I_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 5, 0), ProgressionType.I_IV_I_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_V_I_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 7, 0), ProgressionType.I_V_I_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_IV_V_I_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 5, 7, 0), ProgressionType.I_IV_V_I_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_ii_V_I_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 2, 7, 0), ProgressionType.I_ii_V_I_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_vi_ii_V_I_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 9, 2, 7, 0), ProgressionType.I_vi_ii_V_I_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_vi_IV_V_I_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 9, 5, 7, 0), ProgressionType.I_vi_IV_V_I_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_V_vi_IV_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 7, 9, 5), ProgressionType.I_V_vi_IV_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_vi_IV_V_MAJOR has correct semitone offsets`() {
        assertEquals(listOf(0, 9, 5, 7), ProgressionType.I_vi_IV_V_MAJOR.semitoneOffsets)
    }

    @Test
    fun `I_vi_ii_V_MAJOR has correct semitone offsets`() {
        // New 4-chord progression (replaces 5-chord I-vi-ii-V-I)
        assertEquals(listOf(0, 9, 2, 7), ProgressionType.I_vi_ii_V_MAJOR.semitoneOffsets)
    }

    // ========== Minor semitone offset tests ==========

    @Test
    fun `minor progressions have same semitone offsets as major counterparts`() {
        // 3-chord
        assertEquals(ProgressionType.I_IV_I_MAJOR.semitoneOffsets, ProgressionType.i_iv_i_MINOR.semitoneOffsets)
        assertEquals(ProgressionType.I_V_I_MAJOR.semitoneOffsets, ProgressionType.i_v_i_MINOR.semitoneOffsets)
        // 4-chord resolving
        assertEquals(ProgressionType.I_IV_V_I_MAJOR.semitoneOffsets, ProgressionType.i_iv_v_i_MINOR.semitoneOffsets)
        assertEquals(ProgressionType.I_ii_V_I_MAJOR.semitoneOffsets, ProgressionType.i_iio_v_i_MINOR.semitoneOffsets)
        // 5-chord
        assertEquals(ProgressionType.I_vi_ii_V_I_MAJOR.semitoneOffsets, ProgressionType.i_VI_iio_v_i_MINOR.semitoneOffsets)
        assertEquals(ProgressionType.I_vi_IV_V_I_MAJOR.semitoneOffsets, ProgressionType.i_VI_iv_v_i_MINOR.semitoneOffsets)
        // loops
        assertEquals(ProgressionType.I_V_vi_IV_MAJOR.semitoneOffsets, ProgressionType.i_v_VI_iv_MINOR.semitoneOffsets)
        assertEquals(ProgressionType.I_vi_IV_V_MAJOR.semitoneOffsets, ProgressionType.i_VI_iv_v_MINOR.semitoneOffsets)
    }

    // ========== Major key chord quality tests ==========

    @Test
    fun `I_IV_V_I_MAJOR qualities are all MAJOR`() {
        assertEquals(
            listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR),
            ProgressionType.I_IV_V_I_MAJOR.chordQualities
        )
    }

    @Test
    fun `I_ii_V_I_MAJOR has ii as MINOR`() {
        assertEquals(ChordQuality.MINOR, ProgressionType.I_ii_V_I_MAJOR.chordQualities[1])
    }

    @Test
    fun `I_vi_IV_V_MAJOR has vi as MINOR`() {
        assertEquals(
            listOf(ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MAJOR),
            ProgressionType.I_vi_IV_V_MAJOR.chordQualities
        )
    }

    @Test
    fun `I_V_vi_IV_MAJOR has vi as MINOR`() {
        assertEquals(
            listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR),
            ProgressionType.I_V_vi_IV_MAJOR.chordQualities
        )
    }

    @Test
    fun `I_vi_ii_V_I_MAJOR has correct qualities`() {
        assertEquals(
            listOf(
                ChordQuality.MAJOR,  // I
                ChordQuality.MINOR,  // vi
                ChordQuality.MINOR,  // ii
                ChordQuality.MAJOR,  // V
                ChordQuality.MAJOR   // I
            ),
            ProgressionType.I_vi_ii_V_I_MAJOR.chordQualities
        )
    }

    @Test
    fun `I_vi_ii_V_MAJOR has correct qualities`() {
        // New 4-chord progression (replaces 5-chord I-vi-ii-V-I)
        assertEquals(
            listOf(
                ChordQuality.MAJOR,  // I
                ChordQuality.MINOR,  // vi
                ChordQuality.MINOR,  // ii
                ChordQuality.MAJOR   // V
            ),
            ProgressionType.I_vi_ii_V_MAJOR.chordQualities
        )
    }

    // ========== Minor key chord quality tests ==========

    @Test
    fun `i_iv_v_i_MINOR qualities are all MINOR`() {
        assertEquals(
            listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR),
            ProgressionType.i_iv_v_i_MINOR.chordQualities
        )
    }

    @Test
    fun `i_iio_v_i_MINOR has ii as DIMINISHED`() {
        assertEquals(ChordQuality.DIMINISHED, ProgressionType.i_iio_v_i_MINOR.chordQualities[1])
    }

    @Test
    fun `i_VI_iv_v_MINOR has VI as MAJOR`() {
        assertEquals(
            listOf(ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR),
            ProgressionType.i_VI_iv_v_MINOR.chordQualities
        )
    }

    @Test
    fun `i_v_VI_iv_MINOR has VI as MAJOR`() {
        assertEquals(
            listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR),
            ProgressionType.i_v_VI_iv_MINOR.chordQualities
        )
    }

    @Test
    fun `i_VI_iio_v_i_MINOR has correct qualities`() {
        assertEquals(
            listOf(
                ChordQuality.MINOR,      // i
                ChordQuality.MAJOR,      // VI
                ChordQuality.DIMINISHED, // ii°
                ChordQuality.MINOR,      // v
                ChordQuality.MINOR       // i
            ),
            ProgressionType.i_VI_iio_v_i_MINOR.chordQualities
        )
    }

    @Test
    fun `i_VI_iio_v_MINOR has correct qualities`() {
        // New 4-chord progression (replaces 5-chord i-VI-ii°-v-i)
        assertEquals(
            listOf(
                ChordQuality.MINOR,      // i
                ChordQuality.MAJOR,      // VI
                ChordQuality.DIMINISHED, // ii°
                ChordQuality.MINOR       // v
            ),
            ProgressionType.i_VI_iio_v_MINOR.chordQualities
        )
    }

    @Test
    fun `i_VI_iio_v_MINOR has correct semitone offsets`() {
        // New 4-chord progression (replaces 5-chord i-VI-ii°-v-i)
        assertEquals(listOf(0, 9, 2, 7), ProgressionType.i_VI_iio_v_MINOR.semitoneOffsets)
    }

    // ========== Quality count matches chord count ==========

    @Test
    fun `chord qualities list matches semitone offsets count for all progressions`() {
        ProgressionType.entries.forEach { prog ->
            assertEquals(
                "${prog.name} qualities count should match chord count",
                prog.semitoneOffsets.size,
                prog.chordQualities.size
            )
        }
    }

    // ========== Companion object lists ==========

    @Test
    fun `THREE_CHORD contains 4 progressions (2 major + 2 minor)`() {
        assertEquals(4, ProgressionType.THREE_CHORD.size)
        assertTrue(ProgressionType.THREE_CHORD.contains(ProgressionType.I_IV_I_MAJOR))
        assertTrue(ProgressionType.THREE_CHORD.contains(ProgressionType.I_V_I_MAJOR))
        assertTrue(ProgressionType.THREE_CHORD.contains(ProgressionType.i_iv_i_MINOR))
        assertTrue(ProgressionType.THREE_CHORD.contains(ProgressionType.i_v_i_MINOR))
    }

    @Test
    fun `FOUR_CHORD_RESOLVING contains 4 progressions (2 major + 2 minor)`() {
        // Only progressions that end on I (not the new ones that end on V)
        assertEquals(4, ProgressionType.FOUR_CHORD_RESOLVING.size)
        assertTrue(ProgressionType.FOUR_CHORD_RESOLVING.contains(ProgressionType.I_IV_V_I_MAJOR))
        assertTrue(ProgressionType.FOUR_CHORD_RESOLVING.contains(ProgressionType.I_ii_V_I_MAJOR))
        assertTrue(ProgressionType.FOUR_CHORD_RESOLVING.contains(ProgressionType.i_iv_v_i_MINOR))
        assertTrue(ProgressionType.FOUR_CHORD_RESOLVING.contains(ProgressionType.i_iio_v_i_MINOR))
    }

    @Test
    fun `FIVE_CHORD contains 4 progressions (2 major + 2 minor)`() {
        assertEquals(4, ProgressionType.FIVE_CHORD.size)
        assertTrue(ProgressionType.FIVE_CHORD.contains(ProgressionType.I_vi_ii_V_I_MAJOR))
        assertTrue(ProgressionType.FIVE_CHORD.contains(ProgressionType.I_vi_IV_V_I_MAJOR))
        assertTrue(ProgressionType.FIVE_CHORD.contains(ProgressionType.i_VI_iio_v_i_MINOR))
        assertTrue(ProgressionType.FIVE_CHORD.contains(ProgressionType.i_VI_iv_v_i_MINOR))
    }

    @Test
    fun `LOOPS contains 6 progressions (3 major + 3 minor)`() {
        // 4 original + 2 new (I_vi_ii_V_MAJOR, i_VI_iio_v_MINOR)
        assertEquals(6, ProgressionType.LOOPS.size)
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.I_V_vi_IV_MAJOR))
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.I_vi_IV_V_MAJOR))
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.I_vi_ii_V_MAJOR))
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.i_v_VI_iv_MINOR))
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.i_VI_iv_v_MINOR))
        assertTrue(ProgressionType.LOOPS.contains(ProgressionType.i_VI_iio_v_MINOR))
    }

    // ========== Display name tests ==========

    @Test
    fun `major display names use uppercase Roman numerals`() {
        assertEquals("I - IV - I", ProgressionType.I_IV_I_MAJOR.displayName)
        assertEquals("I - V - I", ProgressionType.I_V_I_MAJOR.displayName)
        assertEquals("I - IV - V - I", ProgressionType.I_IV_V_I_MAJOR.displayName)
        assertEquals("I - ii - V - I", ProgressionType.I_ii_V_I_MAJOR.displayName)
        assertEquals("I - vi - ii - V", ProgressionType.I_vi_ii_V_MAJOR.displayName)
        assertEquals("I - vi - ii - V - I", ProgressionType.I_vi_ii_V_I_MAJOR.displayName)
        assertEquals("I - vi - IV - V - I", ProgressionType.I_vi_IV_V_I_MAJOR.displayName)
        assertEquals("I - V - vi - IV", ProgressionType.I_V_vi_IV_MAJOR.displayName)
        assertEquals("I - vi - IV - V", ProgressionType.I_vi_IV_V_MAJOR.displayName)
    }

    @Test
    fun `minor display names use lowercase Roman numerals`() {
        assertEquals("i - iv - i", ProgressionType.i_iv_i_MINOR.displayName)
        assertEquals("i - v - i", ProgressionType.i_v_i_MINOR.displayName)
        assertEquals("i - iv - v - i", ProgressionType.i_iv_v_i_MINOR.displayName)
        assertEquals("i - ii° - v - i", ProgressionType.i_iio_v_i_MINOR.displayName)
        assertEquals("i - VI - ii° - v", ProgressionType.i_VI_iio_v_MINOR.displayName)
        assertEquals("i - VI - ii° - v - i", ProgressionType.i_VI_iio_v_i_MINOR.displayName)
        assertEquals("i - VI - iv - v - i", ProgressionType.i_VI_iv_v_i_MINOR.displayName)
        assertEquals("i - v - VI - iv", ProgressionType.i_v_VI_iv_MINOR.displayName)
        assertEquals("i - VI - iv - v", ProgressionType.i_VI_iv_v_MINOR.displayName)
    }
}
