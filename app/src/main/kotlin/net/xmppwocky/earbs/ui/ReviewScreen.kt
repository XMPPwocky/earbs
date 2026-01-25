package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ReviewSession
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

private const val TAG = "ReviewScreen"

/**
 * Represents the result of an answer.
 */
sealed class AnswerResult {
    data object Correct : AnswerResult()
    data class Wrong(val actualType: ChordType, val selectedType: ChordType) : AnswerResult()
}

/**
 * Color state for answer buttons after a wrong answer.
 */
enum class ButtonColorState {
    DEFAULT,    // Normal button colors
    CORRECT,    // Green - the correct answer
    WRONG,      // Red - the user's wrong selection
    INACTIVE    // Gray - other buttons
}

/**
 * State for the review screen UI.
 */
data class ReviewScreenState(
    val session: ReviewSession,
    val currentCard: Card? = null,
    val currentRootSemitones: Int? = null,  // Root note for current trial (fixed for replays)
    val lastAnswer: AnswerResult? = null,
    val isPlaying: Boolean = false,
    val hasPlayedThisTrial: Boolean = false,
    val showingFeedback: Boolean = false,
    val inLearningMode: Boolean = false  // True after wrong answer when feature enabled
) {
    val trialNumber: Int get() = minOf(session.currentTrial + 1, session.totalTrials)  // 1-indexed, capped
    val totalTrials: Int get() = session.totalTrials
    val isComplete: Boolean get() = session.isComplete()
    // Playback mode comes from the current card
    val playbackMode: PlaybackMode get() = currentCard?.playbackMode ?: PlaybackMode.ARPEGGIATED
}

/**
 * Simple result data for session completion.
 */
data class SessionResult(
    val correctCount: Int,
    val totalTrials: Int,
    val sessionId: Long,
    val gameType: String
)

@Composable
fun ReviewScreen(
    state: ReviewScreenState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordType) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayChordType: (ChordType) -> Unit = {},  // Play arbitrary chord type (for learning mode)
    onNextClicked: () -> Unit = {},  // Manual advance (for learning mode)
    onAbortSession: () -> Unit = {}  // Abort session and return to home
) {
    var showAbortDialog by remember { mutableStateOf(false) }

    // Handle Android back button/gesture
    BackHandler { showAbortDialog = true }

    // Confirmation dialog for aborting session
    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            title = { Text("Exit Review?") },
            text = { Text("Your progress in this session will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    Log.i(TAG, "User confirmed abort session")
                    onAbortSession()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialog = false }) {
                    Text("Continue")
                }
            }
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
        ProgressIndicator(
            currentTrial = state.trialNumber,
            totalTrials = state.totalTrials,
            onBackClicked = { showAbortDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current card info
        CurrentCardInfo(card = state.currentCard)

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Indicator (read-only, mode comes from card)
        PlaybackModeIndicator(mode = state.playbackMode)

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
        ReviewFeedbackArea(
            answerResult = state.lastAnswer,
            hasPlayedThisTrial = state.hasPlayedThisTrial
        )

        // Flexible spacer pushes remaining content to bottom
        Spacer(modifier = Modifier.weight(1f))

        // Answer Buttons (pinned to bottom)
        ReviewAnswerButtons(
            chordTypes = state.session.getChordTypes(),
            enabled = state.hasPlayedThisTrial && !state.isPlaying &&
                      (!state.showingFeedback || state.inLearningMode),
            isLearningMode = state.inLearningMode,
            answerResult = state.lastAnswer,
            onAnswerClicked = onAnswerClicked,
            onPlayChordType = onPlayChordType
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
private fun ProgressIndicator(
    currentTrial: Int,
    totalTrials: Int,
    onBackClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Trial text and progress on the left
        Column(modifier = Modifier.weight(1f)) {
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

        // Back button on the right
        IconButton(onClick = onBackClicked) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Exit review"
            )
        }
    }
}

@Composable
private fun CurrentCardInfo(card: Card?) {
    Text(
        text = card?.let { "Chord in octave ${it.octave}" } ?: "Loading...",
        fontSize = 16.sp,
        color = Color.Gray
    )
}

@Composable
private fun PlaybackModeIndicator(mode: PlaybackMode) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = when (mode) {
                PlaybackMode.BLOCK -> "Block Mode"
                PlaybackMode.ARPEGGIATED -> "Arpeggiated Mode"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ReviewPlayButton(
    isPlaying: Boolean,
    hasPlayedThisTrial: Boolean,
    showingFeedback: Boolean,
    inLearningMode: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isPlaying && (!showingFeedback || inLearningMode),
        modifier = Modifier.size(width = 140.dp, height = 100.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = when {
                isPlaying -> "Playing..."
                hasPlayedThisTrial -> "Replay"
                else -> "Play"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReviewFeedbackArea(
    answerResult: AnswerResult?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear the chord" to Color.Gray
        answerResult == null -> "What chord type is this?" to Color.Gray
        answerResult is AnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is AnswerResult.Wrong -> "Wrong - it was ${answerResult.actualType.displayName}" to AppColors.Error
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
private fun ReviewAnswerButtons(
    chordTypes: List<ChordType>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: AnswerResult? = null,
    onAnswerClicked: (ChordType) -> Unit,
    onPlayChordType: (ChordType) -> Unit = {}
) {
    // In learning mode, clicking plays that chord instead of answering
    val onClick: (ChordType) -> Unit = if (isLearningMode) onPlayChordType else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(chordType: ChordType): ButtonColorState {
        return when {
            answerResult !is AnswerResult.Wrong -> ButtonColorState.DEFAULT
            chordType == answerResult.selectedType -> ButtonColorState.WRONG
            chordType == answerResult.actualType -> ButtonColorState.CORRECT
            else -> ButtonColorState.INACTIVE
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (chordTypes.isNotEmpty()) {
                ReviewAnswerButton(
                    chordType = chordTypes[0],
                    enabled = enabled,
                    colorState = getColorState(chordTypes[0]),
                    onClick = { onClick(chordTypes[0]) }
                )
            }
            if (chordTypes.size > 1) {
                ReviewAnswerButton(
                    chordType = chordTypes[1],
                    enabled = enabled,
                    colorState = getColorState(chordTypes[1]),
                    onClick = { onClick(chordTypes[1]) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Second row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (chordTypes.size > 2) {
                ReviewAnswerButton(
                    chordType = chordTypes[2],
                    enabled = enabled,
                    colorState = getColorState(chordTypes[2]),
                    onClick = { onClick(chordTypes[2]) }
                )
            }
            if (chordTypes.size > 3) {
                ReviewAnswerButton(
                    chordType = chordTypes[3],
                    enabled = enabled,
                    colorState = getColorState(chordTypes[3]),
                    onClick = { onClick(chordTypes[3]) }
                )
            }
        }

        // Third row (for sessions with more than 4 chord types)
        if (chordTypes.size > 4) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (chordTypes.size > 4) {
                    ReviewAnswerButton(
                        chordType = chordTypes[4],
                        enabled = enabled,
                        colorState = getColorState(chordTypes[4]),
                        onClick = { onClick(chordTypes[4]) }
                    )
                }
                if (chordTypes.size > 5) {
                    ReviewAnswerButton(
                        chordType = chordTypes[5],
                        enabled = enabled,
                        colorState = getColorState(chordTypes[5]),
                        onClick = { onClick(chordTypes[5]) }
                    )
                }
            }
        }

        // Fourth row (for sessions with more than 6 chord types)
        if (chordTypes.size > 6) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (chordTypes.size > 6) {
                    ReviewAnswerButton(
                        chordType = chordTypes[6],
                        enabled = enabled,
                        colorState = getColorState(chordTypes[6]),
                        onClick = { onClick(chordTypes[6]) }
                    )
                }
                if (chordTypes.size > 7) {
                    ReviewAnswerButton(
                        chordType = chordTypes[7],
                        enabled = enabled,
                        colorState = getColorState(chordTypes[7]),
                        onClick = { onClick(chordTypes[7]) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewAnswerButton(
    chordType: ChordType,
    enabled: Boolean,
    colorState: ButtonColorState = ButtonColorState.DEFAULT,
    onClick: () -> Unit
) {
    val buttonColors = when (colorState) {
        ButtonColorState.CORRECT -> ButtonDefaults.buttonColors(
            containerColor = AppColors.Success,
            disabledContainerColor = AppColors.Success
        )
        ButtonColorState.WRONG -> ButtonDefaults.buttonColors(
            containerColor = AppColors.Error,
            disabledContainerColor = AppColors.Error
        )
        ButtonColorState.INACTIVE -> ButtonDefaults.buttonColors(
            containerColor = Color.Gray,
            disabledContainerColor = Color.Gray
        )
        ButtonColorState.DEFAULT -> ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(width = 140.dp, height = 60.dp)
            .testTag("answer_button_${chordType.name}_${colorState.name}"),
        colors = buttonColors
    ) {
        Text(
            text = chordType.displayName,
            fontSize = 18.sp
        )
    }
}
