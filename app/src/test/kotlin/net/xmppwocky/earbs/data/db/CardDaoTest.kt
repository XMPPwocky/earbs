package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardDaoTest : DatabaseTestBase() {

    // ========== Basic Insert/Query Tests ==========

    @Test
    fun `insert and getById returns inserted card`() = runTest {
        val card = CardEntity(
            id = "MAJOR_4_ARPEGGIATED",
            chordType = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED"
        )
        cardDao.insert(card)

        val retrieved = cardDao.getById("MAJOR_4_ARPEGGIATED")

        assertNotNull(retrieved)
        assertEquals("MAJOR", retrieved!!.chordType)
        assertEquals(4, retrieved.octave)
        assertEquals("ARPEGGIATED", retrieved.playbackMode)
        assertTrue(retrieved.unlocked)
    }

    @Test
    fun `getById returns null for non-existent card`() = runTest {
        val retrieved = cardDao.getById("NON_EXISTENT")
        assertNull(retrieved)
    }

    @Test
    fun `insertAll inserts multiple cards`() = runTest {
        val cards = listOf(
            CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"),
            CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"),
            CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED")
        )
        cardDao.insertAll(cards)

        assertEquals(3, cardDao.count())
    }

    @Test
    fun `insert with IGNORE strategy ignores duplicates`() = runTest {
        val card = CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED")
        cardDao.insert(card)
        cardDao.insert(card)  // Duplicate insert

        assertEquals(1, cardDao.count())
    }

    // ========== Unlocked Query Tests ==========

    @Test
    fun `getAllUnlocked returns only unlocked cards`() = runTest {
        val unlockedCard = CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true)
        val lockedCard = CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false)
        cardDao.insertAll(listOf(unlockedCard, lockedCard))

        val unlocked = cardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("MAJOR_4_ARPEGGIATED", unlocked[0].id)
    }

    @Test
    fun `countUnlocked returns correct count`() = runTest {
        val cards = listOf(
            CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true),
            CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true),
            CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = false)
        )
        cardDao.insertAll(cards)

        assertEquals(2, cardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow emits correct count`() = runTest {
        cardDao.insertAll(listOf(
            CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true),
            CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true)
        ))

        val count = cardDao.countUnlockedFlow().first()
        assertEquals(2, count)
    }

    // ========== FSRS Join Query Tests ==========

    @Test
    fun `getAllUnlockedWithFsrs joins cards with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_TYPE.name,
            stability = 3.0,
            difficulty = 4.0,
            dueDate = now
        ))

        val cardsWithFsrs = cardDao.getAllUnlockedWithFsrs()

        assertEquals(1, cardsWithFsrs.size)
        val cardWithFsrs = cardsWithFsrs[0]
        assertEquals("MAJOR_4_ARPEGGIATED", cardWithFsrs.id)
        assertEquals("MAJOR", cardWithFsrs.chordType)
        assertEquals(3.0, cardWithFsrs.stability, 0.01)
        assertEquals(4.0, cardWithFsrs.difficulty, 0.01)
    }

    @Test
    fun `getAllUnlockedWithFsrs ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        // Insert cards with different due dates
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 2000))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 1000))

        val cardsWithFsrs = cardDao.getAllUnlockedWithFsrs()

        assertEquals(3, cardsWithFsrs.size)
        assertEquals("MINOR_4_ARPEGGIATED", cardsWithFsrs[0].id)  // Earliest due
        assertEquals("SUS2_4_ARPEGGIATED", cardsWithFsrs[1].id)
        assertEquals("MAJOR_4_ARPEGGIATED", cardsWithFsrs[2].id)  // Latest due
    }

    @Test
    fun `getByIdWithFsrs returns card with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_TYPE.name,
            stability = 5.5,
            difficulty = 2.5,
            interval = 7,
            dueDate = now,
            reviewCount = 3,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        ))

        val cardWithFsrs = cardDao.getByIdWithFsrs("MAJOR_4_ARPEGGIATED")

        assertNotNull(cardWithFsrs)
        assertEquals(5.5, cardWithFsrs!!.stability, 0.01)
        assertEquals(2.5, cardWithFsrs.difficulty, 0.01)
        assertEquals(7, cardWithFsrs.interval)
        assertEquals(3, cardWithFsrs.reviewCount)
        assertEquals(2, cardWithFsrs.phase)
        assertEquals(1, cardWithFsrs.lapses)
    }

    @Test
    fun `getByIdWithFsrs returns null for card without FSRS state`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))

        val cardWithFsrs = cardDao.getByIdWithFsrs("MAJOR_4_ARPEGGIATED")

        assertNull(cardWithFsrs)
    }

    // ========== Due Card Query Tests ==========

    @Test
    fun `getDueCards returns only cards with dueDate lte now`() = runTest {
        val now = System.currentTimeMillis()

        // Due card (dueDate in the past)
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - 1000))

        // Not due card (dueDate in the future)
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 1000))

        // Exactly due card (dueDate = now)
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        val dueCards = cardDao.getDueCards(now)

        assertEquals(2, dueCards.size)
        assertTrue(dueCards.any { it.id == "MAJOR_4_ARPEGGIATED" })
        assertTrue(dueCards.any { it.id == "SUS2_4_ARPEGGIATED" })
    }

    @Test
    fun `getDueCards ordered by dueDate ascending - most overdue first`() = runTest {
        val now = System.currentTimeMillis()

        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - 3000))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - 2000))

        val dueCards = cardDao.getDueCards(now)

        assertEquals(3, dueCards.size)
        assertEquals("MINOR_4_ARPEGGIATED", dueCards[0].id)  // Most overdue
        assertEquals("SUS2_4_ARPEGGIATED", dueCards[1].id)
        assertEquals("MAJOR_4_ARPEGGIATED", dueCards[2].id)  // Least overdue
    }

    @Test
    fun `countDue returns correct count`() = runTest {
        val now = System.currentTimeMillis()

        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 1000))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        assertEquals(2, cardDao.countDue(now))
    }

    // ========== Non-Due Card Query Tests ==========

    @Test
    fun `getNonDueCardsByGroup filters by octave and mode`() = runTest {
        val now = System.currentTimeMillis()

        // Cards in target group (octave 4, ARPEGGIATED) - not due
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 2 * HOUR_MS))

        // Card in different octave
        cardDao.insert(CardEntity("SUS2_5_ARPEGGIATED", "SUS2", 5, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_5_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + HOUR_MS))

        // Card in different mode
        cardDao.insert(CardEntity("SUS4_4_BLOCK", "SUS4", 4, "BLOCK"))
        fsrsStateDao.insert(FsrsStateEntity("SUS4_4_BLOCK", GameType.CHORD_TYPE.name, dueDate = now + HOUR_MS))

        val nonDueCards = cardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 10)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards.all { it.octave == 4 && it.playbackMode == "ARPEGGIATED" })
    }

    @Test
    fun `getNonDueCardsByGroup respects limit`() = runTest {
        val now = System.currentTimeMillis()

        // Create 5 non-due cards in same group
        listOf("MAJOR", "MINOR", "SUS2", "SUS4", "DOM7").forEachIndexed { i, type ->
            cardDao.insert(CardEntity("${type}_4_ARPEGGIATED", type, 4, "ARPEGGIATED"))
            fsrsStateDao.insert(FsrsStateEntity("${type}_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + (i + 1) * HOUR_MS))
        }

        val nonDueCards = cardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 3)

        assertEquals(3, nonDueCards.size)
    }

    @Test
    fun `getNonDueCardsByGroup orders by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 3 * HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + 2 * HOUR_MS))

        val nonDueCards = cardDao.getNonDueCardsByGroup(now, 4, "ARPEGGIATED", 10)

        assertEquals("MINOR_4_ARPEGGIATED", nonDueCards[0].id)  // Soonest due
        assertEquals("SUS2_4_ARPEGGIATED", nonDueCards[1].id)
        assertEquals("MAJOR_4_ARPEGGIATED", nonDueCards[2].id)  // Latest due
    }

    @Test
    fun `getNonDueCards returns non-due cards from any group`() = runTest {
        val now = System.currentTimeMillis()

        // Non-due cards in different groups
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED"))
        cardDao.insert(CardEntity("MINOR_5_BLOCK", "MINOR", 5, "BLOCK"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_5_BLOCK", GameType.CHORD_TYPE.name, dueDate = now + 2 * HOUR_MS))

        // Due card (should be excluded)
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        val nonDueCards = cardDao.getNonDueCards(now, 10)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards.none { it.dueDate <= now })
    }

    @Test
    fun `getNonDueCards respects limit and orders by dueDate`() = runTest {
        val now = System.currentTimeMillis()

        listOf("MAJOR", "MINOR", "SUS2", "SUS4").forEachIndexed { i, type ->
            cardDao.insert(CardEntity("${type}_4_ARPEGGIATED", type, 4, "ARPEGGIATED"))
            fsrsStateDao.insert(FsrsStateEntity("${type}_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + (4 - i) * HOUR_MS))
        }

        val nonDueCards = cardDao.getNonDueCards(now, 2)

        assertEquals(2, nonDueCards.size)
        // Should get the 2 cards with earliest due dates
        assertTrue(nonDueCards[0].dueDate <= nonDueCards[1].dueDate)
    }
}
