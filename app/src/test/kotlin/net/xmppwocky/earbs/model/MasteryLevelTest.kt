package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.data.db.CardWithFsrs
import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryLevelTest {

    @Test
    fun `fromFsrsState_phaseAdded_returnsLearning`() {
        // Phase 0 (Added) always returns Learning regardless of stability
        val result = MasteryLevel.fromFsrsState(stability = 100.0, phase = 0)
        assertEquals(MasteryLevel.LEARNING, result)
    }

    @Test
    fun `fromFsrsState_phaseReLearning_returnsLearning`() {
        // Phase 1 (ReLearning) always returns Learning regardless of stability
        val result = MasteryLevel.fromFsrsState(stability = 100.0, phase = 1)
        assertEquals(MasteryLevel.LEARNING, result)
    }

    @Test
    fun `fromFsrsState_stability5_returnsLearning`() {
        // Stability below 7 days returns Learning
        val result = MasteryLevel.fromFsrsState(stability = 5.0, phase = 3)
        assertEquals(MasteryLevel.LEARNING, result)
    }

    @Test
    fun `fromFsrsState_stability7_returnsFamiliar`() {
        // Stability at 7 days threshold returns Familiar
        val result = MasteryLevel.fromFsrsState(stability = 7.0, phase = 3)
        assertEquals(MasteryLevel.FAMILIAR, result)
    }

    @Test
    fun `fromFsrsState_stability15_returnsFamiliar`() {
        // Stability between 7 and 21 returns Familiar
        val result = MasteryLevel.fromFsrsState(stability = 15.0, phase = 3)
        assertEquals(MasteryLevel.FAMILIAR, result)
    }

    @Test
    fun `fromFsrsState_stability21_returnsConfident`() {
        // Stability at 21 days threshold returns Confident
        val result = MasteryLevel.fromFsrsState(stability = 21.0, phase = 3)
        assertEquals(MasteryLevel.CONFIDENT, result)
    }

    @Test
    fun `fromFsrsState_stability45_returnsConfident`() {
        // Stability between 21 and 60 returns Confident
        val result = MasteryLevel.fromFsrsState(stability = 45.0, phase = 3)
        assertEquals(MasteryLevel.CONFIDENT, result)
    }

    @Test
    fun `fromFsrsState_stability60_returnsMastered`() {
        // Stability at 60 days threshold returns Mastered
        val result = MasteryLevel.fromFsrsState(stability = 60.0, phase = 3)
        assertEquals(MasteryLevel.MASTERED, result)
    }

    @Test
    fun `fromFsrsState_stability100_returnsMastered`() {
        // Stability above 60 returns Mastered
        val result = MasteryLevel.fromFsrsState(stability = 100.0, phase = 3)
        assertEquals(MasteryLevel.MASTERED, result)
    }

    @Test
    fun `computeMasteryDistribution_excludesLockedCards`() {
        val cards = listOf(
            createCardWithFsrs("1", unlocked = true, stability = 5.0, phase = 3),   // Learning
            createCardWithFsrs("2", unlocked = false, stability = 100.0, phase = 3), // Locked - should be excluded
            createCardWithFsrs("3", unlocked = true, stability = 25.0, phase = 3)   // Confident
        )

        val distribution = computeMasteryDistribution(cards)

        assertEquals(2, distribution.total)
        assertEquals(1, distribution.learning)
        assertEquals(0, distribution.familiar)
        assertEquals(1, distribution.confident)
        assertEquals(0, distribution.mastered)
    }

    @Test
    fun `computeMasteryDistribution_emptyList_returnsZeros`() {
        val distribution = computeMasteryDistribution(emptyList())

        assertEquals(0, distribution.total)
        assertEquals(0, distribution.learning)
        assertEquals(0, distribution.familiar)
        assertEquals(0, distribution.confident)
        assertEquals(0, distribution.mastered)
    }

    @Test
    fun `computeMasteryDistribution_allLevels`() {
        val cards = listOf(
            createCardWithFsrs("1", unlocked = true, stability = 3.0, phase = 3),   // Learning
            createCardWithFsrs("2", unlocked = true, stability = 10.0, phase = 3),  // Familiar
            createCardWithFsrs("3", unlocked = true, stability = 30.0, phase = 3),  // Confident
            createCardWithFsrs("4", unlocked = true, stability = 80.0, phase = 3)   // Mastered
        )

        val distribution = computeMasteryDistribution(cards)

        assertEquals(4, distribution.total)
        assertEquals(1, distribution.learning)
        assertEquals(1, distribution.familiar)
        assertEquals(1, distribution.confident)
        assertEquals(1, distribution.mastered)
    }

    @Test
    fun `masteryDistribution_percentageFor_calculatesCorrectly`() {
        val distribution = MasteryDistribution(
            learning = 8,
            familiar = 6,
            confident = 6,
            mastered = 4
        )

        // Total is 24
        assertEquals(24, distribution.total)
        assertEquals(33.33f, distribution.percentageFor(MasteryLevel.LEARNING), 0.1f)
        assertEquals(25.0f, distribution.percentageFor(MasteryLevel.FAMILIAR), 0.1f)
        assertEquals(25.0f, distribution.percentageFor(MasteryLevel.CONFIDENT), 0.1f)
        assertEquals(16.67f, distribution.percentageFor(MasteryLevel.MASTERED), 0.1f)
    }

    @Test
    fun `masteryDistribution_percentageFor_emptyDistribution_returnsZero`() {
        val distribution = MasteryDistribution()

        assertEquals(0, distribution.total)
        assertEquals(0f, distribution.percentageFor(MasteryLevel.LEARNING), 0.01f)
    }

    @Test
    fun `masteryDistribution_countFor_returnsCorrectCounts`() {
        val distribution = MasteryDistribution(
            learning = 5,
            familiar = 10,
            confident = 15,
            mastered = 20
        )

        assertEquals(5, distribution.countFor(MasteryLevel.LEARNING))
        assertEquals(10, distribution.countFor(MasteryLevel.FAMILIAR))
        assertEquals(15, distribution.countFor(MasteryLevel.CONFIDENT))
        assertEquals(20, distribution.countFor(MasteryLevel.MASTERED))
    }

    private fun createCardWithFsrs(
        id: String,
        unlocked: Boolean,
        stability: Double,
        phase: Int
    ): CardWithFsrs {
        return CardWithFsrs(
            id = id,
            chordType = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED",
            unlocked = unlocked,
            stability = stability,
            difficulty = 2.5,
            interval = 1,
            dueDate = System.currentTimeMillis(),
            reviewCount = 0,
            lastReview = null,
            phase = phase,
            lapses = 0
        )
    }
}
