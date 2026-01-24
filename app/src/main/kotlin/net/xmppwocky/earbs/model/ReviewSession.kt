package net.xmppwocky.earbs.model

import android.util.Log
import kotlin.random.Random

private const val TAG = "ReviewSession"

/**
 * A review session with 4 cards and 40 total trials (~10 per card).
 */
class ReviewSession(
    val cards: List<Card>,
    val totalTrials: Int = 40
) {
    init {
        require(cards.size == 4) { "Review session requires exactly 4 cards, got ${cards.size}" }
        Log.i(TAG, "Starting review session with ${cards.size} cards, $totalTrials trials")
        cards.forEach { card ->
            Log.i(TAG, "  - ${card.displayName}")
        }
    }

    private val _scores: MutableMap<Card, CardScore> = cards.associateWith { CardScore(it) }.toMutableMap()
    val scores: Map<Card, CardScore> get() = _scores

    var currentTrial: Int = 0
        private set

    var currentCard: Card? = null
        private set

    /**
     * Select the next card for this trial using weighted random selection.
     * Cards with fewer trials get higher weights to ensure roughly even distribution.
     */
    fun nextTrial(): Card? {
        if (isComplete()) {
            Log.i(TAG, "Session complete, no more trials")
            return null
        }

        currentTrial++

        // Weight cards inversely by how many trials they've had
        // Cards with fewer trials are more likely to be selected
        val trialsPerCard = cards.map { _scores[it]?.total ?: 0 }
        val maxTrials = trialsPerCard.maxOrNull() ?: 0

        val weights = trialsPerCard.map { trials ->
            // Give higher weight to cards that have had fewer trials
            // Add 1 to avoid zero weights
            (maxTrials - trials + 1).toFloat()
        }

        val totalWeight = weights.sum()
        val random = Random.nextFloat() * totalWeight

        var cumulative = 0f
        var selectedIndex = 0
        for (i in weights.indices) {
            cumulative += weights[i]
            if (random < cumulative) {
                selectedIndex = i
                break
            }
        }

        currentCard = cards[selectedIndex]

        Log.i(TAG, "Trial $currentTrial/$totalTrials: Selected ${currentCard?.displayName}")
        Log.d(TAG, "  Trial counts: ${cards.map { "${it.chordType.displayName}=${_scores[it]?.total}" }}")

        return currentCard
    }

    /**
     * Record the answer for the current trial.
     */
    fun recordAnswer(correct: Boolean) {
        val card = currentCard ?: run {
            Log.w(TAG, "recordAnswer called but no current card")
            return
        }

        val score = _scores[card] ?: run {
            Log.e(TAG, "No score entry for card: ${card.displayName}")
            return
        }

        score.total++
        if (correct) {
            score.correct++
        }

        Log.i(TAG, "Answer for ${card.displayName}: ${if (correct) "CORRECT" else "WRONG"}")
        Log.i(TAG, "  Score now: ${score.correct}/${score.total} (${(score.hitRate * 100).toInt()}%)")
    }

    fun isComplete(): Boolean = currentTrial >= totalTrials

    /**
     * Get the final results summary.
     */
    fun getResults(): List<CardScore> {
        Log.i(TAG, "=== SESSION RESULTS ===")
        return cards.map { card ->
            val score = _scores[card]!!
            Log.i(TAG, "${card.displayName}: ${score.correct}/${score.total} -> ${score.grade.displayName}")
            score
        }
    }
}
