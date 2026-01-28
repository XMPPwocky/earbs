package net.xmppwocky.earbs.audio

import android.util.Log

private const val TAG = "ScaleBuilder"

/**
 * Builder for scale frequencies.
 * Creates the sequence of frequencies needed for a scale based on scale type, root note, and direction.
 */
object ScaleBuilder {

    /**
     * Build a scale (list of frequencies) from a scale type and root note.
     *
     * @param scaleType The type of scale (Major, Minor, etc.)
     * @param rootSemitones The root note as semitones from A4
     * @param direction ASCENDING plays root to octave, DESCENDING plays octave to root,
     *                  BOTH plays ascending then descending
     * @return List of frequencies in Hz for each note in the scale (in playback order)
     */
    fun buildScale(
        scaleType: ScaleType,
        rootSemitones: Int,
        direction: ScaleDirection
    ): List<Float> {
        // Build ascending scale frequencies
        val ascendingFreqs = scaleType.intervals.map { interval ->
            ChordBuilder.noteFrequency(rootSemitones + interval)
        }

        val frequencies = when (direction) {
            ScaleDirection.ASCENDING -> ascendingFreqs
            ScaleDirection.DESCENDING -> ascendingFreqs.reversed()
            ScaleDirection.BOTH -> {
                // Ascending then descending, but don't repeat the top note
                ascendingFreqs + ascendingFreqs.dropLast(1).reversed()
            }
        }

        Log.d(TAG, "Built ${scaleType.displayName} scale ($direction)")
        Log.d(TAG, "  Root: $rootSemitones semitones from A4")
        Log.d(TAG, "  Intervals: ${scaleType.intervals}")
        Log.d(TAG, "  Note count: ${frequencies.size}")
        Log.d(TAG, "  Frequencies: ${frequencies.map { "%.2f".format(it) }}")

        return frequencies
    }

    /**
     * Get a random root note within the given octave for scale playback.
     * Uses ChordBuilder's existing implementation.
     */
    fun randomRootInOctave(octave: Int): Int = ChordBuilder.randomRootInOctave(octave)
}
