package net.xmppwocky.earbs.model

import android.util.Log
import net.xmppwocky.earbs.audio.ChordType

private const val TAG = "ReviewSession"

/**
 * A review session with 20 cards, 1 trial each.
 * Each trial updates FSRS immediately upon answer.
 */
class ReviewSession(val cards: List<Card>) {
    val totalTrials: Int = cards.size

    var currentTrial: Int = 0
        private set

    var correctCount: Int = 0
        private set

    init {
        Log.i(TAG, "Starting review session with ${cards.size} cards")
        cards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.displayName}")
        }
    }

    /**
     * Get the current card for this trial.
     */
    fun getCurrentCard(): Card? = cards.getOrNull(currentTrial)

    /**
     * Record the answer for the current trial and advance to next.
     */
    fun recordAnswer(correct: Boolean) {
        val card = getCurrentCard()
        if (card == null) {
            Log.w(TAG, "recordAnswer called but no current card")
            return
        }

        if (correct) {
            correctCount++
        }

        Log.i(TAG, "Trial ${currentTrial + 1}/${totalTrials}: ${card.displayName} - ${if (correct) "CORRECT" else "WRONG"}")
        Log.i(TAG, "  Running total: $correctCount correct")

        currentTrial++
    }

    /**
     * Check if session is complete.
     */
    fun isComplete(): Boolean = currentTrial >= totalTrials

    /**
     * Get unique chord types in this session (for answer buttons).
     */
    fun getChordTypes(): List<ChordType> = cards.map { it.chordType }.distinct()
}
