package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.ScaleCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleCardDaoTest : DatabaseTestBase() {

    // ========== Basic Insert/Query Tests ==========

    @Test
    fun `insert and getById returns inserted card`() = runTest {
        val card = ScaleCardEntity(
            id = "MAJOR_4_ASCENDING",
            scale = "MAJOR",
            octave = 4,
            direction = "ASCENDING"
        )
        scaleCardDao.insert(card)

        val retrieved = scaleCardDao.getById("MAJOR_4_ASCENDING")

        assertNotNull(retrieved)
        assertEquals("MAJOR", retrieved!!.scale)
        assertEquals(4, retrieved.octave)
        assertEquals("ASCENDING", retrieved.direction)
        assertTrue(retrieved.unlocked)
    }

    @Test
    fun `getById returns null for non-existent card`() = runTest {
        val retrieved = scaleCardDao.getById("NON_EXISTENT")
        assertNull(retrieved)
    }

    @Test
    fun `insertAll inserts multiple cards`() = runTest {
        val cards = listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"),
            ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING")
        )
        scaleCardDao.insertAll(cards)

        assertEquals(3, scaleCardDao.count())
    }

    @Test
    fun `insert with IGNORE strategy ignores duplicates`() = runTest {
        val card = ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING")
        scaleCardDao.insert(card)
        scaleCardDao.insert(card)  // Duplicate insert

        assertEquals(1, scaleCardDao.count())
    }

    // ========== Unlocked Query Tests ==========

    @Test
    fun `getAllUnlocked returns only unlocked cards`() = runTest {
        val unlockedCard = ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", unlocked = true)
        val lockedCard = ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", unlocked = false)
        scaleCardDao.insertAll(listOf(unlockedCard, lockedCard))

        val unlocked = scaleCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("MAJOR_4_ASCENDING", unlocked[0].id)
    }

    @Test
    fun `countUnlocked returns correct count`() = runTest {
        val cards = listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", unlocked = true),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", unlocked = true),
            ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING", unlocked = false)
        )
        scaleCardDao.insertAll(cards)

        assertEquals(2, scaleCardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow emits correct count`() = runTest {
        scaleCardDao.insertAll(listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", unlocked = true),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", unlocked = true)
        ))

        val count = scaleCardDao.countUnlockedFlow().first()
        assertEquals(2, count)
    }

    // ========== Direction and Group Filter Tests ==========

    @Test
    fun `getByGroup filters by octave and direction`() = runTest {
        scaleCardDao.insertAll(listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"),
            ScaleCardEntity("MAJOR_5_ASCENDING", "MAJOR", 5, "ASCENDING"),
            ScaleCardEntity("MAJOR_4_DESCENDING", "MAJOR", 4, "DESCENDING")
        ))

        val group4Asc = scaleCardDao.getByGroup(4, "ASCENDING")
        val group5Asc = scaleCardDao.getByGroup(5, "ASCENDING")
        val group4Desc = scaleCardDao.getByGroup(4, "DESCENDING")

        assertEquals(2, group4Asc.size)
        assertEquals(1, group5Asc.size)
        assertEquals(1, group4Desc.size)
    }

    @Test
    fun `getAllUnlocked excludes deprecated cards`() = runTest {
        scaleCardDao.insertAll(listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", unlocked = true, deprecated = false),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", unlocked = true, deprecated = true)
        ))

        val unlocked = scaleCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("MAJOR_4_ASCENDING", unlocked[0].id)
    }

    // ========== FSRS Join Query Tests ==========

    @Test
    fun `getAllUnlockedWithFsrs joins cards with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ASCENDING",
            gameType = GameType.SCALE.name,
            stability = 3.0,
            difficulty = 4.0,
            dueDate = now
        ))

        val cardsWithFsrs = scaleCardDao.getAllUnlockedWithFsrs()

        assertEquals(1, cardsWithFsrs.size)
        val cardWithFsrs = cardsWithFsrs[0]
        assertEquals("MAJOR_4_ASCENDING", cardWithFsrs.id)
        assertEquals("MAJOR", cardWithFsrs.scale)
        assertEquals("ASCENDING", cardWithFsrs.direction)
        assertEquals(3.0, cardWithFsrs.stability, 0.01)
        assertEquals(4.0, cardWithFsrs.difficulty, 0.01)
    }

    @Test
    fun `getAllUnlockedWithFsrs ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now + 2000))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("NATURAL_MINOR_4_ASCENDING", GameType.SCALE.name, dueDate = now + 1000))

        val cardsWithFsrs = scaleCardDao.getAllUnlockedWithFsrs()

        assertEquals(3, cardsWithFsrs.size)
        assertEquals("DORIAN_4_ASCENDING", cardsWithFsrs[0].id)  // Earliest due
        assertEquals("NATURAL_MINOR_4_ASCENDING", cardsWithFsrs[1].id)
        assertEquals("MAJOR_4_ASCENDING", cardsWithFsrs[2].id)  // Latest due
    }

    @Test
    fun `getByIdWithFsrs returns card with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ASCENDING",
            gameType = GameType.SCALE.name,
            stability = 5.5,
            difficulty = 2.5,
            interval = 7,
            dueDate = now,
            reviewCount = 3,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        ))

        val cardWithFsrs = scaleCardDao.getByIdWithFsrs("MAJOR_4_ASCENDING")

        assertNotNull(cardWithFsrs)
        assertEquals(5.5, cardWithFsrs!!.stability, 0.01)
        assertEquals(7, cardWithFsrs.interval)
        assertEquals(2, cardWithFsrs.phase)
    }

    @Test
    fun `getByIdWithFsrs returns null for card without FSRS state`() = runTest {
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))

        val cardWithFsrs = scaleCardDao.getByIdWithFsrs("MAJOR_4_ASCENDING")

        assertNull(cardWithFsrs)
    }

    // ========== Due Card Query Tests ==========

    @Test
    fun `getDueCards returns only cards with dueDate lte now`() = runTest {
        val now = System.currentTimeMillis()

        // Due card
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now - 1000))

        // Not due card
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now + 1000))

        // Exactly due card
        scaleCardDao.insert(ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("NATURAL_MINOR_4_ASCENDING", GameType.SCALE.name, dueDate = now))

        val dueCards = scaleCardDao.getDueCards(now)

        assertEquals(2, dueCards.size)
        assertTrue(dueCards.any { it.id == "MAJOR_4_ASCENDING" })
        assertTrue(dueCards.any { it.id == "NATURAL_MINOR_4_ASCENDING" })
    }

    @Test
    fun `getDueCards ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now - 3000))
        fsrsStateDao.insert(FsrsStateEntity("NATURAL_MINOR_4_ASCENDING", GameType.SCALE.name, dueDate = now - 2000))

        val dueCards = scaleCardDao.getDueCards(now)

        assertEquals("DORIAN_4_ASCENDING", dueCards[0].id)  // Most overdue
        assertEquals("NATURAL_MINOR_4_ASCENDING", dueCards[1].id)
        assertEquals("MAJOR_4_ASCENDING", dueCards[2].id)
    }

    @Test
    fun `countDue returns correct count`() = runTest {
        val now = System.currentTimeMillis()

        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING"))

        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now + 1000))
        fsrsStateDao.insert(FsrsStateEntity("NATURAL_MINOR_4_ASCENDING", GameType.SCALE.name, dueDate = now))

        assertEquals(2, scaleCardDao.countDue(now))
    }

    @Test
    fun `getDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Due active card
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now - 1000))

        // Due deprecated card
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", deprecated = true))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now - 1000))

        val dueCards = scaleCardDao.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals("MAJOR_4_ASCENDING", dueCards[0].id)
    }

    // ========== Non-Due Card Query Tests ==========

    @Test
    fun `getNonDueCardsByGroup filters by scale, octave and direction`() = runTest {
        val now = System.currentTimeMillis()

        // Cards in target group (MAJOR, octave 4, ASCENDING) - not due
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))

        // Card with different scale
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))

        // Card in different octave
        scaleCardDao.insert(ScaleCardEntity("MAJOR_5_ASCENDING", "MAJOR", 5, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_5_ASCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))

        // Card in different direction
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_DESCENDING", "MAJOR", 4, "DESCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_DESCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))

        val nonDueCards = scaleCardDao.getNonDueCardsByGroup(now, "MAJOR", 4, "ASCENDING", 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("MAJOR_4_ASCENDING", nonDueCards[0].id)
    }

    @Test
    fun `getNonDueCardsByGroup respects limit`() = runTest {
        val now = System.currentTimeMillis()

        // Create 3 non-due cards with same scale, octave, direction
        listOf("MAJOR", "MAJOR", "MAJOR").forEachIndexed { i, scale ->
            val id = "${scale}_4_ASCENDING_$i"
            scaleCardDao.insert(ScaleCardEntity(id, scale, 4, "ASCENDING"))
            fsrsStateDao.insert(FsrsStateEntity(id, GameType.SCALE.name, dueDate = now + (i + 1) * HOUR_MS))
        }

        val nonDueCards = scaleCardDao.getNonDueCardsByGroup(now, "MAJOR", 4, "ASCENDING", 2)

        assertEquals(2, nonDueCards.size)
    }

    @Test
    fun `getNonDueCards returns non-due cards from any group`() = runTest {
        val now = System.currentTimeMillis()

        // Non-due cards in different groups
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING"))
        scaleCardDao.insert(ScaleCardEntity("DORIAN_5_DESCENDING", "DORIAN", 5, "DESCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_5_DESCENDING", GameType.SCALE.name, dueDate = now + 2 * HOUR_MS))

        // Due card (should be excluded)
        scaleCardDao.insert(ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING"))
        fsrsStateDao.insert(FsrsStateEntity("NATURAL_MINOR_4_ASCENDING", GameType.SCALE.name, dueDate = now - HOUR_MS))

        val nonDueCards = scaleCardDao.getNonDueCards(now, 10)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards.none { it.dueDate <= now })
    }

    @Test
    fun `getNonDueCards respects limit and orders by dueDate`() = runTest {
        val now = System.currentTimeMillis()

        listOf("MAJOR", "DORIAN", "NATURAL_MINOR", "HARMONIC_MINOR").forEachIndexed { i, scale ->
            scaleCardDao.insert(ScaleCardEntity("${scale}_4_ASCENDING", scale, 4, "ASCENDING"))
            fsrsStateDao.insert(FsrsStateEntity("${scale}_4_ASCENDING", GameType.SCALE.name, dueDate = now + (4 - i) * HOUR_MS))
        }

        val nonDueCards = scaleCardDao.getNonDueCards(now, 2)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards[0].dueDate <= nonDueCards[1].dueDate)
    }

    @Test
    fun `getNonDueCards excludes deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Non-due active card
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))

        // Non-due deprecated card
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", deprecated = true))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now + HOUR_MS))

        val nonDueCards = scaleCardDao.getNonDueCards(now, 10)

        assertEquals(1, nonDueCards.size)
        assertEquals("MAJOR_4_ASCENDING", nonDueCards[0].id)
    }

    // ========== Unlock/Lock Tests ==========

    @Test
    fun `setUnlocked updates card unlocked status`() = runTest {
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", unlocked = false))

        scaleCardDao.setUnlocked("MAJOR_4_ASCENDING", true)

        val card = scaleCardDao.getById("MAJOR_4_ASCENDING")
        assertTrue(card!!.unlocked)
    }

    @Test
    fun `setDeprecated updates card deprecated status`() = runTest {
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false))

        scaleCardDao.setDeprecated("MAJOR_4_ASCENDING", true)

        val card = scaleCardDao.getById("MAJOR_4_ASCENDING")
        assertTrue(card!!.deprecated)
    }

    // ========== Deprecated Cards Query Tests ==========

    @Test
    fun `getDeprecatedCardsWithFsrs returns only deprecated cards`() = runTest {
        val now = System.currentTimeMillis()

        // Active card
        scaleCardDao.insert(ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ASCENDING", GameType.SCALE.name, dueDate = now))

        // Deprecated card
        scaleCardDao.insert(ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", deprecated = true))
        fsrsStateDao.insert(FsrsStateEntity("DORIAN_4_ASCENDING", GameType.SCALE.name, dueDate = now))

        val deprecatedCards = scaleCardDao.getDeprecatedCardsWithFsrs()

        assertEquals(1, deprecatedCards.size)
        assertEquals("DORIAN_4_ASCENDING", deprecatedCards[0].id)
    }

    @Test
    fun `countDeprecated returns correct count`() = runTest {
        scaleCardDao.insertAll(listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", deprecated = true),
            ScaleCardEntity("NATURAL_MINOR_4_ASCENDING", "NATURAL_MINOR", 4, "ASCENDING", deprecated = true)
        ))

        assertEquals(2, scaleCardDao.countDeprecated())
    }

    // ========== All Cards Ordered Tests ==========

    @Test
    fun `getAllCardsOrdered excludes deprecated cards`() = runTest {
        scaleCardDao.insertAll(listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", deprecated = true)
        ))

        val cards = scaleCardDao.getAllCardsOrdered()

        assertEquals(1, cards.size)
        assertEquals("MAJOR_4_ASCENDING", cards[0].id)
    }

    @Test
    fun `getAllIds returns all card ids including deprecated`() = runTest {
        scaleCardDao.insertAll(listOf(
            ScaleCardEntity("MAJOR_4_ASCENDING", "MAJOR", 4, "ASCENDING", deprecated = false),
            ScaleCardEntity("DORIAN_4_ASCENDING", "DORIAN", 4, "ASCENDING", deprecated = true)
        ))

        val ids = scaleCardDao.getAllIds()

        assertEquals(2, ids.size)
        assertTrue(ids.contains("MAJOR_4_ASCENDING"))
        assertTrue(ids.contains("DORIAN_4_ASCENDING"))
    }
}
