package net.xmppwocky.earbs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.FunctionReviewSession
import net.xmppwocky.earbs.model.KeyQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionReviewScreenTest : ComposeTestBase() {

    private fun createTestSession(
        functions: List<ChordFunction> = listOf(ChordFunction.IV, ChordFunction.V, ChordFunction.vi),
        keyQuality: KeyQuality = KeyQuality.MAJOR
    ): FunctionReviewSession {
        val cards = functions.map { FunctionCard(it, keyQuality, 4, PlaybackMode.ARPEGGIATED) }
        return FunctionReviewSession(cards)
    }

    private fun createTestState(
        session: FunctionReviewSession = createTestSession(),
        currentCard: FunctionCard? = session.getCurrentCard(),
        isPlaying: Boolean = false,
        hasPlayedThisTrial: Boolean = false,
        showingFeedback: Boolean = false,
        lastAnswer: FunctionAnswerResult? = null
    ): FunctionReviewScreenState {
        return FunctionReviewScreenState(
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
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Trial 1 / 3").assertIsDisplayed()
    }

    // ========== Card Info Tests ==========

    @Test
    fun displaysMajorKeyIndicator() {
        val session = createTestSession(keyQuality = KeyQuality.MAJOR)
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(session = session),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("major key").assertIsDisplayed()
    }

    @Test
    fun displaysMinorKeyIndicator() {
        val session = createTestSession(
            functions = listOf(ChordFunction.iv, ChordFunction.v, ChordFunction.VI),
            keyQuality = KeyQuality.MINOR
        )
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(session = session),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("minor key").assertIsDisplayed()
    }

    @Test
    fun displaysOctaveAndKeyInfo() {
        val card = FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = FunctionReviewSession(listOf(card))

        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(session = session, currentCard = card),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Identify the chord function in major key (octave 4)").assertIsDisplayed()
    }

    @Test
    fun displaysArpeggiatedMode() {
        val card = FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.ARPEGGIATED)
        val session = FunctionReviewSession(listOf(card))

        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(session = session, currentCard = card),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Arpeggiated").assertIsDisplayed()
    }

    @Test
    fun displaysBlockMode() {
        val card = FunctionCard(ChordFunction.V, KeyQuality.MAJOR, 4, PlaybackMode.BLOCK)
        val session = FunctionReviewSession(listOf(card))

        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(session = session, currentCard = card),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Block").assertIsDisplayed()
    }

    // ========== Play Button Tests ==========

    @Test
    fun displaysPlayButton_initially() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = false),
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
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Replay").assertIsDisplayed()
    }

    @Test
    fun playButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(),
                onPlayClicked = { clicked = true },
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Play").performClick()

        assertTrue(clicked)
    }

    // ========== Function Answer Button Tests ==========

    @Test
    fun displaysFunctionButtons_majorKey() {
        val session = createTestSession(keyQuality = KeyQuality.MAJOR)
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(session = session, hasPlayedThisTrial = true),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        // Major key functions: ii, iii, IV, V, vi, vii dim
        composeTestRule.onNodeWithText("ii").assertIsDisplayed()
        composeTestRule.onNodeWithText("iii").assertIsDisplayed()
        composeTestRule.onNodeWithText("IV").assertIsDisplayed()
        composeTestRule.onNodeWithText("V").assertIsDisplayed()
        composeTestRule.onNodeWithText("vi").assertIsDisplayed()
    }

    @Test
    fun answerButtons_disabledInitially() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("IV").assertIsNotEnabled()
    }

    @Test
    fun answerButtons_enabledAfterPlaying() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, isPlaying = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("IV").assertIsEnabled()
    }

    @Test
    fun answerButton_triggersCallback() {
        var answeredFunction: ChordFunction? = null
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = true),
                onPlayClicked = {},
                onAnswerClicked = { answeredFunction = it },
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("V").performClick()

        assertEquals(ChordFunction.V, answeredFunction)
    }

    // ========== Feedback Tests ==========

    @Test
    fun displaysInitialPrompt_beforePlaying() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = false),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Tap Play to hear: tonic, then target chord").assertIsDisplayed()
    }

    @Test
    fun displaysQuestionPrompt_afterPlaying() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(hasPlayedThisTrial = true, lastAnswer = null),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("What function is the second chord?").assertIsDisplayed()
    }

    @Test
    fun correctAnswer_showsFeedback() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(
                    hasPlayedThisTrial = true,
                    lastAnswer = FunctionAnswerResult.Correct
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
    fun wrongAnswer_showsActualFunction() {
        composeTestRule.setContent {
            FunctionReviewScreen(
                state = createTestState(
                    hasPlayedThisTrial = true,
                    lastAnswer = FunctionAnswerResult.Wrong(ChordFunction.IV)
                ),
                onPlayClicked = {},
                onAnswerClicked = {},
                onTrialComplete = {},
                onSessionComplete = {}
            )
        }

        composeTestRule.onNodeWithText("Wrong - it was IV").assertIsDisplayed()
    }
}
