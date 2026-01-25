package net.xmppwocky.earbs.model

import android.util.Log

private const val TAG = "GenericReviewSession"

/**
 * A generic review session with N cards, 1 trial each.
 * Each trial updates FSRS immediately upon answer.
 *
 * This replaces the duplicated ReviewSession and FunctionReviewSession classes.
 *
 * @param C The card type (must implement GameCard)
 * @param cards The list of cards for this session
 * @param sessionName Optional name for logging (defaults to "review")
 */
class GenericReviewSession<C : GameCard>(
    val cards: List<C>,
    private val sessionName: String = "review"
) {
    val totalTrials: Int = cards.size

    var currentTrial: Int = 0
        private set

    var correctCount: Int = 0
        private set

    init {
        Log.i(TAG, "Starting $sessionName session with ${cards.size} cards")
        cards.forEachIndexed { index, card ->
            Log.i(TAG, "  $index: ${card.displayName}")
        }
    }

    /**
     * Get the current card for this trial.
     */
    fun getCurrentCard(): C? = cards.getOrNull(currentTrial)

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
}
