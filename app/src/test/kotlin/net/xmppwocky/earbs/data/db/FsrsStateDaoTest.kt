package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsStateDaoTest : DatabaseTestBase() {

    // ========== Basic Insert/Query Tests ==========

    @Test
    fun `insert and getByCardId returns state`() = runTest {
        val state = FsrsStateEntity(
            cardId = "MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_TYPE.name,
            stability = 3.5,
            difficulty = 4.5,
            interval = 7,
            dueDate = System.currentTimeMillis(),
            reviewCount = 5,
            lastReview = System.currentTimeMillis() - DAY_MS,
            phase = 2,
            lapses = 1
        )
        fsrsStateDao.insert(state)

        val retrieved = fsrsStateDao.getByCardId("MAJOR_4_ARPEGGIATED")

        assertNotNull(retrieved)
        assertEquals(3.5, retrieved!!.stability, 0.01)
        assertEquals(4.5, retrieved.difficulty, 0.01)
        assertEquals(7, retrieved.interval)
        assertEquals(5, retrieved.reviewCount)
        assertEquals(2, retrieved.phase)
        assertEquals(1, retrieved.lapses)
    }

    @Test
    fun `getByCardId returns null for non-existent card`() = runTest {
        val retrieved = fsrsStateDao.getByCardId("NON_EXISTENT")
        assertNull(retrieved)
    }

    @Test
    fun `insertAll inserts multiple states`() = runTest {
        val states = listOf(
            FsrsStateEntity("CARD_1", GameType.CHORD_TYPE.name, dueDate = System.currentTimeMillis()),
            FsrsStateEntity("CARD_2", GameType.CHORD_TYPE.name, dueDate = System.currentTimeMillis()),
            FsrsStateEntity("CARD_3", GameType.CHORD_FUNCTION.name, dueDate = System.currentTimeMillis())
        )
        fsrsStateDao.insertAll(states)

        assertEquals(2, fsrsStateDao.countByGameType(GameType.CHORD_TYPE.name))
        assertEquals(1, fsrsStateDao.countByGameType(GameType.CHORD_FUNCTION.name))
    }

    @Test
    fun `insert with REPLACE strategy updates existing state`() = runTest {
        val now = System.currentTimeMillis()
        val originalState = FsrsStateEntity("CARD_1", GameType.CHORD_TYPE.name, stability = 2.5, dueDate = now)
        fsrsStateDao.insert(originalState)

        val updatedState = FsrsStateEntity("CARD_1", GameType.CHORD_TYPE.name, stability = 5.0, dueDate = now)
        fsrsStateDao.insert(updatedState)

        val retrieved = fsrsStateDao.getByCardId("CARD_1")
        assertEquals(5.0, retrieved!!.stability, 0.01)
    }

    // ========== Game Type Filter Tests ==========

    @Test
    fun `getByGameType returns only states for specified game type`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("FUNC_1", GameType.CHORD_FUNCTION.name, dueDate = now)
        ))

        val chordTypeStates = fsrsStateDao.getByGameType(GameType.CHORD_TYPE.name)
        val functionStates = fsrsStateDao.getByGameType(GameType.CHORD_FUNCTION.name)

        assertEquals(2, chordTypeStates.size)
        assertEquals(1, functionStates.size)
        assertTrue(chordTypeStates.all { it.gameType == GameType.CHORD_TYPE.name })
        assertTrue(functionStates.all { it.gameType == GameType.CHORD_FUNCTION.name })
    }

    @Test
    fun `countByGameType returns correct count`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("CHORD_3", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("FUNC_1", GameType.CHORD_FUNCTION.name, dueDate = now)
        ))

        assertEquals(3, fsrsStateDao.countByGameType(GameType.CHORD_TYPE.name))
        assertEquals(1, fsrsStateDao.countByGameType(GameType.CHORD_FUNCTION.name))
    }

    @Test
    fun `countByGameTypeFlow emits correct count`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now)
        ))

        val count = fsrsStateDao.countByGameTypeFlow(GameType.CHORD_TYPE.name).first()
        assertEquals(2, count)
    }

    // ========== Due Date Query Tests ==========

    @Test
    fun `getDueByGameType returns only due cards of specified type`() = runTest {
        val now = System.currentTimeMillis()

        fsrsStateDao.insertAll(listOf(
            // Due chord type cards
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now - 1000),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now),
            // Not due chord type card
            FsrsStateEntity("CHORD_3", GameType.CHORD_TYPE.name, dueDate = now + 1000),
            // Due function card
            FsrsStateEntity("FUNC_1", GameType.CHORD_FUNCTION.name, dueDate = now - 1000)
        ))

        val dueChordType = fsrsStateDao.getDueByGameType(GameType.CHORD_TYPE.name, now)
        val dueFunction = fsrsStateDao.getDueByGameType(GameType.CHORD_FUNCTION.name, now)

        assertEquals(2, dueChordType.size)
        assertEquals(1, dueFunction.size)
    }

    @Test
    fun `getAllDue returns all due cards across game types`() = runTest {
        val now = System.currentTimeMillis()

        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now - 1000),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now + 1000),
            FsrsStateEntity("FUNC_1", GameType.CHORD_FUNCTION.name, dueDate = now)
        ))

        val allDue = fsrsStateDao.getAllDue(now)

        assertEquals(2, allDue.size)
        assertTrue(allDue.all { it.dueDate <= now })
    }

    @Test
    fun `countDueByGameType returns correct count`() = runTest {
        val now = System.currentTimeMillis()

        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now - 1000),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now),
            FsrsStateEntity("CHORD_3", GameType.CHORD_TYPE.name, dueDate = now + 1000)
        ))

        assertEquals(2, fsrsStateDao.countDueByGameType(GameType.CHORD_TYPE.name, now))
    }

    @Test
    fun `countDueByGameTypeFlow emits correct count`() = runTest {
        val now = System.currentTimeMillis()

        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now - 1000),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now + 1000)
        ))

        val count = fsrsStateDao.countDueByGameTypeFlow(GameType.CHORD_TYPE.name, now).first()
        assertEquals(1, count)
    }

    @Test
    fun `countAllDue returns total due count`() = runTest {
        val now = System.currentTimeMillis()

        fsrsStateDao.insertAll(listOf(
            FsrsStateEntity("CHORD_1", GameType.CHORD_TYPE.name, dueDate = now - 1000),
            FsrsStateEntity("FUNC_1", GameType.CHORD_FUNCTION.name, dueDate = now),
            FsrsStateEntity("CHORD_2", GameType.CHORD_TYPE.name, dueDate = now + 1000)
        ))

        assertEquals(2, fsrsStateDao.countAllDue(now))
    }

    // ========== Update Tests ==========

    @Test
    fun `updateFsrsState modifies all fields`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insert(FsrsStateEntity("CARD_1", GameType.CHORD_TYPE.name, dueDate = now))

        val newDueDate = now + DAY_MS
        val newLastReview = now
        fsrsStateDao.updateFsrsState(
            cardId = "CARD_1",
            stability = 10.0,
            difficulty = 3.0,
            interval = 14,
            dueDate = newDueDate,
            reviewCount = 7,
            lastReview = newLastReview,
            phase = 2,
            lapses = 2
        )

        val updated = fsrsStateDao.getByCardId("CARD_1")

        assertNotNull(updated)
        assertEquals(10.0, updated!!.stability, 0.01)
        assertEquals(3.0, updated.difficulty, 0.01)
        assertEquals(14, updated.interval)
        assertEquals(newDueDate, updated.dueDate)
        assertEquals(7, updated.reviewCount)
        assertEquals(newLastReview, updated.lastReview)
        assertEquals(2, updated.phase)
        assertEquals(2, updated.lapses)
    }

    @Test
    fun `updateFsrsState with null lastReview`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insert(FsrsStateEntity("CARD_1", GameType.CHORD_TYPE.name, dueDate = now, lastReview = now))

        fsrsStateDao.updateFsrsState(
            cardId = "CARD_1",
            stability = 5.0,
            difficulty = 2.5,
            interval = 7,
            dueDate = now + DAY_MS,
            reviewCount = 3,
            lastReview = null,  // Setting to null
            phase = 1,
            lapses = 0
        )

        val updated = fsrsStateDao.getByCardId("CARD_1")
        assertNull(updated!!.lastReview)
    }

    // ========== Delete Tests ==========

    @Test
    fun `delete removes state`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insert(FsrsStateEntity("CARD_1", GameType.CHORD_TYPE.name, dueDate = now))

        assertNotNull(fsrsStateDao.getByCardId("CARD_1"))

        fsrsStateDao.delete("CARD_1")

        assertNull(fsrsStateDao.getByCardId("CARD_1"))
    }

    @Test
    fun `delete non-existent card does not throw`() = runTest {
        // Should not throw
        fsrsStateDao.delete("NON_EXISTENT")
    }

    // ========== Default Value Tests ==========

    @Test
    fun `new state has default values`() = runTest {
        val now = System.currentTimeMillis()
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "CARD_1",
            gameType = GameType.CHORD_TYPE.name,
            dueDate = now
        ))

        val state = fsrsStateDao.getByCardId("CARD_1")

        assertNotNull(state)
        assertEquals(2.5, state!!.stability, 0.01)
        assertEquals(2.5, state.difficulty, 0.01)
        assertEquals(0, state.interval)
        assertEquals(0, state.reviewCount)
        assertNull(state.lastReview)
        assertEquals(0, state.phase)
        assertEquals(0, state.lapses)
    }
}
