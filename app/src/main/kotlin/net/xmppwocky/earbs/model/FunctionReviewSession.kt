package net.xmppwocky.earbs.model

import android.util.Log

private const val TAG = "FunctionReviewSession"

/**
 * A review session for the chord function game.
 * User hears tonic chord, then target chord, and identifies the function.
 */
class FunctionReviewSession(val cards: List<FunctionCard>) {
    val totalTrials: Int = cards.size

    var currentTrial: Int = 0
        private set

    var correctCount: Int = 0
        private set

    init {
        Log.i(TAG, "Starting function review session with ${cards.size} cards")
        cards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.displayName}")
        }
    }

    /**
     * Get the current card for this trial.
     */
    fun getCurrentCard(): FunctionCard? = cards.getOrNull(currentTrial)

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
     * Get the key quality for this session (all cards should have the same key quality).
     * For answer buttons, we show functions matching this key quality.
     */
    fun getKeyQuality(): KeyQuality? = cards.firstOrNull()?.keyQuality

    /**
     * Get unique functions in this session (for answer buttons).
     */
    fun getFunctions(): List<ChordFunction> = cards.map { it.function }.distinct()

    /**
     * Get all functions for the current key quality (for answer buttons).
     * This returns all 6 functions for the key, not just those in the session.
     */
    fun getAllFunctionsForKey(): List<ChordFunction> {
        val keyQuality = getKeyQuality() ?: return emptyList()
        return ChordFunction.forKeyQuality(keyQuality)
    }
}
