package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for CardDao unlock screen methods.
 */
class CardDaoUnlockTest : DatabaseTestBase() {

    // ========== getAllCardsOrdered Tests ==========

    @Test
    fun `getAllCardsOrdered returns all cards including locked`() = runTest {
        // Insert mix of locked and unlocked cards
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = true))

        val allCards = cardDao.getAllCardsOrdered()

        assertEquals(3, allCards.size)
        assertTrue(allCards.any { it.id == "MAJOR_4_ARPEGGIATED" })
        assertTrue(allCards.any { it.id == "MINOR_4_ARPEGGIATED" })
        assertTrue(allCards.any { it.id == "SUS2_4_ARPEGGIATED" })
    }

    @Test
    fun `getAllCardsOrdered orders by octave ascending first`() = runTest {
        cardDao.insert(CardEntity("MAJOR_5_ARPEGGIATED", "MAJOR", 5, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MAJOR_3_ARPEGGIATED", "MAJOR", 3, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))

        val allCards = cardDao.getAllCardsOrdered()

        assertEquals(3, allCards.size)
        assertEquals(3, allCards[0].octave)
        assertEquals(4, allCards[1].octave)
        assertEquals(5, allCards[2].octave)
    }

    @Test
    fun `getAllCardsOrdered orders by playbackMode ascending within octave`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_BLOCK", "MAJOR", 4, "BLOCK"))
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))

        val allCards = cardDao.getAllCardsOrdered()

        assertEquals(2, allCards.size)
        assertEquals("ARPEGGIATED", allCards[0].playbackMode)
        assertEquals("BLOCK", allCards[1].playbackMode)
    }

    @Test
    fun `getAllCardsOrdered orders by chordType ascending within octave and mode`() = runTest {
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))

        val allCards = cardDao.getAllCardsOrdered()

        assertEquals(3, allCards.size)
        assertEquals("MAJOR", allCards[0].chordType)
        assertEquals("MINOR", allCards[1].chordType)
        assertEquals("SUS2", allCards[2].chordType)
    }

    // ========== setUnlocked Tests ==========

    @Test
    fun `setUnlocked unlocks a locked card`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        cardDao.setUnlocked("MAJOR_4_ARPEGGIATED", true)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")
        assertNotNull(card)
        assertTrue(card!!.unlocked)
    }

    @Test
    fun `setUnlocked locks an unlocked card`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))

        cardDao.setUnlocked("MAJOR_4_ARPEGGIATED", false)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")
        assertNotNull(card)
        assertFalse(card!!.unlocked)
    }

    @Test
    fun `setUnlocked preserves FSRS state after lock-unlock cycle`() = runTest {
        val now = System.currentTimeMillis()
        val cardId = "MAJOR_4_ARPEGGIATED"

        // Create card with FSRS state
        cardDao.insert(CardEntity(cardId, "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = cardId,
            gameType = GameType.CHORD_TYPE.name,
            stability = 5.5,
            difficulty = 3.2,
            interval = 7,
            dueDate = now + DAY_MS,
            reviewCount = 10,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        ))

        // Lock the card
        cardDao.setUnlocked(cardId, false)

        // Verify FSRS state unchanged
        val fsrsState = fsrsStateDao.getByCardId(cardId)
        assertNotNull(fsrsState)
        assertEquals(5.5, fsrsState!!.stability, 0.01)
        assertEquals(3.2, fsrsState.difficulty, 0.01)
        assertEquals(7, fsrsState.interval)
        assertEquals(10, fsrsState.reviewCount)
        assertEquals(1, fsrsState.lapses)

        // Unlock the card
        cardDao.setUnlocked(cardId, true)

        // Verify FSRS state still unchanged
        val fsrsState2 = fsrsStateDao.getByCardId(cardId)
        assertNotNull(fsrsState2)
        assertEquals(5.5, fsrsState2!!.stability, 0.01)
        assertEquals(3.2, fsrsState2.difficulty, 0.01)
        assertEquals(7, fsrsState2.interval)
        assertEquals(10, fsrsState2.reviewCount)
        assertEquals(1, fsrsState2.lapses)
    }

    // ========== getAllCardsWithFsrsOrdered Tests ==========

    @Test
    fun `getAllCardsWithFsrsOrdered returns all cards including locked`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked card with FSRS
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        // Locked card with FSRS
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        // Locked card without FSRS
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = false))

        val allCards = cardDao.getAllCardsWithFsrsOrdered()

        assertEquals(3, allCards.size)
        assertTrue(allCards.any { it.id == "MAJOR_4_ARPEGGIATED" && it.unlocked })
        assertTrue(allCards.any { it.id == "MINOR_4_ARPEGGIATED" && !it.unlocked })
        assertTrue(allCards.any { it.id == "SUS2_4_ARPEGGIATED" && !it.unlocked })
    }

    @Test
    fun `getAllCardsWithFsrsOrdered returns default values for cards without FSRS`() = runTest {
        // Card without FSRS state
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        val allCards = cardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, allCards.size)
        val card = allCards[0]
        assertEquals("MAJOR_4_ARPEGGIATED", card.id)
        assertEquals(2.5, card.stability, 0.01)  // Default COALESCE value
        assertEquals(2.5, card.difficulty, 0.01)  // Default COALESCE value
        assertEquals(0, card.interval)
        assertEquals(0, card.dueDate)
        assertEquals(0, card.reviewCount)
        assertNull(card.lastReview)
        assertEquals(0, card.phase)
        assertEquals(0, card.lapses)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered preserves unlocked card FSRS data`() = runTest {
        val now = System.currentTimeMillis()

        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_TYPE.name,
            stability = 8.5,
            difficulty = 4.2,
            interval = 14,
            dueDate = now + 7 * DAY_MS,
            reviewCount = 20,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 2
        ))

        val allCards = cardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, allCards.size)
        val card = allCards[0]
        assertEquals(8.5, card.stability, 0.01)
        assertEquals(4.2, card.difficulty, 0.01)
        assertEquals(14, card.interval)
        assertEquals(20, card.reviewCount)
        assertEquals(2, card.phase)
        assertEquals(2, card.lapses)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered orders correctly`() = runTest {
        // Insert in wrong order to verify sorting
        cardDao.insert(CardEntity("MAJOR_5_BLOCK", "MAJOR", 5, "BLOCK"))
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MAJOR_4_BLOCK", "MAJOR", 4, "BLOCK"))
        cardDao.insert(CardEntity("MAJOR_3_ARPEGGIATED", "MAJOR", 3, "ARPEGGIATED"))

        val allCards = cardDao.getAllCardsWithFsrsOrdered()

        assertEquals(5, allCards.size)
        // Order: octave ASC, playbackMode ASC, chordType ASC
        assertEquals("MAJOR_3_ARPEGGIATED", allCards[0].id)
        assertEquals("MAJOR_4_ARPEGGIATED", allCards[1].id)
        assertEquals("MINOR_4_ARPEGGIATED", allCards[2].id)
        assertEquals("MAJOR_4_BLOCK", allCards[3].id)
        assertEquals("MAJOR_5_BLOCK", allCards[4].id)
    }

    // ========== getAllIds Tests ==========

    @Test
    fun `getAllIds returns all card IDs`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = true))

        val allIds = cardDao.getAllIds()

        assertEquals(3, allIds.size)
        assertTrue(allIds.contains("MAJOR_4_ARPEGGIATED"))
        assertTrue(allIds.contains("MINOR_4_ARPEGGIATED"))
        assertTrue(allIds.contains("SUS2_4_ARPEGGIATED"))
    }

    @Test
    fun `getAllIds returns empty list when no cards`() = runTest {
        val allIds = cardDao.getAllIds()

        assertTrue(allIds.isEmpty())
    }

    // ========== Integration Tests ==========

    @Test
    fun `locked cards not included in getDueCards`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked due card
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        // Locked due card
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        val dueCards = cardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", dueCards[0].id)
    }

    @Test
    fun `locked cards not included in getAllUnlockedWithFsrs`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked card
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        // Locked card
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        val unlockedCards = cardDao.getAllUnlockedWithFsrs()

        assertEquals(1, unlockedCards.size)
        assertEquals("MAJOR_4_ARPEGGIATED", unlockedCards[0].id)
    }

    @Test
    fun `countUnlocked only counts unlocked cards`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = false))

        val unlockedCount = cardDao.countUnlocked()
        val totalCount = cardDao.count()

        assertEquals(2, unlockedCount)
        assertEquals(3, totalCount)
    }
}
