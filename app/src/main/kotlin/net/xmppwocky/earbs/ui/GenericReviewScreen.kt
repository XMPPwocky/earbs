package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameCard
import net.xmppwocky.earbs.model.GenericReviewSession
import net.xmppwocky.earbs.ui.components.AbortSessionDialog
import net.xmppwocky.earbs.ui.components.ReviewPlayButton
import net.xmppwocky.earbs.ui.components.ReviewProgressIndicator
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

private const val TAG = "GenericReviewScreen"

/**
 * Generic answer result that works for any game type.
 */
sealed class GenericAnswerResult<A : GameAnswer> {
    abstract val isCorrect: Boolean

    data class Correct<A : GameAnswer>(override val isCorrect: Boolean = true) : GenericAnswerResult<A>()
    data class Wrong<A : GameAnswer>(
        val actualAnswer: A,
        val selectedAnswer: A,
        override val isCorrect: Boolean = false
    ) : GenericAnswerResult<A>()
}

/**
 * Generic state for any review screen.
 *
 * @param C The card type (must implement GameCard)
 * @param A The answer type (must implement GameAnswer)
 */
data class GenericReviewScreenState<C : GameCard, A : GameAnswer>(
    val session: GenericReviewSession<C>,
    val currentCard: C? = null,
    val currentRootSemitones: Int? = null,
    val lastAnswer: GenericAnswerResult<A>? = null,
    val isPlaying: Boolean = false,
    val hasPlayedThisTrial: Boolean = false,
    val showingFeedback: Boolean = false,
    val inLearningMode: Boolean = false
) {
    val trialNumber: Int get() = minOf(session.currentTrial + 1, session.totalTrials)
    val totalTrials: Int get() = session.totalTrials
    val isComplete: Boolean get() = session.isComplete()
    val playbackMode: PlaybackMode get() = currentCard?.playbackMode ?: PlaybackMode.ARPEGGIATED
}

/**
 * Generic review screen that works with any game type.
 * Uses composable slots for game-specific rendering (card info, mode indicators, answer buttons).
 *
 * @param C The card type
 * @param A The answer type
 * @param state The review screen state
 * @param cardInfoContent Composable slot for displaying card-specific info
 * @param modeIndicatorContent Composable slot for mode indicators (playback mode, key quality, etc.)
 * @param feedbackContent Composable slot for feedback text
 * @param answerButtonsContent Composable slot for answer buttons
 * @param autoAdvanceDelayMs Delay before auto-advancing to next trial
 * @param onPlayClicked Called when play button is clicked
 * @param onTrialComplete Called when a trial is complete
 * @param onAutoPlay Called after trial complete to auto-play next card
 * @param onSessionComplete Called when session is complete
 * @param onNextClicked Called when next button is clicked (learning mode)
 * @param onAbortSession Called when session is aborted
 */
@Composable
fun <C : GameCard, A : GameAnswer> GenericReviewScreen(
    state: GenericReviewScreenState<C, A>,
    cardInfoContent: @Composable (C?) -> Unit,
    modeIndicatorContent: @Composable () -> Unit,
    feedbackContent: @Composable (GenericAnswerResult<A>?, Boolean) -> Unit,
    answerButtonsContent: @Composable (enabled: Boolean) -> Unit,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onNextClicked: () -> Unit = {},
    onAbortSession: () -> Unit = {}
) {
    var showAbortDialog by remember { mutableStateOf(false) }

    // Handle Android back button/gesture
    BackHandler { showAbortDialog = true }

    // Confirmation dialog for aborting session
    if (showAbortDialog) {
        AbortSessionDialog(
            onConfirm = onAbortSession,
            onDismiss = { showAbortDialog = false }
        )
    }

    // Auto-advance after showing feedback (only if NOT in learning mode)
    LaunchedEffect(state.showingFeedback, state.inLearningMode) {
        if (state.showingFeedback && !state.inLearningMode) {
            Log.d(TAG, "Showing feedback, will advance in ${autoAdvanceDelayMs}ms")
            delay(autoAdvanceDelayMs)

            if (state.session.isComplete()) {
                Log.i(TAG, "Session complete, navigating to results")
                onSessionComplete()
            } else {
                Log.d(TAG, "Advancing to next trial and auto-playing")
                onTrialComplete()
                onAutoPlay()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator with back button
        ReviewProgressIndicator(
            currentTrial = state.trialNumber,
            totalTrials = state.totalTrials,
            onBackClicked = { showAbortDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Game-specific card info
        cardInfoContent(state.currentCard)

        Spacer(modifier = Modifier.height(16.dp))

        // Game-specific mode indicators
        modeIndicatorContent()

        Spacer(modifier = Modifier.height(24.dp))

        // Play Button (shared across all game types)
        ReviewPlayButton(
            isPlaying = state.isPlaying,
            hasPlayedThisTrial = state.hasPlayedThisTrial,
            showingFeedback = state.showingFeedback,
            inLearningMode = state.inLearningMode,
            onClick = onPlayClicked
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Game-specific feedback
        feedbackContent(state.lastAnswer, state.hasPlayedThisTrial)

        // Flexible spacer pushes remaining content to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Game-specific answer buttons
        val buttonsEnabled = state.hasPlayedThisTrial && !state.isPlaying &&
                (!state.showingFeedback || state.inLearningMode)
        answerButtonsContent(buttonsEnabled)

        // Next button - always takes space, invisible when not in learning mode
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNextClicked,
            enabled = state.inLearningMode && !state.isPlaying,
            modifier = Modifier.alpha(if (state.inLearningMode) 1f else 0f),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Success
            )
        ) {
            Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
