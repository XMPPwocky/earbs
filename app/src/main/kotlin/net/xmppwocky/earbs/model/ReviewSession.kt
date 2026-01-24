package net.xmppwocky.earbs.model

import android.util.Log
import kotlin.random.Random

private const val TAG = "ReviewSession"

/**
 * A review session with 4 cards and 40 total trials (exactly 10 per card).
 */
class ReviewSession(
    val cards: List<Card>,
    private val trialsPerCard: Int = 10
) {
    val totalTrials: Int = cards.size * trialsPerCard

    // Pre-shuffled deck: exactly trialsPerCard copies of each card
    private val trialDeck: MutableList<Card> = cards
        .flatMap { card -> List(trialsPerCard) { card } }
        .shuffled()
        .toMutableList()

    init {
        require(cards.size == 4) { "Review session requires exactly 4 cards, got ${cards.size}" }
        Log.i(TAG, "Starting review session with ${cards.size} cards, $totalTrials trials ($trialsPerCard per card)")
        cards.forEach { card ->
            Log.i(TAG, "  - ${card.displayName}")
        }
        Log.d(TAG, "Shuffled trial order: ${trialDeck.map { it.chordType.displayName }}")
    }

    private val _scores: MutableMap<Card, CardScore> = cards.associateWith { CardScore(it) }.toMutableMap()
    val scores: Map<Card, CardScore> get() = _scores

    var currentTrial: Int = 0
        private set

    var currentCard: Card? = null
        private set

    /**
     * Get the next card from the pre-shuffled deck.
     * Guarantees exactly trialsPerCard trials per card.
     */
    fun nextTrial(): Card? {
        if (isComplete()) {
            Log.i(TAG, "Session complete, no more trials")
            return null
        }

        currentTrial++
        currentCard = trialDeck.removeAt(0)

        Log.i(TAG, "Trial $currentTrial/$totalTrials: Selected ${currentCard?.displayName}")
        Log.d(TAG, "  Remaining in deck: ${trialDeck.size}")

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
