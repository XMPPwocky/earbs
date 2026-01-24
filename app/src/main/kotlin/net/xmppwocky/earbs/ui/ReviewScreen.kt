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
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ReviewSession

private const val TAG = "ReviewScreen"
private const val FEEDBACK_DELAY_MS = 500L

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
    val showingFeedback: Boolean = false
) {
    val trialNumber: Int get() = session.currentTrial + 1  // 1-indexed for display
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
    val totalTrials: Int
)

@Composable
fun ReviewScreen(
    state: ReviewScreenState,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordType) -> Unit,
    onTrialComplete: () -> Unit,
    onSessionComplete: () -> Unit
) {
    // Auto-advance after showing feedback
    LaunchedEffect(state.showingFeedback) {
        if (state.showingFeedback) {
            Log.d(TAG, "Showing feedback, will advance in ${FEEDBACK_DELAY_MS}ms")
            delay(FEEDBACK_DELAY_MS)

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
        ProgressIndicator(
            currentTrial = state.trialNumber,
            totalTrials = state.totalTrials
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current card info
        CurrentCardInfo(card = state.currentCard)

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Indicator (read-only, mode comes from card)
        PlaybackModeIndicator(mode = state.playbackMode)

        Spacer(modifier = Modifier.height(24.dp))

        // Play Button
        ReviewPlayButton(
            isPlaying = state.isPlaying,
            hasPlayedThisTrial = state.hasPlayedThisTrial,
            showingFeedback = state.showingFeedback,
            onClick = onPlayClicked
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feedback Area
        ReviewFeedbackArea(
            answerResult = state.lastAnswer,
            hasPlayedThisTrial = state.hasPlayedThisTrial
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Answer Buttons
        ReviewAnswerButtons(
            chordTypes = state.session.getChordTypes(),
            enabled = state.hasPlayedThisTrial && !state.isPlaying && !state.showingFeedback,
            onAnswerClicked = onAnswerClicked
        )
    }
}

@Composable
private fun ProgressIndicator(
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
private fun ReviewFeedbackArea(
    answerResult: AnswerResult?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear the chord" to Color.Gray
        answerResult == null -> "What chord type is this?" to Color.Gray
        answerResult is AnswerResult.Correct -> "Correct!" to Color(0xFF4CAF50)
        answerResult is AnswerResult.Wrong -> "Wrong - it was ${answerResult.actualType.displayName}" to Color(0xFFF44336)
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
    onAnswerClicked: (ChordType) -> Unit
) {
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
                    onClick = { onAnswerClicked(chordTypes[0]) }
                )
            }
            if (chordTypes.size > 1) {
                ReviewAnswerButton(
                    chordType = chordTypes[1],
                    enabled = enabled,
                    onClick = { onAnswerClicked(chordTypes[1]) }
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
                    onClick = { onAnswerClicked(chordTypes[2]) }
                )
            }
            if (chordTypes.size > 3) {
                ReviewAnswerButton(
                    chordType = chordTypes[3],
                    enabled = enabled,
                    onClick = { onAnswerClicked(chordTypes[3]) }
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
                        onClick = { onAnswerClicked(chordTypes[4]) }
                    )
                }
                if (chordTypes.size > 5) {
                    ReviewAnswerButton(
                        chordType = chordTypes[5],
                        enabled = enabled,
                        onClick = { onAnswerClicked(chordTypes[5]) }
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
                        onClick = { onAnswerClicked(chordTypes[6]) }
                    )
                }
                if (chordTypes.size > 7) {
                    ReviewAnswerButton(
                        chordType = chordTypes[7],
                        enabled = enabled,
                        onClick = { onAnswerClicked(chordTypes[7]) }
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
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 140.dp, height = 60.dp)
    ) {
        Text(
            text = chordType.displayName,
            fontSize = 18.sp
        )
    }
}
