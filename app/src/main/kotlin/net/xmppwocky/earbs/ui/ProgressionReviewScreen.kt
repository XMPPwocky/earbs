package net.xmppwocky.earbs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameTypeConfig
import net.xmppwocky.earbs.model.KeyQuality
import net.xmppwocky.earbs.model.ProgressionCard
import net.xmppwocky.earbs.model.ProgressionType
import net.xmppwocky.earbs.ui.components.ButtonColorState
import net.xmppwocky.earbs.ui.components.CompactPlaybackModeIndicator
import net.xmppwocky.earbs.ui.components.answerButtonColors
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

/**
 * Type alias for progression answer result.
 */
typealias ProgressionAnswerResultGeneric = GenericAnswerResult<GameAnswer.ProgressionAnswer>

/**
 * Type alias for progression review screen state.
 */
typealias ProgressionReviewState = GenericReviewScreenState<ProgressionCard, GameAnswer.ProgressionAnswer>

/**
 * Extension to get all progressions for the session (for answer buttons).
 */
fun ProgressionReviewState.getAllProgressions(): List<ProgressionType> =
    currentCard?.let { card ->
        GameTypeConfig.ProgressionGame.getAnswerOptions(card, session).map { it.progression }
    } ?: emptyList()

/**
 * Progression review screen - thin wrapper around GenericReviewScreen.
 */
@Composable
fun ProgressionReviewScreen(
    state: ProgressionReviewState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ProgressionType) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayProgression: (ProgressionType) -> Unit = {},  // Play arbitrary progression (for learning mode)
    onNextClicked: () -> Unit = {},  // Manual advance (for learning mode)
    onAbortSession: () -> Unit = {}  // Abort session and return to home
) {
    GenericReviewScreen(
        state = state,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        cardInfoContent = { card -> ProgressionCardInfo(card = card) },
        modeIndicatorContent = {
            ProgressionModeIndicators(
                playbackMode = state.playbackMode,
                keyQuality = state.currentCard?.progression?.keyQuality
            )
        },
        feedbackContent = { answerResult, hasPlayedThisTrial ->
            ProgressionFeedbackArea(
                answerResult = answerResult,
                hasPlayedThisTrial = hasPlayedThisTrial
            )
        },
        answerButtonsContent = { enabled ->
            ProgressionAnswerButtons(
                progressions = state.getAllProgressions(),
                enabled = enabled,
                isLearningMode = state.inLearningMode,
                answerResult = state.lastAnswer,
                onAnswerClicked = onAnswerClicked,
                onPlayProgression = onPlayProgression
            )
        },
        onPlayClicked = onPlayClicked,
        onTrialComplete = onTrialComplete,
        onAutoPlay = onAutoPlay,
        onSessionComplete = onSessionComplete,
        onNextClicked = onNextClicked,
        onAbortSession = onAbortSession
    )
}

// ========== Progression Game Specific Composables ==========

@Composable
private fun ProgressionCardInfo(card: ProgressionCard?) {
    Text(
        text = card?.let {
            "Identify the chord progression (octave ${it.octave})"
        } ?: "Loading...",
        fontSize = 16.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProgressionModeIndicators(
    playbackMode: PlaybackMode,
    keyQuality: KeyQuality?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactPlaybackModeIndicator(mode = playbackMode)
        // Show key quality indicator (major vs minor progression)
        keyQuality?.let { ProgressionKeyIndicator(keyQuality = it) }
    }
}

@Composable
private fun ProgressionKeyIndicator(keyQuality: KeyQuality) {
    Surface(
        color = if (keyQuality == KeyQuality.MAJOR)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = keyQuality.name.lowercase(),
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
private fun ProgressionFeedbackArea(
    answerResult: ProgressionAnswerResultGeneric?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear the progression" to Color.Gray
        answerResult == null -> "What progression is this?" to Color.Gray
        answerResult is GenericAnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is GenericAnswerResult.Wrong -> {
            val actualProgression = answerResult.actualAnswer.progression
            "Wrong - it was ${actualProgression.displayName}" to AppColors.Error
        }
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
private fun ProgressionAnswerButtons(
    progressions: List<ProgressionType>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: ProgressionAnswerResultGeneric? = null,
    onAnswerClicked: (ProgressionType) -> Unit,
    onPlayProgression: (ProgressionType) -> Unit = {}
) {
    // In learning mode, clicking plays that progression instead of answering
    val onClick: (ProgressionType) -> Unit = if (isLearningMode) onPlayProgression else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(progression: ProgressionType): ButtonColorState {
        return when {
            answerResult !is GenericAnswerResult.Wrong -> ButtonColorState.DEFAULT
            progression == answerResult.selectedAnswer.progression -> ButtonColorState.WRONG
            progression == answerResult.actualAnswer.progression -> ButtonColorState.CORRECT
            else -> ButtonColorState.INACTIVE
        }
    }

    // Progressions are displayed in a scrollable column with 2 buttons per row
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Group progressions in pairs for 2-column layout
        progressions.chunked(2).forEach { rowProgressions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowProgressions.forEach { progression ->
                    ProgressionAnswerButton(
                        progression = progression,
                        enabled = enabled,
                        colorState = getColorState(progression),
                        onClick = { onClick(progression) }
                    )
                }
                // Add empty space if only one item in row to maintain alignment
                if (rowProgressions.size == 1) {
                    Spacer(modifier = Modifier.width(160.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressionAnswerButton(
    progression: ProgressionType,
    enabled: Boolean,
    colorState: ButtonColorState = ButtonColorState.DEFAULT,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(160.dp)
            .height(48.dp)
            .testTag("progression_answer_button_${progression.name}_${colorState.name}"),
        colors = answerButtonColors(colorState),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = progression.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
