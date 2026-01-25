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
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameTypeConfig
import net.xmppwocky.earbs.model.GenericReviewSession
import net.xmppwocky.earbs.model.KeyQuality
import net.xmppwocky.earbs.ui.components.ButtonColorState
import net.xmppwocky.earbs.ui.components.CompactPlaybackModeIndicator
import net.xmppwocky.earbs.ui.components.answerButtonColors
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

/**
 * Type alias for function answer result.
 */
typealias FunctionAnswerResultGeneric = GenericAnswerResult<GameAnswer.FunctionAnswer>

/**
 * Type alias for function review screen state.
 */
typealias FunctionReviewState = GenericReviewScreenState<FunctionCard, GameAnswer.FunctionAnswer>

/**
 * Extension to get all functions for the current key quality (for answer buttons).
 */
fun FunctionReviewState.getAllFunctionsForKey(): List<ChordFunction> =
    GameTypeConfig.FunctionGame.getAnswerOptions(session).map { it.function }

/**
 * Extension to get the key quality from the session.
 */
val FunctionReviewState.keyQuality: KeyQuality?
    get() = currentCard?.keyQuality

/**
 * Function review screen - thin wrapper around GenericReviewScreen.
 */
@Composable
fun FunctionReviewScreen(
    state: FunctionReviewState,
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
    GenericReviewScreen(
        state = state,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        cardInfoContent = { card -> FunctionCardInfo(card = card) },
        modeIndicatorContent = {
            FunctionModeIndicators(
                playbackMode = state.playbackMode,
                keyQuality = state.keyQuality
            )
        },
        feedbackContent = { answerResult, hasPlayedThisTrial ->
            FunctionFeedbackArea(
                answerResult = answerResult,
                hasPlayedThisTrial = hasPlayedThisTrial
            )
        },
        answerButtonsContent = { enabled ->
            FunctionAnswerButtons(
                functions = state.getAllFunctionsForKey(),
                enabled = enabled,
                isLearningMode = state.inLearningMode,
                answerResult = state.lastAnswer,
                onAnswerClicked = onAnswerClicked,
                onPlayFunction = onPlayFunction
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

// ========== Function Game Specific Composables ==========

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
private fun FunctionModeIndicators(
    playbackMode: PlaybackMode,
    keyQuality: KeyQuality?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactPlaybackModeIndicator(mode = playbackMode)
        keyQuality?.let { KeyQualityIndicator(keyQuality = it) }
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
private fun FunctionFeedbackArea(
    answerResult: FunctionAnswerResultGeneric?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear: tonic, then target chord" to Color.Gray
        answerResult == null -> "What function is the second chord?" to Color.Gray
        answerResult is GenericAnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is GenericAnswerResult.Wrong -> {
            val actualFunction = answerResult.actualAnswer.function
            "Wrong - it was ${actualFunction.displayName}" to AppColors.Error
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
private fun FunctionAnswerButtons(
    functions: List<ChordFunction>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: FunctionAnswerResultGeneric? = null,
    onAnswerClicked: (ChordFunction) -> Unit,
    onPlayFunction: (ChordFunction) -> Unit = {}
) {
    // In learning mode, clicking plays that function instead of answering
    val onClick: (ChordFunction) -> Unit = if (isLearningMode) onPlayFunction else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(function: ChordFunction): ButtonColorState {
        return when {
            answerResult !is GenericAnswerResult.Wrong -> ButtonColorState.DEFAULT
            function == answerResult.selectedAnswer.function -> ButtonColorState.WRONG
            function == answerResult.actualAnswer.function -> ButtonColorState.CORRECT
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

// ========== Backwards Compatibility ==========

/**
 * Legacy FunctionAnswerResult type for backwards compatibility.
 * @deprecated Use FunctionAnswerResultGeneric (GenericAnswerResult<GameAnswer.FunctionAnswer>) instead.
 */
sealed class FunctionAnswerResult {
    data object Correct : FunctionAnswerResult()
    data class Wrong(val actualFunction: ChordFunction, val selectedFunction: ChordFunction) : FunctionAnswerResult()
}

/** Convert legacy FunctionAnswerResult to FunctionAnswerResultGeneric. */
fun FunctionAnswerResult.toGeneric(): FunctionAnswerResultGeneric = when (this) {
    is FunctionAnswerResult.Correct -> GenericAnswerResult.Correct()
    is FunctionAnswerResult.Wrong -> GenericAnswerResult.Wrong(
        actualAnswer = GameAnswer.FunctionAnswer(actualFunction),
        selectedAnswer = GameAnswer.FunctionAnswer(selectedFunction)
    )
}

/** Convert FunctionAnswerResultGeneric to legacy FunctionAnswerResult. */
fun FunctionAnswerResultGeneric.toLegacy(): FunctionAnswerResult = when (this) {
    is GenericAnswerResult.Correct -> FunctionAnswerResult.Correct
    is GenericAnswerResult.Wrong -> FunctionAnswerResult.Wrong(
        actualFunction = actualAnswer.function,
        selectedFunction = selectedAnswer.function
    )
}

/**
 * Legacy FunctionReviewScreenState for backwards compatibility.
 * @deprecated Use FunctionReviewState (GenericReviewScreenState<FunctionCard, GameAnswer.FunctionAnswer>) instead.
 */
data class FunctionReviewScreenState(
    val session: GenericReviewSession<FunctionCard>,
    val currentCard: FunctionCard? = null,
    val currentRootSemitones: Int? = null,
    val lastAnswer: FunctionAnswerResult? = null,
    val isPlaying: Boolean = false,
    val hasPlayedThisTrial: Boolean = false,
    val showingFeedback: Boolean = false,
    val inLearningMode: Boolean = false
) {
    /** Convert to generic state. */
    fun toGeneric(): FunctionReviewState = FunctionReviewState(
        session = session,
        currentCard = currentCard,
        currentRootSemitones = currentRootSemitones,
        lastAnswer = lastAnswer?.toGeneric(),
        isPlaying = isPlaying,
        hasPlayedThisTrial = hasPlayedThisTrial,
        showingFeedback = showingFeedback,
        inLearningMode = inLearningMode
    )

    // Computed properties for convenience
    val trialNumber: Int get() = minOf(session.currentTrial + 1, session.totalTrials)
    val totalTrials: Int get() = session.totalTrials
    val isComplete: Boolean get() = session.isComplete()
    val playbackMode: PlaybackMode get() = currentCard?.playbackMode ?: PlaybackMode.ARPEGGIATED
    val keyQuality: KeyQuality? get() = currentCard?.keyQuality

    /** Get all functions for the current key quality (for answer buttons). */
    fun getAllFunctionsForKey(): List<ChordFunction> =
        GameTypeConfig.FunctionGame.getAnswerOptions(session).map { it.function }
}

/**
 * Backwards compatible FunctionReviewScreen that accepts legacy FunctionReviewScreenState.
 */
@Composable
fun FunctionReviewScreen(
    state: FunctionReviewScreenState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordFunction) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayFunction: (ChordFunction) -> Unit = {},
    onNextClicked: () -> Unit = {},
    onAbortSession: () -> Unit = {}
) {
    FunctionReviewScreen(
        state = state.toGeneric(),
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        onPlayClicked = onPlayClicked,
        onAnswerClicked = onAnswerClicked,
        onTrialComplete = onTrialComplete,
        onAutoPlay = onAutoPlay,
        onSessionComplete = onSessionComplete,
        onPlayFunction = onPlayFunction,
        onNextClicked = onNextClicked,
        onAbortSession = onAbortSession
    )
}
