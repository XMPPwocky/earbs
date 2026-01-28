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
 * Tests for card deprecation functionality in FunctionCardDao.
 * Verifies that deprecated function cards are properly excluded from reviews
 * while remaining accessible for history display.
 */
class FunctionCardDaoDeprecationTest : DatabaseTestBase() {

    private var cardCounter = 0

    private val adapter = object : DeprecationTestAdapter<FunctionCardWithFsrs> {
        override suspend fun createActiveCard(dueDate: Long): String {
            val func = "FUNC_${cardCounter++}"
            createFunctionCard(function = func, dueDate = dueDate)
            return "${func}_MAJOR_4_ARPEGGIATED"
        }

        override suspend fun createDeprecatedCard(dueDate: Long): String {
            val func = "FUNC_${cardCounter++}"
            createFunctionCard(function = func, deprecated = true, dueDate = dueDate)
            return "${func}_MAJOR_4_ARPEGGIATED"
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
            val func = "FUNC_${cardCounter++}"
            createFunctionCard(
                function = func,
                stability = stability,
                difficulty = difficulty,
                interval = interval,
                reviewCount = reviewCount,
                phase = phase,
                lapses = lapses,
                lastReview = lastReview
            )
            return "${func}_MAJOR_4_ARPEGGIATED"
        }

        override suspend fun createUnlockedCard(): String {
            val func = "FUNC_${cardCounter++}"
            createFunctionCard(function = func, unlocked = true)
            return "${func}_MAJOR_4_ARPEGGIATED"
        }

        override suspend fun createLockedCard(): String {
            val func = "FUNC_${cardCounter++}"
            createFunctionCard(function = func, unlocked = false)
            return "${func}_MAJOR_4_ARPEGGIATED"
        }

        override suspend fun createDeprecatedUnlockedCard(): String {
            val func = "FUNC_${cardCounter++}"
            createFunctionCard(function = func, unlocked = true, deprecated = true)
            return "${func}_MAJOR_4_ARPEGGIATED"
        }

        override suspend fun getDueCards(now: Long) = functionCardDao.getDueCards(now)
        override suspend fun getNonDueCards(now: Long, limit: Int) = functionCardDao.getNonDueCards(now, limit)
        override suspend fun countDue(now: Long) = functionCardDao.countDue(now)
        override suspend fun countUnlocked() = functionCardDao.countUnlocked()
        override fun countUnlockedFlow() = functionCardDao.countUnlockedFlow()
        override suspend fun getAllUnlocked() = functionCardDao.getAllUnlocked()
        override suspend fun getAllUnlockedWithFsrs() = functionCardDao.getAllUnlockedWithFsrs()
        override fun getAllUnlockedWithFsrsFlow() = functionCardDao.getAllUnlockedWithFsrsFlow()
        override suspend fun getDeprecatedCardsWithFsrs() = functionCardDao.getDeprecatedCardsWithFsrs()
        override fun getDeprecatedCardsWithFsrsFlow() = functionCardDao.getDeprecatedCardsWithFsrsFlow()
        override suspend fun countDeprecated() = functionCardDao.countDeprecated()
        override suspend fun setDeprecated(id: String, deprecated: Boolean) = functionCardDao.setDeprecated(id, deprecated)
        override suspend fun getByIdWithFsrs(id: String) = functionCardDao.getByIdWithFsrs(id)
        override suspend fun getAllCardsOrdered() = functionCardDao.getAllCardsOrdered()
        override suspend fun getAllCardsWithFsrsOrdered() = functionCardDao.getAllCardsWithFsrsOrdered()
        override fun countDueFlow(now: Long) = functionCardDao.countDueFlow(now)

        override fun getId(card: FunctionCardWithFsrs) = card.id
        override fun isDeprecated(card: FunctionCardWithFsrs) = card.deprecated
        override fun getStability(card: FunctionCardWithFsrs) = card.stability
        override fun getDifficulty(card: FunctionCardWithFsrs) = card.difficulty
        override fun getInterval(card: FunctionCardWithFsrs) = card.interval
        override fun getReviewCount(card: FunctionCardWithFsrs) = card.reviewCount
        override fun getPhase(card: FunctionCardWithFsrs) = card.phase
        override fun getLapses(card: FunctionCardWithFsrs) = card.lapses
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

    // ========== FunctionCardDao-specific: getNonDueCardsByGroup, getByKeyQuality, getByGroup ==========

    @Test
    fun `getNonDueCardsByGroup excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        createFunctionCard(function = "IV", keyQuality = "MAJOR", octave = 4, playbackMode = "ARPEGGIATED", dueDate = now + HOUR_MS)
        createFunctionCard(function = "V", keyQuality = "MAJOR", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = functionCardDao.getNonDueCardsByGroup(now, "MAJOR", 4, "ARPEGGIATED", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `getByKeyQuality excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV", keyQuality = "MAJOR", unlocked = true)
        createFunctionCard(function = "V", keyQuality = "MAJOR", unlocked = true, deprecated = true)

        val majorCards = functionCardDao.getByKeyQuality("MAJOR")

        assertEquals(1, majorCards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", majorCards[0].id)
    }

    @Test
    fun `getByGroup excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV", octave = 4, playbackMode = "ARPEGGIATED", unlocked = true)
        createFunctionCard(function = "V", octave = 4, playbackMode = "ARPEGGIATED", unlocked = true, deprecated = true)

        val cards = functionCardDao.getByGroup(4, "ARPEGGIATED")

        assertEquals(1, cards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", cards[0].id)
    }

    // ========== Archived Cards Retrieval Tests (Shared) ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        DeprecationTestHelper.testGetDeprecatedCardsWithFsrsReturnsOnlyDeprecated(adapter)
    }

    @Test
    fun `getDeprecatedCardsWithFsrs includes FSRS state correctly`() = runTest {
        val now = System.currentTimeMillis()
        createFunctionCard(
            function = "IV",
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

        val deprecatedCards = functionCardDao.getDeprecatedCardsWithFsrs()

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
        createFunctionCard(
            function = "IV",
            keyQuality = "MINOR",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )

        functionCardDao.setDeprecated("IV_MINOR_5_BLOCK", true)

        val card = functionCardDao.getById("IV_MINOR_5_BLOCK")
        assertNotNull(card)
        assertEquals("IV", card!!.function)
        assertEquals("MINOR", card.keyQuality)
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
        createFunctionCard(function = "IV", unlocked = false, deprecated = true)

        val card = functionCardDao.getById("IV_MAJOR_4_ARPEGGIATED")

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

    // ========== FunctionCardDao-specific edge case ==========

    @Test
    fun `deprecated cards in different key qualities are both returned`() = runTest {
        createFunctionCard(function = "IV", keyQuality = "MAJOR", deprecated = true)
        createFunctionCard(function = "iv", keyQuality = "MINOR", deprecated = true)

        val deprecatedCards = functionCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(2, deprecatedCards.size)
        assertTrue(deprecatedCards.any { it.keyQuality == "MAJOR" })
        assertTrue(deprecatedCards.any { it.keyQuality == "MINOR" })
    }
}
