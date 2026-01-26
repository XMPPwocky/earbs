package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for GenericReviewSession using Card (chord type game).
 */
class ReviewSessionTest {

    private lateinit var testCards: List<Card>
    private lateinit var session: GenericReviewSession<Card>

    @Before
    fun setUp() {
        testCards = listOf(
            Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.SUS2, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.SUS4, 4, PlaybackMode.ARPEGGIATED)
        )
        session = GenericReviewSession(testCards, "chord type")
    }

    // ========== Initialization tests ==========

    @Test
    fun `session initializes with correct total trials`() {
        assertEquals(4, session.totalTrials)
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

        session.recordAnswer(true)
        assertEquals(testCards[3], session.getCurrentCard())
    }

    @Test
    fun `getCurrentCard returns null after session complete`() {
        repeat(4) { session.recordAnswer(true) }
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
    fun `multiple correct answers increment count properly`() {
        session.recordAnswer(true)
        session.recordAnswer(true)
        session.recordAnswer(false)
        session.recordAnswer(true)

        assertEquals(3, session.correctCount)
        assertEquals(4, session.currentTrial)
    }

    @Test
    fun `recordAnswer after session complete does nothing`() {
        repeat(4) { session.recordAnswer(true) }
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

        session.recordAnswer(true)
        assertFalse(session.isComplete())
    }

    @Test
    fun `isComplete returns true after all trials`() {
        repeat(4) { session.recordAnswer(true) }
        assertTrue(session.isComplete())
    }

    @Test
    fun `isComplete returns true regardless of correct or wrong answers`() {
        repeat(4) { session.recordAnswer(false) }
        assertTrue(session.isComplete())
    }

    // ========== getChordTypes via GameTypeConfig tests ==========

    @Test
    fun `GameTypeConfig getAnswerOptions returns distinct types only`() {
        val card = testCards[0]
        val answers = GameTypeConfig.ChordTypeGame.getAnswerOptions(card, session)
        assertEquals(4, answers.size)
        assertEquals(answers.toSet().size, answers.size) // All unique
    }

    @Test
    fun `GameTypeConfig getAnswerOptions returns correct types`() {
        val card = testCards[0]
        val answers = GameTypeConfig.ChordTypeGame.getAnswerOptions(card, session)
        val types = answers.map { it.chordType }
        assertTrue(types.contains(ChordType.MAJOR))
        assertTrue(types.contains(ChordType.MINOR))
        assertTrue(types.contains(ChordType.SUS2))
        assertTrue(types.contains(ChordType.SUS4))
    }

    @Test
    fun `GameTypeConfig getAnswerOptions with duplicate types returns distinct`() {
        val duplicateCards = listOf(
            Card(ChordType.MAJOR, 3, PlaybackMode.ARPEGGIATED),
            Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.MAJOR, 5, PlaybackMode.BLOCK)
        )
        val duplicateSession = GenericReviewSession(duplicateCards, "chord type")

        // ChordTypeGame uses session-based strategy, so card parameter doesn't affect result
        val card = duplicateCards[0]
        val answers = GameTypeConfig.ChordTypeGame.getAnswerOptions(card, duplicateSession)
        assertEquals(2, answers.size)
        val types = answers.map { it.chordType }
        assertTrue(types.contains(ChordType.MAJOR))
        assertTrue(types.contains(ChordType.MINOR))
    }

    // ========== Empty session tests ==========

    @Test
    fun `empty session is immediately complete`() {
        val emptySession = GenericReviewSession<Card>(emptyList(), "chord type")
        assertTrue(emptySession.isComplete())
        assertEquals(0, emptySession.totalTrials)
        assertNull(emptySession.getCurrentCard())
    }

    // ========== Single card session tests ==========

    @Test
    fun `single card session works correctly`() {
        val singleSession = GenericReviewSession(listOf(testCards[0]), "chord type")

        assertEquals(1, singleSession.totalTrials)
        assertFalse(singleSession.isComplete())
        assertEquals(testCards[0], singleSession.getCurrentCard())

        singleSession.recordAnswer(true)

        assertTrue(singleSession.isComplete())
        assertEquals(1, singleSession.correctCount)
        assertNull(singleSession.getCurrentCard())
    }
}
