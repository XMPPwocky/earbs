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
 * Each progression is a sequence of chord functions that can be played in either
 * major or minor keys. The semitone offsets define the root of each chord relative
 * to the key tonic, and the chordQualities function computes the quality (major/minor/dim)
 * of each chord based on the key quality.
 *
 * Progressions List:
 * - Resolving (end on I): I-IV-I, I-V-I, I-IV-V-I, I-ii-V-I, I-vi-ii-V-I, I-vi-IV-V-I
 * - Loops: I-V-vi-IV, I-vi-IV-V
 */
enum class ProgressionType(
    val displayName: String,
    val semitoneOffsets: List<Int>,
    val category: ProgressionCategory,
    val chordQualities: (KeyQuality) -> List<ChordQuality>
) {
    // 3-chord progressions (resolving)
    I_IV_I(
        displayName = "I - IV - I",
        semitoneOffsets = listOf(0, 5, 0),
        category = ProgressionCategory.RESOLVING,
        chordQualities = { key ->
            when (key) {
                KeyQuality.MAJOR -> listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR)
                KeyQuality.MINOR -> listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR)
            }
        }
    ),

    I_V_I(
        displayName = "I - V - I",
        semitoneOffsets = listOf(0, 7, 0),
        category = ProgressionCategory.RESOLVING,
        chordQualities = { key ->
            when (key) {
                KeyQuality.MAJOR -> listOf(ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR)
                KeyQuality.MINOR -> listOf(ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR)
            }
        }
    ),

    // 4-chord progressions (resolving)
    I_IV_V_I(
        displayName = "I - IV - V - I",
        semitoneOffsets = listOf(0, 5, 7, 0),
        category = ProgressionCategory.RESOLVING,
        chordQualities = { key ->
            when (key) {
                KeyQuality.MAJOR -> listOf(
                    ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MAJOR
                )
                KeyQuality.MINOR -> listOf(
                    ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MINOR
                )
            }
        }
    ),

    I_ii_V_I(
        displayName = "I - ii - V - I",
        semitoneOffsets = listOf(0, 2, 7, 0),
        category = ProgressionCategory.RESOLVING,
        chordQualities = { key ->
            when (key) {
                // ii is minor in major key
                KeyQuality.MAJOR -> listOf(
                    ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MAJOR
                )
                // ii° is diminished in minor key
                KeyQuality.MINOR -> listOf(
                    ChordQuality.MINOR, ChordQuality.DIMINISHED, ChordQuality.MINOR, ChordQuality.MINOR
                )
            }
        }
    ),

    // 5-chord progressions (resolving)
    I_vi_ii_V_I(
        displayName = "I - vi - ii - V - I",
        semitoneOffsets = listOf(0, 9, 2, 7, 0),
        category = ProgressionCategory.RESOLVING,
        chordQualities = { key ->
            when (key) {
                // vi is minor, ii is minor in major key
                KeyQuality.MAJOR -> listOf(
                    ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR,
                    ChordQuality.MAJOR, ChordQuality.MAJOR
                )
                // VI is major, ii° is diminished in minor key
                KeyQuality.MINOR -> listOf(
                    ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.DIMINISHED,
                    ChordQuality.MINOR, ChordQuality.MINOR
                )
            }
        }
    ),

    I_vi_IV_V_I(
        displayName = "I - vi - IV - V - I",
        semitoneOffsets = listOf(0, 9, 5, 7, 0),
        category = ProgressionCategory.RESOLVING,
        chordQualities = { key ->
            when (key) {
                // vi is minor in major key
                KeyQuality.MAJOR -> listOf(
                    ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR,
                    ChordQuality.MAJOR, ChordQuality.MAJOR
                )
                // VI is major in minor key
                KeyQuality.MINOR -> listOf(
                    ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR,
                    ChordQuality.MINOR, ChordQuality.MINOR
                )
            }
        }
    ),

    // 4-chord loops (do not resolve to I)
    I_V_vi_IV(
        displayName = "I - V - vi - IV",
        semitoneOffsets = listOf(0, 7, 9, 5),
        category = ProgressionCategory.LOOP,
        chordQualities = { key ->
            when (key) {
                // vi is minor in major key
                KeyQuality.MAJOR -> listOf(
                    ChordQuality.MAJOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR
                )
                // VI is major in minor key
                KeyQuality.MINOR -> listOf(
                    ChordQuality.MINOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR
                )
            }
        }
    ),

    I_vi_IV_V(
        displayName = "I - vi - IV - V",
        semitoneOffsets = listOf(0, 9, 5, 7),
        category = ProgressionCategory.LOOP,
        chordQualities = { key ->
            when (key) {
                // vi is minor in major key
                KeyQuality.MAJOR -> listOf(
                    ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MAJOR
                )
                // VI is major in minor key
                KeyQuality.MINOR -> listOf(
                    ChordQuality.MINOR, ChordQuality.MAJOR, ChordQuality.MINOR, ChordQuality.MINOR
                )
            }
        }
    );

    companion object {
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
