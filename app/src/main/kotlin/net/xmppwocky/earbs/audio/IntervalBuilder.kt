package net.xmppwocky.earbs.audio

import android.util.Log

private const val TAG = "IntervalBuilder"

/**
 * Builder for interval frequencies.
 * Creates the two frequencies needed for an interval based on interval type and root note.
 */
object IntervalBuilder {

    /**
     * Build an interval (pair of frequencies) from an interval type and root note.
     *
     * @param intervalType The type of interval (Minor 2nd, Perfect 5th, etc.)
     * @param rootSemitones The lower note as semitones from A4
     * @param direction The direction of the interval (ASCENDING, DESCENDING, HARMONIC)
     * @return Pair of frequencies in Hz - first is the first note to play, second is the second note
     *         For HARMONIC, both are played simultaneously
     */
    fun buildInterval(
        intervalType: IntervalType,
        rootSemitones: Int,
        direction: IntervalDirection
    ): Pair<Float, Float> {
        val lowerFreq = ChordBuilder.noteFrequency(rootSemitones)
        val upperFreq = ChordBuilder.noteFrequency(rootSemitones + intervalType.semitones)

        // For descending, swap the order (higher note plays first)
        val (firstFreq, secondFreq) = when (direction) {
            IntervalDirection.ASCENDING -> Pair(lowerFreq, upperFreq)
            IntervalDirection.DESCENDING -> Pair(upperFreq, lowerFreq)
            IntervalDirection.HARMONIC -> Pair(lowerFreq, upperFreq) // Both played together
        }

        Log.d(TAG, "Built ${intervalType.displayName} interval ($direction)")
        Log.d(TAG, "  Root: $rootSemitones semitones from A4")
        Log.d(TAG, "  Lower freq: ${"%.2f".format(lowerFreq)} Hz")
        Log.d(TAG, "  Upper freq: ${"%.2f".format(upperFreq)} Hz")
        Log.d(TAG, "  First: ${"%.2f".format(firstFreq)} Hz, Second: ${"%.2f".format(secondFreq)} Hz")

        return Pair(firstFreq, secondFreq)
    }

    /**
     * Get a random root note within the given octave for interval playback.
     * Uses ChordBuilder's existing implementation.
     */
    fun randomRootInOctave(octave: Int): Int = ChordBuilder.randomRootInOctave(octave)
}
