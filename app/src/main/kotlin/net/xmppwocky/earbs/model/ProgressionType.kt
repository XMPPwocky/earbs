package net.xmppwocky.earbs.model

/**
 * Category of progression - resolving (ends on I) or looping (cycles).
 */
enum class ProgressionCategory {
    RESOLVING,
    LOOP
}

/**
 * Chord progression types for ear training.
 *
 * Each progression has a fixed key quality (major or minor) and fixed chord qualities.
 * There are 18 total progressions (14 active + 4 deprecated 5-chord).
 *
 * Major progressions use uppercase Roman numerals: I-IV-V-I
 * Minor progressions use lowercase Roman numerals: i-iv-v-i
 *
 * Progression patterns (active):
 * - 3-chord resolving: I-IV-I, I-V-I
 * - 4-chord resolving: I-IV-V-I, I-ii-V-I, I-vi-ii-V, I-vi-IV-V (loop used as resolving)
 * - 4-chord loops: I-V-vi-IV, i-v-VI-iv
 *
 * Deprecated 5-chord progressions (kept for historical data):
 * - I-vi-ii-V-I, I-vi-IV-V-I, i-VI-ii°-v-i, i-VI-iv-v-i
 */
enum class ProgressionType(
    val displayName: String,
    val semitoneOffsets: List<Int>,
    val category: ProgressionCategory,
    val keyQuality: KeyQuality,
    val chordQualities: List<ChordQuality>
) {
    // ========== MAJOR KEY PROGRESSIONS ==========

    // 3-chord progressions (resolving)
    I_IV_I_MAJOR(
        displayName = "I - IV - I",
        semitoneOffsets = listOf(0, 5, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MAJOR,
        chordQualities = listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR)
    ),

    I_V_I_MAJOR(
        displayName = "I - V - I",
        semitoneOffsets = listOf(0, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MAJOR,
        chordQualities = listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR)
    ),

    // 4-chord progressions (resolving)
    I_IV_V_I_MAJOR(
        displayName = "I - IV - V - I",
        semitoneOffsets = listOf(0, 5, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MAJOR,
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR
        )
    ),

    I_ii_V_I_MAJOR(
        displayName = "I - ii - V - I",
        semitoneOffsets = listOf(0, 2, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MAJOR,
        // ii is minor in major key
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MAJOR
        )
    ),

    // New 4-chord progression (replaces 5-chord I-vi-ii-V-I)
    // Categorized as LOOP since it ends on V (doesn't resolve to I)
    I_vi_ii_V_MAJOR(
        displayName = "I - vi - ii - V",
        semitoneOffsets = listOf(0, 9, 2, 7),
        category = ProgressionCategory.LOOP,
        keyQuality = KeyQuality.MAJOR,
        // vi is minor, ii is minor in major key
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MAJOR
        )
    ),

    // 5-chord progressions (resolving) - DEPRECATED: kept for historical data
    I_vi_ii_V_I_MAJOR(
        displayName = "I - vi - ii - V - I",
        semitoneOffsets = listOf(0, 9, 2, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MAJOR,
        // vi is minor, ii is minor in major key
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR,
            ChordQuality.MAJOR, ChordQuality.MAJOR
        )
    ),

    I_vi_IV_V_I_MAJOR(
        displayName = "I - vi - IV - V - I",
        semitoneOffsets = listOf(0, 9, 5, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MAJOR,
        // vi is minor in major key
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR,
            ChordQuality.MAJOR, ChordQuality.MAJOR
        )
    ),

    // 4-chord loops (do not resolve to I)
    I_V_vi_IV_MAJOR(
        displayName = "I - V - vi - IV",
        semitoneOffsets = listOf(0, 7, 9, 5),
        category = ProgressionCategory.LOOP,
        keyQuality = KeyQuality.MAJOR,
        // vi is minor in major key
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR
        )
    ),

    I_vi_IV_V_MAJOR(
        displayName = "I - vi - IV - V",
        semitoneOffsets = listOf(0, 9, 5, 7),
        category = ProgressionCategory.LOOP,
        keyQuality = KeyQuality.MAJOR,
        // vi is minor in major key
        chordQualities = listOf(
            ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MAJOR
        )
    ),

    // ========== MINOR KEY PROGRESSIONS ==========

    // 3-chord progressions (resolving)
    i_iv_i_MINOR(
        displayName = "i - iv - i",
        semitoneOffsets = listOf(0, 5, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MINOR,
        chordQualities = listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR)
    ),

    i_v_i_MINOR(
        displayName = "i - v - i",
        semitoneOffsets = listOf(0, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MINOR,
        chordQualities = listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR)
    ),

    // 4-chord progressions (resolving)
    i_iv_v_i_MINOR(
        displayName = "i - iv - v - i",
        semitoneOffsets = listOf(0, 5, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MINOR,
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR
        )
    ),

    i_iio_v_i_MINOR(
        displayName = "i - ii° - v - i",
        semitoneOffsets = listOf(0, 2, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MINOR,
        // ii° is diminished in minor key
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.DIMINISHED, ChordQuality.MINOR, ChordQuality.MINOR
        )
    ),

    // New 4-chord progression (replaces 5-chord i-VI-ii°-v-i)
    // Categorized as LOOP since it ends on v (doesn't resolve to i)
    i_VI_iio_v_MINOR(
        displayName = "i - VI - ii° - v",
        semitoneOffsets = listOf(0, 9, 2, 7),
        category = ProgressionCategory.LOOP,
        keyQuality = KeyQuality.MINOR,
        // VI is major, ii° is diminished in minor key
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.DIMINISHED, ChordQuality.MINOR
        )
    ),

    // 5-chord progressions (resolving) - DEPRECATED: kept for historical data
    i_VI_iio_v_i_MINOR(
        displayName = "i - VI - ii° - v - i",
        semitoneOffsets = listOf(0, 9, 2, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MINOR,
        // VI is major, ii° is diminished in minor key
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.DIMINISHED,
            ChordQuality.MINOR, ChordQuality.MINOR
        )
    ),

    i_VI_iv_v_i_MINOR(
        displayName = "i - VI - iv - v - i",
        semitoneOffsets = listOf(0, 9, 5, 7, 0),
        category = ProgressionCategory.RESOLVING,
        keyQuality = KeyQuality.MINOR,
        // VI is major in minor key
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR,
            ChordQuality.MINOR, ChordQuality.MINOR
        )
    ),

    // 4-chord loops (do not resolve to i)
    i_v_VI_iv_MINOR(
        displayName = "i - v - VI - iv",
        semitoneOffsets = listOf(0, 7, 9, 5),
        category = ProgressionCategory.LOOP,
        keyQuality = KeyQuality.MINOR,
        // VI is major in minor key
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR
        )
    ),

    i_VI_iv_v_MINOR(
        displayName = "i - VI - iv - v",
        semitoneOffsets = listOf(0, 9, 5, 7),
        category = ProgressionCategory.LOOP,
        keyQuality = KeyQuality.MINOR,
        // VI is major in minor key
        chordQualities = listOf(
            ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR
        )
    );

    companion object {
        /**
         * All major key progressions.
         */
        val MAJOR_PROGRESSIONS = entries.filter { it.keyQuality == KeyQuality.MAJOR }

        /**
         * All minor key progressions.
         */
        val MINOR_PROGRESSIONS = entries.filter { it.keyQuality == KeyQuality.MINOR }

        /**
         * 3-chord progressions (simplest, unlock first).
         */
        val THREE_CHORD = entries.filter { it.semitoneOffsets.size == 3 }

        /**
         * 4-chord resolving progressions.
         */
        val FOUR_CHORD_RESOLVING = entries.filter {
            it.semitoneOffsets.size == 4 && it.category == ProgressionCategory.RESOLVING
        }

        /**
         * 5-chord progressions.
         */
        val FIVE_CHORD = entries.filter { it.semitoneOffsets.size == 5 }

        /**
         * Loop progressions (unlock last).
         */
        val LOOPS = entries.filter { it.category == ProgressionCategory.LOOP }
    }
}
