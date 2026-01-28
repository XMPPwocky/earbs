package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.IntervalType

/**
 * Sealed interface for all game answer types.
 * Each game type has its own answer type with the correct answer value.
 */
sealed interface GameAnswer {
    /** Human-readable display name for answer buttons */
    val displayName: String

    /**
     * Answer for the chord type recognition game.
     * User identifies the chord quality (Major, Minor, Sus2, etc.)
     */
    data class ChordTypeAnswer(val chordType: ChordType) : GameAnswer {
        override val displayName: String get() = chordType.displayName
    }

    /**
     * Answer for the chord function recognition game.
     * User identifies the roman numeral function (ii, IV, V, etc.)
     */
    data class FunctionAnswer(val function: ChordFunction) : GameAnswer {
        override val displayName: String get() = function.displayName
    }

    /**
     * Answer for the chord progression recognition game.
     * User identifies the progression (I-IV-I, I-V-vi-IV, etc.)
     */
    data class ProgressionAnswer(val progression: ProgressionType) : GameAnswer {
        override val displayName: String get() = progression.displayName
    }

    /**
     * Answer for the interval recognition game.
     * User identifies the interval (Minor 2nd, Major 3rd, Perfect 5th, etc.)
     */
    data class IntervalAnswer(val interval: IntervalType) : GameAnswer {
        override val displayName: String get() = interval.displayName
    }
}
