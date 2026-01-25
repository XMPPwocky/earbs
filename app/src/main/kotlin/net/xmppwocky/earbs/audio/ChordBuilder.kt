package net.xmppwocky.earbs.audio

import android.util.Log
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.ChordQuality
import net.xmppwocky.earbs.model.KeyQuality
import net.xmppwocky.earbs.model.ProgressionType
import kotlin.math.pow
import kotlin.random.Random

private const val TAG = "ChordBuilder"

/**
 * Chord types with their intervals (semitones from root).
 * Triads: MAJOR, MINOR, SUS2, SUS4
 * 7th chords: DOM7, MAJ7, MIN7, DIM7
 */
enum class ChordType(val intervals: List<Int>, val displayName: String) {
    // Triads
    MAJOR(listOf(0, 4, 7), "Major"),
    MINOR(listOf(0, 3, 7), "Minor"),
    SUS2(listOf(0, 2, 7), "Sus2"),
    SUS4(listOf(0, 5, 7), "Sus4"),
    // 7th chords
    DOM7(listOf(0, 4, 7, 10), "Dom7"),
    MAJ7(listOf(0, 4, 7, 11), "Maj7"),
    MIN7(listOf(0, 3, 7, 10), "Min7"),
    DIM7(listOf(0, 3, 6, 9), "Dim7");

    companion object {
        /**
         * Triad chord types.
         */
        val TRIADS = listOf(MAJOR, MINOR, SUS2, SUS4)

        /**
         * 7th chord types.
         */
        val SEVENTHS = listOf(DOM7, MAJ7, MIN7, DIM7)

        /**
         * Get the basic chord types for Epic 1.
         */
        val BASIC_TYPES = TRIADS
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

    // ========== Diatonic chord functions for chord function game ==========

    /**
     * Build a tonic chord (I or i) for the given key root.
     *
     * @param keyRootSemitones The root of the key (semitones from A4)
     * @param keyQuality MAJOR for I chord, MINOR for i chord
     * @return List of frequencies in Hz for the tonic chord
     */
    fun buildTonicChord(keyRootSemitones: Int, keyQuality: KeyQuality): List<Float> {
        val intervals = when (keyQuality) {
            KeyQuality.MAJOR -> listOf(0, 4, 7)  // Major triad
            KeyQuality.MINOR -> listOf(0, 3, 7)  // Minor triad
        }

        val frequencies = intervals.map { interval ->
            noteFrequency(keyRootSemitones + interval)
        }

        Log.d(TAG, "Built tonic chord (${if (keyQuality == KeyQuality.MAJOR) "I" else "i"}) at root $keyRootSemitones")
        Log.d(TAG, "Frequencies: ${frequencies.map { "%.2f".format(it) }}")

        return frequencies
    }

    /**
     * Build a diatonic chord for the given chord function within a key.
     *
     * @param keyRootSemitones The root of the key (semitones from A4)
     * @param function The chord function (IV, V, vi, etc.)
     * @return List of frequencies in Hz for the chord
     */
    fun buildDiatonicChord(keyRootSemitones: Int, function: ChordFunction): List<Float> {
        // Chord root is key root + function's semitone offset
        val chordRootSemitones = keyRootSemitones + function.semitoneOffset

        // Get intervals based on chord quality
        val intervals = function.quality.intervals

        val frequencies = intervals.map { interval ->
            noteFrequency(chordRootSemitones + interval)
        }

        Log.d(TAG, "Built diatonic chord ${function.displayName} at key root $keyRootSemitones")
        Log.d(TAG, "Chord root: $chordRootSemitones (offset ${function.semitoneOffset})")
        Log.d(TAG, "Quality: ${function.quality.name}, intervals: $intervals")
        Log.d(TAG, "Frequencies: ${frequencies.map { "%.2f".format(it) }}")

        return frequencies
    }

    // ========== Chord progressions for chord progression game ==========

    /**
     * Build a chord progression for the given progression type.
     * Returns a list of chords, each chord being a list of frequencies.
     *
     * @param keyRootSemitones The root of the key (semitones from A4)
     * @param progression The progression type - includes fixed key quality and chord qualities
     * @return List of chords, each chord being a List<Float> of frequencies
     */
    fun buildProgression(
        keyRootSemitones: Int,
        progression: ProgressionType
    ): List<List<Float>> {
        Log.d(TAG, "Building progression ${progression.displayName} in ${progression.keyQuality.name} key")
        Log.d(TAG, "Key root: $keyRootSemitones semitones from A4")

        val chordQualities = progression.chordQualities
        val semitoneOffsets = progression.semitoneOffsets

        val chords = semitoneOffsets.mapIndexed { index, offset ->
            val chordRoot = keyRootSemitones + offset
            val quality = chordQualities[index]
            val intervals = quality.intervals

            val frequencies = intervals.map { interval ->
                noteFrequency(chordRoot + interval)
            }

            Log.d(TAG, "  Chord $index: offset=$offset, quality=${quality.name}, freqs=${frequencies.map { "%.2f".format(it) }}")

            frequencies
        }

        Log.d(TAG, "Progression built: ${chords.size} chords")
        return chords
    }
}
