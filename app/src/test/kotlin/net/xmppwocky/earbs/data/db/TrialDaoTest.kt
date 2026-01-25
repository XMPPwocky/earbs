package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialDaoTest : DatabaseTestBase() {

    private suspend fun createSession(gameType: GameType = GameType.CHORD_TYPE): Long {
        val session = ReviewSessionEntity(
            startedAt = System.currentTimeMillis(),
            gameType = gameType.name
        )
        return reviewSessionDao.insert(session)
    }

    // ========== Basic Insert/Query Tests ==========

    @Test
    fun `insert and getTrialsForSession returns trials`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = "MAJOR_4_ARPEGGIATED",
            timestamp = now,
            wasCorrect = true,
            gameType = GameType.CHORD_TYPE.name
        ))

        val trials = trialDao.getTrialsForSession(sessionId)

        assertEquals(1, trials.size)
        assertEquals("MAJOR_4_ARPEGGIATED", trials[0].cardId)
        assertTrue(trials[0].wasCorrect)
    }

    @Test
    fun `insertAll inserts multiple trials`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        val trials = listOf(
            TrialEntity(sessionId = sessionId, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name),
            TrialEntity(sessionId = sessionId, cardId = "MINOR_4_ARPEGGIATED", timestamp = now + 1000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name),
            TrialEntity(sessionId = sessionId, cardId = "SUS2_4_ARPEGGIATED", timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name)
        )
        trialDao.insertAll(trials)

        val retrieved = trialDao.getTrialsForSession(sessionId)
        assertEquals(3, retrieved.size)
    }

    @Test
    fun `getTrialsForSession returns empty list for non-existent session`() = runTest {
        val trials = trialDao.getTrialsForSession(999)
        assertTrue(trials.isEmpty())
    }

    // ========== Ordering Tests ==========

    @Test
    fun `getTrialsForSession ordered by timestamp ascending`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        // Insert in random order
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "CARD_2", timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "CARD_1", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "CARD_3", timestamp = now + 1000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        val trials = trialDao.getTrialsForSession(sessionId)

        assertEquals(3, trials.size)
        assertEquals("CARD_1", trials[0].cardId)  // Earliest
        assertEquals("CARD_3", trials[1].cardId)
        assertEquals("CARD_2", trials[2].cardId)  // Latest
    }

    // ========== Count Tests ==========

    @Test
    fun `countTrialsForCard returns correct total count`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        // Multiple trials for same card across different sessions
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now + 1000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "MINOR_4_ARPEGGIATED", timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        assertEquals(2, trialDao.countTrialsForCard("MAJOR_4_ARPEGGIATED"))
        assertEquals(1, trialDao.countTrialsForCard("MINOR_4_ARPEGGIATED"))
        assertEquals(0, trialDao.countTrialsForCard("NON_EXISTENT"))
    }

    @Test
    fun `countCorrectTrialsForCard returns only correct trials`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now + 1000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        assertEquals(2, trialDao.countCorrectTrialsForCard("MAJOR_4_ARPEGGIATED"))
    }

    // ========== Session Isolation Tests ==========

    @Test
    fun `getTrialsForSession only returns trials for specified session`() = runTest {
        val session1 = createSession()
        val session2 = createSession()
        val now = System.currentTimeMillis()

        trialDao.insert(TrialEntity(sessionId = session1, cardId = "CARD_1", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = session1, cardId = "CARD_2", timestamp = now + 1000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = session2, cardId = "CARD_3", timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        val session1Trials = trialDao.getTrialsForSession(session1)
        val session2Trials = trialDao.getTrialsForSession(session2)

        assertEquals(2, session1Trials.size)
        assertEquals(1, session2Trials.size)
        assertTrue(session1Trials.all { it.sessionId == session1 })
        assertTrue(session2Trials.all { it.sessionId == session2 })
    }

    // ========== Recent Trials Flow Tests ==========

    @Test
    fun `getRecentTrials returns trials ordered by timestamp descending`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "CARD_1", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "CARD_2", timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = "CARD_3", timestamp = now + 1000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        val recentTrials = trialDao.getRecentTrials(10).first()

        assertEquals(3, recentTrials.size)
        assertEquals("CARD_2", recentTrials[0].cardId)  // Most recent
        assertEquals("CARD_3", recentTrials[1].cardId)
        assertEquals("CARD_1", recentTrials[2].cardId)  // Oldest
    }

    @Test
    fun `getRecentTrials respects limit`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        repeat(10) { i ->
            trialDao.insert(TrialEntity(
                sessionId = sessionId,
                cardId = "CARD_$i",
                timestamp = now + i * 1000L,
                wasCorrect = true,
                gameType = GameType.CHORD_TYPE.name
            ))
        }

        val recentTrials = trialDao.getRecentTrials(5).first()

        assertEquals(5, recentTrials.size)
    }

    // ========== Game Type and Answer Tracking Tests ==========

    @Test
    fun `trial stores game type correctly`() = runTest {
        val chordSession = createSession(GameType.CHORD_TYPE)
        val functionSession = createSession(GameType.CHORD_FUNCTION)
        val now = System.currentTimeMillis()

        trialDao.insert(TrialEntity(sessionId = chordSession, cardId = "MAJOR_4_ARPEGGIATED", timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = functionSession, cardId = "V_MAJOR_4_ARPEGGIATED", timestamp = now + 1000, wasCorrect = true, gameType = GameType.CHORD_FUNCTION.name))

        val chordTrials = trialDao.getTrialsForSession(chordSession)
        val functionTrials = trialDao.getTrialsForSession(functionSession)

        assertEquals(GameType.CHORD_TYPE.name, chordTrials[0].gameType)
        assertEquals(GameType.CHORD_FUNCTION.name, functionTrials[0].gameType)
    }

    @Test
    fun `trial stores answered chord type for wrong answers`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()

        // Correct answer - no answeredChordType
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = "MAJOR_4_ARPEGGIATED",
            timestamp = now,
            wasCorrect = true,
            gameType = GameType.CHORD_TYPE.name,
            answeredChordType = null
        ))

        // Wrong answer - store what user answered
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = "MINOR_4_ARPEGGIATED",
            timestamp = now + 1000,
            wasCorrect = false,
            gameType = GameType.CHORD_TYPE.name,
            answeredChordType = "MAJOR"  // User thought it was Major
        ))

        val trials = trialDao.getTrialsForSession(sessionId)

        assertEquals(null, trials[0].answeredChordType)
        assertEquals("MAJOR", trials[1].answeredChordType)
    }

    @Test
    fun `trial stores answered function for wrong answers`() = runTest {
        val sessionId = createSession(GameType.CHORD_FUNCTION)
        val now = System.currentTimeMillis()

        // Correct answer - no answeredFunction
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = "V_MAJOR_4_ARPEGGIATED",
            timestamp = now,
            wasCorrect = true,
            gameType = GameType.CHORD_FUNCTION.name,
            answeredFunction = null
        ))

        // Wrong answer - store what user answered
        trialDao.insert(TrialEntity(
            sessionId = sessionId,
            cardId = "IV_MAJOR_4_ARPEGGIATED",
            timestamp = now + 1000,
            wasCorrect = false,
            gameType = GameType.CHORD_FUNCTION.name,
            answeredFunction = "V"  // User thought it was V
        ))

        val trials = trialDao.getTrialsForSession(sessionId)

        assertEquals(null, trials[0].answeredFunction)
        assertEquals("V", trials[1].answeredFunction)
    }

    // ========== Card Session Accuracy Tests ==========

    @Test
    fun `getCardSessionAccuracy aggregates correctly`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()
        val cardId = "MAJOR_4_ARPEGGIATED"

        // 3 trials for same card: 2 correct, 1 wrong
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = cardId, timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = cardId, timestamp = now + 1000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = cardId, timestamp = now + 2000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        val accuracy = trialDao.getCardSessionAccuracy(cardId)

        assertEquals(1, accuracy.size)
        assertEquals(sessionId, accuracy[0].sessionId)
        assertEquals(3, accuracy[0].trialsInSession)
        assertEquals(2, accuracy[0].correctInSession)
    }

    @Test
    fun `getCardSessionAccuracy handles multiple sessions`() = runTest {
        val session1 = createSession()
        val session2 = createSession()
        val now = System.currentTimeMillis()
        val cardId = "MAJOR_4_ARPEGGIATED"

        // Session 1: 2 trials, 1 correct
        trialDao.insert(TrialEntity(sessionId = session1, cardId = cardId, timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = session1, cardId = cardId, timestamp = now + 1000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))

        // Session 2: 3 trials, 3 correct
        trialDao.insert(TrialEntity(sessionId = session2, cardId = cardId, timestamp = now + 10000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = session2, cardId = cardId, timestamp = now + 11000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = session2, cardId = cardId, timestamp = now + 12000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        val accuracy = trialDao.getCardSessionAccuracy(cardId)

        assertEquals(2, accuracy.size)
        // Ordered by session startedAt
        assertEquals(session1, accuracy[0].sessionId)
        assertEquals(2, accuracy[0].trialsInSession)
        assertEquals(1, accuracy[0].correctInSession)
        assertEquals(session2, accuracy[1].sessionId)
        assertEquals(3, accuracy[1].trialsInSession)
        assertEquals(3, accuracy[1].correctInSession)
    }

    @Test
    fun `getCardSessionAccuracy returns empty for new card`() = runTest {
        val accuracy = trialDao.getCardSessionAccuracy("NON_EXISTENT_CARD")
        assertTrue(accuracy.isEmpty())
    }

    @Test
    fun `getCardSessionAccuracy only includes trials for specific card`() = runTest {
        val sessionId = createSession()
        val now = System.currentTimeMillis()
        val targetCard = "MAJOR_4_ARPEGGIATED"
        val otherCard = "MINOR_4_ARPEGGIATED"

        // Target card: 2 trials, 2 correct
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = targetCard, timestamp = now, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = targetCard, timestamp = now + 1000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))

        // Other card: 3 trials, 0 correct (should not affect target card accuracy)
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = otherCard, timestamp = now + 2000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = otherCard, timestamp = now + 3000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = sessionId, cardId = otherCard, timestamp = now + 4000, wasCorrect = false, gameType = GameType.CHORD_TYPE.name))

        val accuracy = trialDao.getCardSessionAccuracy(targetCard)

        assertEquals(1, accuracy.size)
        assertEquals(2, accuracy[0].trialsInSession)
        assertEquals(2, accuracy[0].correctInSession)
    }
}
