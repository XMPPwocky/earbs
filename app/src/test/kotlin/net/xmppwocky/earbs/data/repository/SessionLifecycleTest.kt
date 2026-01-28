package net.xmppwocky.earbs.data.repository

import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.KeyQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLifecycleTest : RepositoryTestBase() {

    // ========== Session Start Tests ==========

    @Test
    fun `startSession inserts session entity`() = runTest {
        val sessionId = repository.startSession()

        val session = reviewSessionDao.getById(sessionId)
        assertNotNull(session)
    }

    @Test
    fun `startSession returns unique session id`() = runTest {
        val session1 = repository.startSession()
        val session2 = repository.startSession()

        assertTrue(session1 != session2)
    }

    @Test
    fun `startSession sets startedAt timestamp`() = runTest {
        val before = System.currentTimeMillis()
        val sessionId = repository.startSession()
        val after = System.currentTimeMillis()

        val session = reviewSessionDao.getById(sessionId)!!
        assertTrue(session.startedAt in before..after)
    }

    @Test
    fun `startSession sets game type`() = runTest {
        val chordSession = repository.startSession(GameType.CHORD_TYPE)
        val functionSession = repository.startSession(GameType.CHORD_FUNCTION)

        assertEquals(GameType.CHORD_TYPE.name, reviewSessionDao.getById(chordSession)!!.gameType)
        assertEquals(GameType.CHORD_FUNCTION.name, reviewSessionDao.getById(functionSession)!!.gameType)
    }

    @Test
    fun `startSession has null completedAt`() = runTest {
        val sessionId = repository.startSession()

        val session = reviewSessionDao.getById(sessionId)!!
        assertNull(session.completedAt)
    }

    // ========== Trial Recording Tests ==========

    @Test
    fun `recordTrialAndUpdateFsrs inserts trial`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals(1, trials.size)
    }

    @Test
    fun `recordTrialAndUpdateFsrs sets correct wasCorrect flag`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)
        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals(true, trials[0].wasCorrect)
        assertEquals(false, trials[1].wasCorrect)
    }

    @Test
    fun `recordTrialAndUpdateFsrs stores answered chord type for wrong answers`() = runTest {
        val card = setupTestCard()  // MAJOR chord
        val sessionId = repository.startSession()

        // Wrong answer - thought it was MINOR
        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals("MINOR", trials[0].answeredChordType)
    }

    @Test
    fun `recordTrialAndUpdateFsrs stores null answeredChordType for correct answers`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertNull(trials[0].answeredChordType)
    }

    @Test
    fun `recordTrialAndUpdateFsrs sets game type to CHORD_TYPE`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession(GameType.CHORD_TYPE)

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals(GameType.CHORD_TYPE.name, trials[0].gameType)
    }

    // ========== FSRS Update Tests (Correct Answer) ==========

    @Test
    fun `recordTrialAndUpdateFsrs increments review count on correct`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        val fsrsBefore = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(0, fsrsBefore.reviewCount)

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(1, fsrsAfter.reviewCount)
    }

    @Test
    fun `recordTrialAndUpdateFsrs extends due date on correct`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        val before = System.currentTimeMillis()
        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertTrue("Due date should be extended", fsrsAfter.dueDate > before)
    }

    @Test
    fun `recordTrialAndUpdateFsrs sets lastReview on correct`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        val fsrsBefore = fsrsStateDao.getByCardId(card.id)!!
        assertNull(fsrsBefore.lastReview)

        val before = System.currentTimeMillis()
        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)
        val after = System.currentTimeMillis()

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertNotNull(fsrsAfter.lastReview)
        assertTrue(fsrsAfter.lastReview!! in before..after)
    }

    @Test
    fun `recordTrialAndUpdateFsrs does not increment lapses on correct`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(0, fsrsAfter.lapses)
    }

    // ========== FSRS Update Tests (Wrong Answer) ==========

    @Test
    fun `recordTrialAndUpdateFsrs increments review count on wrong`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(1, fsrsAfter.reviewCount)
    }

    @Test
    fun `recordTrialAndUpdateFsrs increments lapses on wrong`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        val fsrsBefore = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(0, fsrsBefore.lapses)

        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(1, fsrsAfter.lapses)
    }

    @Test
    fun `recordTrialAndUpdateFsrs sets relearning phase on wrong`() = runTest {
        // First need to get card out of Added phase
        val card = setupTestCard()
        val sessionId = repository.startSession()

        // First answer correct to move to Review phase
        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        // Now answer wrong
        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(1, fsrsAfter.phase)  // ReLearning = 1
    }

    @Test
    fun `recordTrialAndUpdateFsrs shortens due date on wrong`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        // First answer correct to extend due date
        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)

        val dueDateAfterCorrect = fsrsStateDao.getByCardId(card.id)!!.dueDate

        // Now answer wrong - should reduce interval
        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val dueDateAfterWrong = fsrsStateDao.getByCardId(card.id)!!.dueDate
        assertTrue("Due date should be earlier after wrong answer", dueDateAfterWrong <= dueDateAfterCorrect)
    }

    // ========== Multiple Trials Tests ==========

    @Test
    fun `multiple trials accumulate review count`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repeat(5) {
            repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)
        }

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(5, fsrsAfter.reviewCount)
    }

    @Test
    fun `multiple wrong answers accumulate lapses`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repeat(3) {
            repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)
        }

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(3, fsrsAfter.lapses)
    }

    // ========== Session Complete Tests ==========

    @Test
    fun `completeSession sets completedAt`() = runTest {
        val sessionId = repository.startSession()

        val before = System.currentTimeMillis()
        repository.completeSession(sessionId)
        val after = System.currentTimeMillis()

        val session = reviewSessionDao.getById(sessionId)!!
        assertNotNull(session.completedAt)
        assertTrue(session.completedAt!! in before..after)
    }

    @Test
    fun `completeSession preserves trials`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)
        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)
        repository.completeSession(sessionId)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals(2, trials.size)
    }

    // ========== Function Card Trial Tests ==========

    @Test
    fun `recordFunctionTrialAndUpdateFsrs inserts trial`() = runTest {
        val card = setupTestFunctionCard()
        val sessionId = repository.startSession(GameType.CHORD_FUNCTION)

        repository.recordFunctionTrialAndUpdateFsrs(sessionId, card, true, ChordFunction.V)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals(1, trials.size)
    }

    @Test
    fun `recordFunctionTrialAndUpdateFsrs sets game type to CHORD_FUNCTION`() = runTest {
        val card = setupTestFunctionCard()
        val sessionId = repository.startSession(GameType.CHORD_FUNCTION)

        repository.recordFunctionTrialAndUpdateFsrs(sessionId, card, true, ChordFunction.V)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals(GameType.CHORD_FUNCTION.name, trials[0].gameType)
    }

    @Test
    fun `recordFunctionTrialAndUpdateFsrs stores answered function for wrong answers`() = runTest {
        val card = setupTestFunctionCard()  // V function
        val sessionId = repository.startSession(GameType.CHORD_FUNCTION)

        // Wrong answer - thought it was IV
        repository.recordFunctionTrialAndUpdateFsrs(sessionId, card, false, ChordFunction.IV)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertEquals("IV", trials[0].answeredFunction)
    }

    @Test
    fun `recordFunctionTrialAndUpdateFsrs stores null answeredFunction for correct answers`() = runTest {
        val card = setupTestFunctionCard()
        val sessionId = repository.startSession(GameType.CHORD_FUNCTION)

        repository.recordFunctionTrialAndUpdateFsrs(sessionId, card, true, ChordFunction.V)

        val trials = trialDao.getTrialsForSession(sessionId)
        assertNull(trials[0].answeredFunction)
    }

    @Test
    fun `recordFunctionTrialAndUpdateFsrs updates FSRS state`() = runTest {
        val card = setupTestFunctionCard()
        val sessionId = repository.startSession(GameType.CHORD_FUNCTION)

        val fsrsBefore = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(0, fsrsBefore.reviewCount)

        repository.recordFunctionTrialAndUpdateFsrs(sessionId, card, true, ChordFunction.V)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(1, fsrsAfter.reviewCount)
    }

    @Test
    fun `recordFunctionTrialAndUpdateFsrs increments lapses on wrong`() = runTest {
        val card = setupTestFunctionCard()
        val sessionId = repository.startSession(GameType.CHORD_FUNCTION)

        repository.recordFunctionTrialAndUpdateFsrs(sessionId, card, false, ChordFunction.IV)

        val fsrsAfter = fsrsStateDao.getByCardId(card.id)!!
        assertEquals(1, fsrsAfter.lapses)
    }

    // ========== Get Trials Tests ==========

    @Test
    fun `getTrialsForSession returns trials in order`() = runTest {
        val card = setupTestCard()
        val sessionId = repository.startSession()

        repository.recordTrialAndUpdateFsrs(sessionId, card, true, ChordType.MAJOR)
        Thread.sleep(10)  // Small delay to ensure different timestamps
        repository.recordTrialAndUpdateFsrs(sessionId, card, false, ChordType.MINOR)

        val trials = repository.getTrialsForSession(sessionId)

        assertEquals(2, trials.size)
        assertTrue(trials[0].timestamp < trials[1].timestamp)
    }

    // ========== Helper Methods ==========

    private val Card.id: String
        get() = "${chordType.name}_${octave}_${playbackMode.name}"

    private suspend fun setupTestCard(): Card {
        val now = System.currentTimeMillis()
        val cardId = "MAJOR_4_ARPEGGIATED"

        cardDao.insert(CardEntity(cardId, "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = cardId,
            gameType = GameType.CHORD_TYPE.name,
            dueDate = now - HOUR_MS
        ))

        return Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
    }

    private suspend fun setupTestFunctionCard(): FunctionCard {
        val now = System.currentTimeMillis()
        val cardId = "V_MAJOR_4_ARPEGGIATED"

        functionCardDao.insert(FunctionCardEntity(cardId, "V", "MAJOR", 4, "ARPEGGIATED"))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = cardId,
            gameType = GameType.CHORD_FUNCTION.name,
            dueDate = now - HOUR_MS
        ))

        return FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
    }
}
