package net.xmppwocky.earbs.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.ui.theme.AppColors

private const val TAG = "ReviewComponents"

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
 * Computes button colors based on the button's color state.
 */
@Composable
fun answerButtonColors(colorState: ButtonColorState): ButtonColors {
    return when (colorState) {
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
}

/**
 * Generic progress indicator for review screens.
 */
@Composable
fun ReviewProgressIndicator(
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

/**
 * Generic play button for review screens.
 */
@Composable
fun ReviewPlayButton(
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

/**
 * Playback mode indicator for review screens.
 */
@Composable
fun PlaybackModeIndicator(mode: PlaybackMode) {
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

/**
 * Compact playback mode indicator (for screens with multiple indicators).
 */
@Composable
fun CompactPlaybackModeIndicator(mode: PlaybackMode) {
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

/**
 * Abort session confirmation dialog.
 */
@Composable
fun AbortSessionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit Review?") },
        text = { Text("Your progress in this session will be lost.") },
        confirmButton = {
            TextButton(onClick = {
                Log.i(TAG, "User confirmed abort session")
                onConfirm()
            }) {
                Text("Exit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue")
            }
        }
    )
}
