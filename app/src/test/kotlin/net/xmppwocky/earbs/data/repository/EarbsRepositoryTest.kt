package net.xmppwocky.earbs.data.repository

import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.ProgressionCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EarbsRepositoryTest : RepositoryTestBase() {

    // ========== Initialization Tests (Chord Type) ==========

    @Test
    fun `initializeStartingDeck creates FSRS states for existing cards`() = runTest {
        // Pre-create cards (as migration would do)
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))

        repository.initializeStartingDeck()

        val fsrsStates = fsrsStateDao.getByGameType(GameType.CHORD_TYPE.name)
        assertEquals(2, fsrsStates.size)
        assertTrue(fsrsStates.all { it.gameType == GameType.CHORD_TYPE.name })
    }

    @Test
    fun `initializeStartingDeck does nothing when FSRS states exist`() = runTest {
        val now = System.currentTimeMillis()
        // Pre-create cards and FSRS states
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_TYPE.name,
            stability = 5.0,  // Non-default value to verify it wasn't recreated
            dueDate = now
        ))

        repository.initializeStartingDeck()

        val fsrsState = fsrsStateDao.getByCardId("MAJOR_4_ARPEGGIATED")
        assertEquals(5.0, fsrsState!!.stability, 0.01)  // Should preserve existing value
    }

    // ========== Card Unlock Tests ==========

    @Test
    fun `setCardUnlocked unlocks a locked card`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        repository.setCardUnlocked("MAJOR_4_ARPEGGIATED", true)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")
        assertTrue(card!!.unlocked)
    }

    @Test
    fun `setCardUnlocked locks an unlocked card`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))

        repository.setCardUnlocked("MAJOR_4_ARPEGGIATED", false)

        val card = cardDao.getById("MAJOR_4_ARPEGGIATED")
        assertFalse(card!!.unlocked)
    }

    @Test
    fun `setCardUnlocked preserves FSRS state`() = runTest {
        val now = System.currentTimeMillis()
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_TYPE.name,
            stability = 8.5,
            difficulty = 3.2,
            interval = 14,
            dueDate = now + DAY_MS,
            reviewCount = 10
        ))

        // Lock the card
        repository.setCardUnlocked("MAJOR_4_ARPEGGIATED", false)

        // Verify FSRS state is preserved
        val fsrsState = fsrsStateDao.getByCardId("MAJOR_4_ARPEGGIATED")
        assertEquals(8.5, fsrsState!!.stability, 0.01)
        assertEquals(3.2, fsrsState.difficulty, 0.01)
        assertEquals(14, fsrsState.interval)
        assertEquals(10, fsrsState.reviewCount)
    }

    // ========== Count Tests ==========

    @Test
    fun `getUnlockedCount returns correct count`() = runTest {
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = false))

        assertEquals(2, repository.getUnlockedCount())
    }

    @Test
    fun `getDueCount returns count of due cards with FSRS state`() = runTest {
        val now = System.currentTimeMillis()

        // Due cards with FSRS state
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        // Not due card
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now + DAY_MS))

        assertEquals(2, repository.getDueCount())
    }

    // ========== Initialization Tests (Function Cards) ==========

    @Test
    fun `initializeFunctionStartingDeck creates FSRS states for existing cards`() = runTest {
        // Pre-create cards (as migration would do)
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))

        repository.initializeFunctionStartingDeck()

        val fsrsStates = fsrsStateDao.getByGameType(GameType.CHORD_FUNCTION.name)
        assertEquals(2, fsrsStates.size)
        assertTrue(fsrsStates.all { it.gameType == GameType.CHORD_FUNCTION.name })
    }

    @Test
    fun `initializeFunctionStartingDeck does nothing when FSRS states exist`() = runTest {
        val now = System.currentTimeMillis()
        // Pre-create cards and FSRS states
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = "IV_MAJOR_4_ARPEGGIATED",
            gameType = GameType.CHORD_FUNCTION.name,
            stability = 6.0,  // Non-default value
            dueDate = now
        ))

        repository.initializeFunctionStartingDeck()

        val fsrsState = fsrsStateDao.getByCardId("IV_MAJOR_4_ARPEGGIATED")
        assertEquals(6.0, fsrsState!!.stability, 0.01)  // Should preserve existing value
    }

    // ========== Function Card Count Tests ==========

    @Test
    fun `getFunctionUnlockedCount returns correct count`() = runTest {
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED", unlocked = false))

        assertEquals(2, repository.getFunctionUnlockedCount())
    }

    @Test
    fun `getFunctionDueCount returns count of due function cards with FSRS state`() = runTest {
        val now = System.currentTimeMillis()

        // Due function cards with FSRS state
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        assertEquals(2, repository.getFunctionDueCount())
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
    fun `chord type and function cards have independent FSRS states`() = runTest {
        val now = System.currentTimeMillis()

        // Create chord type cards with FSRS
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        // Create function cards with FSRS
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        functionCardDao.insert(FunctionCardEntity("vi_MAJOR_4_ARPEGGIATED", "vi", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))
        fsrsStateDao.insert(FsrsStateEntity("vi_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        val chordFsrs = fsrsStateDao.getByGameType(GameType.CHORD_TYPE.name)
        val functionFsrs = fsrsStateDao.getByGameType(GameType.CHORD_FUNCTION.name)

        assertEquals(2, chordFsrs.size)
        assertEquals(3, functionFsrs.size)
    }

    @Test
    fun `getUnlockedCount only counts unlocked cards`() = runTest {
        val now = System.currentTimeMillis()

        // Unlocked chord cards
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        // Locked chord card
        cardDao.insert(CardEntity("SUS2_4_ARPEGGIATED", "SUS2", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("SUS2_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        // getUnlockedCount only counts unlocked cards
        assertEquals(2, repository.getUnlockedCount())
        // Total cards = 3
        assertEquals(3, cardDao.count())
    }

    // ========== Due Count Excludes Locked Cards ==========

    @Test
    fun `getDueCount excludes locked cards`() = runTest {
        val now = System.currentTimeMillis()

        // Locked card with past due date - should NOT count
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        // Unlocked card with past due date - should count
        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        assertEquals(1, repository.getDueCount())
    }

    @Test
    fun `getDueCount returns zero when all due cards are locked`() = runTest {
        val now = System.currentTimeMillis()

        // All cards locked with past due dates
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        cardDao.insert(CardEntity("MINOR_4_ARPEGGIATED", "MINOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("MINOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now - HOUR_MS))

        assertEquals(0, repository.getDueCount())
    }

    @Test
    fun `getFunctionDueCount excludes locked cards`() = runTest {
        val now = System.currentTimeMillis()

        // Locked function card with past due date - should NOT count
        functionCardDao.insert(FunctionCardEntity("IV_MAJOR_4_ARPEGGIATED", "IV", "MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("IV_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        // Unlocked function card with past due date - should count
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now - HOUR_MS))

        assertEquals(1, repository.getFunctionDueCount())
    }

    @Test
    fun `getProgressionDueCount excludes locked cards`() = runTest {
        val now = System.currentTimeMillis()

        // Locked progression card with past due date - should NOT count
        progressionCardDao.insert(ProgressionCardEntity("I_IV_I_MAJOR_4_ARPEGGIATED", "I_IV_I_MAJOR", 4, "ARPEGGIATED", unlocked = false))
        fsrsStateDao.insert(FsrsStateEntity("I_IV_I_MAJOR_4_ARPEGGIATED", GameType.CHORD_PROGRESSION.name, dueDate = now - HOUR_MS))

        // Unlocked progression card with past due date - should count
        progressionCardDao.insert(ProgressionCardEntity("I_V_I_MAJOR_4_ARPEGGIATED", "I_V_I_MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("I_V_I_MAJOR_4_ARPEGGIATED", GameType.CHORD_PROGRESSION.name, dueDate = now - HOUR_MS))

        assertEquals(1, repository.getProgressionDueCount())
    }

    // ========== getCardWithFsrs Game Type Tests ==========

    @Test
    fun `getCardWithFsrs returns chord type card for CHORD_TYPE game`() = runTest {
        val now = System.currentTimeMillis()

        // Create chord type card with FSRS state
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        val result = repository.getCardWithFsrs("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE)

        assertNotNull(result)
        assertEquals("MAJOR_4_ARPEGGIATED", result!!.id)
        assertEquals("Major", result.displayName)
        assertEquals(4, result.octave)
        assertEquals("ARPEGGIATED", result.playbackMode)
        assertTrue(result.unlocked)
    }

    @Test
    fun `getCardWithFsrs returns function card for CHORD_FUNCTION game`() = runTest {
        val now = System.currentTimeMillis()

        // Create function card with FSRS state
        functionCardDao.insert(FunctionCardEntity("V_MAJOR_4_ARPEGGIATED", "V", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION.name, dueDate = now))

        val result = repository.getCardWithFsrs("V_MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION)

        assertNotNull(result)
        assertEquals("V_MAJOR_4_ARPEGGIATED", result!!.id)
        assertEquals("V (major)", result.displayName)
        assertEquals(4, result.octave)
        assertEquals("ARPEGGIATED", result.playbackMode)
        assertTrue(result.unlocked)
    }

    @Test
    fun `getCardWithFsrs returns progression card for CHORD_PROGRESSION game`() = runTest {
        val now = System.currentTimeMillis()

        // Create progression card with FSRS state
        progressionCardDao.insert(ProgressionCardEntity("I_IV_V_I_MAJOR_4_ARPEGGIATED", "I_IV_V_I_MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("I_IV_V_I_MAJOR_4_ARPEGGIATED", GameType.CHORD_PROGRESSION.name, dueDate = now))

        val result = repository.getCardWithFsrs("I_IV_V_I_MAJOR_4_ARPEGGIATED", GameType.CHORD_PROGRESSION)

        assertNotNull(result)
        assertEquals("I_IV_V_I_MAJOR_4_ARPEGGIATED", result!!.id)
        assertEquals("I-IV-V-I (major)", result.displayName)
        assertEquals(4, result.octave)
        assertEquals("ARPEGGIATED", result.playbackMode)
        assertTrue(result.unlocked)
    }

    @Test
    fun `getCardWithFsrs returns null when card not found`() = runTest {
        val result = repository.getCardWithFsrs("NONEXISTENT_CARD", GameType.CHORD_TYPE)

        assertNull(result)
    }

    @Test
    fun `getCardWithFsrs returns null when querying wrong game type`() = runTest {
        val now = System.currentTimeMillis()

        // Create chord type card
        cardDao.insert(CardEntity("MAJOR_4_ARPEGGIATED", "MAJOR", 4, "ARPEGGIATED", unlocked = true))
        fsrsStateDao.insert(FsrsStateEntity("MAJOR_4_ARPEGGIATED", GameType.CHORD_TYPE.name, dueDate = now))

        // Query with wrong game type should return null
        val result = repository.getCardWithFsrs("MAJOR_4_ARPEGGIATED", GameType.CHORD_FUNCTION)

        assertNull(result)
    }
}
