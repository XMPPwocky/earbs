package net.xmppwocky.earbs.audio

import android.util.Log
import kotlin.math.pow
import kotlin.random.Random

private const val TAG = "ChordBuilder"

/**
 * Chord types with their intervals (semitones from root).
 * For Epic 1, only the basic 4 types are used.
 */
enum class ChordType(val intervals: List<Int>, val displayName: String) {
    MAJOR(listOf(0, 4, 7), "Major"),
    MINOR(listOf(0, 3, 7), "Minor"),
    SUS2(listOf(0, 2, 7), "Sus2"),
    SUS4(listOf(0, 5, 7), "Sus4"),
    // Future chord types (not used in Epic 1)
    DOM7(listOf(0, 4, 7, 10), "Dom7"),
    MAJ7(listOf(0, 4, 7, 11), "Maj7"),
    MIN7(listOf(0, 3, 7, 10), "Min7");

    companion object {
        /**
         * Get the basic chord types for Epic 1.
         */
        val BASIC_TYPES = listOf(MAJOR, MINOR, SUS2, SUS4)
    }
}

object ChordBuilder {

    /**
     * Calculate frequency for a note given semitones from A4.
     * A4 = 440Hz
     * Frequency = 440 * 2^(semitones/12)
     */
    fun noteFrequency(semitonesFromA4: Int): Float {
        return 440f * 2f.pow(semitonesFromA4 / 12f)
    }

    /**
     * Build a chord (list of frequencies) from a chord type and root note.
     *
     * @param chordType The type of chord (Major, Minor, etc.)
     * @param rootSemitones The root note as semitones from A4
     * @return List of frequencies in Hz for each note in the chord
     */
    fun buildChord(chordType: ChordType, rootSemitones: Int): List<Float> {
        val frequencies = chordType.intervals.map { interval ->
            noteFrequency(rootSemitones + interval)
        }

        Log.d(TAG, "Built ${chordType.displayName} chord at root $rootSemitones semitones from A4")
        Log.d(TAG, "Intervals: ${chordType.intervals}")
        Log.d(TAG, "Frequencies: ${frequencies.map { "%.2f".format(it) }}")

        return frequencies
    }

    /**
     * Get a random root note within the given octave.
     *
     * Octave numbering: A4 = 440Hz is in octave 4.
     * Each octave spans 12 semitones (C to B).
     *
     * Reference points (semitones from A4):
     * - C4 = -9 (middle C)
     * - A4 = 0
     * - C5 = +3
     *
     * For each octave, we select from C to B (12 notes):
     * - Octave 3: C3 (-21) to B3 (-10)
     * - Octave 4: C4 (-9) to B4 (+2)
     * - Octave 5: C5 (+3) to B5 (+14)
     *
     * @param octave The octave number (3, 4, or 5)
     * @return Semitones from A4 for a random note in that octave
     */
    fun randomRootInOctave(octave: Int): Int {
        // C4 is -9 semitones from A4
        // Each octave shift is 12 semitones
        val cOfOctave = -9 + (octave - 4) * 12
        val randomOffset = Random.nextInt(12) // 0-11 gives C through B
        val semitones = cOfOctave + randomOffset

        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val noteName = noteNames[randomOffset]

        Log.d(TAG, "Random root in octave $octave: $noteName$octave ($semitones semitones from A4)")

        return semitones
    }

    /**
     * Pick a random chord type from the basic types.
     */
    fun randomChordType(): ChordType {
        val type = ChordType.BASIC_TYPES.random()
        Log.d(TAG, "Random chord type selected: ${type.displayName}")
        return type
    }
}
