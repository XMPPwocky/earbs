package net.xmppwocky.earbs.audio

/**
 * Musical intervals from unison to octave.
 * Each interval is defined by its semitone distance from the root.
 */
enum class IntervalType(val semitones: Int, val displayName: String) {
    MINOR_2ND(1, "Minor 2nd"),
    MAJOR_2ND(2, "Major 2nd"),
    MINOR_3RD(3, "Minor 3rd"),
    MAJOR_3RD(4, "Major 3rd"),
    PERFECT_4TH(5, "Perfect 4th"),
    TRITONE(6, "Tritone"),
    PERFECT_5TH(7, "Perfect 5th"),
    MINOR_6TH(8, "Minor 6th"),
    MAJOR_6TH(9, "Major 6th"),
    MINOR_7TH(10, "Minor 7th"),
    MAJOR_7TH(11, "Major 7th"),
    OCTAVE(12, "Octave");

    companion object {
        fun fromSemitones(semitones: Int): IntervalType? =
            entries.find { it.semitones == semitones }
    }
}
