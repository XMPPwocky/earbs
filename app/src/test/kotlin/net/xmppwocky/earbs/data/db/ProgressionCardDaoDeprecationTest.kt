package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for card deprecation functionality in ProgressionCardDao.
 * Verifies that deprecated progression cards are properly excluded from reviews
 * while remaining accessible for history display.
 */
class ProgressionCardDaoDeprecationTest : DatabaseTestBase() {

    private var cardCounter = 0

    private val adapter = object : DeprecationTestAdapter<ProgressionCardWithFsrs> {
        override suspend fun createActiveCard(dueDate: Long): String {
            val prog = "PROG_${cardCounter++}"
            createProgressionCard(progression = prog, dueDate = dueDate)
            return "${prog}_4_ARPEGGIATED"
        }

        override suspend fun createDeprecatedCard(dueDate: Long): String {
            val prog = "PROG_${cardCounter++}"
            createProgressionCard(progression = prog, deprecated = true, dueDate = dueDate)
            return "${prog}_4_ARPEGGIATED"
        }

        override suspend fun createActiveCardWithFsrs(
            stability: Double,
            difficulty: Double,
            interval: Int,
            reviewCount: Int,
            phase: Int,
            lapses: Int,
            lastReview: Long
        ): String {
            val prog = "PROG_${cardCounter++}"
            createProgressionCard(
                progression = prog,
                stability = stability,
                difficulty = difficulty,
                interval = interval,
                reviewCount = reviewCount,
                phase = phase,
                lapses = lapses,
                lastReview = lastReview
            )
            return "${prog}_4_ARPEGGIATED"
        }

        override suspend fun createUnlockedCard(): String {
            val prog = "PROG_${cardCounter++}"
            createProgressionCard(progression = prog, unlocked = true)
            return "${prog}_4_ARPEGGIATED"
        }

        override suspend fun createLockedCard(): String {
            val prog = "PROG_${cardCounter++}"
            createProgressionCard(progression = prog, unlocked = false)
            return "${prog}_4_ARPEGGIATED"
        }

        override suspend fun createDeprecatedUnlockedCard(): String {
            val prog = "PROG_${cardCounter++}"
            createProgressionCard(progression = prog, unlocked = true, deprecated = true)
            return "${prog}_4_ARPEGGIATED"
        }

        override suspend fun getDueCards(now: Long) = progressionCardDao.getDueCards(now)
        override suspend fun getNonDueCards(now: Long, limit: Int) = progressionCardDao.getNonDueCards(now, limit)
        override suspend fun countDue(now: Long) = progressionCardDao.countDue(now)
        override suspend fun countUnlocked() = progressionCardDao.countUnlocked()
        override fun countUnlockedFlow() = progressionCardDao.countUnlockedFlow()
        override suspend fun getAllUnlocked() = progressionCardDao.getAllUnlocked()
        override suspend fun getAllUnlockedWithFsrs() = progressionCardDao.getAllUnlockedWithFsrs()
        override fun getAllUnlockedWithFsrsFlow() = progressionCardDao.getAllUnlockedWithFsrsFlow()
        override suspend fun getDeprecatedCardsWithFsrs() = progressionCardDao.getDeprecatedCardsWithFsrs()
        override fun getDeprecatedCardsWithFsrsFlow() = progressionCardDao.getDeprecatedCardsWithFsrsFlow()
        override suspend fun countDeprecated() = progressionCardDao.countDeprecated()
        override suspend fun setDeprecated(id: String, deprecated: Boolean) = progressionCardDao.setDeprecated(id, deprecated)
        override suspend fun getByIdWithFsrs(id: String) = progressionCardDao.getByIdWithFsrs(id)
        override suspend fun getAllCardsOrdered() = progressionCardDao.getAllCardsOrdered()
        override suspend fun getAllCardsWithFsrsOrdered() = progressionCardDao.getAllCardsWithFsrsOrdered()
        override fun countDueFlow(now: Long) = progressionCardDao.countDueFlow(now)

        override fun getId(card: ProgressionCardWithFsrs) = card.id
        override fun isDeprecated(card: ProgressionCardWithFsrs) = card.deprecated
        override fun getStability(card: ProgressionCardWithFsrs) = card.stability
        override fun getDifficulty(card: ProgressionCardWithFsrs) = card.difficulty
        override fun getInterval(card: ProgressionCardWithFsrs) = card.interval
        override fun getReviewCount(card: ProgressionCardWithFsrs) = card.reviewCount
        override fun getPhase(card: ProgressionCardWithFsrs) = card.phase
        override fun getLapses(card: ProgressionCardWithFsrs) = card.lapses
    }

    // ========== Review Exclusion Tests (Shared) ==========

    @Test
    fun `getDueCards excludes deprecated cards even when due`() = runTest {
        val now = System.currentTimeMillis()
        DeprecationTestHelper.testDueCardsExcludesDeprecated(adapter, now, HOUR_MS)
    }

    @Test
    fun `getNonDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()
        DeprecationTestHelper.testNonDueCardsExcludesDeprecated(adapter, now, HOUR_MS)
    }

    @Test
    fun `countDue excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()
        DeprecationTestHelper.testCountDueExcludesDeprecated(adapter, now, HOUR_MS)
    }

    @Test
    fun `countUnlocked excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testCountUnlockedExcludesDeprecated(adapter)
    }

    @Test
    fun `countUnlockedFlow excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testCountUnlockedFlowExcludesDeprecated(adapter)
    }

    @Test
    fun `getAllUnlocked excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testGetAllUnlockedExcludesDeprecated(adapter)
    }

    @Test
    fun `getAllUnlockedWithFsrs excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testGetAllUnlockedWithFsrsExcludesDeprecated(adapter)
    }

    @Test
    fun `getAllUnlockedWithFsrsFlow excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testGetAllUnlockedWithFsrsFlowExcludesDeprecated(adapter)
    }

    // ========== ProgressionCardDao-specific: getNonDueCardsByGroup, getByGroup ==========

    @Test
    fun `getNonDueCardsByGroup excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        createProgressionCard(progression = "I_IV_V_I", octave = 4, playbackMode = "ARPEGGIATED", dueDate = now + HOUR_MS)
        createProgressionCard(progression = "I_V_vi_IV", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = progressionCardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `getByGroup excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", octave = 4, playbackMode = "ARPEGGIATED", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", octave = 4, playbackMode = "ARPEGGIATED", unlocked = true, deprecated = true)

        val cards = progressionCardDao.getByGroup(4, "ARPEGGIATED")

        assertEquals(1, cards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", cards[0].id)
    }

    // ========== Archived Cards Retrieval Tests (Shared) ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        DeprecationTestHelper.testGetDeprecatedCardsWithFsrsReturnsOnlyDeprecated(adapter)
    }

    @Test
    fun `getDeprecatedCardsWithFsrs includes FSRS state correctly`() = runTest {
        val now = System.currentTimeMillis()
        createProgressionCard(
            progression = "I_IV_V_I",
            deprecated = true,
            dueDate = now,
            stability = 8.5,
            difficulty = 3.2,
            interval = 14,
            reviewCount = 10,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        )

        val deprecatedCards = progressionCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(1, deprecatedCards.size)
        val card = deprecatedCards[0]
        assertEquals(8.5, card.stability, 0.01)
        assertEquals(3.2, card.difficulty, 0.01)
        assertEquals(14, card.interval)
        assertEquals(10, card.reviewCount)
        assertEquals(2, card.phase)
        assertEquals(1, card.lapses)
    }

    @Test
    fun `getDeprecatedCardsWithFsrsFlow emits deprecated cards`() = runTest {
        DeprecationTestHelper.testGetDeprecatedCardsWithFsrsFlowEmitsDeprecated(adapter)
    }

    @Test
    fun `countDeprecated returns correct count`() = runTest {
        DeprecationTestHelper.testCountDeprecatedReturnsCorrectCount(adapter)
    }

    @Test
    fun `getByIdWithFsrs returns deprecated cards for history display`() = runTest {
        DeprecationTestHelper.testGetByIdWithFsrsReturnsDeprecatedCards(adapter)
    }

    // ========== Data Preservation Tests (Shared) ==========

    @Test
    fun `setDeprecated preserves FSRS state`() = runTest {
        DeprecationTestHelper.testSetDeprecatedPreservesFsrsState(adapter, DAY_MS)
    }

    @Test
    fun `setDeprecated preserves card fields`() = runTest {
        createProgressionCard(
            progression = "I_IV_V_I",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )

        progressionCardDao.setDeprecated("I_IV_V_I_5_BLOCK", true)

        val card = progressionCardDao.getById("I_IV_V_I_5_BLOCK")
        assertNotNull(card)
        assertEquals("I_IV_V_I", card!!.progression)
        assertEquals(5, card.octave)
        assertEquals("BLOCK", card.playbackMode)
        assertTrue(card.unlocked)
        assertTrue(card.deprecated)
    }

    // ========== Toggle Tests (Shared) ==========

    @Test
    fun `setDeprecated sets deprecated flag`() = runTest {
        DeprecationTestHelper.testSetDeprecatedSetsFlag(adapter)
    }

    @Test
    fun `setDeprecated false clears deprecated flag`() = runTest {
        DeprecationTestHelper.testSetDeprecatedFalseClearsFlag(adapter)
    }

    @Test
    fun `toggle deprecated back and forth works correctly`() = runTest {
        DeprecationTestHelper.testToggleDeprecatedBackAndForth(adapter)
    }

    // ========== Edge Cases (Shared) ==========

    @Test
    fun `all cards deprecated returns empty due list`() = runTest {
        val now = System.currentTimeMillis()
        DeprecationTestHelper.testAllCardsDeprecatedReturnsEmptyDueList(adapter, now, HOUR_MS)
    }

    @Test
    fun `all cards deprecated returns empty non-due list`() = runTest {
        val now = System.currentTimeMillis()
        DeprecationTestHelper.testAllCardsDeprecatedReturnsEmptyNonDueList(adapter, now, HOUR_MS)
    }

    @Test
    fun `deprecated locked card preserves both flags`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", unlocked = false, deprecated = true)

        val card = progressionCardDao.getById("I_IV_V_I_4_ARPEGGIATED")

        assertFalse(card!!.unlocked)
        assertTrue(card.deprecated)
    }

    @Test
    fun `getAllCardsOrdered excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testGetAllCardsOrderedExcludesDeprecated(adapter)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered excludes deprecated cards`() = runTest {
        DeprecationTestHelper.testGetAllCardsWithFsrsOrderedExcludesDeprecated(adapter)
    }

    @Test
    fun `countDueFlow excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()
        DeprecationTestHelper.testCountDueFlowExcludesDeprecated(adapter, now, HOUR_MS)
    }

    // ========== ProgressionCardDao-specific edge case ==========

    @Test
    fun `deprecated cards across different octaves and modes are all returned`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true)
        createProgressionCard(progression = "I_IV_V_I", octave = 5, playbackMode = "BLOCK", deprecated = true)

        val deprecatedCards = progressionCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(2, deprecatedCards.size)
        assertTrue(deprecatedCards.any { it.octave == 4 && it.playbackMode == "ARPEGGIATED" })
        assertTrue(deprecatedCards.any { it.octave == 5 && it.playbackMode == "BLOCK" })
    }
}
