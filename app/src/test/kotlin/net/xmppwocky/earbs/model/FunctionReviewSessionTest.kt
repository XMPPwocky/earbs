package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FunctionReviewSessionTest {

    private lateinit var testCards: List<FunctionCard>
    private lateinit var session: FunctionReviewSession

    @Before
    fun setUp() {
        testCards = listOf(
            FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.vi, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        )
        session = FunctionReviewSession(testCards)
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

    // ========== getKeyQuality tests ==========

    @Test
    fun `getKeyQuality returns major for major session`() {
        assertEquals(KeyQuality.MAJOR, session.getKeyQuality())
    }

    @Test
    fun `getKeyQuality returns minor for minor session`() {
        val minorCards = listOf(
            FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.v, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)
        )
        val minorSession = FunctionReviewSession(minorCards)
        assertEquals(KeyQuality.MINOR, minorSession.getKeyQuality())
    }

    @Test
    fun `getKeyQuality returns null for empty session`() {
        val emptySession = FunctionReviewSession(emptyList())
        assertNull(emptySession.getKeyQuality())
    }

    // ========== getFunctions tests ==========

    @Test
    fun `getFunctions returns distinct functions`() {
        val functions = session.getFunctions()
        assertEquals(3, functions.size)
        assertTrue(functions.contains(ChordFunction.IV))
        assertTrue(functions.contains(ChordFunction.V))
        assertTrue(functions.contains(ChordFunction.vi))
    }

    @Test
    fun `getFunctions with duplicates returns distinct`() {
        val duplicateCards = listOf(
            FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 3, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.IV, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        )
        val duplicateSession = FunctionReviewSession(duplicateCards)

        val functions = duplicateSession.getFunctions()
        assertEquals(2, functions.size)
        assertTrue(functions.contains(ChordFunction.IV))
        assertTrue(functions.contains(ChordFunction.V))
    }

    // ========== getAllFunctionsForKey tests ==========

    @Test
    fun `getAllFunctionsForKey returns 6 functions for major key`() {
        val allFunctions = session.getAllFunctionsForKey()
        assertEquals(6, allFunctions.size)
    }

    @Test
    fun `getAllFunctionsForKey returns correct major key functions`() {
        val allFunctions = session.getAllFunctionsForKey()

        assertTrue(allFunctions.contains(ChordFunction.ii))
        assertTrue(allFunctions.contains(ChordFunction.iii))
        assertTrue(allFunctions.contains(ChordFunction.IV))
        assertTrue(allFunctions.contains(ChordFunction.V))
        assertTrue(allFunctions.contains(ChordFunction.vi))
        assertTrue(allFunctions.contains(ChordFunction.vii_dim))
    }

    @Test
    fun `getAllFunctionsForKey returns correct minor key functions`() {
        val minorCards = listOf(
            FunctionCard(ChordFunction.iv, KeyQuality.MINOR, 4, PlaybackMode.ARPEGGIATED)
        )
        val minorSession = FunctionReviewSession(minorCards)

        val allFunctions = minorSession.getAllFunctionsForKey()
        assertEquals(6, allFunctions.size)

        assertTrue(allFunctions.contains(ChordFunction.ii_dim))
        assertTrue(allFunctions.contains(ChordFunction.III))
        assertTrue(allFunctions.contains(ChordFunction.iv))
        assertTrue(allFunctions.contains(ChordFunction.v))
        assertTrue(allFunctions.contains(ChordFunction.VI))
        assertTrue(allFunctions.contains(ChordFunction.VII))
    }

    @Test
    fun `getAllFunctionsForKey returns empty for empty session`() {
        val emptySession = FunctionReviewSession(emptyList())
        assertTrue(emptySession.getAllFunctionsForKey().isEmpty())
    }

    // ========== Empty session tests ==========

    @Test
    fun `empty session is immediately complete`() {
        val emptySession = FunctionReviewSession(emptyList())
        assertTrue(emptySession.isComplete())
        assertEquals(0, emptySession.totalTrials)
        assertNull(emptySession.getCurrentCard())
    }

    // ========== Single card session tests ==========

    @Test
    fun `single card session works correctly`() {
        val singleSession = FunctionReviewSession(listOf(testCards[0]))

        assertEquals(1, singleSession.totalTrials)
        assertFalse(singleSession.isComplete())
        assertEquals(testCards[0], singleSession.getCurrentCard())

        singleSession.recordAnswer(true)

        assertTrue(singleSession.isComplete())
        assertEquals(1, singleSession.correctCount)
        assertNull(singleSession.getCurrentCard())
    }
}
