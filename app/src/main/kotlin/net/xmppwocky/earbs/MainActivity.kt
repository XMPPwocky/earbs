package net.xmppwocky.earbs

import android.content.Context
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
import net.xmppwocky.earbs.model.ReviewSession
import net.xmppwocky.earbs.ui.AnswerResult
import net.xmppwocky.earbs.ui.HistoryScreen
import net.xmppwocky.earbs.ui.HomeScreen
import net.xmppwocky.earbs.ui.ResultsScreen
import net.xmppwocky.earbs.ui.ReviewScreen
import net.xmppwocky.earbs.ui.ReviewScreenState
import net.xmppwocky.earbs.ui.SessionResult
import kotlinx.coroutines.launch

private const val PREFS_NAME = "earbs_prefs"

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
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repository = EarbsRepository(
            cardDao = database.cardDao(),
            reviewSessionDao = database.reviewSessionDao(),
            trialDao = database.trialDao(),
            historyDao = database.historyDao(),
            prefs = prefs
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
    var sessionResult by remember { mutableStateOf<SessionResult?>(null) }
    var dueCount by remember { mutableIntStateOf(0) }
    var unlockedCount by remember { mutableIntStateOf(4) }
    var canUnlockMore by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize on first composition
    LaunchedEffect(Unit) {
        Log.i(TAG, "Initializing app...")
        repository.initializeStartingDeck()
        dueCount = repository.getDueCount()
        unlockedCount = repository.getUnlockedCount()
        canUnlockMore = repository.canUnlockMore()
        isLoading = false
        Log.i(TAG, "Initialization complete, $dueCount cards due, $unlockedCount unlocked")
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
                unlockedCount = unlockedCount,
                canUnlockMore = canUnlockMore,
                onStartReviewClicked = {
                    coroutineScope.launch {
                        Log.i(TAG, "Starting new review session")
                        val cards = repository.selectCardsForReview()

                        if (cards.isEmpty()) {
                            Log.w(TAG, "No cards available for session")
                            return@launch
                        }

                        val newSession = ReviewSession(cards)
                        val sessionId = repository.startSession()

                        session = newSession
                        dbSessionId = sessionId
                        currentScreen = Screen.REVIEW
                    }
                },
                onAddCardsClicked = {
                    coroutineScope.launch {
                        Log.i(TAG, "Unlocking next group of cards")
                        val unlocked = repository.unlockNextGroup()
                        if (unlocked) {
                            unlockedCount = repository.getUnlockedCount()
                            canUnlockMore = repository.canUnlockMore()
                            dueCount = repository.getDueCount()
                            Log.i(TAG, "Unlocked new cards, now $unlockedCount total, $dueCount due")
                        }
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
                    sessionId = dbSessionId ?: 0L,
                    repository = repository,
                    onSessionComplete = { result ->
                        coroutineScope.launch {
                            Log.i(TAG, "Session complete: ${result.correctCount}/${result.totalTrials}")

                            // Mark session complete
                            val sessionId = dbSessionId
                            if (sessionId != null) {
                                repository.completeSession(sessionId)
                            } else {
                                Log.e(TAG, "No session ID for persistence!")
                            }

                            // Update due count for home screen
                            dueCount = repository.getDueCount()

                            sessionResult = result
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
            sessionResult?.let { result ->
                ResultsScreen(
                    result = result,
                    onDoneClicked = {
                        Log.i(TAG, "Results acknowledged, returning to home")
                        session = null
                        dbSessionId = null
                        sessionResult = null
                        currentScreen = Screen.HOME
                    }
                )
            } ?: run {
                Log.w(TAG, "Results screen but no result, returning to home")
                currentScreen = Screen.HOME
            }
        }

        Screen.HISTORY -> {
            val sessions by repository.getSessionOverviews().collectAsState(initial = emptyList())
            val cards by repository.getAllCardsFlow().collectAsState(initial = emptyList())
            val cardStats by repository.getCardStats().collectAsState(initial = emptyList())

            HistoryScreen(
                sessions = sessions,
                cards = cards,
                cardStats = cardStats,
                onBackClicked = {
                    coroutineScope.launch {
                        dueCount = repository.getDueCount()
                        unlockedCount = repository.getUnlockedCount()
                        canUnlockMore = repository.canUnlockMore()
                    }
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}

@Composable
private fun ReviewSessionScreen(
    session: ReviewSession,
    sessionId: Long,
    repository: EarbsRepository,
    onSessionComplete: (SessionResult) -> Unit
) {
    var reviewState by remember {
        mutableStateOf(
            ReviewScreenState(
                session = session,
                currentCard = session.getCurrentCard()
            )
        )
    }
    val coroutineScope = rememberCoroutineScope()

    ReviewScreen(
        state = reviewState,
        onPlayClicked = {
            val currentCard = reviewState.currentCard ?: return@ReviewScreen

            Log.i(TAG, "Play button clicked for trial ${session.currentTrial + 1}")

            val rootSemitones = ChordBuilder.randomRootInOctave(currentCard.octave)
            val frequencies = ChordBuilder.buildChord(currentCard.chordType, rootSemitones)

            // Use the card's playback mode (not a user toggle)
            val playbackMode = currentCard.playbackMode
            Log.i(TAG, "Playing ${currentCard.displayName}, root: $rootSemitones, mode: $playbackMode")

            reviewState = reviewState.copy(
                isPlaying = true,
                lastAnswer = null
            )

            coroutineScope.launch {
                try {
                    AudioEngine.playChord(
                        frequencies = frequencies,
                        mode = playbackMode,
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

            val result = if (isCorrect) {
                Log.i(TAG, "CORRECT! User answered ${answeredType.displayName}")
                AnswerResult.Correct
            } else {
                Log.i(TAG, "WRONG! User answered ${answeredType.displayName}, actual was ${currentCard.chordType.displayName}")
                AnswerResult.Wrong(currentCard.chordType)
            }

            // Record trial and update FSRS immediately
            coroutineScope.launch {
                repository.recordTrialAndUpdateFsrs(sessionId, currentCard, isCorrect)
            }

            // Update session state
            session.recordAnswer(isCorrect)

            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true
            )
        },
        onTrialComplete = {
            val nextCard = session.getCurrentCard()
            Log.i(TAG, "Trial complete, next card: ${nextCard?.displayName}")

            reviewState = reviewState.copy(
                currentCard = nextCard,
                lastAnswer = null,
                hasPlayedThisTrial = false,
                showingFeedback = false
            )
        },
        onSessionComplete = {
            onSessionComplete(SessionResult(
                correctCount = session.correctCount,
                totalTrials = session.totalTrials
            ))
        }
    )
}
