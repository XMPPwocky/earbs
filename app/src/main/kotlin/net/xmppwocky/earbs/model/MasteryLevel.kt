package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.FunctionCardWithFsrs
import net.xmppwocky.earbs.data.db.ProgressionCardWithFsrs

/**
 * Mastery levels based on FSRS stability thresholds.
 */
enum class MasteryLevel(val displayName: String, val order: Int) {
    LEARNING("Learning", 0),
    FAMILIAR("Familiar", 1),
    CONFIDENT("Confident", 2),
    MASTERED("Mastered", 3);

    companion object {
        const val THRESHOLD_FAMILIAR = 7.0
        const val THRESHOLD_CONFIDENT = 21.0
        const val THRESHOLD_MASTERED = 60.0

        /**
         * Determine mastery level from FSRS state.
         * @param stability FSRS stability value (in days)
         * @param phase FSRS phase (0=Added, 1=ReLearning, 2=Learning, 3=Review)
         */
        fun fromFsrsState(stability: Double, phase: Int): MasteryLevel {
            // Phase 0 (Added) or 1 (ReLearning) always counts as Learning
            if (phase == 0 || phase == 1) return LEARNING
            return when {
                stability >= THRESHOLD_MASTERED -> MASTERED
                stability >= THRESHOLD_CONFIDENT -> CONFIDENT
                stability >= THRESHOLD_FAMILIAR -> FAMILIAR
                else -> LEARNING
            }
        }
    }
}

/**
 * Distribution of cards across mastery levels.
 */
data class MasteryDistribution(
    val learning: Int = 0,
    val familiar: Int = 0,
    val confident: Int = 0,
    val mastered: Int = 0
) {
    val total: Int get() = learning + familiar + confident + mastered

    fun countFor(level: MasteryLevel): Int = when (level) {
        MasteryLevel.LEARNING -> learning
        MasteryLevel.FAMILIAR -> familiar
        MasteryLevel.CONFIDENT -> confident
        MasteryLevel.MASTERED -> mastered
    }

    fun percentageFor(level: MasteryLevel): Float {
        if (total == 0) return 0f
        return countFor(level).toFloat() / total * 100f
    }
}

/**
 * Compute mastery distribution for chord type cards.
 * Only counts unlocked cards.
 */
fun computeMasteryDistribution(cards: List<CardWithFsrs>): MasteryDistribution {
    var learning = 0
    var familiar = 0
    var confident = 0
    var mastered = 0

    for (card in cards) {
        if (!card.unlocked) continue
        when (MasteryLevel.fromFsrsState(card.stability, card.phase)) {
            MasteryLevel.LEARNING -> learning++
            MasteryLevel.FAMILIAR -> familiar++
            MasteryLevel.CONFIDENT -> confident++
            MasteryLevel.MASTERED -> mastered++
        }
    }

    return MasteryDistribution(learning, familiar, confident, mastered)
}

/**
 * Compute mastery distribution for function cards.
 * Only counts unlocked cards.
 */
fun computeFunctionMasteryDistribution(cards: List<FunctionCardWithFsrs>): MasteryDistribution {
    var learning = 0
    var familiar = 0
    var confident = 0
    var mastered = 0

    for (card in cards) {
        if (!card.unlocked) continue
        when (MasteryLevel.fromFsrsState(card.stability, card.phase)) {
            MasteryLevel.LEARNING -> learning++
            MasteryLevel.FAMILIAR -> familiar++
            MasteryLevel.CONFIDENT -> confident++
            MasteryLevel.MASTERED -> mastered++
        }
    }

    return MasteryDistribution(learning, familiar, confident, mastered)
}

/**
 * Compute mastery distribution for progression cards.
 * Only counts unlocked cards.
 */
fun computeProgressionMasteryDistribution(cards: List<ProgressionCardWithFsrs>): MasteryDistribution {
    var learning = 0
    var familiar = 0
    var confident = 0
    var mastered = 0

    for (card in cards) {
        if (!card.unlocked) continue
        when (MasteryLevel.fromFsrsState(card.stability, card.phase)) {
            MasteryLevel.LEARNING -> learning++
            MasteryLevel.FAMILIAR -> familiar++
            MasteryLevel.CONFIDENT -> confident++
            MasteryLevel.MASTERED -> mastered++
        }
    }

    return MasteryDistribution(learning, familiar, confident, mastered)
}
