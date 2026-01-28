package net.xmppwocky.earbs.audio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IntervalBuilderTest {

    companion object {
        private const val FREQUENCY_TOLERANCE = 0.01f
    }

    // ========== Basic interval building tests ==========

    @Test
    fun `buildInterval for octave returns octave apart frequencies`() {
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.OCTAVE,
            0, // A4
            IntervalDirection.ASCENDING
        )

        assertEquals(440.0f, first, FREQUENCY_TOLERANCE)
        assertEquals(880.0f, second, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildInterval for perfect 5th returns correct interval`() {
        // Perfect 5th = 7 semitones
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.PERFECT_5TH,
            0, // A4
            IntervalDirection.ASCENDING
        )

        assertEquals(440.0f, first, FREQUENCY_TOLERANCE)
        // A4 + 7 semitones = E5
        val expectedSecond = ChordBuilder.noteFrequency(7)
        assertEquals(expectedSecond, second, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildInterval for minor 2nd returns correct interval`() {
        // Minor 2nd = 1 semitone
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.MINOR_2ND,
            0, // A4
            IntervalDirection.ASCENDING
        )

        assertEquals(440.0f, first, FREQUENCY_TOLERANCE)
        // A4 + 1 semitone = A#4/Bb4
        val expectedSecond = ChordBuilder.noteFrequency(1)
        assertEquals(expectedSecond, second, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildInterval for tritone returns correct interval`() {
        // Tritone = 6 semitones
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.TRITONE,
            0, // A4
            IntervalDirection.ASCENDING
        )

        assertEquals(440.0f, first, FREQUENCY_TOLERANCE)
        // A4 + 6 semitones = D#5/Eb5
        val expectedSecond = ChordBuilder.noteFrequency(6)
        assertEquals(expectedSecond, second, FREQUENCY_TOLERANCE)
    }

    // ========== Direction tests ==========

    @Test
    fun `ascending direction returns lower note first`() {
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.PERFECT_5TH,
            0,
            IntervalDirection.ASCENDING
        )

        assertTrue("First note should be lower than second", first < second)
    }

    @Test
    fun `descending direction returns higher note first`() {
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.PERFECT_5TH,
            0,
            IntervalDirection.DESCENDING
        )

        assertTrue("First note should be higher than second", first > second)
    }

    @Test
    fun `harmonic direction returns lower note first`() {
        // For harmonic intervals, both notes play together
        // Convention: return lower note as first (matches ascending)
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.PERFECT_5TH,
            0,
            IntervalDirection.HARMONIC
        )

        assertTrue("Lower note should be first", first < second)
    }

    @Test
    fun `descending swaps the same notes as ascending`() {
        val (ascFirst, ascSecond) = IntervalBuilder.buildInterval(
            IntervalType.MAJOR_3RD,
            0,
            IntervalDirection.ASCENDING
        )
        val (descFirst, descSecond) = IntervalBuilder.buildInterval(
            IntervalType.MAJOR_3RD,
            0,
            IntervalDirection.DESCENDING
        )

        // Descending should swap the notes
        assertEquals(ascFirst, descSecond, FREQUENCY_TOLERANCE)
        assertEquals(ascSecond, descFirst, FREQUENCY_TOLERANCE)
    }

    // ========== Root offset tests ==========

    @Test
    fun `buildInterval applies root offset correctly`() {
        // Perfect 5th at A5 (12 semitones from A4)
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.PERFECT_5TH,
            12, // A5
            IntervalDirection.ASCENDING
        )

        assertEquals(880.0f, first, FREQUENCY_TOLERANCE)
        // A5 + 7 semitones = E6
        val expectedSecond = ChordBuilder.noteFrequency(12 + 7)
        assertEquals(expectedSecond, second, FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildInterval works with negative root semitones`() {
        // Perfect 5th at A3 (-12 semitones from A4)
        val (first, second) = IntervalBuilder.buildInterval(
            IntervalType.PERFECT_5TH,
            -12, // A3
            IntervalDirection.ASCENDING
        )

        assertEquals(220.0f, first, FREQUENCY_TOLERANCE)
        // A3 + 7 semitones = E4
        val expectedSecond = ChordBuilder.noteFrequency(-12 + 7)
        assertEquals(expectedSecond, second, FREQUENCY_TOLERANCE)
    }

    // ========== Interval type semitone verification ==========

    @Test
    fun `all interval types have correct semitones`() {
        // Verify semitone values for all interval types (12 intervals, minor 2nd through octave)
        assertEquals(1, IntervalType.MINOR_2ND.semitones)
        assertEquals(2, IntervalType.MAJOR_2ND.semitones)
        assertEquals(3, IntervalType.MINOR_3RD.semitones)
        assertEquals(4, IntervalType.MAJOR_3RD.semitones)
        assertEquals(5, IntervalType.PERFECT_4TH.semitones)
        assertEquals(6, IntervalType.TRITONE.semitones)
        assertEquals(7, IntervalType.PERFECT_5TH.semitones)
        assertEquals(8, IntervalType.MINOR_6TH.semitones)
        assertEquals(9, IntervalType.MAJOR_6TH.semitones)
        assertEquals(10, IntervalType.MINOR_7TH.semitones)
        assertEquals(11, IntervalType.MAJOR_7TH.semitones)
        assertEquals(12, IntervalType.OCTAVE.semitones)
    }

    @Test
    fun `IntervalType has 12 intervals`() {
        assertEquals(12, IntervalType.entries.size)
    }

    @Test
    fun `buildInterval produces consistent frequencies for all interval types`() {
        val root = 0 // A4

        // Test that each interval type produces frequencies at expected distance
        IntervalType.entries.forEach { intervalType ->
            val (first, second) = IntervalBuilder.buildInterval(
                intervalType,
                root,
                IntervalDirection.ASCENDING
            )

            val expectedFirst = ChordBuilder.noteFrequency(root)
            val expectedSecond = ChordBuilder.noteFrequency(root + intervalType.semitones)

            assertEquals(
                "First frequency for ${intervalType.name}",
                expectedFirst,
                first,
                FREQUENCY_TOLERANCE
            )
            assertEquals(
                "Second frequency for ${intervalType.name}",
                expectedSecond,
                second,
                FREQUENCY_TOLERANCE
            )
        }
    }

    // ========== Random root tests ==========

    @Test
    fun `randomRootInOctave delegates to ChordBuilder`() {
        // Octave 4: C4 (-9) to B4 (+2)
        repeat(100) {
            val semitones = IntervalBuilder.randomRootInOctave(4)
            assertTrue("Semitone $semitones should be >= -9", semitones >= -9)
            assertTrue("Semitone $semitones should be <= 2", semitones <= 2)
        }
    }
}
