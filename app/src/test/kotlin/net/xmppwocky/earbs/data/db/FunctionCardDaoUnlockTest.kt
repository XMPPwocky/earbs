package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for FunctionCardDao unlock screen methods.
 */
class FunctionCardDaoUnlockTest : DatabaseTestBase() {

    // ========== getAllCardsOrdered Tests ==========

    @Test
    fun `getAllCardsOrdered returns all cards including locked`() = runTest {
        // Insert mix of locked and unlocked cards
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))

        val allCards = functionCardDao.getAllCardsOrdered()

        assertEquals(3, allCards.size)
        assertTrue(allCards.any { it.id == "I_MAJOR_4_ARPEGGIATED" })
        assertTrue(allCards.any { it.id == "IV_MAJOR_4_ARPEGGIATED" })
        assertTrue(allCards.any { it.id == "V_MAJOR_4_ARPEGGIATED" })
    }

    @Test
    fun `getAllCardsOrdered orders by keyQuality ascending first`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MINOR_4_ARPEGGIATED", "I", "MINOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED"))

        val allCards = functionCardDao.getAllCardsOrdered()

        assertEquals(2, allCards.size)
        assertEquals("MAJOR", allCards[0].keyQuality)
        assertEquals("MINOR", allCards[1].keyQuality)
    }

    @Test
    fun `getAllCardsOrdered orders by octave ascending within keyQuality`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_5_ARPEGGIATED", "I", "MAJOR", 5, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_3_ARPEGGIATED", "I", "MAJOR", 3, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED"))

        val allCards = functionCardDao.getAllCardsOrdered()

        assertEquals(3, allCards.size)
        assertEquals(3, allCards[0].octave)
        assertEquals(4, allCards[1].octave)
        assertEquals(5, allCards[2].octave)
    }

    @Test
    fun `getAllCardsOrdered orders by playbackMode ascending within keyQuality and octave`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_BLOCK", "I", "MAJOR", 4, "BLOCK"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED"))

        val allCards = functionCardDao.getAllCardsOrdered()

        assertEquals(2, allCards.size)
        assertEquals("ARPEGGIATED", allCards[0].playbackMode)
        assertEquals("BLOCK", allCards[1].playbackMode)
    }

    @Test
    fun `getAllCardsOrdered orders by function ascending within keyQuality, octave, and mode`() = runTest {
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))

        val allCards = functionCardDao.getAllCardsOrdered()

        assertEquals(3, allCards.size)
        assertEquals("I", allCards[0].function)
        assertEquals("IV", allCards[1].function)
        assertEquals("V", allCards[2].function)
    }

    // ========== setUnlocked Tests ==========

    @Test
    fun `setUnlocked unlocks a locked card`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        functionCardDao.setUnlocked("I_MAJOR_4_ARPEGGIATED", true)

        val card = functionCardDao.getById("I_MAJOR_4_ARPEGGIATED")
        assertNotNull(card)
        assertTrue(card!!.unlocked)
    }

    @Test
    fun `setUnlocked locks an unlocked card`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))

        functionCardDao.setUnlocked("I_MAJOR_4_ARPEGGIATED", false)

        val card = functionCardDao.getById("I_MAJOR_4_ARPEGGIATED")
        assertNotNull(card)
        assertFalse(card!!.unlocked)
    }

    @Test
    fun `setUnlocked preserves FSRS state after lock-unlock cycle`() = runTest {
        val now = System.currentTimeMillis()
        val cardId = "I_MAJOR_4_ARPEGGIATED"

        // Create card with FSRS state
        functionCardDao.insert(FunctionCardEntity(cardId, "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = cardId,
            gameType = GameType.CHORD_FUNCTION.name,
            stability = 6.5,
            difficulty = 4.2,
            interval = 10,
            dueDate = now + DAY_MS,
            reviewCount = 15,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 2
        ))

        // Lock the card
        functionCardDao.setUnlocked(cardId, false)

        // Verify FSRS state unchanged
        val fsrsState = fsrsStateDao.getByCardId(cardId)
        assertNotNull(fsrsState)
        assertEquals(6.5, fsrsState!!.stability, 0.01)
        assertEquals(4.2, fsrsState.difficulty, 0.01)
        assertEquals(10, fsrsState.interval)
        assertEquals(15, fsrsState.reviewCount)
        assertEquals(2, fsrsState.lapses)

        // Unlock the card
        functionCardDao.setUnlocked(cardId, true)

        // Verify FSRS state still unchanged
        val fsrsState2 = fsrsStateDao.getByCardId(cardId)
        assertNotNull(fsrsState2)
        assertEquals(6.5, fsrsState2!!.stability, 0.01)
        assertEquals(4.2, fsrsState2.difficulty, 0.01)
        assertEquals(10, fsrsState2.interval)
        assertEquals(15, fsrsState2.reviewCount)
        assertEquals(2, fsrsState2.lapses)
    }

    // ========== getAllCardsWithFsrsOrdered Tests ==========

    @Test
    fun `getAllCardsWithFsrsOrdered returns all cards including locked`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked card with FSRS
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("I_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        // Locked card with FSRS
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        // Locked card without FSRS
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        val allCards = functionCardDao.getAllCardsWithFsrsOrdered()

        assertEquals(3, allCards.size)
        assertTrue(allCards.any { it.id == "I_MAJOR_4_ARPEGGIATED" && it.unlocked })
        assertTrue(allCards.any { it.id == "IV_MAJOR_4_ARPEGGIATED" && !it.unlocked })
        assertTrue(allCards.any { it.id == "V_MAJOR_4_ARPEGGIATED" && !it.unlocked })
    }

    @Test
    fun `getAllCardsWithFsrsOrdered returns default values for cards without FSRS`() = runTest {
        // Card without FSRS state
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        val allCards = functionCardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, allCards.size)
        val card = allCards[0]
        assertEquals("I_MAJOR_4_ARPEGGIATED", card.id)
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

        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "I_MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_FUNCTION.name,
            stability = 9.5,
            difficulty = 3.8,
            interval = 21,
            dueDate = now + 14 * DAY_MS,
            reviewCount = 25,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        ))

        val allCards = functionCardDao.getAllCardsWithFsrsOrdered()

        assertEquals(1, allCards.size)
        val card = allCards[0]
        assertEquals(9.5, card.stability, 0.01)
        assertEquals(3.8, card.difficulty, 0.01)
        assertEquals(21, card.interval)
        assertEquals(25, card.reviewCount)
        assertEquals(2, card.phase)
        assertEquals(1, card.lapses)
    }

    @Test
    fun `getAllCardsWithFsrsOrdered orders correctly`() = runTest {
        // Insert in wrong order to verify sorting
        functionCardDao.insert(FunctionCardEntity("I_MINOR_4_ARPEGGIATED", "I", "MINOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_BLOCK", "I", "MAJOR", 4, "BLOCK"))
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_3_ARPEGGIATED", "I", "MAJOR", 3, "ARPEGGIATED"))

        val allCards = functionCardDao.getAllCardsWithFsrsOrdered()

        assertEquals(5, allCards.size)
        // Order: keyQuality ASC, octave ASC, playbackMode ASC, function ASC
        assertEquals("I_MAJOR_3_ARPEGGIATED", allCards[0].id)
        assertEquals("I_MAJOR_4_ARPEGGIATED", allCards[1].id)
        assertEquals("V_MAJOR_4_ARPEGGIATED", allCards[2].id)
        assertEquals("I_MAJOR_4_BLOCK", allCards[3].id)
        assertEquals("I_MINOR_4_ARPEGGIATED", allCards[4].id)
    }

    // ========== getAllIds Tests ==========

    @Test
    fun `getAllIds returns all card IDs`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))

        val allIds = functionCardDao.getAllIds()

        assertEquals(3, allIds.size)
        assertTrue(allIds.contains("I_MAJOR_4_ARPEGGIATED"))
        assertTrue(allIds.contains("IV_MAJOR_4_ARPEGGIATED"))
        assertTrue(allIds.contains("V_MAJOR_4_ARPEGGIATED"))
    }

    @Test
    fun `getAllIds returns empty list when no cards`() = runTest {
        val allIds = functionCardDao.getAllIds()

        assertTrue(allIds.isEmpty())
    }

    // ========== Integration Tests ==========

    @Test
    fun `locked cards not included in getDueCards`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked due card
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("I_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        // Locked due card
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        val dueCards = functionCardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("I_MAJOR_4_ARPEGGIATED", dueCards[0].id)
    }

    @Test
    fun `locked cards not included in getAllUnlockedWithFsrs`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked card
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("I_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        // Locked card
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        val unlockedCards = functionCardDao.getAllUnlockedWithFsrs()

        assertEquals(1, unlockedCards.size)
        assertEquals("I_MAJOR_4_ARPEGGIATED", unlockedCards[0].id)
    }

    @Test
    fun `countUnlocked only counts unlocked cards`() = runTest {
        functionCardDao.insert(FunctionCardEntity("I_MAJOR_4_ARPEGGIATED", "I", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        val unlockedCount = functionCardDao.countUnlocked()
        val totalCount = functionCardDao.count()

        assertEquals(2, unlockedCount)
        assertEquals(3, totalCount)
    }
}
