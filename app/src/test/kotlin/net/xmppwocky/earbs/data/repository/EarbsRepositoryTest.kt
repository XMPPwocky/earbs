package net.xmppwocky.earbs.data.repository

import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.model.Deck
import net.xmppwocky.earbs.model.FunctionDeck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EarbsRepositoryTest : DatabaseTestBase() {

    private lateinit var repository: EarbsRepository

    @Before
    fun setupRepository() {
        repository = EarbsRepository(
            cardDao = cardDao,
            functionCardDao = functionCardDao,
            fsrsStateDao = fsrsStateDao,
            reviewSessionDao = reviewSessionDao,
            trialDao = trialDao,
            historyDao = historyDao,
            prefs = prefs
        )
    }

    // ========== Initialization Tests (Chord Type) ==========

    @Test
    fun `initializeStartingDeck creates 4 cards when empty`() = runTest {
        repository.initializeStartingDeck()

        val count = cardDao.count()
        assertEquals(4, count)
    }

    @Test
    fun `initializeStartingDeck creates correct chord types`() = runTest {
        repository.initializeStartingDeck()

        val cards = cardDao.getAllUnlocked()
        val chordTypes = cards.map { it.chordType }.toSet()

        assertEquals(setOf("MAJOR", "MINOR", "SUS2", "SUS4"), chordTypes)
    }

    @Test
    fun `initializeStartingDeck creates cards at octave 4 arpeggiated`() = runTest {
        repository.initializeStartingDeck()

        val cards = cardDao.getAllUnlocked()
        assertTrue(cards.all { it.octave == 4 })
        assertTrue(cards.all { it.playbackMode == "ARPEGGIATED" })
    }

    @Test
    fun `initializeStartingDeck creates FSRS states`() = runTest {
        repository.initializeStartingDeck()

        val fsrsStates = fsrsStateDao.getByGameType(GameType.CHORD_TYPE.name)
        assertEquals(4, fsrsStates.size)
        assertTrue(fsrsStates.all { it.gameType == GameType.CHORD_TYPE.name })
    }

    @Test
    fun `initializeStartingDeck does nothing when cards exist`() = runTest {
        // Insert existing card
        cardDao.insert(CardEntity("EXISTING_CARD", "MAJOR", 4, "ARPEGGIATED"))

        repository.initializeStartingDeck()

        // Should not add starting deck
        assertEquals(1, cardDao.count())
    }

    @Test
    fun `initializeStartingDeck sets unlock level to 0`() = runTest {
        repository.initializeStartingDeck()

        assertEquals(0, repository.getUnlockLevel())
    }

    // ========== Unlock Tests (Chord Type) ==========

    @Test
    fun `unlockNextGroup adds new cards`() = runTest {
        repository.initializeStartingDeck()
        assertEquals(4, cardDao.count())

        val result = repository.unlockNextGroup()

        assertTrue(result)
        assertEquals(8, cardDao.count())
    }

    @Test
    fun `unlockNextGroup increments unlock level`() = runTest {
        repository.initializeStartingDeck()
        assertEquals(0, repository.getUnlockLevel())

        repository.unlockNextGroup()

        assertEquals(1, repository.getUnlockLevel())
    }

    @Test
    fun `unlockNextGroup creates FSRS states for new cards`() = runTest {
        repository.initializeStartingDeck()

        repository.unlockNextGroup()

        val fsrsStates = fsrsStateDao.getByGameType(GameType.CHORD_TYPE.name)
        assertEquals(8, fsrsStates.size)
    }

    @Test
    fun `unlockNextGroup returns false at max level`() = runTest {
        repository.initializeStartingDeck()

        // Unlock all groups
        repeat(Deck.MAX_UNLOCK_LEVEL) {
            repository.unlockNextGroup()
        }

        assertEquals(Deck.MAX_UNLOCK_LEVEL, repository.getUnlockLevel())

        val result = repository.unlockNextGroup()

        assertFalse(result)
        assertEquals(Deck.MAX_UNLOCK_LEVEL, repository.getUnlockLevel())
    }

    @Test
    fun `canUnlockMore returns true when not at max`() = runTest {
        repository.initializeStartingDeck()

        assertTrue(repository.canUnlockMore())
    }

    @Test
    fun `canUnlockMore returns false at max level`() = runTest {
        repository.initializeStartingDeck()

        repeat(Deck.MAX_UNLOCK_LEVEL) {
            repository.unlockNextGroup()
        }

        assertFalse(repository.canUnlockMore())
    }

    @Test
    fun `full unlock results in 48 cards`() = runTest {
        repository.initializeStartingDeck()

        while (repository.canUnlockMore()) {
            repository.unlockNextGroup()
        }

        assertEquals(Deck.TOTAL_CARDS, cardDao.count())
    }

    // ========== Initialization Tests (Function Cards) ==========

    @Test
    fun `initializeFunctionStartingDeck creates 3 cards when empty`() = runTest {
        repository.initializeFunctionStartingDeck()

        val count = functionCardDao.count()
        assertEquals(3, count)
    }

    @Test
    fun `initializeFunctionStartingDeck creates correct functions`() = runTest {
        repository.initializeFunctionStartingDeck()

        val cards = functionCardDao.getAllUnlocked()
        val functions = cards.map { it.function }.toSet()

        assertEquals(setOf("IV", "V", "vi"), functions)
    }

    @Test
    fun `initializeFunctionStartingDeck creates major key cards`() = runTest {
        repository.initializeFunctionStartingDeck()

        val cards = functionCardDao.getAllUnlocked()
        assertTrue(cards.all { it.keyQuality == "MAJOR" })
        assertTrue(cards.all { it.octave == 4 })
        assertTrue(cards.all { it.playbackMode == "ARPEGGIATED" })
    }

    @Test
    fun `initializeFunctionStartingDeck creates FSRS states`() = runTest {
        repository.initializeFunctionStartingDeck()

        val fsrsStates = fsrsStateDao.getByGameType(GameType.CHORD_FUNCTION.name)
        assertEquals(3, fsrsStates.size)
        assertTrue(fsrsStates.all { it.gameType == GameType.CHORD_FUNCTION.name })
    }

    @Test
    fun `initializeFunctionStartingDeck does nothing when cards exist`() = runTest {
        functionCardDao.insert(FunctionCardEntity("EXISTING", "V", "MAJOR", 4, "ARPEGGIATED"))

        repository.initializeFunctionStartingDeck()

        assertEquals(1, functionCardDao.count())
    }

    @Test
    fun `initializeFunctionStartingDeck sets unlock level to 0`() = runTest {
        repository.initializeFunctionStartingDeck()

        assertEquals(0, repository.getFunctionUnlockLevel())
    }

    // ========== Unlock Tests (Function Cards) ==========

    @Test
    fun `unlockNextFunctionGroup adds new cards`() = runTest {
        repository.initializeFunctionStartingDeck()
        assertEquals(3, functionCardDao.count())

        val result = repository.unlockNextFunctionGroup()

        assertTrue(result)
        assertEquals(6, functionCardDao.count())
    }

    @Test
    fun `unlockNextFunctionGroup increments unlock level`() = runTest {
        repository.initializeFunctionStartingDeck()
        assertEquals(0, repository.getFunctionUnlockLevel())

        repository.unlockNextFunctionGroup()

        assertEquals(1, repository.getFunctionUnlockLevel())
    }

    @Test
    fun `unlockNextFunctionGroup creates FSRS states for new cards`() = runTest {
        repository.initializeFunctionStartingDeck()

        repository.unlockNextFunctionGroup()

        val fsrsStates = fsrsStateDao.getByGameType(GameType.CHORD_FUNCTION.name)
        assertEquals(6, fsrsStates.size)
    }

    @Test
    fun `unlockNextFunctionGroup returns false at max level`() = runTest {
        repository.initializeFunctionStartingDeck()

        // Unlock all groups
        repeat(FunctionDeck.MAX_UNLOCK_LEVEL) {
            repository.unlockNextFunctionGroup()
        }

        assertEquals(FunctionDeck.MAX_UNLOCK_LEVEL, repository.getFunctionUnlockLevel())

        val result = repository.unlockNextFunctionGroup()

        assertFalse(result)
    }

    @Test
    fun `canUnlockMoreFunctions returns true when not at max`() = runTest {
        repository.initializeFunctionStartingDeck()

        assertTrue(repository.canUnlockMoreFunctions())
    }

    @Test
    fun `canUnlockMoreFunctions returns false at max level`() = runTest {
        repository.initializeFunctionStartingDeck()

        repeat(FunctionDeck.MAX_UNLOCK_LEVEL) {
            repository.unlockNextFunctionGroup()
        }

        assertFalse(repository.canUnlockMoreFunctions())
    }

    @Test
    fun `full function unlock results in 72 cards`() = runTest {
        repository.initializeFunctionStartingDeck()

        while (repository.canUnlockMoreFunctions()) {
            repository.unlockNextFunctionGroup()
        }

        assertEquals(FunctionDeck.TOTAL_CARDS, functionCardDao.count())
    }

    // ========== Count Tests ==========

    @Test
    fun `getUnlockedCount returns correct count`() = runTest {
        repository.initializeStartingDeck()

        assertEquals(4, repository.getUnlockedCount())

        repository.unlockNextGroup()

        assertEquals(8, repository.getUnlockedCount())
    }

    @Test
    fun `getDueCount returns cards due now`() = runTest {
        repository.initializeStartingDeck()

        // All starting cards should be immediately due
        val dueCount = repository.getDueCount()

        assertEquals(4, dueCount)
    }

    @Test
    fun `getFunctionUnlockedCount returns correct count`() = runTest {
        repository.initializeFunctionStartingDeck()

        assertEquals(3, repository.getFunctionUnlockedCount())

        repository.unlockNextFunctionGroup()

        assertEquals(6, repository.getFunctionUnlockedCount())
    }

    @Test
    fun `getFunctionDueCount returns function cards due now`() = runTest {
        repository.initializeFunctionStartingDeck()

        val dueCount = repository.getFunctionDueCount()

        assertEquals(3, dueCount)
    }

    // ========== Session Size Tests ==========

    @Test
    fun `getSessionSize returns default when not set`() {
        val size = repository.getSessionSize()

        assertEquals(20, size)
    }

    @Test
    fun `getSessionSize returns configured value`() {
        prefs.edit().putInt("session_size", 10).apply()

        val size = repository.getSessionSize()

        assertEquals(10, size)
    }

    // ========== Independent Game Tracking ==========

    @Test
    fun `chord type and function cards have independent unlock levels`() = runTest {
        repository.initializeStartingDeck()
        repository.initializeFunctionStartingDeck()

        repository.unlockNextGroup()
        repository.unlockNextGroup()

        assertEquals(2, repository.getUnlockLevel())
        assertEquals(0, repository.getFunctionUnlockLevel())

        repository.unlockNextFunctionGroup()

        assertEquals(2, repository.getUnlockLevel())
        assertEquals(1, repository.getFunctionUnlockLevel())
    }

    @Test
    fun `chord type and function cards have independent FSRS states`() = runTest {
        repository.initializeStartingDeck()
        repository.initializeFunctionStartingDeck()

        val chordFsrs = fsrsStateDao.getByGameType(GameType.CHORD_TYPE.name)
        val functionFsrs = fsrsStateDao.getByGameType(GameType.CHORD_FUNCTION.name)

        assertEquals(4, chordFsrs.size)
        assertEquals(3, functionFsrs.size)
    }
}
