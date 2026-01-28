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
import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameTypeConfig
import net.xmppwocky.earbs.model.IntervalCard
import net.xmppwocky.earbs.ui.components.ButtonColorState
import net.xmppwocky.earbs.ui.components.answerButtonColors
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

/**
 * Type alias for interval answer result.
 */
typealias IntervalAnswerResultGeneric = GenericAnswerResult<GameAnswer.IntervalAnswer>

/**
 * Type alias for interval review screen state.
 */
typealias IntervalReviewState = GenericReviewScreenState<IntervalCard, GameAnswer.IntervalAnswer>

/**
 * Extension to get all intervals for the session (for answer buttons).
 */
fun IntervalReviewState.getAllIntervals(): List<IntervalType> =
    currentCard?.let { card ->
        GameTypeConfig.IntervalGame.getAnswerOptions(card, session).map { it.interval }
    } ?: emptyList()

/**
 * Interval review screen - thin wrapper around GenericReviewScreen.
 */
@Composable
fun IntervalReviewScreen(
    state: IntervalReviewState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (IntervalType) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayInterval: (IntervalType) -> Unit = {},  // Play arbitrary interval (for learning mode)
    onNextClicked: () -> Unit = {},  // Manual advance (for learning mode)
    onAbortSession: () -> Unit = {}  // Abort session and return to home
) {
    GenericReviewScreen(
        state = state,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        cardInfoContent = { card -> IntervalCardInfo(card = card) },
        modeIndicatorContent = {
            IntervalModeIndicators(
                direction = state.currentCard?.direction
            )
        },
        feedbackContent = { answerResult, hasPlayedThisTrial ->
            IntervalFeedbackArea(
                answerResult = answerResult,
                hasPlayedThisTrial = hasPlayedThisTrial
            )
        },
        answerButtonsContent = { enabled ->
            IntervalAnswerButtons(
                intervals = state.getAllIntervals(),
                enabled = enabled,
                isLearningMode = state.inLearningMode,
                answerResult = state.lastAnswer,
                onAnswerClicked = onAnswerClicked,
                onPlayInterval = onPlayInterval
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

// ========== Interval Game Specific Composables ==========

@Composable
private fun IntervalCardInfo(card: IntervalCard?) {
    Text(
        text = card?.let {
            "Identify the interval (octave ${it.octave})"
        } ?: "Loading...",
        fontSize = 16.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun IntervalModeIndicators(
    direction: IntervalDirection?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show direction indicator
        direction?.let { IntervalDirectionIndicator(direction = it) }
    }
}

@Composable
private fun IntervalDirectionIndicator(direction: IntervalDirection) {
    val (text, containerColor, contentColor) = when (direction) {
        IntervalDirection.ASCENDING -> Triple(
            "ascending",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        IntervalDirection.DESCENDING -> Triple(
            "descending",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        IntervalDirection.HARMONIC -> Triple(
            "harmonic",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun IntervalFeedbackArea(
    answerResult: IntervalAnswerResultGeneric?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear the interval" to Color.Gray
        answerResult == null -> "What interval is this?" to Color.Gray
        answerResult is GenericAnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is GenericAnswerResult.Wrong -> {
            val actualInterval = answerResult.actualAnswer.interval
            "Wrong - it was ${actualInterval.displayName}" to AppColors.Error
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
private fun IntervalAnswerButtons(
    intervals: List<IntervalType>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: IntervalAnswerResultGeneric? = null,
    onAnswerClicked: (IntervalType) -> Unit,
    onPlayInterval: (IntervalType) -> Unit = {}
) {
    // In learning mode, clicking plays that interval instead of answering
    val onClick: (IntervalType) -> Unit = if (isLearningMode) onPlayInterval else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(interval: IntervalType): ButtonColorState {
        return when {
            answerResult !is GenericAnswerResult.Wrong -> ButtonColorState.DEFAULT
            interval == answerResult.selectedAnswer.interval -> ButtonColorState.WRONG
            interval == answerResult.actualAnswer.interval -> ButtonColorState.CORRECT
            else -> ButtonColorState.INACTIVE
        }
    }

    // Intervals are displayed in a scrollable column with 2 buttons per row
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Group intervals in pairs for 2-column layout
        intervals.chunked(2).forEach { rowIntervals ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowIntervals.forEach { interval ->
                    IntervalAnswerButton(
                        interval = interval,
                        enabled = enabled,
                        colorState = getColorState(interval),
                        onClick = { onClick(interval) }
                    )
                }
                // Add empty space if only one item in row to maintain alignment
                if (rowIntervals.size == 1) {
                    Spacer(modifier = Modifier.width(160.dp))
                }
            }
        }
    }
}

@Composable
private fun IntervalAnswerButton(
    interval: IntervalType,
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
            .testTag("interval_answer_button_${interval.name}_${colorState.name}"),
        colors = answerButtonColors(colorState),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = interval.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
