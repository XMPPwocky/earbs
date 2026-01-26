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
 * Tests for card deprecation functionality in ProgressionCardDao.
 * Verifies that deprecated progression cards are properly excluded from reviews
 * while remaining accessible for history display.
 */
class ProgressionCardDaoDeprecationTest : DatabaseTestBase() {

    // ========== Review Exclusion Tests ==========

    @Test
    fun `getDueCards excludes deprecated cards even when due`() = runTest {
        val now = System.currentTimeMillis()

        // Active due card
        createProgressionCard(progression = "I_IV_V_I", dueDate = now - HOUR_MS)

        // Deprecated due card (should be excluded)
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true, dueDate = now - HOUR_MS)

        val dueCards = progressionCardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", dueCards[0].id)
    }

    @Test
    fun `getNonDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active non-due card
        createProgressionCard(progression = "I_IV_V_I", dueDate = now + HOUR_MS)

        // Deprecated non-due card (should be excluded)
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = progressionCardDao.getNonDueCards(now, 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `getNonDueCardsByGroup excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active non-due card in group
        createProgressionCard(progression = "I_IV_V_I", octave = 4, playbackMode = "ARPEGGIATED", dueDate = now + HOUR_MS)

        // Deprecated non-due card in same group (should be excluded)
        createProgressionCard(progression = "I_V_vi_IV", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = progressionCardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `countDue excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // 2 active due cards
        createProgressionCard(progression = "I_IV_V_I", dueDate = now - HOUR_MS)
        createProgressionCard(progression = "I_V_vi_IV", dueDate = now - HOUR_MS)

        // 1 deprecated due card (should not be counted)
        createProgressionCard(progression = "ii_V_I", deprecated = true, dueDate = now - HOUR_MS)

        assertEquals(2, progressionCardDao.countDue(now))
    }

    @Test
    fun `countUnlocked excludes deprecated cards`() = runTest {
        // 2 active unlocked cards
        createProgressionCard(progression = "I_IV_V_I", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", unlocked = true)

        // 1 deprecated unlocked card (should not be counted)
        createProgressionCard(progression = "ii_V_I", unlocked = true, deprecated = true)

        // 1 locked card (should not be counted)
        createProgressionCard(progression = "I_vi_IV_V", unlocked = false)

        assertEquals(2, progressionCardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", unlocked = true, deprecated = true)

        val count = progressionCardDao.countUnlockedFlow().first()
        assertEquals(1, count)
    }

    @Test
    fun `getAllUnlocked excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", unlocked = true, deprecated = true)

        val unlocked = progressionCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", unlocked[0].id)
    }

    @Test
    fun `getAllUnlockedWithFsrs excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", unlocked = true, deprecated = true)

        val unlockedWithFsrs = progressionCardDao.getAllUnlockedWithFsrs()

        assertEquals(1, unlockedWithFsrs.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", unlockedWithFsrs[0].id)
    }

    @Test
    fun `getAllUnlockedWithFsrsFlow excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", unlocked = true, deprecated = true)

        val cards = progressionCardDao.getAllUnlockedWithFsrsFlow().first()

        assertEquals(1, cards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `getByGroup excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", octave = 4, playbackMode = "ARPEGGIATED", unlocked = true)
        createProgressionCard(progression = "I_V_vi_IV", octave = 4, playbackMode = "ARPEGGIATED", unlocked = true, deprecated = true)

        val cards = progressionCardDao.getByGroup(4, "ARPEGGIATED")

        assertEquals(1, cards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", cards[0].id)
    }

    // ========== Archived Cards Retrieval Tests ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        // Active cards (should not be returned)
        createProgressionCard(progression = "I_IV_V_I")
        createProgressionCard(progression = "I_V_vi_IV")

        // Deprecated cards (should be returned)
        createProgressionCard(progression = "ii_V_I", deprecated = true)
        createProgressionCard(progression = "I_vi_IV_V", deprecated = true)

        val deprecatedCards = progressionCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(2, deprecatedCards.size)
        assertTrue(deprecatedCards.all { it.deprecated })
        assertTrue(deprecatedCards.any { it.progression == "ii_V_I" })
        assertTrue(deprecatedCards.any { it.progression == "I_vi_IV_V" })
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
        createProgressionCard(progression = "I_IV_V_I", deprecated = true)
        createProgressionCard(progression = "I_V_vi_IV")

        val deprecatedCards = progressionCardDao.getDeprecatedCardsWithFsrsFlow().first()

        assertEquals(1, deprecatedCards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", deprecatedCards[0].id)
    }

    @Test
    fun `countDeprecated returns correct count`() = runTest {
        // Active cards
        createProgressionCard(progression = "I_IV_V_I")
        createProgressionCard(progression = "I_V_vi_IV")

        // Deprecated cards
        createProgressionCard(progression = "ii_V_I", deprecated = true)
        createProgressionCard(progression = "I_vi_IV_V", deprecated = true)
        createProgressionCard(progression = "vi_IV_I_V", deprecated = true)

        assertEquals(3, progressionCardDao.countDeprecated())
    }

    @Test
    fun `getByIdWithFsrs returns deprecated cards for history display`() = runTest {
        createProgressionCard(
            progression = "I_IV_V_I",
            deprecated = true,
            stability = 5.0,
            difficulty = 4.0
        )

        val card = progressionCardDao.getByIdWithFsrs("I_IV_V_I_4_ARPEGGIATED")

        assertNotNull(card)
        assertTrue(card!!.deprecated)
        assertEquals("I_IV_V_I", card.progression)
        assertEquals(5.0, card.stability, 0.01)
    }

    // ========== Data Preservation Tests ==========

    @Test
    fun `setDeprecated preserves FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        createProgressionCard(
            progression = "I_IV_V_I",
            stability = 8.5,
            difficulty = 3.2,
            interval = 14,
            reviewCount = 10,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        )

        // Deprecate the card
        progressionCardDao.setDeprecated("I_IV_V_I_4_ARPEGGIATED", true)

        // Verify FSRS state is preserved
        val card = progressionCardDao.getByIdWithFsrs("I_IV_V_I_4_ARPEGGIATED")
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
        createProgressionCard(
            progression = "I_IV_V_I",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )

        // Deprecate the card
        progressionCardDao.setDeprecated("I_IV_V_I_5_BLOCK", true)

        // Verify card fields are preserved
        val card = progressionCardDao.getById("I_IV_V_I_5_BLOCK")
        assertNotNull(card)
        assertEquals("I_IV_V_I", card!!.progression)
        assertEquals(5, card.octave)
        assertEquals("BLOCK", card.playbackMode)
        assertTrue(card.unlocked)
        assertTrue(card.deprecated)
    }

    // ========== Toggle Tests ==========

    @Test
    fun `setDeprecated sets deprecated flag`() = runTest {
        createProgressionCard(progression = "I_IV_V_I")

        progressionCardDao.setDeprecated("I_IV_V_I_4_ARPEGGIATED", true)

        val card = progressionCardDao.getById("I_IV_V_I_4_ARPEGGIATED")
        assertTrue(card!!.deprecated)
    }

    @Test
    fun `setDeprecated false clears deprecated flag`() = runTest {
        createProgressionCard(progression = "I_IV_V_I", deprecated = true)

        progressionCardDao.setDeprecated("I_IV_V_I_4_ARPEGGIATED", false)

        val card = progressionCardDao.getById("I_IV_V_I_4_ARPEGGIATED")
        assertFalse(card!!.deprecated)
    }

    @Test
    fun `toggle deprecated back and forth works correctly`() = runTest {
        createProgressionCard(progression = "I_IV_V_I")

        // Deprecate
        progressionCardDao.setDeprecated("I_IV_V_I_4_ARPEGGIATED", true)
        assertTrue(progressionCardDao.getById("I_IV_V_I_4_ARPEGGIATED")!!.deprecated)

        // Un-deprecate
        progressionCardDao.setDeprecated("I_IV_V_I_4_ARPEGGIATED", false)
        assertFalse(progressionCardDao.getById("I_IV_V_I_4_ARPEGGIATED")!!.deprecated)

        // Deprecate again
        progressionCardDao.setDeprecated("I_IV_V_I_4_ARPEGGIATED", true)
        assertTrue(progressionCardDao.getById("I_IV_V_I_4_ARPEGGIATED")!!.deprecated)
    }

    // ========== Edge Cases ==========

    @Test
    fun `all cards deprecated returns empty due list`() = runTest {
        val now = System.currentTimeMillis()

        createProgressionCard(progression = "I_IV_V_I", deprecated = true, dueDate = now - HOUR_MS)
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true, dueDate = now - HOUR_MS)

        val dueCards = progressionCardDao.getDueCards(now)

        assertTrue(dueCards.isEmpty())
    }

    @Test
    fun `all cards deprecated returns empty non-due list`() = runTest {
        val now = System.currentTimeMillis()

        createProgressionCard(progression = "I_IV_V_I", deprecated = true, dueDate = now + HOUR_MS)
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = progressionCardDao.getNonDueCards(now, 10)

        assertTrue(nonDueCards.isEmpty())
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
        createProgressionCard(progression = "I_IV_V_I")
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true)

        val cards = progressionCardDao.getAllCardsOrdered()

        assertEquals(1, cards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered excludes deprecated cards`() = runTest {
        createProgressionCard(progression = "I_IV_V_I")
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true)

        val cards = progressionCardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, cards.size)
        assertEquals("I_IV_V_I_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `countDueFlow excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        createProgressionCard(progression = "I_IV_V_I", dueDate = now - HOUR_MS)
        createProgressionCard(progression = "I_V_vi_IV", deprecated = true, dueDate = now - HOUR_MS)

        val count = progressionCardDao.countDueFlow(now).first()
        assertEquals(1, count)
    }

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
