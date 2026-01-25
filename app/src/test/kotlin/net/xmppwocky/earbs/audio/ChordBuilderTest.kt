package net.xmppwocky.earbs.audio

import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.ChordQuality
import net.xmppwocky.earbs.model.KeyQuality
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ChordBuilderTest {

    companion object {
        private const val FREQUENCY_TOLERANCE = 0.01f
    }

    // ========== Frequency calculation tests ==========

    @Test
    fun `noteFrequency at 0 semitones returns 440 Hz (A4)`() {
        val freq = ChordBuilder.noteFrequency(0)
        assertEquals(440.0f, freq, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `noteFrequency at 12 semitones returns 880 Hz (A5)`() {
        val freq = ChordBuilder.noteFrequency(12)
        assertEquals(880.0f, freq, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `noteFrequency at -12 semitones returns 220 Hz (A3)`() {
        val freq = ChordBuilder.noteFrequency(-12)
        assertEquals(220.0f, freq, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `noteFrequency at 3 semitones returns C5 frequency`() {
        // C5 is 3 semitones above A4
        // Expected: 440 * 2^(3/12) ≈ 523.25 Hz
        val freq = ChordBuilder.noteFrequency(3)
        assertEquals(523.25f, freq, 0.1f)
    }

    @Test
    fun `noteFrequency at -9 semitones returns C4 (middle C)`() {
        // C4 is 9 semitones below A4
        // Expected: 440 * 2^(-9/12) ≈ 261.63 Hz
        val freq = ChordBuilder.noteFrequency(-9)
        assertEquals(261.63f, freq, 0.1f)
    }

    // ========== Chord type interval tests ==========

    @Test
    fun `Major chord has intervals 0, 4, 7`() {
        assertEquals(listOf(0, 4, 7), ChordType.MAJOR.intervals)
    }

    @Test
    fun `Minor chord has intervals 0, 3, 7`() {
        assertEquals(listOf(0, 3, 7), ChordType.MINOR.intervals)
    }

    @Test
    fun `Sus2 chord has intervals 0, 2, 7`() {
        assertEquals(listOf(0, 2, 7), ChordType.SUS2.intervals)
    }

    @Test
    fun `Sus4 chord has intervals 0, 5, 7`() {
        assertEquals(listOf(0, 5, 7), ChordType.SUS4.intervals)
    }

    @Test
    fun `Dom7 chord has intervals 0, 4, 7, 10`() {
        assertEquals(listOf(0, 4, 7, 10), ChordType.DOM7.intervals)
    }

    @Test
    fun `Maj7 chord has intervals 0, 4, 7, 11`() {
        assertEquals(listOf(0, 4, 7, 11), ChordType.MAJ7.intervals)
    }

    @Test
    fun `Min7 chord has intervals 0, 3, 7, 10`() {
        assertEquals(listOf(0, 3, 7, 10), ChordType.MIN7.intervals)
    }

    @Test
    fun `Dim7 chord has intervals 0, 3, 6, 9`() {
        assertEquals(listOf(0, 3, 6, 9), ChordType.DIM7.intervals)
    }

    @Test
    fun `TRIADS contains exactly 4 chord types`() {
        assertEquals(4, ChordType.TRIADS.size)
        assertTrue(ChordType.TRIADS.containsAll(listOf(
            ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4
        )))
    }

    @Test
    fun `SEVENTHS contains exactly 4 chord types`() {
        assertEquals(4, ChordType.SEVENTHS.size)
        assertTrue(ChordType.SEVENTHS.containsAll(listOf(
            ChordType.DOM7, ChordType.MAJ7, ChordType.MIN7, ChordType.DIM7
        )))
    }

    // ========== Chord building tests ==========

    @Test
    fun `buildChord returns correct number of frequencies for triads`() {
        val frequencies = ChordBuilder.buildChord(ChordType.MAJOR, 0)
        assertEquals(3, frequencies.size)
    }

    @Test
    fun `buildChord returns correct number of frequencies for 7th chords`() {
        val frequencies = ChordBuilder.buildChord(ChordType.DOM7, 0)
        assertEquals(4, frequencies.size)
    }

    @Test
    fun `buildChord applies root offset correctly`() {
        // A4 major chord at root=0: A4, C#5, E5
        val freqsAtA4 = ChordBuilder.buildChord(ChordType.MAJOR, 0)
        assertEquals(440.0f, freqsAtA4[0], FREQUENCY_TOLERANCE)

        // A5 major chord at root=12: A5, C#6, E6
        val freqsAtA5 = ChordBuilder.buildChord(ChordType.MAJOR, 12)
        assertEquals(880.0f, freqsAtA5[0], FREQUENCY_TOLERANCE)

        // Second note of A5 chord should be exactly double the second note of A4 chord
        assertEquals(freqsAtA4[1] * 2, freqsAtA5[1], FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildChord frequencies increase within chord`() {
        val frequencies = ChordBuilder.buildChord(ChordType.MAJOR, 0)
        assertTrue(frequencies[0] < frequencies[1])
        assertTrue(frequencies[1] < frequencies[2])
    }

    // ========== Root note randomization tests ==========

    @Test
    fun `randomRootInOctave 4 returns value in octave 4 range`() {
        // Octave 4: C4 (-9) to B4 (+2)
        repeat(100) {
            val semitones = ChordBuilder.randomRootInOctave(4)
            assertTrue("Semitone $semitones should be >= -9", semitones >= -9)
            assertTrue("Semitone $semitones should be <= 2", semitones <= 2)
        }
    }

    @Test
    fun `randomRootInOctave 3 returns value in octave 3 range`() {
        // Octave 3: C3 (-21) to B3 (-10)
        repeat(100) {
            val semitones = ChordBuilder.randomRootInOctave(3)
            assertTrue("Semitone $semitones should be >= -21", semitones >= -21)
            assertTrue("Semitone $semitones should be <= -10", semitones <= -10)
        }
    }

    @Test
    fun `randomRootInOctave 5 returns value in octave 5 range`() {
        // Octave 5: C5 (+3) to B5 (+14)
        repeat(100) {
            val semitones = ChordBuilder.randomRootInOctave(5)
            assertTrue("Semitone $semitones should be >= 3", semitones >= 3)
            assertTrue("Semitone $semitones should be <= 14", semitones <= 14)
        }
    }

    @Test
    fun `randomRootInOctave produces varied values`() {
        // Check that we get at least a few different values
        val values = (1..50).map { ChordBuilder.randomRootInOctave(4) }.toSet()
        assertTrue("Should have variation in random values", values.size > 1)
    }

    // ========== Diatonic chord building tests ==========

    @Test
    fun `buildTonicChord for major key returns major triad`() {
        val frequencies = ChordBuilder.buildTonicChord(0, KeyQuality.MAJOR)
        assertEquals(3, frequencies.size)
        // Should have major third (4 semitones from root)
        val expectedThird = ChordBuilder.noteFrequency(4)
        assertEquals(expectedThird, frequencies[1], FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildTonicChord for minor key returns minor triad`() {
        val frequencies = ChordBuilder.buildTonicChord(0, KeyQuality.MINOR)
        assertEquals(3, frequencies.size)
        // Should have minor third (3 semitones from root)
        val expectedThird = ChordBuilder.noteFrequency(3)
        assertEquals(expectedThird, frequencies[1], FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildDiatonicChord applies function offset correctly`() {
        // V chord is 7 semitones above the key root
        val keyRoot = 0 // A
        val vChord = ChordBuilder.buildDiatonicChord(keyRoot, ChordFunction.V)

        // Root of V chord should be 7 semitones above A4
        val expectedRoot = ChordBuilder.noteFrequency(7)
        assertEquals(expectedRoot, vChord[0], FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildDiatonicChord uses correct quality for each function`() {
        val keyRoot = 0

        // V is major
        val vChord = ChordBuilder.buildDiatonicChord(keyRoot, ChordFunction.V)
        assertEquals(3, vChord.size)
        // Major third = 4 semitones above chord root (which is at offset 7)
        assertEquals(ChordBuilder.noteFrequency(7 + 4), vChord[1], FREQUENCY_TOLERANCE)

        // ii is minor
        val iiChord = ChordBuilder.buildDiatonicChord(keyRoot, ChordFunction.ii)
        assertEquals(3, iiChord.size)
        // Minor third = 3 semitones above chord root (which is at offset 2)
        assertEquals(ChordBuilder.noteFrequency(2 + 3), iiChord[1], FREQUENCY_TOLERANCE)

        // vii° is diminished
        val viiChord = ChordBuilder.buildDiatonicChord(keyRoot, ChordFunction.vii_dim)
        assertEquals(3, viiChord.size)
        // Diminished fifth = 6 semitones above chord root (which is at offset 11)
        assertEquals(ChordBuilder.noteFrequency(11 + 6), viiChord[2], FREQUENCY_TOLERANCE)
    }
}
