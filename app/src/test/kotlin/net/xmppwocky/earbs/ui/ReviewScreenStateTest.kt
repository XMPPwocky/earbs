package net.xmppwocky.earbs.ui

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ReviewSession
import org.junit.Assert.*
import org.junit.Test

class ReviewScreenStateTest {

    @Test
    fun `trialNumber does not exceed totalTrials after last answer`() {
        val cards = listOf(
            Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED)
        )
        val session = ReviewSession(cards)

        // Answer both trials
        session.recordAnswer(true)
        session.recordAnswer(true)

        // currentTrial is now 2, but totalTrials is 2
        val state = ReviewScreenState(session = session)

        // trialNumber should be capped at totalTrials (2), not currentTrial + 1 (3)
        assertEquals(2, state.trialNumber)
        assertEquals(2, state.totalTrials)
    }

    @Test
    fun `trialNumber is correct during normal progression`() {
        val cards = listOf(
            Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED),
            Card(ChordType.SUS2, 4, PlaybackMode.ARPEGGIATED)
        )
        val session = ReviewSession(cards)

        // Before any answers, trial 1 of 3
        val state1 = ReviewScreenState(session = session)
        assertEquals(1, state1.trialNumber)
        assertEquals(3, state1.totalTrials)

        // After first answer, trial 2 of 3
        session.recordAnswer(true)
        val state2 = ReviewScreenState(session = session)
        assertEquals(2, state2.trialNumber)

        // After second answer, trial 3 of 3
        session.recordAnswer(false)
        val state3 = ReviewScreenState(session = session)
        assertEquals(3, state3.trialNumber)
    }
}
