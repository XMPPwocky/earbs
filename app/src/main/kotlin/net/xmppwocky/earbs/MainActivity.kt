package net.xmppwocky.earbs

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.xmppwocky.earbs.audio.AudioEngine
import net.xmppwocky.earbs.audio.ChordBuilder
import net.xmppwocky.earbs.data.db.EarbsDatabase
import net.xmppwocky.earbs.data.repository.EarbsRepository
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.CardScore
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
    RESULTS,
    HISTORY
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: EarbsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate() - app starting")

        // Initialize database and repository
        val database = EarbsDatabase.getDatabase(applicationContext)
        repository = EarbsRepository(
            cardDao = database.cardDao(),
            reviewSessionDao = database.reviewSessionDao(),
            trialDao = database.trialDao(),
            sessionCardSummaryDao = database.sessionCardSummaryDao(),
            historyDao = database.historyDao()
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EarbsApp(repository)
                }
            }
        }
    }
}

@Composable
private fun EarbsApp(repository: EarbsRepository) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var session by remember { mutableStateOf<ReviewSession?>(null) }
    var dbSessionId by remember { mutableStateOf<Long?>(null) }
    var sessionResults by remember { mutableStateOf<List<CardScore>>(emptyList()) }
    var dueCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize on first composition
    LaunchedEffect(Unit) {
        Log.i(TAG, "Initializing app...")
        repository.initializeStartingDeck()
        dueCount = repository.getDueCount()
        isLoading = false
        Log.i(TAG, "Initialization complete, $dueCount cards due")
    }

    Log.d(TAG, "EarbsApp composing, screen: $currentScreen")

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                dueCount = dueCount,
                onStartReviewClicked = {
                    coroutineScope.launch {
                        Log.i(TAG, "Starting new review session")
                        val cards = repository.selectCardsForReview()

                        if (cards.size < 4) {
                            Log.w(TAG, "Not enough cards for session: ${cards.size}")
                            return@launch
                        }

                        val newSession = ReviewSession(cards)
                        val octave = newSession.octave
                        val sessionId = repository.startSession(octave)

                        session = newSession
                        dbSessionId = sessionId
                        session?.nextTrial()
                        currentScreen = Screen.REVIEW
                    }
                },
                onHistoryClicked = {
                    currentScreen = Screen.HISTORY
                }
            )
        }

        Screen.REVIEW -> {
            session?.let { activeSession ->
                ReviewSessionScreen(
                    session = activeSession,
                    onSessionComplete = { results ->
                        coroutineScope.launch {
                            Log.i(TAG, "Session complete, persisting results")

                            // Persist the session
                            val sessionId = dbSessionId
                            if (sessionId != null) {
                                repository.completeSession(
                                    sessionId = sessionId,
                                    trialRecords = activeSession.getTrialRecords(),
                                    cardScores = results
                                )
                            } else {
                                Log.e(TAG, "No session ID for persistence!")
                            }

                            // Update due count for home screen
                            dueCount = repository.getDueCount()

                            sessionResults = results
                            currentScreen = Screen.RESULTS
                        }
                    }
                )
            } ?: run {
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
                    dbSessionId = null
                    sessionResults = emptyList()
                    currentScreen = Screen.HOME
                }
            )
        }

        Screen.HISTORY -> {
            // TODO: Implement HistoryScreen
            // For now, go back to home
            LaunchedEffect(Unit) {
                currentScreen = Screen.HOME
            }
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
