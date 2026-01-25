package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionCardDaoTest : DatabaseTestBase() {

    // ========== Basic Insert/Query Tests ==========

    @Test
    fun `insert and getById returns inserted card`() = runTest {
        val card = FunctionCardEntity(
            id = "V_MAJOR_4_ARPEGGIATED",
            function = "V",
            keyQuality = "MAJOR",
            octave = 4,
            playbackMode = "ARPEGGIATED"
        )
        functionCardDao.insert(card)

        val retrieved = functionCardDao.getById("V_MAJOR_4_ARPEGGIATED")

        assertNotNull(retrieved)
        assertEquals("V", retrieved!!.function)
        assertEquals("MAJOR", retrieved.keyQuality)
        assertEquals(4, retrieved.octave)
        assertEquals("ARPEGGIATED", retrieved.playbackMode)
        assertTrue(retrieved.unlocked)
    }

    @Test
    fun `getById returns null for non-existent card`() = runTest {
        val retrieved = functionCardDao.getById("NON_EXISTENT")
        assertNull(retrieved)
    }

    @Test
    fun `insertAll inserts multiple cards`() = runTest {
        val cards = listOf(
            FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"),
            FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"),
            FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED")
        )
        functionCardDao.insertAll(cards)

        assertEquals(3, functionCardDao.count())
    }

    @Test
    fun `insert with IGNORE strategy ignores duplicates`() = runTest {
        val card = FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED")
        functionCardDao.insert(card)
        functionCardDao.insert(card)  // Duplicate insert

        assertEquals(1, functionCardDao.count())
    }

    // ========== Unlocked Query Tests ==========

    @Test
    fun `getAllUnlocked returns only unlocked cards`() = runTest {
        val unlockedCard = FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true)
        val lockedCard = FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false)
        functionCardDao.insertAll(listOf(unlockedCard, lockedCard))

        val unlocked = functionCardDao.getAllUnlocked()

        assertEquals(1, unlocked.size)
        assertEquals("V_MAJOR_4_ARPEGGIATED", unlocked[0].id)
    }

    @Test
    fun `countUnlocked returns correct count`() = runTest {
        val cards = listOf(
            FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true),
            FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true),
            FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED", unlocked = false)
        )
        functionCardDao.insertAll(cards)

        assertEquals(2, functionCardDao.countUnlocked())
    }

    @Test
    fun `countUnlockedFlow emits correct count`() = runTest {
        functionCardDao.insertAll(listOf(
            FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true),
            FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true)
        ))

        val count = functionCardDao.countUnlockedFlow().first()
        assertEquals(2, count)
    }

    // ========== Key Quality and Group Filter Tests ==========

    @Test
    fun `getByKeyQuality filters by key quality`() = runTest {
        functionCardDao.insertAll(listOf(
            FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"),
            FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"),
            FunctionCardEntity("iv_MINOR_4_ARPEGGIATED", "iv", "MINOR", 4, "ARPEGGIATED")
        ))

        val majorCards = functionCardDao.getByKeyQuality("MAJOR")
        val minorCards = functionCardDao.getByKeyQuality("MINOR")

        assertEquals(2, majorCards.size)
        assertEquals(1, minorCards.size)
        assertTrue(majorCards.all { it.keyQuality == "MAJOR" })
        assertTrue(minorCards.all { it.keyQuality == "MINOR" })
    }

    @Test
    fun `getByGroup filters by octave and mode`() = runTest {
        functionCardDao.insertAll(listOf(
            FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"),
            FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"),
            FunctionCardEntity("IV_MAJOR_5_ARPEGGIATED", "IV", "MAJOR", 5, "ARPEGGIATED"),
            FunctionCardEntity("IV_MAJOR_4_BLOCK", "IV", "MAJOR", 4, "BLOCK")
        ))

        val group4Arp = functionCardDao.getByGroup(4, "ARPEGGIATED")
        val group5Arp = functionCardDao.getByGroup(5, "ARPEGGIATED")
        val group4Block = functionCardDao.getByGroup(4, "BLOCK")

        assertEquals(2, group4Arp.size)
        assertEquals(1, group5Arp.size)
        assertEquals(1, group4Block.size)
    }

    // ========== FSRS Join Query Tests ==========

    @Test
    fun `getAllUnlockedWithFsrs joins cards with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "V_MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_FUNCTION.name,
            stability = 3.0,
            difficulty = 4.0,
            dueDate = now
        ))

        val cardsWithFsrs = functionCardDao.getAllUnlockedWithFsrs()

        assertEquals(1, cardsWithFsrs.size)
        val cardWithFsrs = cardsWithFsrs[0]
        assertEquals("V_MAJOR_4_ARPEGGIATED", cardWithFsrs.id)
        assertEquals("V", cardWithFsrs.function)
        assertEquals("MAJOR", cardWithFsrs.keyQuality)
        assertEquals(3.0, cardWithFsrs.stability, 0.01)
        assertEquals(4.0, cardWithFsrs.difficulty, 0.01)
    }

    @Test
    fun `getAllUnlockedWithFsrs ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + 2000))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("vi_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + 1000))

        val cardsWithFsrs = functionCardDao.getAllUnlockedWithFsrs()

        assertEquals(3, cardsWithFsrs.size)
        assertEquals("V_MAJOR_4_ARPEGGIATED", cardsWithFsrs[0].id)  // Earliest due
        assertEquals("vi_MAJOR_4_ARPEGGIATED", cardsWithFsrs[1].id)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", cardsWithFsrs[2].id)  // Latest due
    }

    @Test
    fun `getByIdWithFsrs returns card with FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "V_MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_FUNCTION.name,
            stability = 5.5,
            difficulty = 2.5,
            interval = 7,
            dueDate = now,
            reviewCount = 3,
            lastReview = now - DAY_MS,
            phase = 2,
            lapses = 1
        ))

        val cardWithFsrs = functionCardDao.getByIdWithFsrs("V_MAJOR_4_ARPEGGIATED")

        assertNotNull(cardWithFsrs)
        assertEquals(5.5, cardWithFsrs!!.stability, 0.01)
        assertEquals(7, cardWithFsrs.interval)
        assertEquals(2, cardWithFsrs.phase)
    }

    @Test
    fun `getByIdWithFsrs returns null for card without FSRS state`() = runTest {
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))

        val cardWithFsrs = functionCardDao.getByIdWithFsrs("V_MAJOR_4_ARPEGGIATED")

        assertNull(cardWithFsrs)
    }

    // ========== Due Card Query Tests ==========

    @Test
    fun `getDueCards returns only cards with dueDate lte now`() = runTest {
        val now = System.currentTimeMillis()

        // Due card
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - 1000))

        // Not due card
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + 1000))

        // Exactly due card
        functionCardDao.insert(FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("vi_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        val dueCards = functionCardDao.getDueCards(now)

        assertEquals(2, dueCards.size)
        assertTrue(dueCards.any { it.id == "IV_MAJOR_4_ARPEGGIATED" })
        assertTrue(dueCards.any { it.id == "vi_MAJOR_4_ARPEGGIATED" })
    }

    @Test
    fun `getDueCards ordered by dueDate ascending`() = runTest {
        val now = System.currentTimeMillis()

        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - 3000))
        fsrsStateDao.insert(FsrsStateEntity("vi_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - 2000))

        val dueCards = functionCardDao.getDueCards(now)

        assertEquals("V_MAJOR_4_ARPEGGIATED", dueCards[0].id)  // Most overdue
        assertEquals("vi_MAJOR_4_ARPEGGIATED", dueCards[1].id)
        assertEquals("IV_MAJOR_4_ARPEGGIATED", dueCards[2].id)
    }

    @Test
    fun `countDue returns correct count`() = runTest {
        val now = System.currentTimeMillis()

        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED"))

        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - 1000))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + 1000))
        fsrsStateDao.insert(FsrsStateEntity("vi_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        assertEquals(2, functionCardDao.countDue(now))
    }

    // ========== Non-Due Card Query Tests ==========

    @Test
    fun `getNonDueCardsByGroup filters by keyQuality, octave and mode`() = runTest {
        val now = System.currentTimeMillis()

        // Cards in target group (MAJOR, octave 4, ARPEGGIATED) - not due
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + 2 * HOUR_MS))

        // Card in different key quality
        functionCardDao.insert(FunctionCardEntity("iv_MINOR_4_ARPEGGIATED", "iv", "MINOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("iv_MINOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + HOUR_MS))

        // Card in different octave
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_5_ARPEGGIATED", "IV", "MAJOR", 5, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_5_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + HOUR_MS))

        // Card in different mode
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_BLOCK", "IV", "MAJOR", 4, "BLOCK"))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_BLOCK", GameType.CHORD_FUNCTION.name, dueDate = now + HOUR_MS))

        val nonDueCards = functionCardDao.getNonDueCardsByGroup(now, "MAJOR", 4, "ARPEGGIATED", 10)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards.all { it.keyQuality == "MAJOR" && it.octave == 4 && it.playbackMode == "ARPEGGIATED" })
    }

    @Test
    fun `getNonDueCardsByGroup respects limit`() = runTest {
        val now = System.currentTimeMillis()

        // Create 5 non-due cards in same group
        listOf("IV", "V", "vi", "ii", "iii").forEachIndexed { i, func ->
            functionCardDao.insert(FunctionCardEntity("${func}_MAJOR_4_ARPEGGIATED", func, "MAJOR", 4, "ARPEGGIATED"))
            fsrsStateDao.insert(FsrsStateEntity("${func}_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + (i + 1) * HOUR_MS))
        }

        val nonDueCards = functionCardDao.getNonDueCardsByGroup(now, "MAJOR", 4, "ARPEGGIATED", 3)

        assertEquals(3, nonDueCards.size)
    }

    @Test
    fun `getNonDueCards returns non-due cards from any group`() = runTest {
        val now = System.currentTimeMillis()

        // Non-due cards in different groups
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED"))
        functionCardDao.insert(FunctionCardEntity("iv_MINOR_5_BLOCK", "iv", "MINOR", 5, "BLOCK"))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("iv_MINOR_5_BLOCK", GameType.CHORD_FUNCTION.name, dueDate = now + 2 * HOUR_MS))

        // Due card (should be excluded)
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        val nonDueCards = functionCardDao.getNonDueCards(now, 10)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards.none { it.dueDate <= now })
    }

    @Test
    fun `getNonDueCards respects limit and orders by dueDate`() = runTest {
        val now = System.currentTimeMillis()

        listOf("IV", "V", "vi", "ii").forEachIndexed { i, func ->
            functionCardDao.insert(FunctionCardEntity("${func}_MAJOR_4_ARPEGGIATED", func, "MAJOR", 4, "ARPEGGIATED"))
            fsrsStateDao.insert(FsrsStateEntity("${func}_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now + (4 - i) * HOUR_MS))
        }

        val nonDueCards = functionCardDao.getNonDueCards(now, 2)

        assertEquals(2, nonDueCards.size)
        assertTrue(nonDueCards[0].dueDate <= nonDueCards[1].dueDate)
    }
}
