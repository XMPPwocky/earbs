package net.xmppwocky.earbs

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import net.xmppwocky.earbs.audio.AudioEngine
import net.xmppwocky.earbs.audio.ChordBuilder
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.ui.AnswerResult
import net.xmppwocky.earbs.ui.GameScreen
import net.xmppwocky.earbs.ui.GameState
import kotlinx.coroutines.launch

private const val TAG = "Earbs"
private const val DEFAULT_OCTAVE = 4

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate() - app starting")

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EarbsApp()
                }
            }
        }
    }
}

@Composable
private fun EarbsApp() {
    var gameState by remember { mutableStateOf(GameState()) }
    val coroutineScope = rememberCoroutineScope()

    Log.d(TAG, "EarbsApp composing, state: $gameState")

    GameScreen(
        gameState = gameState,
        onPlayClicked = {
            Log.i(TAG, "Play button clicked")

            // Pick a random chord type and root
            val chordType = ChordBuilder.randomChordType()
            val rootSemitones = ChordBuilder.randomRootInOctave(DEFAULT_OCTAVE)
            val frequencies = ChordBuilder.buildChord(chordType, rootSemitones)

            Log.i(TAG, "Generated chord: ${chordType.displayName}, root: $rootSemitones, octave: $DEFAULT_OCTAVE")

            // Update state: set current chord, clear feedback, mark as playing
            gameState = gameState.copy(
                currentChordType = chordType,
                lastAnswer = null,
                isPlaying = true
            )

            // Play the chord asynchronously
            coroutineScope.launch {
                try {
                    AudioEngine.playChord(
                        frequencies = frequencies,
                        mode = gameState.playbackMode,
                        durationMs = 500,
                        chordType = chordType.displayName,
                        rootSemitones = rootSemitones
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing chord", e)
                } finally {
                    gameState = gameState.copy(isPlaying = false)
                    Log.i(TAG, "Playback finished, ready for answer")
                }
            }
        },
        onAnswerClicked = { answeredType ->
            Log.i(TAG, "Answer clicked: ${answeredType.displayName}")

            val currentType = gameState.currentChordType
            if (currentType == null) {
                Log.w(TAG, "Answer clicked but no chord has been played")
                return@GameScreen
            }

            val isCorrect = answeredType == currentType
            val result = if (isCorrect) {
                Log.i(TAG, "CORRECT! User answered ${answeredType.displayName}")
                AnswerResult.Correct
            } else {
                Log.i(TAG, "WRONG! User answered ${answeredType.displayName}, actual was ${currentType.displayName}")
                AnswerResult.Wrong(currentType)
            }

            gameState = gameState.copy(lastAnswer = result)
        },
        onModeChanged = { newMode ->
            Log.i(TAG, "Playback mode changed to: $newMode")
            gameState = gameState.copy(playbackMode = newMode)
        }
    )
}
