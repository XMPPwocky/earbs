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
import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameTypeConfig
import net.xmppwocky.earbs.model.ScaleCard
import net.xmppwocky.earbs.ui.components.ButtonColorState
import net.xmppwocky.earbs.ui.components.answerButtonColors
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

/**
 * Type alias for scale answer result.
 */
typealias ScaleAnswerResultGeneric = GenericAnswerResult<GameAnswer.ScaleAnswer>

/**
 * Type alias for scale review screen state.
 */
typealias ScaleReviewState = GenericReviewScreenState<ScaleCard, GameAnswer.ScaleAnswer>

/**
 * Extension to get all scales for the session (for answer buttons).
 */
fun ScaleReviewState.getAllScales(): List<ScaleType> =
    currentCard?.let { card ->
        GameTypeConfig.ScaleGame.getAnswerOptions(card, session).map { it.scale }
    } ?: emptyList()

/**
 * Scale review screen - thin wrapper around GenericReviewScreen.
 */
@Composable
fun ScaleReviewScreen(
    state: ScaleReviewState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ScaleType) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayScale: (ScaleType) -> Unit = {},  // Play arbitrary scale (for learning mode)
    onNextClicked: () -> Unit = {},  // Manual advance (for learning mode)
    onAbortSession: () -> Unit = {}  // Abort session and return to home
) {
    GenericReviewScreen(
        state = state,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        cardInfoContent = { card -> ScaleCardInfo(card = card) },
        modeIndicatorContent = {
            ScaleModeIndicators(
                direction = state.currentCard?.direction
            )
        },
        feedbackContent = { answerResult, hasPlayedThisTrial ->
            ScaleFeedbackArea(
                answerResult = answerResult,
                hasPlayedThisTrial = hasPlayedThisTrial
            )
        },
        answerButtonsContent = { enabled ->
            ScaleAnswerButtons(
                scales = state.getAllScales(),
                enabled = enabled,
                isLearningMode = state.inLearningMode,
                answerResult = state.lastAnswer,
                onAnswerClicked = onAnswerClicked,
                onPlayScale = onPlayScale
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

// ========== Scale Game Specific Composables ==========

@Composable
private fun ScaleCardInfo(card: ScaleCard?) {
    Text(
        text = card?.let {
            "Identify the scale (octave ${it.octave})"
        } ?: "Loading...",
        fontSize = 16.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ScaleModeIndicators(
    direction: ScaleDirection?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show direction indicator
        direction?.let { ScaleDirectionIndicator(direction = it) }
    }
}

@Composable
private fun ScaleDirectionIndicator(direction: ScaleDirection) {
    val (text, containerColor, contentColor) = when (direction) {
        ScaleDirection.ASCENDING -> Triple(
            "ascending",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        ScaleDirection.DESCENDING -> Triple(
            "descending",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        ScaleDirection.BOTH -> Triple(
            "ascending & descending",
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
private fun ScaleFeedbackArea(
    answerResult: ScaleAnswerResultGeneric?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear the scale" to Color.Gray
        answerResult == null -> "What scale is this?" to Color.Gray
        answerResult is GenericAnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is GenericAnswerResult.Wrong -> {
            val actualScale = answerResult.actualAnswer.scale
            "Wrong - it was ${actualScale.displayName}" to AppColors.Error
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
private fun ScaleAnswerButtons(
    scales: List<ScaleType>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: ScaleAnswerResultGeneric? = null,
    onAnswerClicked: (ScaleType) -> Unit,
    onPlayScale: (ScaleType) -> Unit = {}
) {
    // In learning mode, clicking plays that scale instead of answering
    val onClick: (ScaleType) -> Unit = if (isLearningMode) onPlayScale else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(scale: ScaleType): ButtonColorState {
        return when {
            answerResult !is GenericAnswerResult.Wrong -> ButtonColorState.DEFAULT
            scale == answerResult.selectedAnswer.scale -> ButtonColorState.WRONG
            scale == answerResult.actualAnswer.scale -> ButtonColorState.CORRECT
            else -> ButtonColorState.INACTIVE
        }
    }

    // Scales are displayed in a scrollable column with 2 buttons per row
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Group scales in pairs for 2-column layout
        scales.chunked(2).forEach { rowScales ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowScales.forEach { scale ->
                    ScaleAnswerButton(
                        scale = scale,
                        enabled = enabled,
                        colorState = getColorState(scale),
                        onClick = { onClick(scale) }
                    )
                }
                // Add empty space if only one item in row to maintain alignment
                if (rowScales.size == 1) {
                    Spacer(modifier = Modifier.width(160.dp))
                }
            }
        }
    }
}

@Composable
private fun ScaleAnswerButton(
    scale: ScaleType,
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
            .testTag("scale_answer_button_${scale.name}_${colorState.name}"),
        colors = answerButtonColors(colorState),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = scale.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
