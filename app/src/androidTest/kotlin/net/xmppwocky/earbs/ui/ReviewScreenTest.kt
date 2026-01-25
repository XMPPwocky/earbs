package net.xmppwocky.earbs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ReviewSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewScreenTest : ComposeTestBase() {

    private fun createTestSession(chordTypes: List<ChordType> = listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4)): ReviewSession {
        val cards = chordTypes.map { Card(it, 4, PlaybackMode.ARPEGGIATED) }
        return ReviewSession(cards)
    }

    private fun createTestState(
        session: ReviewSession = createTestSession(),
        currentCard: Card? = session.getCurrentCard(),
        isPlaying: Boolean = false,
        hasPlayedThisTrial: Boolean = false,
        showingFeedback: Boolean = false,
        lastAnswer: AnswerResult? = null
    ): ReviewScreenState {
        return ReviewScreenState(
            session = session,
            currentCard = currentCard,
            currentRootSemitones = 0,
            lastAnswer = lastAnswer,
            isPlaying = isPlaying,
            hasPlayedThisTrial = hasPlayedThisTrial,
            showingFeedback = showingFeedback
        )
    }

    // ========== Progress Indicator Tests ==========

    @Test
    fun displaysProgressIndicator() {
        val session = createTestSession()
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Trial 1 / 4").assertIsDisplayed()
    }

    @Test
    fun progressIndicator_updatesWithTrial() {
        val session = createTestSession()
        session.recordAnswer(true)  // Advance to trial 2

        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Trial 2 / 4").assertIsDisplayed()
    }

    // ========== Card Info Tests ==========

    @Test
    fun displaysOctaveInfo() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card))

        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session, currentCard = card),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Chord in octave 4").assertIsDisplayed()
    }

    @Test
    fun displaysArpeggiatedMode() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card))

        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session, currentCard = card),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Arpeggiated Mode").assertIsDisplayed()
    }

    @Test
    fun displaysBlockMode() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.BLOCK)
        val session = ReviewSession(listOf(card))

        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session, currentCard = card),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Block Mode").assertIsDisplayed()
    }

    // ========== Play Button Tests ==========

    @Test
    fun displaysPlayButton_initially() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = false, isPlaying = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Play").assertIsDisplayed()
    }

    @Test
    fun displaysReplayButton_afterPlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, isPlaying = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Replay").assertIsDisplayed()
    }

    @Test
    fun displaysPlayingText_whilePlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(isPlaying = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Playing...").assertIsDisplayed()
    }

    @Test
    fun playButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(isPlaying = false),
                onPlayClicked = { clicked = true },
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Play").performClick()

        assertTrue(clicked)
    }

    @Test
    fun playButton_disabled_whilePlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(isPlaying = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Playing...").assertIsNotEnabled()
    }

    @Test
    fun playButton_disabled_duringFeedback() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(showingFeedback = true, hasPlayedThisTrial = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Replay").assertIsNotEnabled()
    }

    // ========== Answer Button Tests ==========

    @Test
    fun displaysChordTypeButtons() {
        val chordTypes = listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4)
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session, hasPlayedThisTrial = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Major").assertIsDisplayed()
        composeTestRule.onNodeWithText("Minor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sus2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sus4").assertIsDisplayed()
    }

    @Test
    fun answerButtons_disabledInitially() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Major").assertIsNotEnabled()
    }

    @Test
    fun answerButtons_enabledAfterPlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, isPlaying = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Major").assertIsEnabled()
    }

    @Test
    fun answerButtons_disabled_whilePlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, isPlaying = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Major").assertIsNotEnabled()
    }

    @Test
    fun answerButtons_disabled_duringFeedback() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, showingFeedback = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Major").assertIsNotEnabled()
    }

    @Test
    fun answerButton_triggersCallback() {
        var answeredType: ChordType? = null
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, isPlaying = false),
                onPlayClicked = {},
                onAnswerClicked = { answeredType = it },
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Major").performClick()

        assertEquals(ChordType.MAJOR, answeredType)
    }

    // ========== Feedback Tests ==========

    @Test
    fun displaysInitialPrompt_beforePlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Tap Play to hear the chord").assertIsDisplayed()
    }

    @Test
    fun displaysQuestionPrompt_afterPlaying() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, lastAnswer = null),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("What chord type is this?").assertIsDisplayed()
    }

    @Test
    fun correctAnswer_showsFeedback() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(
                    hasPlayedThisTrial = true,
                    lastAnswer = AnswerResult.Correct
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Correct!").assertIsDisplayed()
    }

    @Test
    fun wrongAnswer_showsActualType() {
        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(
                    hasPlayedThisTrial = true,
                    lastAnswer = AnswerResult.Wrong(ChordType.MINOR, ChordType.MAJOR)
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Wrong - it was Minor").assertIsDisplayed()
    }

    // ========== Seventh Chord Tests ==========

    @Test
    fun displaysSeventhChordButtons() {
        val chordTypes = listOf(
            ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4,
            ChordType.DOM7, ChordType.MAJ7, ChordType.MIN7, ChordType.DIM7
        )
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = createTestState(session = session, hasPlayedThisTrial = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Dom7").assertIsDisplayed()
        composeTestRule.onNodeWithText("Maj7").assertIsDisplayed()
        composeTestRule.onNodeWithText("Min7").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dim7").assertIsDisplayed()
    }

    // ========== Learning Mode Tests ==========

    @Test
    fun learningMode_showsNextButton() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card))

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = card,
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(ChordType.MAJOR, ChordType.MINOR),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = true
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {},
                onNextClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun learningMode_nextButton_triggersCallback() {
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card))
        var nextClicked = false

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = card,
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(ChordType.MAJOR, ChordType.MINOR),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = true
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {},
                onNextClicked = { nextClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Next").performClick()

        assertTrue(nextClicked)
    }

    @Test
    fun learningMode_lastTrial_nextButton_callsSessionComplete() {
        // Create a single-card session
        val card = Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card))

        // Record wrong answer - session is now complete
        session.recordAnswer(false)
        assertTrue("Session should be complete after recording answer", session.isComplete())

        var sessionCompleted = false
        var stuckOnLoading = false

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = card,  // Still showing the card for learning
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(ChordType.MAJOR, ChordType.MINOR),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = true
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = { sessionCompleted = true },
                onNextClicked = {
                    // Simulate MainActivity's onNextClicked logic
                    if (session.isComplete()) {
                        sessionCompleted = true
                    } else {
                        // Would advance to next card - but if session is complete,
                        // getCurrentCard() returns null and we'd be stuck
                        val nextCard = session.getCurrentCard()
                        if (nextCard == null) {
                            stuckOnLoading = true
                        }
                    }
                }
            )
        }

        composeTestRule.onNodeWithText("Next").performClick()

        assertTrue("Session should be marked complete", sessionCompleted)
        assertTrue("Should not be stuck on loading", !stuckOnLoading)
    }

    // ========== Button Color Feedback Tests ==========

    @Test
    fun wrongAnswer_selectedButton_showsWrongColor() {
        // User selected Major but correct was Minor
        val card = Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card, Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)))

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = card,
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(
                        actualType = ChordType.MINOR,
                        selectedType = ChordType.MAJOR
                    ),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = false
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // Selected button (Major) should have WRONG color state
        composeTestRule.onNodeWithTag("answer_button_MAJOR_WRONG").assertIsDisplayed()
    }

    @Test
    fun wrongAnswer_correctButton_showsCorrectColor() {
        // User selected Major but correct was Minor
        val card = Card(ChordType.MINOR, 4, PlaybackMode.ARPEGGIATED)
        val session = ReviewSession(listOf(card, Card(ChordType.MAJOR, 4, PlaybackMode.ARPEGGIATED)))

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = card,
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(
                        actualType = ChordType.MINOR,
                        selectedType = ChordType.MAJOR
                    ),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = false
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // Correct button (Minor) should have CORRECT color state
        composeTestRule.onNodeWithTag("answer_button_MINOR_CORRECT").assertIsDisplayed()
    }

    @Test
    fun wrongAnswer_otherButtons_showInactiveColor() {
        // User selected Major but correct was Minor, Sus2 and Sus4 should be inactive
        val chordTypes = listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4)
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = session.getCurrentCard(),
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(
                        actualType = ChordType.MINOR,
                        selectedType = ChordType.MAJOR
                    ),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = false
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // Other buttons (Sus2, Sus4) should have INACTIVE color state
        composeTestRule.onNodeWithTag("answer_button_SUS2_INACTIVE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS4_INACTIVE").assertIsDisplayed()
    }

    @Test
    fun correctAnswer_allButtons_showDefaultColor() {
        val chordTypes = listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4)
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = session.getCurrentCard(),
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Correct,
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = false
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // All buttons should have DEFAULT color state
        composeTestRule.onNodeWithTag("answer_button_MAJOR_DEFAULT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_MINOR_DEFAULT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS2_DEFAULT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS4_DEFAULT").assertIsDisplayed()
    }

    @Test
    fun beforeAnswering_allButtons_showDefaultColor() {
        val chordTypes = listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4)
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = session.getCurrentCard(),
                    currentRootSemitones = 0,
                    lastAnswer = null,
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = false,
                    inLearningMode = false
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // All buttons should have DEFAULT color state before answering
        composeTestRule.onNodeWithTag("answer_button_MAJOR_DEFAULT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_MINOR_DEFAULT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS2_DEFAULT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS4_DEFAULT").assertIsDisplayed()
    }

    @Test
    fun wrongAnswer_withSeventhChords_showsCorrectColors() {
        // Test with 7th chords: user selected Dom7 but correct was Maj7
        val chordTypes = listOf(
            ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4,
            ChordType.DOM7, ChordType.MAJ7, ChordType.MIN7, ChordType.DIM7
        )
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = session.getCurrentCard(),
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(
                        actualType = ChordType.MAJ7,
                        selectedType = ChordType.DOM7
                    ),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = false
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // Selected button (Dom7) should be WRONG
        composeTestRule.onNodeWithTag("answer_button_DOM7_WRONG").assertIsDisplayed()
        // Correct button (Maj7) should be CORRECT
        composeTestRule.onNodeWithTag("answer_button_MAJ7_CORRECT").assertIsDisplayed()
        // Other buttons should be INACTIVE
        composeTestRule.onNodeWithTag("answer_button_MAJOR_INACTIVE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_MIN7_INACTIVE").assertIsDisplayed()
    }

    @Test
    fun learningMode_afterWrongAnswer_showsColorFeedback() {
        // In learning mode after wrong answer, colors should still show
        val chordTypes = listOf(ChordType.MAJOR, ChordType.MINOR, ChordType.SUS2, ChordType.SUS4)
        val session = createTestSession(chordTypes)

        composeTestRule.setContent {
            ReviewScreen(
                state = ReviewScreenState(
                    session = session,
                    currentCard = session.getCurrentCard(),
                    currentRootSemitones = 0,
                    lastAnswer = AnswerResult.Wrong(
                        actualType = ChordType.MINOR,
                        selectedType = ChordType.MAJOR
                    ),
                    isPlaying = false,
                    hasPlayedThisTrial = true,
                    showingFeedback = true,
                    inLearningMode = true
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {},
                onNextClicked = {}
            )
        }

        // Colors should be applied even in learning mode
        composeTestRule.onNodeWithTag("answer_button_MAJOR_WRONG").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_MINOR_CORRECT").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS2_INACTIVE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_button_SUS4_INACTIVE").assertIsDisplayed()
    }
}
