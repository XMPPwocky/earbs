package net.xmppwocky.earbs.model

/**
 * Quality of a chord (major, minor, or diminished).
 */
enum class ChordQuality(val intervals: List<Int>) {
    MAJOR(listOf(0, 4, 7)),
    MINOR(listOf(0, 3, 7)),
    DIMINISHED(listOf(0, 3, 6))
}

/**
 * Quality of the key (major or minor).
 */
enum class KeyQuality {
    MAJOR,
    MINOR
}

/**
 * Chord function (roman numeral) relative to a tonic.
 *
 * Major key: I, ii, iii, IV, V, vi, vii°
 * Minor key: i, ii°, III, iv, v, VI, VII
 *
 * Note: I/i (tonic) is omitted since identifying tonic vs tonic is trivial.
 */
enum class ChordFunction(
    val displayName: String,
    val semitoneOffset: Int,
    val quality: ChordQuality,
    val keyQuality: KeyQuality
) {
    // Major key functions (skip I since tonic vs tonic is trivial)
    ii("ii", 2, ChordQuality.MINOR, KeyQuality.MAJOR),
    iii("iii", 4, ChordQuality.MINOR, KeyQuality.MAJOR),
    IV("IV", 5, ChordQuality.MAJOR, KeyQuality.MAJOR),
    V("V", 7, ChordQuality.MAJOR, KeyQuality.MAJOR),
    vi("vi", 9, ChordQuality.MINOR, KeyQuality.MAJOR),
    vii_dim("vii\u00B0", 11, ChordQuality.DIMINISHED, KeyQuality.MAJOR),

    // Minor key functions (skip i since tonic vs tonic is trivial)
    ii_dim("ii\u00B0", 2, ChordQuality.DIMINISHED, KeyQuality.MINOR),
    III("III", 3, ChordQuality.MAJOR, KeyQuality.MINOR),
    iv("iv", 5, ChordQuality.MINOR, KeyQuality.MINOR),
    v("v", 7, ChordQuality.MINOR, KeyQuality.MINOR),
    VI("VI", 8, ChordQuality.MAJOR, KeyQuality.MINOR),
    VII("VII", 10, ChordQuality.MAJOR, KeyQuality.MINOR);

    companion object {
        /**
         * Get all functions for a given key quality.
         */
        fun forKeyQuality(keyQuality: KeyQuality): List<ChordFunction> {
            return entries.filter { it.keyQuality == keyQuality }
        }

        /**
         * Major key functions.
         */
        val MAJOR_FUNCTIONS = entries.filter { it.keyQuality == KeyQuality.MAJOR }

        /**
         * Minor key functions.
         */
        val MINOR_FUNCTIONS = entries.filter { it.keyQuality == KeyQuality.MINOR }
    }
}
