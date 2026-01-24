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
import net.xmppwocky.earbs.model.CardScore
import net.xmppwocky.earbs.model.Deck
import net.xmppwocky.earbs.model.ReviewSession
import net.xmppwocky.earbs.ui.AnswerResult
import net.xmppwocky.earbs.ui.HomeScreen
import net.xmppwocky.earbs.ui.ResultsScreen
import net.xmppwocky.earbs.ui.ReviewScreen
import net.xmppwocky.earbs.ui.ReviewScreenState
import kotlinx.coroutines.launch

private const val TAG = "Earbs"

/**
 * Navigation state for the app.
 */
enum class Screen {
    HOME,
    REVIEW,
    RESULTS
}

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
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var session by remember { mutableStateOf<ReviewSession?>(null) }
    var sessionResults by remember { mutableStateOf<List<CardScore>>(emptyList()) }

    Log.d(TAG, "EarbsApp composing, screen: $currentScreen")

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                onStartReviewClicked = {
                    Log.i(TAG, "Starting new review session")
                    session = ReviewSession(Deck.STARTING_CARDS)
                    session?.nextTrial() // Start first trial
                    currentScreen = Screen.REVIEW
                }
            )
        }

        Screen.REVIEW -> {
            session?.let { activeSession ->
                ReviewSessionScreen(
                    session = activeSession,
                    onSessionComplete = { results ->
                        Log.i(TAG, "Session complete, showing results")
                        sessionResults = results
                        currentScreen = Screen.RESULTS
                    }
                )
            } ?: run {
                // Session is null, go back to home
                Log.w(TAG, "Review screen but no session, returning to home")
                currentScreen = Screen.HOME
            }
        }

        Screen.RESULTS -> {
            ResultsScreen(
                results = sessionResults,
                onDoneClicked = {
                    Log.i(TAG, "Results acknowledged, returning to home")
                    session = null
                    sessionResults = emptyList()
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}

@Composable
private fun ReviewSessionScreen(
    session: ReviewSession,
    onSessionComplete: (List<CardScore>) -> Unit
) {
    var reviewState by remember {
        mutableStateOf(
            ReviewScreenState(
                session = session,
                currentCard = session.currentCard
            )
        )
    }
    val coroutineScope = rememberCoroutineScope()

    ReviewScreen(
        state = reviewState,
        onPlayClicked = {
            val currentCard = reviewState.currentCard ?: return@ReviewScreen

            Log.i(TAG, "Play button clicked for trial ${session.currentTrial}")

            // Generate random root in card's octave
            val rootSemitones = ChordBuilder.randomRootInOctave(currentCard.octave)
            val frequencies = ChordBuilder.buildChord(currentCard.chordType, rootSemitones)

            Log.i(TAG, "Playing ${currentCard.displayName}, root: $rootSemitones")

            reviewState = reviewState.copy(
                isPlaying = true,
                lastAnswer = null
            )

            coroutineScope.launch {
                try {
                    AudioEngine.playChord(
                        frequencies = frequencies,
                        mode = reviewState.playbackMode,
                        durationMs = 500,
                        chordType = currentCard.chordType.displayName,
                        rootSemitones = rootSemitones
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing chord", e)
                } finally {
                    reviewState = reviewState.copy(
                        isPlaying = false,
                        hasPlayedThisTrial = true
                    )
                    Log.i(TAG, "Playback finished, ready for answer")
                }
            }
        },
        onAnswerClicked = { answeredType ->
            val currentCard = reviewState.currentCard ?: return@ReviewScreen

            Log.i(TAG, "Answer clicked: ${answeredType.displayName}")

            val isCorrect = answeredType == currentCard.chordType
            session.recordAnswer(isCorrect)

            val result = if (isCorrect) {
                Log.i(TAG, "CORRECT! User answered ${answeredType.displayName}")
                AnswerResult.Correct
            } else {
                Log.i(TAG, "WRONG! User answered ${answeredType.displayName}, actual was ${currentCard.chordType.displayName}")
                AnswerResult.Wrong(currentCard.chordType)
            }

            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true
            )
        },
        onModeChanged = { newMode ->
            Log.i(TAG, "Playback mode changed to: $newMode")
            reviewState = reviewState.copy(playbackMode = newMode)
        },
        onTrialComplete = {
            // Advance to next trial
            val nextCard = session.nextTrial()
            Log.i(TAG, "Trial complete, next card: ${nextCard?.displayName}")

            reviewState = reviewState.copy(
                currentCard = nextCard,
                lastAnswer = null,
                hasPlayedThisTrial = false,
                showingFeedback = false
            )
        },
        onSessionComplete = onSessionComplete
    )
}
