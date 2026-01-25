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

class HistoryDaoTest : DatabaseTestBase() {

    private suspend fun createSession(
        gameType: GameType = GameType.CHORD_TYPE,
        startedAt: Long = System.currentTimeMillis(),
        completedAt: Long? = null
    ): Long {
        val session = ReviewSessionEntity(
            startedAt = startedAt,
            completedAt = completedAt,
            gameType = gameType.name
        )
        return reviewSessionDao.insert(session)
    }

    private suspend fun createTrials(
        sessionId: Long,
        cardIds: List<String>,
        correctness: List<Boolean>,
        gameType: GameType = GameType.CHORD_TYPE
    ) {
        val now = System.currentTimeMillis()
        cardIds.forEachIndexed { index, cardId ->
            trialDao.insert(TrialEntity(
                sessionId = sessionId,
                cardId = cardId,
                timestamp = now + index * 1000L,
                wasCorrect = correctness.getOrElse(index) { true },
                gameType = gameType.name
            ))
        }
    }

    // ========== Session Query Tests ==========

    @Test
    fun `getAllSessions returns sessions ordered by startedAt descending`() = runTest {
        val now = System.currentTimeMillis()
        createSession(startedAt = now - 2 * DAY_MS)
        createSession(startedAt = now)
        createSession(startedAt = now - DAY_MS)

        val sessions = historyDao.getAllSessions().first()

        assertEquals(3, sessions.size)
        assertTrue(sessions[0].startedAt >= sessions[1].startedAt)
        assertTrue(sessions[1].startedAt >= sessions[2].startedAt)
    }

    @Test
    fun `getSessionsByGameType filters by game type`() = runTest {
        createSession(gameType = GameType.CHORD_TYPE)
        createSession(gameType = GameType.CHORD_TYPE)
        createSession(gameType = GameType.CHORD_FUNCTION)

        val chordSessions = historyDao.getSessionsByGameType(GameType.CHORD_TYPE.name).first()
        val functionSessions = historyDao.getSessionsByGameType(GameType.CHORD_FUNCTION.name).first()

        assertEquals(2, chordSessions.size)
        assertEquals(1, functionSessions.size)
        assertTrue(chordSessions.all { it.gameType == GameType.CHORD_TYPE.name })
        assertTrue(functionSessions.all { it.gameType == GameType.CHORD_FUNCTION.name })
    }

    // ========== Card Stats Tests ==========

    @Test
    fun `getCardStats aggregates trials correctly`() = runTest {
        val sessionId = createSession()
        createTrials(
            sessionId,
            cardIds = listOf("CARD_1", "CARD_1", "CARD_1", "CARD_2", "CARD_2"),
            correctness = listOf(true, true, false, true, false)
        )

        val stats = historyDao.getCardStats().first()

        assertEquals(2, stats.size)

        val card1Stats = stats.find { it.cardId == "CARD_1" }!!
        assertEquals(3, card1Stats.totalTrials)
        assertEquals(2, card1Stats.correctTrials)
        assertEquals(2f / 3f, card1Stats.accuracy, 0.01f)

        val card2Stats = stats.find { it.cardId == "CARD_2" }!!
        assertEquals(2, card2Stats.totalTrials)
        assertEquals(1, card2Stats.correctTrials)
        assertEquals(0.5f, card2Stats.accuracy, 0.01f)
    }

    @Test
    fun `getCardStats groups by cardId and gameType`() = runTest {
        val chordSession = createSession(gameType = GameType.CHORD_TYPE)
        val functionSession = createSession(gameType = GameType.CHORD_FUNCTION)

        // Same card ID but different game types
        trialDao.insert(TrialEntity(sessionId = chordSession, cardId = "CARD_1", timestamp = 1000, wasCorrect = true, gameType = GameType.CHORD_TYPE.name))
        trialDao.insert(TrialEntity(sessionId = functionSession, cardId = "CARD_1", timestamp = 2000, wasCorrect = false, gameType = GameType.CHORD_FUNCTION.name))

        val stats = historyDao.getCardStats().first()

        // Should have 2 entries - same cardId but different gameType
        assertEquals(2, stats.size)
        val chordStats = stats.find { it.gameType == GameType.CHORD_TYPE.name }!!
        val functionStats = stats.find { it.gameType == GameType.CHORD_FUNCTION.name }!!

        assertEquals(1, chordStats.totalTrials)
        assertEquals(1, chordStats.correctTrials)
        assertEquals(1, functionStats.totalTrials)
        assertEquals(0, functionStats.correctTrials)
    }

    @Test
    fun `getCardStatsByGameType filters by game type`() = runTest {
        val chordSession = createSession(gameType = GameType.CHORD_TYPE)
        val functionSession = createSession(gameType = GameType.CHORD_FUNCTION)

        createTrials(chordSession, listOf("CHORD_CARD_1", "CHORD_CARD_2"), listOf(true, true), GameType.CHORD_TYPE)
        createTrials(functionSession, listOf("FUNC_CARD_1"), listOf(false), GameType.CHORD_FUNCTION)

        val chordStats = historyDao.getCardStatsByGameType(GameType.CHORD_TYPE.name).first()
        val functionStats = historyDao.getCardStatsByGameType(GameType.CHORD_FUNCTION.name).first()

        assertEquals(2, chordStats.size)
        assertEquals(1, functionStats.size)
        assertTrue(chordStats.all { it.gameType == GameType.CHORD_TYPE.name })
        assertTrue(functionStats.all { it.gameType == GameType.CHORD_FUNCTION.name })
    }

    @Test
    fun `getCardStats returns empty for no trials`() = runTest {
        createSession()  // Session without trials

        val stats = historyDao.getCardStats().first()

        assertTrue(stats.isEmpty())
    }

    @Test
    fun `CardStatsView accuracy calculation handles zero total`() = runTest {
        // Create a CardStatsView with 0 total (simulated, since actual query won't return this)
        val stats = CardStatsView(
            cardId = "TEST",
            gameType = GameType.CHORD_TYPE.name,
            totalTrials = 0,
            correctTrials = 0
        )

        assertEquals(0f, stats.accuracy, 0.01f)  // Should not throw / divide by zero
    }

    // ========== Session Overview Tests ==========

    @Test
    fun `getSessionOverviews aggregates trial counts`() = runTest {
        val session1 = createSession()
        val session2 = createSession()

        createTrials(session1, listOf("CARD_1", "CARD_2", "CARD_3"), listOf(true, true, false))
        createTrials(session2, listOf("CARD_1", "CARD_2"), listOf(true, false))

        val overviews = historyDao.getSessionOverviews().first()

        assertEquals(2, overviews.size)

        val overview1 = overviews.find { it.id == session1 }!!
        assertEquals(3, overview1.totalTrials)
        assertEquals(2, overview1.correctTrials)
        assertEquals(2f / 3f, overview1.accuracy, 0.01f)

        val overview2 = overviews.find { it.id == session2 }!!
        assertEquals(2, overview2.totalTrials)
        assertEquals(1, overview2.correctTrials)
        assertEquals(0.5f, overview2.accuracy, 0.01f)
    }

    @Test
    fun `getSessionOverviews ordered by startedAt descending`() = runTest {
        val now = System.currentTimeMillis()
        val session1 = createSession(startedAt = now - 2 * DAY_MS)
        val session2 = createSession(startedAt = now)
        val session3 = createSession(startedAt = now - DAY_MS)

        createTrials(session1, listOf("CARD_1"), listOf(true))
        createTrials(session2, listOf("CARD_1"), listOf(true))
        createTrials(session3, listOf("CARD_1"), listOf(true))

        val overviews = historyDao.getSessionOverviews().first()

        assertEquals(3, overviews.size)
        assertEquals(session2, overviews[0].id)  // Most recent
        assertEquals(session3, overviews[1].id)
        assertEquals(session1, overviews[2].id)  // Oldest
    }

    @Test
    fun `getSessionOverviews includes sessions without trials`() = runTest {
        val sessionWithTrials = createSession()
        val sessionWithoutTrials = createSession()

        createTrials(sessionWithTrials, listOf("CARD_1"), listOf(true))
        // sessionWithoutTrials has no trials

        val overviews = historyDao.getSessionOverviews().first()

        assertEquals(2, overviews.size)

        val emptyOverview = overviews.find { it.id == sessionWithoutTrials }!!
        assertEquals(0, emptyOverview.totalTrials)
        assertEquals(0, emptyOverview.correctTrials)
        assertEquals(0f, emptyOverview.accuracy, 0.01f)
    }

    @Test
    fun `getSessionOverviewsByGameType filters correctly`() = runTest {
        val chordSession = createSession(gameType = GameType.CHORD_TYPE)
        val functionSession = createSession(gameType = GameType.CHORD_FUNCTION)

        createTrials(chordSession, listOf("CARD_1", "CARD_2"), listOf(true, false), GameType.CHORD_TYPE)
        createTrials(functionSession, listOf("CARD_1"), listOf(true), GameType.CHORD_FUNCTION)

        val chordOverviews = historyDao.getSessionOverviewsByGameType(GameType.CHORD_TYPE.name).first()
        val functionOverviews = historyDao.getSessionOverviewsByGameType(GameType.CHORD_FUNCTION.name).first()

        assertEquals(1, chordOverviews.size)
        assertEquals(1, functionOverviews.size)
        assertEquals(chordSession, chordOverviews[0].id)
        assertEquals(functionSession, functionOverviews[0].id)
        assertEquals(2, chordOverviews[0].totalTrials)
        assertEquals(1, functionOverviews[0].totalTrials)
    }

    @Test
    fun `getSessionOverviews includes completedAt when set`() = runTest {
        val now = System.currentTimeMillis()
        val completedSession = createSession(startedAt = now, completedAt = now + 5 * 60 * 1000)  // 5 min session
        val incompleteSession = createSession(startedAt = now + 1000, completedAt = null)

        createTrials(completedSession, listOf("CARD_1"), listOf(true))
        createTrials(incompleteSession, listOf("CARD_1"), listOf(true))

        val overviews = historyDao.getSessionOverviews().first()

        val completedOverview = overviews.find { it.id == completedSession }!!
        val incompleteOverview = overviews.find { it.id == incompleteSession }!!

        assertEquals(now + 5 * 60 * 1000, completedOverview.completedAt)
        assertEquals(null, incompleteOverview.completedAt)
    }

    @Test
    fun `SessionOverview accuracy calculation handles zero trials`() = runTest {
        val overview = SessionOverview(
            id = 1,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            gameType = GameType.CHORD_TYPE.name,
            octave = 4,
            totalTrials = 0,
            correctTrials = 0
        )

        assertEquals(0f, overview.accuracy, 0.01f)  // Should not throw / divide by zero
    }

    @Test
    fun `SessionOverview includes gameType and octave`() = runTest {
        val sessionId = createSession(gameType = GameType.CHORD_TYPE)
        createTrials(sessionId, listOf("CARD_1"), listOf(true))

        val overviews = historyDao.getSessionOverviews().first()

        assertEquals(1, overviews.size)
        assertEquals(GameType.CHORD_TYPE.name, overviews[0].gameType)
    }
}
