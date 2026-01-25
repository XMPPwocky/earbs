package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

private const val TAG = "FunctionReviewScreen"

/**
 * Represents the result of a function answer.
 */
sealed class FunctionAnswerResult {
    data object Correct : FunctionAnswerResult()
    data class Wrong(val actualFunction: ChordFunction) : FunctionAnswerResult()
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
    val showingFeedback: Boolean = false
) {
    val trialNumber: Int get() = session.currentTrial + 1
    val totalTrials: Int get() = session.totalTrials
    val isComplete: Boolean get() = session.isComplete()
    val playbackMode: PlaybackMode get() = currentCard?.playbackMode ?: PlaybackMode.ARPEGGIATED
    val keyQuality: KeyQuality? get() = currentCard?.keyQuality
}

@Composable
fun FunctionReviewScreen(
    state: FunctionReviewScreenState,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordFunction) -> Unit,
    onTrialComplete: () -> Unit,
    onSessionComplete: () -> Unit
) {
    // Auto-advance after showing feedback
    LaunchedEffect(state.showingFeedback) {
        if (state.showingFeedback) {
            Log.d(TAG, "Showing feedback, will advance in ${Timing.FEEDBACK_DELAY_MS}ms")
            delay(Timing.FEEDBACK_DELAY_MS)

            if (state.session.isComplete()) {
                Log.i(TAG, "Session complete, navigating to results")
                onSessionComplete()
            } else {
                Log.d(TAG, "Advancing to next trial")
                onTrialComplete()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress indicator
        FunctionProgressIndicator(
            currentTrial = state.trialNumber,
            totalTrials = state.totalTrials
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current card info
        FunctionCardInfo(card = state.currentCard)

        Spacer(modifier = Modifier.height(16.dp))

        // Mode and Key Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FunctionPlaybackModeIndicator(mode = state.playbackMode)
            state.keyQuality?.let { KeyQualityIndicator(keyQuality = it) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Play Button
        FunctionPlayButton(
            isPlaying = state.isPlaying,
            hasPlayedThisTrial = state.hasPlayedThisTrial,
            showingFeedback = state.showingFeedback,
            onClick = onPlayClicked
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feedback Area
        FunctionFeedbackArea(
            answerResult = state.lastAnswer,
            hasPlayedThisTrial = state.hasPlayedThisTrial
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Answer Buttons
        FunctionAnswerButtons(
            functions = state.session.getAllFunctionsForKey(),
            enabled = state.hasPlayedThisTrial && !state.isPlaying && !state.showingFeedback,
            onAnswerClicked = onAnswerClicked
        )
    }
}

@Composable
private fun FunctionProgressIndicator(
    currentTrial: Int,
    totalTrials: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Trial $currentTrial / $totalTrials",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { currentTrial.toFloat() / totalTrials },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
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
private fun FunctionPlaybackModeIndicator(mode: PlaybackMode) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = when (mode) {
                PlaybackMode.BLOCK -> "Block"
                PlaybackMode.ARPEGGIATED -> "Arpeggiated"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
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
private fun FunctionPlayButton(
    isPlaying: Boolean,
    hasPlayedThisTrial: Boolean,
    showingFeedback: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isPlaying && !showingFeedback,
        modifier = Modifier.size(150.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = when {
                isPlaying -> "Playing..."
                hasPlayedThisTrial -> "Replay"
                else -> "Play"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
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
    onAnswerClicked: (ChordFunction) -> Unit
) {
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
                    onClick = { onAnswerClicked(function) }
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
                    onClick = { onAnswerClicked(function) }
                )
            }
        }
    }
}

@Composable
private fun FunctionAnswerButton(
    function: ChordFunction,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 100.dp, height = 56.dp)
    ) {
        Text(
            text = function.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
