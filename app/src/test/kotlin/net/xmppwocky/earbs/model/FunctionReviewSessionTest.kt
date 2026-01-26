package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for GenericReviewSession using FunctionCard (chord function game).
 */
class FunctionReviewSessionTest {

    private lateinit var testCards: List<FunctionCard>
    private lateinit var session: GenericReviewSession<FunctionCard>

    @Before
    fun setUp() {
        testCards = listOf(
            FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.vi, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        )
        session = GenericReviewSession(testCards, "function")
    }

    // ========== Initialization tests ==========

    @Test
    fun `session initializes with correct total trials`() {
        assertEquals(3, session.totalTrials)
    }

    @Test
    fun `session initializes with zero current trial`() {
        assertEquals(0, session.currentTrial)
    }

    @Test
    fun `session initializes with zero correct count`() {
        assertEquals(0, session.correctCount)
    }

    @Test
    fun `session stores cards correctly`() {
        assertEquals(testCards, session.cards)
    }

    // ========== getCurrentCard tests ==========

    @Test
    fun `getCurrentCard returns first card at start`() {
        assertEquals(testCards[0], session.getCurrentCard())
    }

    @Test
    fun `getCurrentCard returns correct card at each trial`() {
        assertEquals(testCards[0], session.getCurrentCard())

        session.recordAnswer(true)
        assertEquals(testCards[1], session.getCurrentCard())

        session.recordAnswer(false)
        assertEquals(testCards[2], session.getCurrentCard())
    }

    @Test
    fun `getCurrentCard returns null after session complete`() {
        repeat(3) { session.recordAnswer(true) }
        assertNull(session.getCurrentCard())
    }

    // ========== recordAnswer tests ==========

    @Test
    fun `recordAnswer true increments correct count`() {
        session.recordAnswer(true)
        assertEquals(1, session.correctCount)
    }

    @Test
    fun `recordAnswer false does not increment correct count`() {
        session.recordAnswer(false)
        assertEquals(0, session.correctCount)
    }

    @Test
    fun `recordAnswer increments current trial`() {
        assertEquals(0, session.currentTrial)
        session.recordAnswer(true)
        assertEquals(1, session.currentTrial)
    }

    @Test
    fun `multiple answers track correctly`() {
        session.recordAnswer(true)
        session.recordAnswer(false)
        session.recordAnswer(true)

        assertEquals(2, session.correctCount)
        assertEquals(3, session.currentTrial)
    }

    @Test
    fun `recordAnswer after session complete does nothing`() {
        repeat(3) { session.recordAnswer(true) }
        val countBefore = session.correctCount
        val trialBefore = session.currentTrial

        session.recordAnswer(true)

        assertEquals(countBefore, session.correctCount)
        assertEquals(trialBefore, session.currentTrial)
    }

    // ========== isComplete tests ==========

    @Test
    fun `isComplete returns false at start`() {
        assertFalse(session.isComplete())
    }

    @Test
    fun `isComplete returns false during session`() {
        session.recordAnswer(true)
        assertFalse(session.isComplete())

        session.recordAnswer(false)
        assertFalse(session.isComplete())
    }

    @Test
    fun `isComplete returns true after all trials`() {
        repeat(3) { session.recordAnswer(true) }
        assertTrue(session.isComplete())
    }

    // ========== getKeyQuality tests (accessing card directly) ==========

    @Test
    fun `keyQuality returns major for major session`() {
        assertEquals(KeyQuality.MAJOR, session.cards.firstOrNull()?.keyQuality)
    }

    @Test
    fun `keyQuality returns minor for minor session`() {
        val minorCards = listOf(
            FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.v, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)
        )
        val minorSession = GenericReviewSession(minorCards, "function")
        assertEquals(KeyQuality.MINOR, minorSession.cards.firstOrNull()?.keyQuality)
    }

    @Test
    fun `keyQuality returns null for empty session`() {
        val emptySession = GenericReviewSession<FunctionCard>(emptyList(), "function")
        assertNull(emptySession.cards.firstOrNull()?.keyQuality)
    }

    // ========== GameTypeConfig getAnswerOptions tests (replaces getFunctions/getAllFunctionsForKey) ==========

    @Test
    fun `GameTypeConfig getAnswerOptions returns 6 functions for major key`() {
        val card = testCards[0]  // MAJOR key card
        val answers = GameTypeConfig.FunctionGame.getAnswerOptions(card, session)
        assertEquals(6, answers.size)
    }

    @Test
    fun `GameTypeConfig getAnswerOptions returns correct major key functions`() {
        val card = testCards[0]  // MAJOR key card
        val answers = GameTypeConfig.FunctionGame.getAnswerOptions(card, session)
        val functions = answers.map { it.function }

        assertTrue(functions.contains(ChordFunction.ii))
        assertTrue(functions.contains(ChordFunction.iii))
        assertTrue(functions.contains(ChordFunction.IV))
        assertTrue(functions.contains(ChordFunction.V))
        assertTrue(functions.contains(ChordFunction.vi))
        assertTrue(functions.contains(ChordFunction.vii_dim))
    }

    @Test
    fun `GameTypeConfig getAnswerOptions returns correct minor key functions`() {
        val minorCard = FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)
        val minorCards = listOf(minorCard)
        val minorSession = GenericReviewSession(minorCards, "function")

        val answers = GameTypeConfig.FunctionGame.getAnswerOptions(minorCard, minorSession)
        assertEquals(6, answers.size)
        val functions = answers.map { it.function }

        assertTrue(functions.contains(ChordFunction.ii_dim))
        assertTrue(functions.contains(ChordFunction.III))
        assertTrue(functions.contains(ChordFunction.iv))
        assertTrue(functions.contains(ChordFunction.v))
        assertTrue(functions.contains(ChordFunction.VI))
        assertTrue(functions.contains(ChordFunction.VII))
    }

    @Test
    fun `GameTypeConfig getAnswerOptions uses current card key quality not first card`() {
        // Create mixed session: MAJOR card first, MINOR card second
        val majorCard = FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val minorCard = FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)
        val mixedSession = GenericReviewSession(listOf(majorCard, minorCard), "function")

        // When asking for answer options for the MINOR card, should get MINOR functions
        val answers = GameTypeConfig.FunctionGame.getAnswerOptions(minorCard, mixedSession)
        val functions = answers.map { it.function }

        // Should contain MINOR key functions (iv, v, etc.) not MAJOR (IV, V, etc.)
        assertTrue(functions.contains(ChordFunction.iv))
        assertTrue(functions.contains(ChordFunction.v))
        assertFalse(functions.contains(ChordFunction.IV))
        assertFalse(functions.contains(ChordFunction.V))
    }

    // ========== Empty session tests ==========

    @Test
    fun `empty session is immediately complete`() {
        val emptySession = GenericReviewSession<FunctionCard>(emptyList(), "function")
        assertTrue(emptySession.isComplete())
        assertEquals(0, emptySession.totalTrials)
        assertNull(emptySession.getCurrentCard())
    }

    // ========== Single card session tests ==========

    @Test
    fun `single card session works correctly`() {
        val singleSession = GenericReviewSession(listOf(testCards[0]), "function")

        assertEquals(1, singleSession.totalTrials)
        assertFalse(singleSession.isComplete())
        assertEquals(testCards[0], singleSession.getCurrentCard())

        singleSession.recordAnswer(true)

        assertTrue(singleSession.isComplete())
        assertEquals(1, singleSession.correctCount)
        assertNull(singleSession.getCurrentCard())
    }
}
