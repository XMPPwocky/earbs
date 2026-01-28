package net.xmppwocky.earbs.audio

/**
 * Musical scales and modes.
 * Each scale is defined by its intervals (semitones from root, including root and octave).
 */
enum class ScaleType(
    val intervals: List<Int>,
    val displayName: String
) {
    MAJOR(listOf(0, 2, 4, 5, 7, 9, 11, 12), "Major"),
    NATURAL_MINOR(listOf(0, 2, 3, 5, 7, 8, 10, 12), "Natural Minor"),
    HARMONIC_MINOR(listOf(0, 2, 3, 5, 7, 8, 11, 12), "Harmonic Minor"),
    MELODIC_MINOR(listOf(0, 2, 3, 5, 7, 9, 11, 12), "Melodic Minor"),
    DORIAN(listOf(0, 2, 3, 5, 7, 9, 10, 12), "Dorian"),
    MIXOLYDIAN(listOf(0, 2, 4, 5, 7, 9, 10, 12), "Mixolydian"),
    PHRYGIAN(listOf(0, 1, 3, 5, 7, 8, 10, 12), "Phrygian"),
    LYDIAN(listOf(0, 2, 4, 6, 7, 9, 11, 12), "Lydian"),
    MAJOR_PENTATONIC(listOf(0, 2, 4, 7, 9, 12), "Major Pentatonic"),
    MINOR_PENTATONIC(listOf(0, 3, 5, 7, 10, 12), "Minor Pentatonic");

    /** Number of notes in the scale (including root and octave) */
    val noteCount: Int get() = intervals.size
}
