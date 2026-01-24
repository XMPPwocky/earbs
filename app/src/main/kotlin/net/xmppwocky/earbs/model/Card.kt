package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * A card represents a (chord_type, octave, playback_mode) tuple for spaced repetition.
 * User answers chord type only; octave and playback mode affect how the chord sounds.
 *
 * Playback mode is now a property of the card (not a user toggle). This prevents
 * users from "cheesing" by identifying the playback mode instead of the chord quality.
 */
data class Card(
    val chordType: ChordType,
    val octave: Int,
    val playbackMode: PlaybackMode
) {
    val id: String get() = "${chordType.name}_${octave}_${playbackMode.name}"
    val displayName: String get() = "${chordType.displayName} @ Oct $octave (${playbackMode.name.lowercase()})"
}

/**
 * Tracks performance for a single card within a review session.
 */
data class CardScore(
    val card: Card,
    var correct: Int = 0,
    var total: Int = 0
) {
    val hitRate: Float get() = if (total > 0) correct.toFloat() / total else 0f

    /**
     * Calculate grade based on number correct out of 10 trials.
     * - 10/10 → Easy
     * - 9/10 → Good
     * - 8/10 → Hard
     * - ≤7/10 → Again
     */
    val grade: Grade get() = when (correct) {
        10 -> Grade.EASY
        9 -> Grade.GOOD
        8 -> Grade.HARD
        else -> Grade.AGAIN
    }
}

/**
 * FSRS-style grades for spaced repetition scheduling.
 */
enum class Grade(val displayName: String) {
    EASY("Easy"),
    GOOD("Good"),
    HARD("Hard"),
    AGAIN("Again")
}
