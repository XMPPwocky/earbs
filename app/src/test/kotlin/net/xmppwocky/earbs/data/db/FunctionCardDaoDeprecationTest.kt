package net.xmppwocky.earbs.data.db

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

    // ========== Review Exclusion Tests ==========

    @Test
    fun `getDueCards excludes deprecated cards even when due`() = runTest {
        val now = System.currentTimeMillis()

        // Active due card
        createFunctionCard(function = "IV", dueDate = now - HOUR_MS)

        // Deprecated due card (should be excluded)
        createFunctionCard(function = "V", deprecated = true, dueDate = now - HOUR_MS)

        val dueCards = functionCardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", dueCards[0].id)
    }

    @Test
    fun `getNonDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active non-due card
        createFunctionCard(function = "IV", dueDate = now + HOUR_MS)

        // Deprecated non-due card (should be excluded)
        createFunctionCard(function = "V", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = functionCardDao.getNonDueCards(now, 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `getNonDueCardsByGroup excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active non-due card in group
        createFunctionCard(function = "IV", keyQuality = "MAJOR", octave = 4, playbackMode = "ARPEGGIATED", dueDate = now + HOUR_MS)

        // Deprecated non-due card in same group (should be excluded)
        createFunctionCard(function = "V", keyQuality = "MAJOR", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = functionCardDao.getNonDueCardsByGroup(now, "MAJOR", 4, "ARPEGGIATED", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `countDue excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // 2 active due cards
        createFunctionCard(function = "IV", dueDate = now - HOUR_MS)
        createFunctionCard(function = "V", dueDate = now - HOUR_MS)

        // 1 deprecated due card (should not be counted)
        createFunctionCard(function = "vi", deprecated = true, dueDate = now - HOUR_MS)

        assertEquals(2, functionCardDao.countDue(now))
    }

    @Test
    fun `countUnlocked excludes deprecated cards`() = runTest {
        // 2 active unlocked cards
        createFunctionCard(function = "IV", unlocked = true)
        createFunctionCard(function = "V", unlocked = true)

        // 1 deprecated unlocked card (should not be counted)
        createFunctionCard(function = "vi", unlocked = true, deprecated = true)

        // 1 locked card (should not be counted)
        createFunctionCard(function = "ii", unlocked = false)

        assertEquals(2, functionCardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV", unlocked = true)
        createFunctionCard(function = "V", unlocked = true, deprecated = true)

        val count = functionCardDao.countUnlockedFlow().first()
        assertEquals(1, count)
    }

    @Test
    fun `getAllUnlocked excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV", unlocked = true)
        createFunctionCard(function = "V", unlocked = true, deprecated = true)

        val unlocked = functionCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", unlocked[0].id)
    }

    @Test
    fun `getAllUnlockedWithFsrs excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV", unlocked = true)
        createFunctionCard(function = "V", unlocked = true, deprecated = true)

        val unlockedWithFsrs = functionCardDao.getAllUnlockedWithFsrs()

        assertEquals(1, unlockedWithFsrs.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", unlockedWithFsrs[0].id)
    }

    @Test
    fun `getAllUnlockedWithFsrsFlow excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV", unlocked = true)
        createFunctionCard(function = "V", unlocked = true, deprecated = true)

        val cards = functionCardDao.getAllUnlockedWithFsrsFlow().first()

        assertEquals(1, cards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", cards[0].id)
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

    // ========== Archived Cards Retrieval Tests ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        // Active cards (should not be returned)
        createFunctionCard(function = "IV")
        createFunctionCard(function = "V")

        // Deprecated cards (should be returned)
        createFunctionCard(function = "vi", deprecated = true)
        createFunctionCard(function = "ii", deprecated = true)

        val deprecatedCards = functionCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(2, deprecatedCards.size)
        assertTrue(deprecatedCards.all { it.deprecated })
        assertTrue(deprecatedCards.any { it.function == "vi" })
        assertTrue(deprecatedCards.any { it.function == "ii" })
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
        createFunctionCard(function = "IV", deprecated = true)
        createFunctionCard(function = "V")

        val deprecatedCards = functionCardDao.getDeprecatedCardsWithFsrsFlow().first()

        assertEquals(1, deprecatedCards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", deprecatedCards[0].id)
    }

    @Test
    fun `countDeprecated returns correct count`() = runTest {
        // Active cards
        createFunctionCard(function = "IV")
        createFunctionCard(function = "V")

        // Deprecated cards
        createFunctionCard(function = "vi", deprecated = true)
        createFunctionCard(function = "ii", deprecated = true)
        createFunctionCard(function = "iii", deprecated = true)

        assertEquals(3, functionCardDao.countDeprecated())
    }

    @Test
    fun `getByIdWithFsrs returns deprecated cards for history display`() = runTest {
        createFunctionCard(
            function = "IV",
            deprecated = true,
            stability = 5.0,
            difficulty = 4.0
        )

        val card = functionCardDao.getByIdWithFsrs("IV_MAJOR_4_ARPEGGIATED")

        assertNotNull(card)
        assertTrue(card!!.deprecated)
        assertEquals("IV", card.function)
        assertEquals(5.0, card.stability, 0.01)
    }

    // ========== Data Preservation Tests ==========

    @Test
    fun `setDeprecated preserves FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        createFunctionCard(
            function = "IV",
            stability = 8.5,
            difficulty = 3.2,
            interval = 14,
            reviewCount = 10,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        )

        // Deprecate the card
        functionCardDao.setDeprecated("IV_MAJOR_4_ARPEGGIATED", true)

        // Verify FSRS state is preserved
        val card = functionCardDao.getByIdWithFsrs("IV_MAJOR_4_ARPEGGIATED")
        assertNotNull(card)
        assertEquals(8.5, card!!.stability, 0.01)
        assertEquals(3.2, card.difficulty, 0.01)
        assertEquals(14, card.interval)
        assertEquals(10, card.reviewCount)
        assertEquals(2, card.phase)
        assertEquals(1, card.lapses)
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

        // Deprecate the card
        functionCardDao.setDeprecated("IV_MINOR_5_BLOCK", true)

        // Verify card fields are preserved
        val card = functionCardDao.getById("IV_MINOR_5_BLOCK")
        assertNotNull(card)
        assertEquals("IV", card!!.function)
        assertEquals("MINOR", card.keyQuality)
        assertEquals(5, card.octave)
        assertEquals("BLOCK", card.playbackMode)
        assertTrue(card.unlocked)
        assertTrue(card.deprecated)
    }

    // ========== Toggle Tests ==========

    @Test
    fun `setDeprecated sets deprecated flag`() = runTest {
        createFunctionCard(function = "IV")

        functionCardDao.setDeprecated("IV_MAJOR_4_ARPEGGIATED", true)

        val card = functionCardDao.getById("IV_MAJOR_4_ARPEGGIATED")
        assertTrue(card!!.deprecated)
    }

    @Test
    fun `setDeprecated false clears deprecated flag`() = runTest {
        createFunctionCard(function = "IV", deprecated = true)

        functionCardDao.setDeprecated("IV_MAJOR_4_ARPEGGIATED", false)

        val card = functionCardDao.getById("IV_MAJOR_4_ARPEGGIATED")
        assertFalse(card!!.deprecated)
    }

    @Test
    fun `toggle deprecated back and forth works correctly`() = runTest {
        createFunctionCard(function = "IV")

        // Deprecate
        functionCardDao.setDeprecated("IV_MAJOR_4_ARPEGGIATED", true)
        assertTrue(functionCardDao.getById("IV_MAJOR_4_ARPEGGIATED")!!.deprecated)

        // Un-deprecate
        functionCardDao.setDeprecated("IV_MAJOR_4_ARPEGGIATED", false)
        assertFalse(functionCardDao.getById("IV_MAJOR_4_ARPEGGIATED")!!.deprecated)

        // Deprecate again
        functionCardDao.setDeprecated("IV_MAJOR_4_ARPEGGIATED", true)
        assertTrue(functionCardDao.getById("IV_MAJOR_4_ARPEGGIATED")!!.deprecated)
    }

    // ========== Edge Cases ==========

    @Test
    fun `all cards deprecated returns empty due list`() = runTest {
        val now = System.currentTimeMillis()

        createFunctionCard(function = "IV", deprecated = true, dueDate = now - HOUR_MS)
        createFunctionCard(function = "V", deprecated = true, dueDate = now - HOUR_MS)

        val dueCards = functionCardDao.getDueCards(now)

        assertTrue(dueCards.isEmpty())
    }

    @Test
    fun `all cards deprecated returns empty non-due list`() = runTest {
        val now = System.currentTimeMillis()

        createFunctionCard(function = "IV", deprecated = true, dueDate = now + HOUR_MS)
        createFunctionCard(function = "V", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = functionCardDao.getNonDueCards(now, 10)

        assertTrue(nonDueCards.isEmpty())
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
        createFunctionCard(function = "IV")
        createFunctionCard(function = "V", deprecated = true)

        val cards = functionCardDao.getAllCardsOrdered()

        assertEquals(1, cards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered excludes deprecated cards`() = runTest {
        createFunctionCard(function = "IV")
        createFunctionCard(function = "V", deprecated = true)

        val cards = functionCardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, cards.size)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `countDueFlow excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        createFunctionCard(function = "IV", dueDate = now - HOUR_MS)
        createFunctionCard(function = "V", deprecated = true, dueDate = now - HOUR_MS)

        val count = functionCardDao.countDueFlow(now).first()
        assertEquals(1, count)
    }

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
