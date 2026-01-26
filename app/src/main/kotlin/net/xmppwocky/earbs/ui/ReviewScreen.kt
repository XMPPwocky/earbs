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
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameTypeConfig
import net.xmppwocky.earbs.model.GenericReviewSession
import net.xmppwocky.earbs.ui.components.ButtonColorState
import net.xmppwocky.earbs.ui.components.PlaybackModeIndicator
import net.xmppwocky.earbs.ui.components.answerButtonColors
import net.xmppwocky.earbs.ui.theme.AppColors
import net.xmppwocky.earbs.ui.theme.Timing

/**
 * Type alias for chord type answer result.
 */
typealias ChordTypeAnswerResult = GenericAnswerResult<GameAnswer.ChordTypeAnswer>

/**
 * Type alias for chord type review screen state.
 */
typealias ChordTypeReviewState = GenericReviewScreenState<Card, GameAnswer.ChordTypeAnswer>

/**
 * Simple result data for session completion.
 */
data class SessionResult(
    val correctCount: Int,
    val totalTrials: Int,
    val sessionId: Long,
    val gameType: String
)

/**
 * Extension to get chord types from session for answer buttons.
 */
fun ChordTypeReviewState.getChordTypes(): List<ChordType> =
    currentCard?.let { card ->
        GameTypeConfig.ChordTypeGame.getAnswerOptions(card, session).map { it.chordType }
    } ?: emptyList()

/**
 * Chord type review screen - thin wrapper around GenericReviewScreen.
 */
@Composable
fun ReviewScreen(
    state: ChordTypeReviewState,
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
    GenericReviewScreen(
        state = state,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        cardInfoContent = { card -> ChordTypeCardInfo(card = card) },
        modeIndicatorContent = { PlaybackModeIndicator(mode = state.playbackMode) },
        feedbackContent = { answerResult, hasPlayedThisTrial ->
            ChordTypeFeedbackArea(
                answerResult = answerResult,
                hasPlayedThisTrial = hasPlayedThisTrial
            )
        },
        answerButtonsContent = { enabled ->
            ChordTypeAnswerButtons(
                chordTypes = state.getChordTypes(),
                enabled = enabled,
                isLearningMode = state.inLearningMode,
                answerResult = state.lastAnswer,
                onAnswerClicked = onAnswerClicked,
                onPlayChordType = onPlayChordType
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

// ========== Chord Type Specific Composables ==========

@Composable
private fun ChordTypeCardInfo(card: Card?) {
    Text(
        text = card?.let { "Chord in octave ${it.octave}" } ?: "Loading...",
        fontSize = 16.sp,
        color = Color.Gray
    )
}

@Composable
private fun ChordTypeFeedbackArea(
    answerResult: ChordTypeAnswerResult?,
    hasPlayedThisTrial: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedThisTrial -> "Tap Play to hear the chord" to Color.Gray
        answerResult == null -> "What chord type is this?" to Color.Gray
        answerResult is GenericAnswerResult.Correct -> "Correct!" to AppColors.Success
        answerResult is GenericAnswerResult.Wrong -> {
            val actualType = answerResult.actualAnswer.chordType
            "Wrong - it was ${actualType.displayName}" to AppColors.Error
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
private fun ChordTypeAnswerButtons(
    chordTypes: List<ChordType>,
    enabled: Boolean,
    isLearningMode: Boolean = false,
    answerResult: ChordTypeAnswerResult? = null,
    onAnswerClicked: (ChordType) -> Unit,
    onPlayChordType: (ChordType) -> Unit = {}
) {
    // In learning mode, clicking plays that chord instead of answering
    val onClick: (ChordType) -> Unit = if (isLearningMode) onPlayChordType else onAnswerClicked

    // Compute color state for a button based on answer result
    fun getColorState(chordType: ChordType): ButtonColorState {
        return when {
            answerResult !is GenericAnswerResult.Wrong -> ButtonColorState.DEFAULT
            chordType == answerResult.selectedAnswer.chordType -> ButtonColorState.WRONG
            chordType == answerResult.actualAnswer.chordType -> ButtonColorState.CORRECT
            else -> ButtonColorState.INACTIVE
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display buttons in rows of 2
        chordTypes.chunked(2).forEachIndexed { index, rowChordTypes ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowChordTypes.forEach { chordType ->
                    ChordTypeAnswerButton(
                        chordType = chordType,
                        enabled = enabled,
                        colorState = getColorState(chordType),
                        onClick = { onClick(chordType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChordTypeAnswerButton(
    chordType: ChordType,
    enabled: Boolean,
    colorState: ButtonColorState = ButtonColorState.DEFAULT,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(width = 140.dp, height = 60.dp)
            .testTag("answer_button_${chordType.name}_${colorState.name}"),
        colors = answerButtonColors(colorState)
    ) {
        Text(
            text = chordType.displayName,
            fontSize = 18.sp
        )
    }
}

// ========== Backwards Compatibility ==========

/**
 * Legacy AnswerResult type for backwards compatibility.
 * @deprecated Use ChordTypeAnswerResult (GenericAnswerResult<GameAnswer.ChordTypeAnswer>) instead.
 */
sealed class AnswerResult {
    data object Correct : AnswerResult()
    data class Wrong(val actualType: ChordType, val selectedType: ChordType) : AnswerResult()
}

/** Convert legacy AnswerResult to ChordTypeAnswerResult. */
fun AnswerResult.toGeneric(): ChordTypeAnswerResult = when (this) {
    is AnswerResult.Correct -> GenericAnswerResult.Correct()
    is AnswerResult.Wrong -> GenericAnswerResult.Wrong(
        actualAnswer = GameAnswer.ChordTypeAnswer(actualType),
        selectedAnswer = GameAnswer.ChordTypeAnswer(selectedType)
    )
}

/** Convert ChordTypeAnswerResult to legacy AnswerResult. */
fun ChordTypeAnswerResult.toLegacy(): AnswerResult = when (this) {
    is GenericAnswerResult.Correct -> AnswerResult.Correct
    is GenericAnswerResult.Wrong -> AnswerResult.Wrong(
        actualType = actualAnswer.chordType,
        selectedType = selectedAnswer.chordType
    )
}

/**
 * Legacy ReviewScreenState for backwards compatibility.
 * @deprecated Use ChordTypeReviewState (GenericReviewScreenState<Card, GameAnswer.ChordTypeAnswer>) instead.
 */
data class ReviewScreenState(
    val session: GenericReviewSession<Card>,
    val currentCard: Card? = null,
    val currentRootSemitones: Int? = null,
    val lastAnswer: AnswerResult? = null,
    val isPlaying: Boolean = false,
    val hasPlayedThisTrial: Boolean = false,
    val showingFeedback: Boolean = false,
    val inLearningMode: Boolean = false
) {
    /** Convert to generic state. */
    fun toGeneric(): ChordTypeReviewState = ChordTypeReviewState(
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
    val playbackMode get() = currentCard?.playbackMode ?: net.xmppwocky.earbs.audio.PlaybackMode.ARPEGGIATED

    /** Get chord types in this session (for answer buttons). */
    fun getChordTypes(): List<ChordType> = currentCard?.let { card ->
        GameTypeConfig.ChordTypeGame.getAnswerOptions(card, session).map { it.chordType }
    } ?: emptyList()
}

/**
 * Backwards compatible ReviewScreen that accepts legacy ReviewScreenState.
 */
@Composable
fun ReviewScreen(
    state: ReviewScreenState,
    autoAdvanceDelayMs: Long = Timing.FEEDBACK_DELAY_MS,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordType) -> Unit,
    onTrialComplete: () -> Unit,
    onAutoPlay: () -> Unit = {},
    onSessionComplete: () -> Unit,
    onPlayChordType: (ChordType) -> Unit = {},
    onNextClicked: () -> Unit = {},
    onAbortSession: () -> Unit = {}
) {
    ReviewScreen(
        state = state.toGeneric(),
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        onPlayClicked = onPlayClicked,
        onAnswerClicked = onAnswerClicked,
        onTrialComplete = onTrialComplete,
        onAutoPlay = onAutoPlay,
        onSessionComplete = onSessionComplete,
        onPlayChordType = onPlayChordType,
        onNextClicked = onNextClicked,
        onAbortSession = onAbortSession
    )
}
