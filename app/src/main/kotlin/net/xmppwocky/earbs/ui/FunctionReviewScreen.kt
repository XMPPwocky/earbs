package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.FunctionReviewSession
import net.xmppwocky.earbs.model.KeyQuality
import net.xmppwocky.earbs.ui.components.AbortSessionDialog
import net.xmppwocky.earbs.ui.components.ButtonColorState
import net.xmppwocky.earbs.ui.components.CompactPlaybackModeIndicator
import net.xmppwocky.earbs.ui.components.ReviewPlayButton
import net.xmppwocky.earbs.ui.components.ReviewProgressIndicator
import net.xmppwocky.earbs.ui.components.answerButtonColors
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

private const val TAG = "FunctionReviewScreen"

/**
 * Represents the result of a function answer.
 */
sealed class FunctionAnswerResult {
    data object Correct : FunctionAnswerResult()
    data class Wrong(val actualFunction: ChordFunction, val selectedFunction: ChordFunction) : FunctionAnswerResult()
}

/**
 * State for the function review screen UI.
 */
data class FunctionReviewScreenState(
    val session: FunctionReviewSession,
    val currentCard: FunctionCard? = null,
    val currentRootSemitones: Int? = null,
    val lastAnswer: FunctionAnswerResult? = null,
    val isPlaying: Boolean = false,
    val hasPlayedThisTrial: Boolean = false,
    val showingFeedback: Boolean = false,
    val inLearningMode: Boolean = false  // True after wrong answer when feature enabled
) {
    val trialNumber: Int get() = minOf(session.currentTrial + 1, session.totalTrials)  // 1-indexed, capped
    val totalTrials: Int get() = session.totalTrials
    val isComplete: Boolean get() = session.isComplete()
    val playbackMode: PlaybackMode get() = currentCard?.playbackMode ?: PlaybackMode.ARPEGGIATED
    val keyQuality: KeyQuality? get() = currentCard?.keyQuality
}

@Composable
fun FunctionReviewScreen(
    state: FunctionReviewScreenState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordFunction) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayFunction: (ChordFunction) -> Unit = {},  // Play arbitrary function (for learning mode)
    onNextClicked: () -> Unit = {},  // Manual advance (for learning mode)
    onAbortSession: () -> Unit = {}  // Abort session and return to home
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

        // Current card info
        FunctionCardInfo(card = state.currentCard)

        Spacer(modifier = Modifier.height(16.dp))

        // Mode and Key Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactPlaybackModeIndicator(mode = state.playbackMode)
            state.keyQuality?.let { KeyQualityIndicator(keyQuality = it) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Play Button (enabled in learning mode to replay correct chord)
        ReviewPlayButton(
            isPlaying = state.isPlaying,
            hasPlayedThisTrial = state.hasPlayedThisTrial,
            showingFeedback = state.showingFeedback,
            inLearningMode = state.inLearningMode,
            onClick = onPlayClicked
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feedback Area
        FunctionFeedbackArea(
            answerResult = state.lastAnswer,
            hasPlayedThisTrial = state.hasPlayedThisTrial
        )

        // Flexible spacer pushes remaining content to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Answer Buttons (pinned to bottom)
        FunctionAnswerButtons(
            functions = state.session.getAllFunctionsForKey(),
            enabled = state.hasPlayedThisTrial && !state.isPlaying &&
                      (!state.showingFeedback || state.inLearningMode),
            isLearningMode = state.inLearningMode,
            answerResult = state.lastAnswer,
            onAnswerClicked = onAnswerClicked,
            onPlayFunction = onPlayFunction
        )

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

@Composable
private fun FunctionCardInfo(card: FunctionCard?) {
    Text(
        text = card?.let {
            "Identify the chord function in ${it.keyQuality.name.lowercase()} key (octave ${it.octave})"
        } ?: "Loading...",
        fontSize = 16.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun KeyQualityIndicator(keyQuality: KeyQuality) {
    Surface(
        color = if (keyQuality == KeyQuality.MAJOR)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "${keyQuality.name.lowercase()} key",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (keyQuality == KeyQuality.MAJOR)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun FunctionFeedbackArea(
    answerResult: FunctionAnswerResult?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear: tonic, then target chord" to Color.Gray
        answerResult == null -> "What function is the second chord?" to Color.Gray
        answerResult is FunctionAnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is FunctionAnswerResult.Wrong -> "Wrong - it was ${answerResult.actualFunction.displayName}" to AppColors.Error
        else -> "" to Color.Gray
    }

    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun FunctionAnswerButtons(
    functions: List<ChordFunction>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: FunctionAnswerResult? = null,
    onAnswerClicked: (ChordFunction) -> Unit,
    onPlayFunction: (ChordFunction) -> Unit = {}
) {
    // In learning mode, clicking plays that function instead of answering
    val onClick: (ChordFunction) -> Unit = if (isLearningMode) onPlayFunction else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(function: ChordFunction): ButtonColorState {
        return when {
            answerResult !is FunctionAnswerResult.Wrong -> ButtonColorState.DEFAULT
            function == answerResult.selectedFunction -> ButtonColorState.WRONG
            function == answerResult.actualFunction -> ButtonColorState.CORRECT
            else -> ButtonColorState.INACTIVE
        }
    }

    // Functions are displayed in a 3x2 grid for 6 functions
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First row: first 3 functions
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            functions.take(3).forEach { function ->
                FunctionAnswerButton(
                    function = function,
                    enabled = enabled,
                    colorState = getColorState(function),
                    onClick = { onClick(function) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Second row: remaining functions
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            functions.drop(3).forEach { function ->
                FunctionAnswerButton(
                    function = function,
                    enabled = enabled,
                    colorState = getColorState(function),
                    onClick = { onClick(function) }
                )
            }
        }
    }
}

@Composable
private fun FunctionAnswerButton(
    function: ChordFunction,
    enabled: Boolean,
    colorState: ButtonColorState = ButtonColorState.DEFAULT,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(width = 100.dp, height = 56.dp)
            .testTag("function_answer_button_${function.name}_${colorState.name}"),
        colors = answerButtonColors(colorState)
    ) {
        Text(
            text = function.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
