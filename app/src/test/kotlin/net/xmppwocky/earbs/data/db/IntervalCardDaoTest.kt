package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.IntervalCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalCardDaoTest : DatabaseTestBase() {

    // ========== Basic Insert/Query Tests ==========

    @Test
    fun `insert and getById returns inserted card`() = runTest {
        val card = IntervalCardEntity(
            id = "PERFECT_5TH_4_ASCENDING",
            interval = "PERFECT_5TH",
            octave = 4,
            direction = "ASCENDING"
        )
        intervalCardDao.insert(card)

        val retrieved = intervalCardDao.getById("PERFECT_5TH_4_ASCENDING")

        assertNotNull(retrieved)
        assertEquals("PERFECT_5TH", retrieved!!.interval)
        assertEquals(4, retrieved.octave)
        assertEquals("ASCENDING", retrieved.direction)
        assertTrue(retrieved.unlocked)
    }

    @Test
    fun `getById returns null for non-existent card`() = runTest {
        val retrieved = intervalCardDao.getById("NON_EXISTENT")
        assertNull(retrieved)
    }

    @Test
    fun `insertAll inserts multiple cards`() = runTest {
        val cards = listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"),
            IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING")
        )
        intervalCardDao.insertAll(cards)

        assertEquals(3, intervalCardDao.count())
    }

    @Test
    fun `insert with IGNORE strategy ignores duplicates`() = runTest {
        val card = IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING")
        intervalCardDao.insert(card)
        intervalCardDao.insert(card)  // Duplicate insert

        assertEquals(1, intervalCardDao.count())
    }

    // ========== Unlocked Query Tests ==========

    @Test
    fun `getAllUnlocked returns only unlocked cards`() = runTest {
        val unlockedCard = IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", unlocked = true)
        val lockedCard = IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", unlocked = false)
        intervalCardDao.insertAll(listOf(unlockedCard, lockedCard))

        val unlocked = intervalCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("PERFECT_5TH_4_ASCENDING", unlocked[0].id)
    }

    @Test
    fun `countUnlocked returns correct count`() = runTest {
        val cards = listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", unlocked = true),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", unlocked = true),
            IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING", unlocked = false)
        )
        intervalCardDao.insertAll(cards)

        assertEquals(2, intervalCardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow emits correct count`() = runTest {
        intervalCardDao.insertAll(listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", unlocked = true),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", unlocked = true)
        ))

        val count = intervalCardDao.countUnlockedFlow().first()
        assertEquals(2, count)
    }

    // ========== Direction and Group Filter Tests ==========

    @Test
    fun `getByGroup filters by octave and direction`() = runTest {
        intervalCardDao.insertAll(listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"),
            IntervalCardEntity("PERFECT_5TH_5_ASCENDING", "PERFECT_5TH", 5, "ASCENDING"),
            IntervalCardEntity("PERFECT_5TH_4_DESCENDING", "PERFECT_5TH", 4, "DESCENDING")
        ))

        val group4Asc = intervalCardDao.getByGroup(4, "ASCENDING")
        val group5Asc = intervalCardDao.getByGroup(5, "ASCENDING")
        val group4Desc = intervalCardDao.getByGroup(4, "DESCENDING")

        assertEquals(2, group4Asc.size)
        assertEquals(1, group5Asc.size)
        assertEquals(1, group4Desc.size)
    }

    @Test
    fun `getAllUnlocked excludes deprecated cards`() = runTest {
        intervalCardDao.insertAll(listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", unlocked = true, deprecated = false),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", unlocked = true, deprecated = true)
        ))

        val unlocked = intervalCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("PERFECT_5TH_4_ASCENDING", unlocked[0].id)
    }

    // ========== FSRS Join Query Tests ==========

    @Test
    fun `getAllUnlockedWithFsrs joins cards with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "PERFECT_5TH_4_ASCENDING",
            gameType = GameType.INTERVAL.name,
            stability = 3.0,
            difficulty = 4.0,
            dueDate = now
        ))

        val cardsWithFsrs = intervalCardDao.getAllUnlockedWithFsrs()

        assertEquals(1, cardsWithFsrs.size)
        val cardWithFsrs = cardsWithFsrs[0]
        assertEquals("PERFECT_5TH_4_ASCENDING", cardWithFsrs.id)
        assertEquals("PERFECT_5TH", cardWithFsrs.interval)
        assertEquals("ASCENDING", cardWithFsrs.direction)
        assertEquals(3.0, cardWithFsrs.stability, 0.01)
        assertEquals(4.0, cardWithFsrs.difficulty, 0.01)
    }

    @Test
    fun `getAllUnlockedWithFsrs ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING"))

        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + 2000))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("OCTAVE_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + 1000))

        val cardsWithFsrs = intervalCardDao.getAllUnlockedWithFsrs()

        assertEquals(3, cardsWithFsrs.size)
        assertEquals("MAJOR_3RD_4_ASCENDING", cardsWithFsrs[0].id)  // Earliest due
        assertEquals("OCTAVE_4_ASCENDING", cardsWithFsrs[1].id)
        assertEquals("PERFECT_5TH_4_ASCENDING", cardsWithFsrs[2].id)  // Latest due
    }

    @Test
    fun `getByIdWithFsrs returns card with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "PERFECT_5TH_4_ASCENDING",
            gameType = GameType.INTERVAL.name,
            stability = 5.5,
            difficulty = 2.5,
            interval = 7,
            dueDate = now,
            reviewCount = 3,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        ))

        val cardWithFsrs = intervalCardDao.getByIdWithFsrs("PERFECT_5TH_4_ASCENDING")

        assertNotNull(cardWithFsrs)
        assertEquals(5.5, cardWithFsrs!!.stability, 0.01)
        assertEquals(7, cardWithFsrs.interval_)
        assertEquals(2, cardWithFsrs.phase)
    }

    @Test
    fun `getByIdWithFsrs returns null for card without FSRS state`() = runTest {
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))

        val cardWithFsrs = intervalCardDao.getByIdWithFsrs("PERFECT_5TH_4_ASCENDING")

        assertNull(cardWithFsrs)
    }

    // ========== Due Card Query Tests ==========

    @Test
    fun `getDueCards returns only cards with dueDate lte now`() = runTest {
        val now = System.currentTimeMillis()

        // Due card
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 1000))

        // Not due card
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + 1000))

        // Exactly due card
        intervalCardDao.insert(IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("OCTAVE_4_ASCENDING", GameType.INTERVAL.name, dueDate = now))

        val dueCards = intervalCardDao.getDueCards(now)

        assertEquals(2, dueCards.size)
        assertTrue(dueCards.any { it.id == "PERFECT_5TH_4_ASCENDING" })
        assertTrue(dueCards.any { it.id == "OCTAVE_4_ASCENDING" })
    }

    @Test
    fun `getDueCards ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING"))

        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 3000))
        fsrsStateDao.insert(FsrsStateEntity("OCTAVE_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 2000))

        val dueCards = intervalCardDao.getDueCards(now)

        assertEquals("MAJOR_3RD_4_ASCENDING", dueCards[0].id)  // Most overdue
        assertEquals("OCTAVE_4_ASCENDING", dueCards[1].id)
        assertEquals("PERFECT_5TH_4_ASCENDING", dueCards[2].id)
    }

    @Test
    fun `countDue returns correct count`() = runTest {
        val now = System.currentTimeMillis()

        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING"))

        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + 1000))
        fsrsStateDao.insert(FsrsStateEntity("OCTAVE_4_ASCENDING", GameType.INTERVAL.name, dueDate = now))

        assertEquals(2, intervalCardDao.countDue(now))
    }

    @Test
    fun `getDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Due active card
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 1000))

        // Due deprecated card
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", deprecated = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - 1000))

        val dueCards = intervalCardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("PERFECT_5TH_4_ASCENDING", dueCards[0].id)
    }

    // ========== Non-Due Card Query Tests ==========

    @Test
    fun `getNonDueCardsByGroup filters by interval, octave and direction`() = runTest {
        val now = System.currentTimeMillis()

        // Cards in target group (PERFECT_5TH, octave 4, ASCENDING) - not due
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))

        // Card with different interval
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))

        // Card in different octave
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_5_ASCENDING", "PERFECT_5TH", 5, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_5_ASCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))

        // Card in different direction
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_DESCENDING", "PERFECT_5TH", 4, "DESCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_DESCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))

        val nonDueCards = intervalCardDao.getNonDueCardsByGroup(now, "PERFECT_5TH", 4, "ASCENDING", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("PERFECT_5TH_4_ASCENDING", nonDueCards[0].id)
    }

    @Test
    fun `getNonDueCardsByGroup respects limit`() = runTest {
        val now = System.currentTimeMillis()

        // Create 5 non-due cards with same interval, octave, direction
        listOf("PERFECT_5TH", "PERFECT_5TH", "PERFECT_5TH").forEachIndexed { i, interval ->
            val id = "${interval}_4_ASCENDING_$i"
            intervalCardDao.insert(IntervalCardEntity(id, interval, 4, "ASCENDING"))
            fsrsStateDao.insert(FsrsStateEntity(id, GameType.INTERVAL.name, dueDate = now + (i + 1) * HOUR_MS))
        }

        val nonDueCards = intervalCardDao.getNonDueCardsByGroup(now, "PERFECT_5TH", 4, "ASCENDING", 2)

        assertEquals(2, nonDueCards.size)
    }

    @Test
    fun `getNonDueCards returns non-due cards from any group`() = runTest {
        val now = System.currentTimeMillis()

        // Non-due cards in different groups
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING"))
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_5_DESCENDING", "MAJOR_3RD", 5, "DESCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_5_DESCENDING", GameType.INTERVAL.name, dueDate = now + 2 * HOUR_MS))

        // Due card (should be excluded)
        intervalCardDao.insert(IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("OCTAVE_4_ASCENDING", GameType.INTERVAL.name, dueDate = now - HOUR_MS))

        val nonDueCards = intervalCardDao.getNonDueCards(now, 10)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards.none { it.dueDate <= now })
    }

    @Test
    fun `getNonDueCards respects limit and orders by dueDate`() = runTest {
        val now = System.currentTimeMillis()

        listOf("PERFECT_5TH", "MAJOR_3RD", "OCTAVE", "MINOR_3RD").forEachIndexed { i, interval ->
            intervalCardDao.insert(IntervalCardEntity("${interval}_4_ASCENDING", interval, 4, "ASCENDING"))
            fsrsStateDao.insert(FsrsStateEntity("${interval}_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + (4 - i) * HOUR_MS))
        }

        val nonDueCards = intervalCardDao.getNonDueCards(now, 2)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards[0].dueDate <= nonDueCards[1].dueDate)
    }

    @Test
    fun `getNonDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Non-due active card
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))

        // Non-due deprecated card
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", deprecated = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now + HOUR_MS))

        val nonDueCards = intervalCardDao.getNonDueCards(now, 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("PERFECT_5TH_4_ASCENDING", nonDueCards[0].id)
    }

    // ========== Unlock/Lock Tests ==========

    @Test
    fun `setUnlocked updates card unlocked status`() = runTest {
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", unlocked = false))

        intervalCardDao.setUnlocked("PERFECT_5TH_4_ASCENDING", true)

        val card = intervalCardDao.getById("PERFECT_5TH_4_ASCENDING")
        assertTrue(card!!.unlocked)
    }

    @Test
    fun `setDeprecated updates card deprecated status`() = runTest {
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false))

        intervalCardDao.setDeprecated("PERFECT_5TH_4_ASCENDING", true)

        val card = intervalCardDao.getById("PERFECT_5TH_4_ASCENDING")
        assertTrue(card!!.deprecated)
    }

    // ========== Deprecated Cards Query Tests ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active card
        intervalCardDao.insert(IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false))
        fsrsStateDao.insert(FsrsStateEntity("PERFECT_5TH_4_ASCENDING", GameType.INTERVAL.name, dueDate = now))

        // Deprecated card
        intervalCardDao.insert(IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", deprecated = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_3RD_4_ASCENDING", GameType.INTERVAL.name, dueDate = now))

        val deprecatedCards = intervalCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(1, deprecatedCards.size)
        assertEquals("MAJOR_3RD_4_ASCENDING", deprecatedCards[0].id)
    }

    @Test
    fun `countDeprecated returns correct count`() = runTest {
        intervalCardDao.insertAll(listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", deprecated = true),
            IntervalCardEntity("OCTAVE_4_ASCENDING", "OCTAVE", 4, "ASCENDING", deprecated = true)
        ))

        assertEquals(2, intervalCardDao.countDeprecated())
    }

    // ========== All Cards Ordered Tests ==========

    @Test
    fun `getAllCardsOrdered excludes deprecated cards`() = runTest {
        intervalCardDao.insertAll(listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", deprecated = true)
        ))

        val cards = intervalCardDao.getAllCardsOrdered()

        assertEquals(1, cards.size)
        assertEquals("PERFECT_5TH_4_ASCENDING", cards[0].id)
    }

    @Test
    fun `getAllIds returns all card ids including deprecated`() = runTest {
        intervalCardDao.insertAll(listOf(
            IntervalCardEntity("PERFECT_5TH_4_ASCENDING", "PERFECT_5TH", 4, "ASCENDING", deprecated = false),
            IntervalCardEntity("MAJOR_3RD_4_ASCENDING", "MAJOR_3RD", 4, "ASCENDING", deprecated = true)
        ))

        val ids = intervalCardDao.getAllIds()

        assertEquals(2, ids.size)
        assertTrue(ids.contains("PERFECT_5TH_4_ASCENDING"))
        assertTrue(ids.contains("MAJOR_3RD_4_ASCENDING"))
    }
}
