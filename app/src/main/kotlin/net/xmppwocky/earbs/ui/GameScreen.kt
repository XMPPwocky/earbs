package net.xmppwocky.earbs.ui

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
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode

/**
 * Represents the result of an answer.
 */
sealed class AnswerResult {
    data object Correct : AnswerResult()
    data class Wrong(val actualType: ChordType) : AnswerResult()
}

/**
 * Game state for the UI.
 */
data class GameState(
    val currentChordType: ChordType? = null,
    val lastAnswer: AnswerResult? = null,
    val playbackMode: PlaybackMode = PlaybackMode.BLOCK,
    val isPlaying: Boolean = false
)

@Composable
fun GameScreen(
    gameState: GameState,
    onPlayClicked: () -> Unit,
    onAnswerClicked: (ChordType) -> Unit,
    onModeChanged: (PlaybackMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Earbs",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Mode Toggle
        ModeToggle(
            currentMode = gameState.playbackMode,
            onModeChanged = onModeChanged
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Play Button
        PlayButton(
            isPlaying = gameState.isPlaying,
            onClick = onPlayClicked
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feedback Area
        FeedbackArea(
            answerResult = gameState.lastAnswer,
            hasPlayedChord = gameState.currentChordType != null
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Answer Buttons
        AnswerButtons(
            enabled = gameState.currentChordType != null && !gameState.isPlaying,
            onAnswerClicked = onAnswerClicked
        )
    }
}

@Composable
private fun ModeToggle(
    currentMode: PlaybackMode,
    onModeChanged: (PlaybackMode) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Block",
            fontWeight = if (currentMode == PlaybackMode.BLOCK) FontWeight.Bold else FontWeight.Normal
        )

        Switch(
            checked = currentMode == PlaybackMode.ARPEGGIATED,
            onCheckedChange = { isArpeggiated ->
                onModeChanged(if (isArpeggiated) PlaybackMode.ARPEGGIATED else PlaybackMode.BLOCK)
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = "Arpeggiated",
            fontWeight = if (currentMode == PlaybackMode.ARPEGGIATED) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isPlaying,
        modifier = Modifier
            .size(150.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = if (isPlaying) "Playing..." else "Play",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FeedbackArea(
    answerResult: AnswerResult?,
    hasPlayedChord: Boolean
) {
    val (text, color) = when {
        answerResult == null && !hasPlayedChord -> "Tap Play to hear a chord" to Color.Gray
        answerResult == null -> "What chord was that?" to Color.Gray
        answerResult is AnswerResult.Correct -> "Correct!" to Color(0xFF4CAF50) // Green
        answerResult is AnswerResult.Wrong -> "Wrong — it was ${answerResult.actualType.displayName}" to Color(0xFFF44336) // Red
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
private fun AnswerButtons(
    enabled: Boolean,
    onAnswerClicked: (ChordType) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // First row: Major, Minor
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnswerButton(
                chordType = ChordType.MAJOR,
                enabled = enabled,
                onClick = { onAnswerClicked(ChordType.MAJOR) }
            )
            AnswerButton(
                chordType = ChordType.MINOR,
                enabled = enabled,
                onClick = { onAnswerClicked(ChordType.MINOR) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Second row: Sus2, Sus4
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnswerButton(
                chordType = ChordType.SUS2,
                enabled = enabled,
                onClick = { onAnswerClicked(ChordType.SUS2) }
            )
            AnswerButton(
                chordType = ChordType.SUS4,
                enabled = enabled,
                onClick = { onAnswerClicked(ChordType.SUS4) }
            )
        }
    }
}

@Composable
private fun AnswerButton(
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
