package net.xmppwocky.earbs

import android.content.Context
import android.content.SharedPreferences
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
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.repository.EarbsRepository
import net.xmppwocky.earbs.model.FunctionReviewSession
import net.xmppwocky.earbs.model.ReviewSession
import net.xmppwocky.earbs.ui.AnswerResult
import net.xmppwocky.earbs.ui.FunctionAnswerResult
import net.xmppwocky.earbs.ui.FunctionReviewScreen
import net.xmppwocky.earbs.ui.FunctionReviewScreenState
import net.xmppwocky.earbs.ui.HistoryScreen
import net.xmppwocky.earbs.ui.HomeScreen
import net.xmppwocky.earbs.ui.ResultsScreen
import net.xmppwocky.earbs.ui.ReviewScreen
import net.xmppwocky.earbs.ui.ReviewScreenState
import net.xmppwocky.earbs.ui.SessionResult
import net.xmppwocky.earbs.ui.SettingsScreen
import net.xmppwocky.earbs.ui.DEFAULT_PLAYBACK_DURATION
import net.xmppwocky.earbs.ui.PREF_KEY_PLAYBACK_DURATION
import kotlinx.coroutines.launch

private const val PREFS_NAME = "earbs_prefs"

private const val TAG = "Earbs"

/**
 * Navigation state for the app.
 */
enum class Screen {
    HOME,
    REVIEW,
    FUNCTION_REVIEW,
    RESULTS,
    HISTORY,
    SETTINGS
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
            functionCardDao = database.functionCardDao(),
            fsrsStateDao = database.fsrsStateDao(),
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
                    EarbsApp(repository, prefs)
                }
            }
        }
    }
}

@Composable
private fun EarbsApp(repository: EarbsRepository, prefs: SharedPreferences) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedGameMode by remember { mutableStateOf(GameType.CHORD_TYPE) }

    // Chord type game state
    var chordTypeSession by remember { mutableStateOf<ReviewSession?>(null) }
    var chordTypeDueCount by remember { mutableIntStateOf(0) }
    var chordTypeUnlockedCount by remember { mutableIntStateOf(4) }
    var canUnlockMoreChordTypes by remember { mutableStateOf(true) }

    // Function game state
    var functionSession by remember { mutableStateOf<FunctionReviewSession?>(null) }
    var functionDueCount by remember { mutableIntStateOf(0) }
    var functionUnlockedCount by remember { mutableIntStateOf(0) }
    var canUnlockMoreFunctions by remember { mutableStateOf(true) }

    // Shared state
    var dbSessionId by remember { mutableStateOf<Long?>(null) }
    var sessionResult by remember { mutableStateOf<SessionResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize on first composition
    LaunchedEffect(Unit) {
        Log.i(TAG, "Initializing app...")

        // Initialize chord type game
        repository.initializeStartingDeck()
        chordTypeDueCount = repository.getDueCount()
        chordTypeUnlockedCount = repository.getUnlockedCount()
        canUnlockMoreChordTypes = repository.canUnlockMore()

        // Initialize function game (if not already)
        repository.initializeFunctionStartingDeck()
        functionDueCount = repository.getFunctionDueCount()
        functionUnlockedCount = repository.getFunctionUnlockedCount()
        canUnlockMoreFunctions = repository.canUnlockMoreFunctions()

        isLoading = false
        Log.i(TAG, "Initialization complete")
        Log.i(TAG, "  Chord type: $chordTypeDueCount due, $chordTypeUnlockedCount unlocked")
        Log.i(TAG, "  Function: $functionDueCount due, $functionUnlockedCount unlocked")
    }

    Log.d(TAG, "EarbsApp composing, screen: $currentScreen, gameMode: $selectedGameMode")

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
                selectedGameMode = selectedGameMode,
                onGameModeChanged = { selectedGameMode = it },
                chordTypeDueCount = chordTypeDueCount,
                chordTypeUnlockedCount = chordTypeUnlockedCount,
                canUnlockMoreChordTypes = canUnlockMoreChordTypes,
                functionDueCount = functionDueCount,
                functionUnlockedCount = functionUnlockedCount,
                canUnlockMoreFunctions = canUnlockMoreFunctions,
                onStartReviewClicked = {
                    coroutineScope.launch {
                        when (selectedGameMode) {
                            GameType.CHORD_TYPE -> {
                                Log.i(TAG, "Starting chord type review session")
                                val cards = repository.selectCardsForReview()
                                if (cards.isEmpty()) {
                                    Log.w(TAG, "No cards available for session")
                                    return@launch
                                }
                                chordTypeSession = ReviewSession(cards)
                                dbSessionId = repository.startSession(GameType.CHORD_TYPE)
                                currentScreen = Screen.REVIEW
                            }
                            GameType.CHORD_FUNCTION -> {
                                Log.i(TAG, "Starting function review session")
                                val cards = repository.selectFunctionCardsForReview()
                                if (cards.isEmpty()) {
                                    Log.w(TAG, "No function cards available for session")
                                    return@launch
                                }
                                functionSession = FunctionReviewSession(cards)
                                dbSessionId = repository.startSession(GameType.CHORD_FUNCTION)
                                currentScreen = Screen.FUNCTION_REVIEW
                            }
                        }
                    }
                },
                onAddCardsClicked = {
                    coroutineScope.launch {
                        when (selectedGameMode) {
                            GameType.CHORD_TYPE -> {
                                Log.i(TAG, "Unlocking next chord type group")
                                if (repository.unlockNextGroup()) {
                                    chordTypeUnlockedCount = repository.getUnlockedCount()
                                    canUnlockMoreChordTypes = repository.canUnlockMore()
                                    chordTypeDueCount = repository.getDueCount()
                                }
                            }
                            GameType.CHORD_FUNCTION -> {
                                Log.i(TAG, "Unlocking next function group")
                                if (repository.unlockNextFunctionGroup()) {
                                    functionUnlockedCount = repository.getFunctionUnlockedCount()
                                    canUnlockMoreFunctions = repository.canUnlockMoreFunctions()
                                    functionDueCount = repository.getFunctionDueCount()
                                }
                            }
                        }
                    }
                },
                onHistoryClicked = {
                    currentScreen = Screen.HISTORY
                },
                onSettingsClicked = {
                    currentScreen = Screen.SETTINGS
                }
            )
        }

        Screen.REVIEW -> {
            chordTypeSession?.let { activeSession ->
                ChordTypeReviewSessionScreen(
                    session = activeSession,
                    sessionId = dbSessionId ?: 0L,
                    repository = repository,
                    prefs = prefs,
                    onSessionComplete = { result ->
                        coroutineScope.launch {
                            Log.i(TAG, "Chord type session complete: ${result.correctCount}/${result.totalTrials}")
                            dbSessionId?.let { repository.completeSession(it) }
                            chordTypeDueCount = repository.getDueCount()
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

        Screen.FUNCTION_REVIEW -> {
            functionSession?.let { activeSession ->
                FunctionReviewSessionScreen(
                    session = activeSession,
                    sessionId = dbSessionId ?: 0L,
                    repository = repository,
                    prefs = prefs,
                    onSessionComplete = { result ->
                        coroutineScope.launch {
                            Log.i(TAG, "Function session complete: ${result.correctCount}/${result.totalTrials}")
                            dbSessionId?.let { repository.completeSession(it) }
                            functionDueCount = repository.getFunctionDueCount()
                            sessionResult = result
                            currentScreen = Screen.RESULTS
                        }
                    }
                )
            } ?: run {
                Log.w(TAG, "Function review screen but no session, returning to home")
                currentScreen = Screen.HOME
            }
        }

        Screen.RESULTS -> {
            sessionResult?.let { result ->
                ResultsScreen(
                    result = result,
                    onDoneClicked = {
                        Log.i(TAG, "Results acknowledged, returning to home")
                        chordTypeSession = null
                        functionSession = null
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
            val cards by repository.getAllCardsWithFsrsFlow().collectAsState(initial = emptyList())
            val cardStats by repository.getCardStats().collectAsState(initial = emptyList())

            HistoryScreen(
                sessions = sessions,
                cards = cards,
                cardStats = cardStats,
                onBackClicked = {
                    coroutineScope.launch {
                        chordTypeDueCount = repository.getDueCount()
                        chordTypeUnlockedCount = repository.getUnlockedCount()
                        canUnlockMoreChordTypes = repository.canUnlockMore()
                        functionDueCount = repository.getFunctionDueCount()
                        functionUnlockedCount = repository.getFunctionUnlockedCount()
                        canUnlockMoreFunctions = repository.canUnlockMoreFunctions()
                    }
                    currentScreen = Screen.HOME
                },
                onLoadTrials = { sessionId ->
                    repository.getTrialsForSession(sessionId)
                },
                onLoadChordConfusion = { octave ->
                    repository.getChordTypeConfusionData(octave)
                },
                onLoadFunctionConfusion = { keyQuality ->
                    repository.getFunctionConfusionData(keyQuality)
                }
            )
        }

        Screen.SETTINGS -> {
            SettingsScreen(
                prefs = prefs,
                onBackClicked = {
                    currentScreen = Screen.HOME
                }
            )
        }
    }
}

@Composable
private fun ChordTypeReviewSessionScreen(
    session: ReviewSession,
    sessionId: Long,
    repository: EarbsRepository,
    prefs: SharedPreferences,
    onSessionComplete: (SessionResult) -> Unit
) {
    val initialCard = session.getCurrentCard()
    var reviewState by remember {
        mutableStateOf(
            ReviewScreenState(
                session = session,
                currentCard = initialCard,
                currentRootSemitones = initialCard?.let { ChordBuilder.randomRootInOctave(it.octave) }
            )
        )
    }
    val coroutineScope = rememberCoroutineScope()

    ReviewScreen(
        state = reviewState,
        onPlayClicked = {
            val currentCard = reviewState.currentCard ?: return@ReviewScreen
            val rootSemitones = reviewState.currentRootSemitones ?: return@ReviewScreen

            Log.i(TAG, "Play button clicked for trial ${session.currentTrial + 1}")

            val frequencies = ChordBuilder.buildChord(currentCard.chordType, rootSemitones)
            val playbackMode = currentCard.playbackMode
            Log.i(TAG, "Playing ${currentCard.displayName}, root: $rootSemitones, mode: $playbackMode")

            reviewState = reviewState.copy(
                isPlaying = true,
                lastAnswer = null
            )

            coroutineScope.launch {
                try {
                    val playbackDuration = prefs.getInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION)
                    AudioEngine.playChord(
                        frequencies = frequencies,
                        mode = playbackMode,
                        durationMs = playbackDuration,
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

            coroutineScope.launch {
                repository.recordTrialAndUpdateFsrs(sessionId, currentCard, isCorrect, answeredType)
            }

            session.recordAnswer(isCorrect)

            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true
            )
        },
        onTrialComplete = {
            val nextCard = session.getCurrentCard()
            val nextRootSemitones = nextCard?.let { ChordBuilder.randomRootInOctave(it.octave) }
            Log.i(TAG, "Trial complete, next card: ${nextCard?.displayName}, root: $nextRootSemitones")

            reviewState = reviewState.copy(
                currentCard = nextCard,
                currentRootSemitones = nextRootSemitones,
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

@Composable
private fun FunctionReviewSessionScreen(
    session: FunctionReviewSession,
    sessionId: Long,
    repository: EarbsRepository,
    prefs: SharedPreferences,
    onSessionComplete: (SessionResult) -> Unit
) {
    val initialCard = session.getCurrentCard()
    var reviewState by remember {
        mutableStateOf(
            FunctionReviewScreenState(
                session = session,
                currentCard = initialCard,
                currentRootSemitones = initialCard?.let { ChordBuilder.randomRootInOctave(it.octave) }
            )
        )
    }
    val coroutineScope = rememberCoroutineScope()

    FunctionReviewScreen(
        state = reviewState,
        onPlayClicked = {
            val currentCard = reviewState.currentCard ?: return@FunctionReviewScreen
            val rootSemitones = reviewState.currentRootSemitones ?: return@FunctionReviewScreen

            Log.i(TAG, "Play button clicked for function trial ${session.currentTrial + 1}")

            // Build reference (tonic) chord and target chord
            val referenceFreqs = ChordBuilder.buildTonicChord(rootSemitones, currentCard.keyQuality)
            val targetFreqs = ChordBuilder.buildDiatonicChord(rootSemitones, currentCard.function)
            val playbackMode = currentCard.playbackMode

            Log.i(TAG, "Playing function ${currentCard.displayName}, root: $rootSemitones, mode: $playbackMode")

            reviewState = reviewState.copy(
                isPlaying = true,
                lastAnswer = null
            )

            coroutineScope.launch {
                try {
                    val playbackDuration = prefs.getInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION)
                    AudioEngine.playChordPair(
                        referenceFreqs = referenceFreqs,
                        targetFreqs = targetFreqs,
                        mode = playbackMode,
                        durationMs = playbackDuration,
                        pauseMs = 300,
                        keyQuality = currentCard.keyQuality.name,
                        function = currentCard.function.displayName,
                        rootSemitones = rootSemitones
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing chord pair", e)
                } finally {
                    reviewState = reviewState.copy(
                        isPlaying = false,
                        hasPlayedThisTrial = true
                    )
                    Log.i(TAG, "Chord pair playback finished, ready for answer")
                }
            }
        },
        onAnswerClicked = { answeredFunction ->
            val currentCard = reviewState.currentCard ?: return@FunctionReviewScreen

            Log.i(TAG, "Function answer clicked: ${answeredFunction.displayName}")

            val isCorrect = answeredFunction == currentCard.function

            val result = if (isCorrect) {
                Log.i(TAG, "CORRECT! User answered ${answeredFunction.displayName}")
                FunctionAnswerResult.Correct
            } else {
                Log.i(TAG, "WRONG! User answered ${answeredFunction.displayName}, actual was ${currentCard.function.displayName}")
                FunctionAnswerResult.Wrong(currentCard.function)
            }

            coroutineScope.launch {
                repository.recordFunctionTrialAndUpdateFsrs(sessionId, currentCard, isCorrect, answeredFunction)
            }

            session.recordAnswer(isCorrect)

            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true
            )
        },
        onTrialComplete = {
            val nextCard = session.getCurrentCard()
            val nextRootSemitones = nextCard?.let { ChordBuilder.randomRootInOctave(it.octave) }
            Log.i(TAG, "Function trial complete, next card: ${nextCard?.displayName}, root: $nextRootSemitones")

            reviewState = reviewState.copy(
                currentCard = nextCard,
                currentRootSemitones = nextRootSemitones,
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
