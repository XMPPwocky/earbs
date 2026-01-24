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
import net.xmppwocky.earbs.model.CardScore
import net.xmppwocky.earbs.model.ReviewSession

private const val TAG = "ReviewScreen"
private const val FEEDBACK_DELAY_MS = 500L

/**
 * State for the review screen UI.
 */
data class ReviewScreenState(
    val session: ReviewSession,
    val currentCard: Card? = null,
    val lastAnswer: AnswerResult? = null,
    val playbackMode: PlaybackMode = PlaybackMode.BLOCK,
    val isPlaying: Boolean = false,
    val hasPlayedThisTrial: Boolean = false,
    val showingFeedback: Boolean = false
) {
    val trialNumber: Int get() = session.currentTrial
    val totalTrials: Int get() = session.totalTrials
    val isComplete: Boolean get() = session.isComplete()
}

@Composable
fun ReviewScreen(
    state: ReviewScreenState,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordType) -> Unit,
    onModeChanged: (PlaybackMode) -> Unit,
    onTrialComplete: () -> Unit,
    onSessionComplete: (List<CardScore>) -> Unit
) {
    // Auto-advance after showing feedback
    LaunchedEffect(state.showingFeedback) {
        if (state.showingFeedback) {
            Log.d(TAG, "Showing feedback, will advance in ${FEEDBACK_DELAY_MS}ms")
            delay(FEEDBACK_DELAY_MS)

            if (state.session.isComplete()) {
                Log.i(TAG, "Session complete, navigating to results")
                onSessionComplete(state.session.getResults())
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

        // Mode Toggle
        ReviewModeToggle(
            currentMode = state.playbackMode,
            onModeChanged = onModeChanged,
            enabled = !state.isPlaying && !state.showingFeedback
        )

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
            cards = state.session.cards,
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
private fun ReviewModeToggle(
    currentMode: PlaybackMode,
    onModeChanged: (PlaybackMode) -> Unit,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Block",
            fontWeight = if (currentMode == PlaybackMode.BLOCK) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) Color.Unspecified else Color.Gray
        )

        Switch(
            checked = currentMode == PlaybackMode.ARPEGGIATED,
            onCheckedChange = { isArpeggiated ->
                onModeChanged(if (isArpeggiated) PlaybackMode.ARPEGGIATED else PlaybackMode.BLOCK)
            },
            enabled = enabled,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = "Arpeggiated",
            fontWeight = if (currentMode == PlaybackMode.ARPEGGIATED) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) Color.Unspecified else Color.Gray
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
        answerResult is AnswerResult.Wrong -> "Wrong — it was ${answerResult.actualType.displayName}" to Color(0xFFF44336)
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
    cards: List<Card>,
    enabled: Boolean,
    onAnswerClicked: (ChordType) -> Unit
) {
    // Get unique chord types from the session's cards
    val chordTypes = cards.map { it.chordType }.distinct()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (chordTypes.size > 0) {
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
