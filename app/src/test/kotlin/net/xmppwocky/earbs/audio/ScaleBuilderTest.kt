package net.xmppwocky.earbs.audio

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScaleBuilderTest {

    companion object {
        private const val FREQUENCY_TOLERANCE = 0.01f
    }

    // ========== Basic scale building tests ==========

    @Test
    fun `buildScale for major scale returns correct 8 notes ascending`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR,
            0, // A4
            ScaleDirection.ASCENDING
        )

        assertEquals(8, frequencies.size)
        // Major scale intervals: 0, 2, 4, 5, 7, 9, 11, 12
        assertEquals(440.0f, frequencies[0], FREQUENCY_TOLERANCE) // Root A4
        assertEquals(ChordBuilder.noteFrequency(2), frequencies[1], FREQUENCY_TOLERANCE) // B4
        assertEquals(ChordBuilder.noteFrequency(4), frequencies[2], FREQUENCY_TOLERANCE) // C#5
        assertEquals(ChordBuilder.noteFrequency(5), frequencies[3], FREQUENCY_TOLERANCE) // D5
        assertEquals(ChordBuilder.noteFrequency(7), frequencies[4], FREQUENCY_TOLERANCE) // E5
        assertEquals(ChordBuilder.noteFrequency(9), frequencies[5], FREQUENCY_TOLERANCE) // F#5
        assertEquals(ChordBuilder.noteFrequency(11), frequencies[6], FREQUENCY_TOLERANCE) // G#5
        assertEquals(880.0f, frequencies[7], FREQUENCY_TOLERANCE) // Octave A5
    }

    @Test
    fun `buildScale for natural minor scale returns correct notes ascending`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.NATURAL_MINOR,
            0, // A4
            ScaleDirection.ASCENDING
        )

        assertEquals(8, frequencies.size)
        // Natural minor intervals: 0, 2, 3, 5, 7, 8, 10, 12
        assertEquals(440.0f, frequencies[0], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(3), frequencies[2], FREQUENCY_TOLERANCE) // Minor 3rd
        assertEquals(ChordBuilder.noteFrequency(8), frequencies[5], FREQUENCY_TOLERANCE) // Minor 6th
        assertEquals(ChordBuilder.noteFrequency(10), frequencies[6], FREQUENCY_TOLERANCE) // Minor 7th
    }

    @Test
    fun `buildScale for major pentatonic returns correct 6 notes`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR_PENTATONIC,
            0, // A4
            ScaleDirection.ASCENDING
        )

        assertEquals(6, frequencies.size)
        // Major pentatonic intervals: 0, 2, 4, 7, 9, 12
        assertEquals(440.0f, frequencies[0], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(2), frequencies[1], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(4), frequencies[2], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(7), frequencies[3], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(9), frequencies[4], FREQUENCY_TOLERANCE)
        assertEquals(880.0f, frequencies[5], FREQUENCY_TOLERANCE)
    }

    @Test
    fun `buildScale for minor pentatonic returns correct 6 notes`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MINOR_PENTATONIC,
            0, // A4
            ScaleDirection.ASCENDING
        )

        assertEquals(6, frequencies.size)
        // Minor pentatonic intervals: 0, 3, 5, 7, 10, 12
        assertEquals(440.0f, frequencies[0], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(3), frequencies[1], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(5), frequencies[2], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(7), frequencies[3], FREQUENCY_TOLERANCE)
        assertEquals(ChordBuilder.noteFrequency(10), frequencies[4], FREQUENCY_TOLERANCE)
        assertEquals(880.0f, frequencies[5], FREQUENCY_TOLERANCE)
    }

    // ========== Direction tests ==========

    @Test
    fun `ascending direction returns notes from low to high`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR,
            0,
            ScaleDirection.ASCENDING
        )

        // Verify each subsequent note is higher
        for (i in 0 until frequencies.size - 1) {
            assertTrue(
                "Note ${i + 1} should be higher than note $i",
                frequencies[i] < frequencies[i + 1]
            )
        }
    }

    @Test
    fun `descending direction returns notes from high to low`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR,
            0,
            ScaleDirection.DESCENDING
        )

        // Verify each subsequent note is lower
        for (i in 0 until frequencies.size - 1) {
            assertTrue(
                "Note ${i + 1} should be lower than note $i",
                frequencies[i] > frequencies[i + 1]
            )
        }
    }

    @Test
    fun `both direction returns ascending then descending without repeating top note`() {
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR,
            0,
            ScaleDirection.BOTH
        )

        // Major scale: 8 notes ascending + 7 notes descending (no repeat of top) = 15 notes
        assertEquals(15, frequencies.size)

        // First 8 notes should be ascending
        for (i in 0 until 7) {
            assertTrue(
                "First half: Note ${i + 1} should be higher than note $i",
                frequencies[i] < frequencies[i + 1]
            )
        }

        // Last 7 notes should be descending
        for (i in 8 until frequencies.size - 1) {
            assertTrue(
                "Second half: Note ${i + 1} should be lower than note $i",
                frequencies[i] > frequencies[i + 1]
            )
        }

        // First and last notes should be the same (root)
        assertEquals(frequencies[0], frequencies[frequencies.size - 1], FREQUENCY_TOLERANCE)
    }

    @Test
    fun `descending swaps the same notes as ascending`() {
        val ascending = ScaleBuilder.buildScale(ScaleType.MAJOR, 0, ScaleDirection.ASCENDING)
        val descending = ScaleBuilder.buildScale(ScaleType.MAJOR, 0, ScaleDirection.DESCENDING)

        // Descending should be the reverse of ascending
        assertEquals(ascending.size, descending.size)
        for (i in ascending.indices) {
            assertEquals(
                ascending[i],
                descending[ascending.size - 1 - i],
                FREQUENCY_TOLERANCE
            )
        }
    }

    // ========== Root offset tests ==========

    @Test
    fun `buildScale applies root offset correctly`() {
        // Major scale at A5 (12 semitones from A4)
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR,
            12, // A5
            ScaleDirection.ASCENDING
        )

        assertEquals(880.0f, frequencies[0], FREQUENCY_TOLERANCE) // Root A5
        assertEquals(1760.0f, frequencies[7], FREQUENCY_TOLERANCE) // Octave A6
    }

    @Test
    fun `buildScale works with negative root semitones`() {
        // Major scale at A3 (-12 semitones from A4)
        val frequencies = ScaleBuilder.buildScale(
            ScaleType.MAJOR,
            -12, // A3
            ScaleDirection.ASCENDING
        )

        assertEquals(220.0f, frequencies[0], FREQUENCY_TOLERANCE) // Root A3
        assertEquals(440.0f, frequencies[7], FREQUENCY_TOLERANCE) // Octave A4
    }

    // ========== Scale type interval verification ==========

    @Test
    fun `all scale types have correct intervals`() {
        // Verify intervals for all 10 scale types
        assertEquals(listOf(0, 2, 4, 5, 7, 9, 11, 12), ScaleType.MAJOR.intervals)
        assertEquals(listOf(0, 2, 3, 5, 7, 8, 10, 12), ScaleType.NATURAL_MINOR.intervals)
        assertEquals(listOf(0, 2, 3, 5, 7, 8, 11, 12), ScaleType.HARMONIC_MINOR.intervals)
        assertEquals(listOf(0, 2, 3, 5, 7, 9, 11, 12), ScaleType.MELODIC_MINOR.intervals)
        assertEquals(listOf(0, 2, 3, 5, 7, 9, 10, 12), ScaleType.DORIAN.intervals)
        assertEquals(listOf(0, 2, 4, 5, 7, 9, 10, 12), ScaleType.MIXOLYDIAN.intervals)
        assertEquals(listOf(0, 1, 3, 5, 7, 8, 10, 12), ScaleType.PHRYGIAN.intervals)
        assertEquals(listOf(0, 2, 4, 6, 7, 9, 11, 12), ScaleType.LYDIAN.intervals)
        assertEquals(listOf(0, 2, 4, 7, 9, 12), ScaleType.MAJOR_PENTATONIC.intervals)
        assertEquals(listOf(0, 3, 5, 7, 10, 12), ScaleType.MINOR_PENTATONIC.intervals)
    }

    @Test
    fun `ScaleType has 10 scales`() {
        assertEquals(10, ScaleType.entries.size)
    }

    @Test
    fun `buildScale produces consistent frequencies for all scale types`() {
        val root = 0 // A4

        ScaleType.entries.forEach { scaleType ->
            val frequencies = ScaleBuilder.buildScale(
                scaleType,
                root,
                ScaleDirection.ASCENDING
            )

            assertEquals(
                "Scale ${scaleType.name} should have ${scaleType.intervals.size} notes",
                scaleType.intervals.size,
                frequencies.size
            )

            // Verify each note corresponds to the expected interval
            scaleType.intervals.forEachIndexed { index, interval ->
                val expected = ChordBuilder.noteFrequency(root + interval)
                assertEquals(
                    "Frequency at index $index for ${scaleType.name}",
                    expected,
                    frequencies[index],
                    FREQUENCY_TOLERANCE
                )
            }
        }
    }

    // ========== Random root tests ==========

    @Test
    fun `randomRootInOctave delegates to ChordBuilder`() {
        // Octave 4: C4 (-9) to B4 (+2)
        repeat(100) {
            val semitones = ScaleBuilder.randomRootInOctave(4)
            assertTrue("Semitone $semitones should be >= -9", semitones >= -9)
            assertTrue("Semitone $semitones should be <= 2", semitones <= 2)
        }
    }

    // ========== Direction enum tests ==========

    @Test
    fun `ScaleDirection has 3 values`() {
        assertEquals(3, ScaleDirection.entries.size)
    }

    @Test
    fun `ScaleDirection display names are correct`() {
        assertEquals("Ascending", ScaleDirection.ASCENDING.displayName)
        assertEquals("Descending", ScaleDirection.DESCENDING.displayName)
        assertEquals("Both", ScaleDirection.BOTH.displayName)
    }
}
