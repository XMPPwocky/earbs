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
 * Tests for card deprecation functionality in CardDao.
 * Verifies that deprecated cards are properly excluded from reviews
 * while remaining accessible for history display.
 */
class CardDaoDeprecationTest : DatabaseTestBase() {

    private var cardCounter = 0

    private val adapter = object : DeprecationTestAdapter<CardWithFsrs> {
        override suspend fun createActiveCard(dueDate: Long): String {
            val type = "CHORD_${cardCounter++}"
            createCard(chordType = type, dueDate = dueDate)
            return "${type}_4_ARPEGGIATED"
        }

        override suspend fun createDeprecatedCard(dueDate: Long): String {
            val type = "CHORD_${cardCounter++}"
            createCard(chordType = type, deprecated = true, dueDate = dueDate)
            return "${type}_4_ARPEGGIATED"
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
            val type = "CHORD_${cardCounter++}"
            createCard(
                chordType = type,
                stability = stability,
                difficulty = difficulty,
                interval = interval,
                reviewCount = reviewCount,
                phase = phase,
                lapses = lapses,
                lastReview = lastReview
            )
            return "${type}_4_ARPEGGIATED"
        }

        override suspend fun createUnlockedCard(): String {
            val type = "CHORD_${cardCounter++}"
            createCard(chordType = type, unlocked = true)
            return "${type}_4_ARPEGGIATED"
        }

        override suspend fun createLockedCard(): String {
            val type = "CHORD_${cardCounter++}"
            createCard(chordType = type, unlocked = false)
            return "${type}_4_ARPEGGIATED"
        }

        override suspend fun createDeprecatedUnlockedCard(): String {
            val type = "CHORD_${cardCounter++}"
            createCard(chordType = type, unlocked = true, deprecated = true)
            return "${type}_4_ARPEGGIATED"
        }

        override suspend fun getDueCards(now: Long) = cardDao.getDueCards(now)
        override suspend fun getNonDueCards(now: Long, limit: Int) = cardDao.getNonDueCards(now, limit)
        override suspend fun countDue(now: Long) = cardDao.countDue(now)
        override suspend fun countUnlocked() = cardDao.countUnlocked()
        override fun countUnlockedFlow() = cardDao.countUnlockedFlow()
        override suspend fun getAllUnlocked() = cardDao.getAllUnlocked()
        override suspend fun getAllUnlockedWithFsrs() = cardDao.getAllUnlockedWithFsrs()
        override fun getAllUnlockedWithFsrsFlow() = cardDao.getAllUnlockedWithFsrsFlow()
        override suspend fun getDeprecatedCardsWithFsrs() = cardDao.getDeprecatedCardsWithFsrs()
        override fun getDeprecatedCardsWithFsrsFlow() = cardDao.getDeprecatedCardsWithFsrsFlow()
        override suspend fun countDeprecated() = cardDao.countDeprecated()
        override suspend fun setDeprecated(id: String, deprecated: Boolean) = cardDao.setDeprecated(id, deprecated)
        override suspend fun getByIdWithFsrs(id: String) = cardDao.getByIdWithFsrs(id)
        override suspend fun getAllCardsOrdered() = cardDao.getAllCardsOrdered()
        override suspend fun getAllCardsWithFsrsOrdered() = cardDao.getAllCardsWithFsrsOrdered()
        override fun countDueFlow(now: Long) = cardDao.countDueFlow(now)

        override fun getId(card: CardWithFsrs) = card.id
        override fun isDeprecated(card: CardWithFsrs) = card.deprecated
        override fun getStability(card: CardWithFsrs) = card.stability
        override fun getDifficulty(card: CardWithFsrs) = card.difficulty
        override fun getInterval(card: CardWithFsrs) = card.interval
        override fun getReviewCount(card: CardWithFsrs) = card.reviewCount
        override fun getPhase(card: CardWithFsrs) = card.phase
        override fun getLapses(card: CardWithFsrs) = card.lapses
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

    // ========== CardDao-specific: getNonDueCardsByGroup ==========

    @Test
    fun `getNonDueCardsByGroup excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        createCard(chordType = "MAJOR", octave = 4, playbackMode = "ARPEGGIATED", dueDate = now + HOUR_MS)
        createCard(chordType = "MINOR", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = cardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", nonDueCards[0].id)
    }

    // ========== Archived Cards Retrieval Tests (Shared) ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        DeprecationTestHelper.testGetDeprecatedCardsWithFsrsReturnsOnlyDeprecated(adapter)
    }

    @Test
    fun `getDeprecatedCardsWithFsrs includes FSRS state correctly`() = runTest {
        val now = System.currentTimeMillis()
        createCard(
            chordType = "MAJOR",
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

        val deprecatedCards = cardDao.getDeprecatedCardsWithFsrs()

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
        createCard(
            chordType = "MAJOR",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )

        cardDao.setDeprecated("MAJOR_5_BLOCK", true)

        val card = cardDao.getById("MAJOR_5_BLOCK")
        assertNotNull(card)
        assertEquals("MAJOR", card!!.chordType)
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
        createCard(chordType = "MAJOR", unlocked = false, deprecated = true)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")

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
}
