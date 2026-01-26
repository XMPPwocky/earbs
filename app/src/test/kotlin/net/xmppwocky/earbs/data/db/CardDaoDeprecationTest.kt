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
 * Tests for card deprecation functionality in CardDao.
 * Verifies that deprecated cards are properly excluded from reviews
 * while remaining accessible for history display.
 */
class CardDaoDeprecationTest : DatabaseTestBase() {

    // ========== Review Exclusion Tests ==========

    @Test
    fun `getDueCards excludes deprecated cards even when due`() = runTest {
        val now = System.currentTimeMillis()

        // Active due card
        createCard(chordType = "MAJOR", dueDate = now - HOUR_MS)

        // Deprecated due card (should be excluded)
        createCard(chordType = "MINOR", deprecated = true, dueDate = now - HOUR_MS)

        val dueCards = cardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", dueCards[0].id)
    }

    @Test
    fun `getNonDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active non-due card
        createCard(chordType = "MAJOR", dueDate = now + HOUR_MS)

        // Deprecated non-due card (should be excluded)
        createCard(chordType = "MINOR", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = cardDao.getNonDueCards(now, 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `getNonDueCardsByGroup excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active non-due card in group
        createCard(chordType = "MAJOR", octave = 4, playbackMode = "ARPEGGIATED", dueDate = now + HOUR_MS)

        // Deprecated non-due card in same group (should be excluded)
        createCard(chordType = "MINOR", octave = 4, playbackMode = "ARPEGGIATED", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = cardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", nonDueCards[0].id)
    }

    @Test
    fun `countDue excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // 2 active due cards
        createCard(chordType = "MAJOR", dueDate = now - HOUR_MS)
        createCard(chordType = "MINOR", dueDate = now - HOUR_MS)

        // 1 deprecated due card (should not be counted)
        createCard(chordType = "SUS2", deprecated = true, dueDate = now - HOUR_MS)

        assertEquals(2, cardDao.countDue(now))
    }

    @Test
    fun `countUnlocked excludes deprecated cards`() = runTest {
        // 2 active unlocked cards
        createCard(chordType = "MAJOR", unlocked = true)
        createCard(chordType = "MINOR", unlocked = true)

        // 1 deprecated unlocked card (should not be counted)
        createCard(chordType = "SUS2", unlocked = true, deprecated = true)

        // 1 locked card (should not be counted)
        createCard(chordType = "SUS4", unlocked = false)

        assertEquals(2, cardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow excludes deprecated cards`() = runTest {
        createCard(chordType = "MAJOR", unlocked = true)
        createCard(chordType = "MINOR", unlocked = true, deprecated = true)

        val count = cardDao.countUnlockedFlow().first()
        assertEquals(1, count)
    }

    @Test
    fun `getAllUnlocked excludes deprecated cards`() = runTest {
        createCard(chordType = "MAJOR", unlocked = true)
        createCard(chordType = "MINOR", unlocked = true, deprecated = true)

        val unlocked = cardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("MAJOR_4_ARPEGGIATED", unlocked[0].id)
    }

    @Test
    fun `getAllUnlockedWithFsrs excludes deprecated cards`() = runTest {
        createCard(chordType = "MAJOR", unlocked = true)
        createCard(chordType = "MINOR", unlocked = true, deprecated = true)

        val unlockedWithFsrs = cardDao.getAllUnlockedWithFsrs()

        assertEquals(1, unlockedWithFsrs.size)
        assertEquals("MAJOR_4_ARPEGGIATED", unlockedWithFsrs[0].id)
    }

    @Test
    fun `getAllUnlockedWithFsrsFlow excludes deprecated cards`() = runTest {
        createCard(chordType = "MAJOR", unlocked = true)
        createCard(chordType = "MINOR", unlocked = true, deprecated = true)

        val cards = cardDao.getAllUnlockedWithFsrsFlow().first()

        assertEquals(1, cards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", cards[0].id)
    }

    // ========== Archived Cards Retrieval Tests ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        // Active cards (should not be returned)
        createCard(chordType = "MAJOR")
        createCard(chordType = "MINOR")

        // Deprecated cards (should be returned)
        createCard(chordType = "SUS2", deprecated = true)
        createCard(chordType = "SUS4", deprecated = true)

        val deprecatedCards = cardDao.getDeprecatedCardsWithFsrs()

        assertEquals(2, deprecatedCards.size)
        assertTrue(deprecatedCards.all { it.deprecated })
        assertTrue(deprecatedCards.any { it.chordType == "SUS2" })
        assertTrue(deprecatedCards.any { it.chordType == "SUS4" })
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
        createCard(chordType = "MAJOR", deprecated = true)
        createCard(chordType = "MINOR")

        val deprecatedCards = cardDao.getDeprecatedCardsWithFsrsFlow().first()

        assertEquals(1, deprecatedCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", deprecatedCards[0].id)
    }

    @Test
    fun `countDeprecated returns correct count`() = runTest {
        // Active cards
        createCard(chordType = "MAJOR")
        createCard(chordType = "MINOR")

        // Deprecated cards
        createCard(chordType = "SUS2", deprecated = true)
        createCard(chordType = "SUS4", deprecated = true)
        createCard(chordType = "DOM7", deprecated = true)

        assertEquals(3, cardDao.countDeprecated())
    }

    @Test
    fun `getByIdWithFsrs returns deprecated cards for history display`() = runTest {
        val now = System.currentTimeMillis()
        createCard(
            chordType = "MAJOR",
            deprecated = true,
            stability = 5.0,
            difficulty = 4.0
        )

        val card = cardDao.getByIdWithFsrs("MAJOR_4_ARPEGGIATED")

        assertNotNull(card)
        assertTrue(card!!.deprecated)
        assertEquals("MAJOR", card.chordType)
        assertEquals(5.0, card.stability, 0.01)
    }

    // ========== Data Preservation Tests ==========

    @Test
    fun `setDeprecated preserves FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        createCard(
            chordType = "MAJOR",
            stability = 8.5,
            difficulty = 3.2,
            interval = 14,
            reviewCount = 10,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        )

        // Deprecate the card
        cardDao.setDeprecated("MAJOR_4_ARPEGGIATED", true)

        // Verify FSRS state is preserved
        val card = cardDao.getByIdWithFsrs("MAJOR_4_ARPEGGIATED")
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
        createCard(
            chordType = "MAJOR",
            octave = 5,
            playbackMode = "BLOCK",
            unlocked = true
        )

        // Deprecate the card
        cardDao.setDeprecated("MAJOR_5_BLOCK", true)

        // Verify card fields are preserved
        val card = cardDao.getById("MAJOR_5_BLOCK")
        assertNotNull(card)
        assertEquals("MAJOR", card!!.chordType)
        assertEquals(5, card.octave)
        assertEquals("BLOCK", card.playbackMode)
        assertTrue(card.unlocked)
        assertTrue(card.deprecated)
    }

    // ========== Toggle Tests ==========

    @Test
    fun `setDeprecated sets deprecated flag`() = runTest {
        createCard(chordType = "MAJOR")

        cardDao.setDeprecated("MAJOR_4_ARPEGGIATED", true)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")
        assertTrue(card!!.deprecated)
    }

    @Test
    fun `setDeprecated false clears deprecated flag`() = runTest {
        createCard(chordType = "MAJOR", deprecated = true)

        cardDao.setDeprecated("MAJOR_4_ARPEGGIATED", false)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")
        assertFalse(card!!.deprecated)
    }

    @Test
    fun `toggle deprecated back and forth works correctly`() = runTest {
        createCard(chordType = "MAJOR")

        // Deprecate
        cardDao.setDeprecated("MAJOR_4_ARPEGGIATED", true)
        assertTrue(cardDao.getById("MAJOR_4_ARPEGGIATED")!!.deprecated)

        // Un-deprecate
        cardDao.setDeprecated("MAJOR_4_ARPEGGIATED", false)
        assertFalse(cardDao.getById("MAJOR_4_ARPEGGIATED")!!.deprecated)

        // Deprecate again
        cardDao.setDeprecated("MAJOR_4_ARPEGGIATED", true)
        assertTrue(cardDao.getById("MAJOR_4_ARPEGGIATED")!!.deprecated)
    }

    // ========== Edge Cases ==========

    @Test
    fun `all cards deprecated returns empty due list`() = runTest {
        val now = System.currentTimeMillis()

        createCard(chordType = "MAJOR", deprecated = true, dueDate = now - HOUR_MS)
        createCard(chordType = "MINOR", deprecated = true, dueDate = now - HOUR_MS)

        val dueCards = cardDao.getDueCards(now)

        assertTrue(dueCards.isEmpty())
    }

    @Test
    fun `all cards deprecated returns empty non-due list`() = runTest {
        val now = System.currentTimeMillis()

        createCard(chordType = "MAJOR", deprecated = true, dueDate = now + HOUR_MS)
        createCard(chordType = "MINOR", deprecated = true, dueDate = now + HOUR_MS)

        val nonDueCards = cardDao.getNonDueCards(now, 10)

        assertTrue(nonDueCards.isEmpty())
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
        createCard(chordType = "MAJOR")
        createCard(chordType = "MINOR", deprecated = true)

        val cards = cardDao.getAllCardsOrdered()

        assertEquals(1, cards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered excludes deprecated cards`() = runTest {
        createCard(chordType = "MAJOR")
        createCard(chordType = "MINOR", deprecated = true)

        val cards = cardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, cards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", cards[0].id)
    }

    @Test
    fun `countDueFlow excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        createCard(chordType = "MAJOR", dueDate = now - HOUR_MS)
        createCard(chordType = "MINOR", deprecated = true, dueDate = now - HOUR_MS)

        val count = cardDao.countDueFlow(now).first()
        assertEquals(1, count)
    }
}
