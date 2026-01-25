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
import net.xmppwocky.earbs.ui.CardDetailsScreen
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
import net.xmppwocky.earbs.ui.DEFAULT_AUTO_ADVANCE_DELAY
import net.xmppwocky.earbs.ui.PREF_KEY_AUTO_ADVANCE_DELAY
import net.xmppwocky.earbs.ui.DEFAULT_LEARN_FROM_MISTAKES
import net.xmppwocky.earbs.ui.PREF_KEY_LEARN_FROM_MISTAKES
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.model.ChordFunction
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
    CARD_DETAILS,
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
    var selectedCardId by remember { mutableStateOf<String?>(null) }
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
                    },
                    onAbortSession = {
                        Log.i(TAG, "Chord type session aborted by user")
                        chordTypeSession = null
                        dbSessionId = null
                        currentScreen = Screen.HOME
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
                    },
                    onAbortSession = {
                        Log.i(TAG, "Function session aborted by user")
                        functionSession = null
                        dbSessionId = null
                        currentScreen = Screen.HOME
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
                    repository = repository,
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
                },
                onResetFsrs = { cardId ->
                    repository.resetFsrsState(cardId, GameType.CHORD_TYPE)
                },
                onCardClicked = { cardId ->
                    Log.i(TAG, "Card clicked: $cardId")
                    selectedCardId = cardId
                    currentScreen = Screen.CARD_DETAILS
                }
            )
        }

        Screen.CARD_DETAILS -> {
            selectedCardId?.let { cardId ->
                CardDetailsScreen(
                    cardId = cardId,
                    gameType = GameType.CHORD_TYPE,
                    repository = repository,
                    onBackClicked = {
                        currentScreen = Screen.HISTORY
                    }
                )
            } ?: run {
                Log.w(TAG, "Card details screen but no card selected, returning to history")
                currentScreen = Screen.HISTORY
            }
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
    onSessionComplete: (SessionResult) -> Unit,
    onAbortSession: () -> Unit
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

    // Read settings from prefs
    val autoAdvanceDelayMs = prefs.getInt(PREF_KEY_AUTO_ADVANCE_DELAY, DEFAULT_AUTO_ADVANCE_DELAY).toLong()
    val learnFromMistakesEnabled = prefs.getBoolean(PREF_KEY_LEARN_FROM_MISTAKES, DEFAULT_LEARN_FROM_MISTAKES)
    val playbackDuration = prefs.getInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION)

    // Play arbitrary chord type (for learning mode)
    fun playChordType(chordType: ChordType) {
        val rootSemitones = reviewState.currentRootSemitones ?: return
        val playbackMode = reviewState.currentCard?.playbackMode ?: return

        Log.i(TAG, "Playing chord type ${chordType.displayName} for learning mode")
        reviewState = reviewState.copy(isPlaying = true)

        coroutineScope.launch {
            try {
                val frequencies = ChordBuilder.buildChord(chordType, rootSemitones)
                AudioEngine.playChord(
                    frequencies = frequencies,
                    mode = playbackMode,
                    durationMs = playbackDuration,
                    chordType = chordType.displayName,
                    rootSemitones = rootSemitones
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error playing chord type", e)
            } finally {
                reviewState = reviewState.copy(isPlaying = false)
            }
        }
    }

    // Shared play function for both manual play and auto-play
    fun playCurrentChord() {
        val currentCard = reviewState.currentCard ?: return
        val rootSemitones = reviewState.currentRootSemitones ?: return

        Log.i(TAG, "Playing chord for trial ${session.currentTrial + 1}")

        val frequencies = ChordBuilder.buildChord(currentCard.chordType, rootSemitones)
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
    }

    ReviewScreen(
        state = reviewState,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        onPlayClicked = {
            Log.i(TAG, "Play button clicked for trial ${session.currentTrial + 1}")
            playCurrentChord()
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

            val enterLearningMode = learnFromMistakesEnabled && !isCorrect
            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true,
                inLearningMode = enterLearningMode
            )

            // Auto-play incorrect chord if entering learning mode
            if (enterLearningMode) {
                Log.i(TAG, "Entering learning mode - playing incorrect chord: ${answeredType.displayName}")
                playChordType(answeredType)
            }
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
                showingFeedback = false,
                inLearningMode = false
            )
        },
        onAutoPlay = {
            Log.i(TAG, "Auto-playing next chord")
            playCurrentChord()
        },
        onSessionComplete = {
            onSessionComplete(SessionResult(
                correctCount = session.correctCount,
                totalTrials = session.totalTrials,
                sessionId = sessionId,
                gameType = GameType.CHORD_TYPE.name
            ))
        },
        onPlayChordType = { chordType ->
            Log.i(TAG, "Learning mode: playing chord type ${chordType.displayName}")
            playChordType(chordType)
        },
        onNextClicked = {
            Log.i(TAG, "Learning mode: Next clicked")
            if (session.isComplete()) {
                Log.i(TAG, "Session complete after learning mode, navigating to results")
                onSessionComplete(SessionResult(
                    correctCount = session.correctCount,
                    totalTrials = session.totalTrials,
                    sessionId = sessionId,
                    gameType = GameType.CHORD_TYPE.name
                ))
            } else {
                Log.i(TAG, "Advancing to next trial")
                val nextCard = session.getCurrentCard()
                val nextRootSemitones = nextCard?.let { ChordBuilder.randomRootInOctave(it.octave) }

                reviewState = reviewState.copy(
                    currentCard = nextCard,
                    currentRootSemitones = nextRootSemitones,
                    lastAnswer = null,
                    hasPlayedThisTrial = false,
                    showingFeedback = false,
                    inLearningMode = false
                )

                // Auto-play next chord
                playCurrentChord()
            }
        },
        onAbortSession = onAbortSession
    )
}

@Composable
private fun FunctionReviewSessionScreen(
    session: FunctionReviewSession,
    sessionId: Long,
    repository: EarbsRepository,
    prefs: SharedPreferences,
    onSessionComplete: (SessionResult) -> Unit,
    onAbortSession: () -> Unit
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

    // Read settings from prefs
    val autoAdvanceDelayMs = prefs.getInt(PREF_KEY_AUTO_ADVANCE_DELAY, DEFAULT_AUTO_ADVANCE_DELAY).toLong()
    val learnFromMistakesEnabled = prefs.getBoolean(PREF_KEY_LEARN_FROM_MISTAKES, DEFAULT_LEARN_FROM_MISTAKES)
    val playbackDuration = prefs.getInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION)

    // Play arbitrary function (for learning mode)
    fun playFunction(function: ChordFunction) {
        val currentCard = reviewState.currentCard ?: return
        val rootSemitones = reviewState.currentRootSemitones ?: return
        val playbackMode = currentCard.playbackMode

        Log.i(TAG, "Playing function ${function.displayName} for learning mode")
        reviewState = reviewState.copy(isPlaying = true)

        coroutineScope.launch {
            try {
                // Build reference (tonic) chord and target chord for the selected function
                val referenceFreqs = ChordBuilder.buildTonicChord(rootSemitones, currentCard.keyQuality)
                val targetFreqs = ChordBuilder.buildDiatonicChord(rootSemitones, function)
                AudioEngine.playChordPair(
                    referenceFreqs = referenceFreqs,
                    targetFreqs = targetFreqs,
                    mode = playbackMode,
                    durationMs = playbackDuration,
                    pauseMs = 300,
                    keyQuality = currentCard.keyQuality.name,
                    function = function.displayName,
                    rootSemitones = rootSemitones
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error playing function", e)
            } finally {
                reviewState = reviewState.copy(isPlaying = false)
            }
        }
    }

    // Shared play function for both manual play and auto-play
    fun playCurrentChordPair() {
        val currentCard = reviewState.currentCard ?: return
        val rootSemitones = reviewState.currentRootSemitones ?: return

        Log.i(TAG, "Playing chord pair for function trial ${session.currentTrial + 1}")

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
    }

    FunctionReviewScreen(
        state = reviewState,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        onPlayClicked = {
            Log.i(TAG, "Play button clicked for function trial ${session.currentTrial + 1}")
            playCurrentChordPair()
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

            val enterLearningMode = learnFromMistakesEnabled && !isCorrect
            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true,
                inLearningMode = enterLearningMode
            )

            // Auto-play incorrect function if entering learning mode
            if (enterLearningMode) {
                Log.i(TAG, "Entering learning mode - playing incorrect function: ${answeredFunction.displayName}")
                playFunction(answeredFunction)
            }
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
                showingFeedback = false,
                inLearningMode = false
            )
        },
        onAutoPlay = {
            Log.i(TAG, "Auto-playing next function chord pair")
            playCurrentChordPair()
        },
        onSessionComplete = {
            onSessionComplete(SessionResult(
                correctCount = session.correctCount,
                totalTrials = session.totalTrials,
                sessionId = sessionId,
                gameType = GameType.CHORD_FUNCTION.name
            ))
        },
        onPlayFunction = { function ->
            Log.i(TAG, "Learning mode: playing function ${function.displayName}")
            playFunction(function)
        },
        onNextClicked = {
            Log.i(TAG, "Learning mode: Next clicked")
            if (session.isComplete()) {
                Log.i(TAG, "Function session complete after learning mode, navigating to results")
                onSessionComplete(SessionResult(
                    correctCount = session.correctCount,
                    totalTrials = session.totalTrials,
                    sessionId = sessionId,
                    gameType = GameType.CHORD_FUNCTION.name
                ))
            } else {
                Log.i(TAG, "Advancing to next function trial")
                val nextCard = session.getCurrentCard()
                val nextRootSemitones = nextCard?.let { ChordBuilder.randomRootInOctave(it.octave) }

                reviewState = reviewState.copy(
                    currentCard = nextCard,
                    currentRootSemitones = nextRootSemitones,
                    lastAnswer = null,
                    hasPlayedThisTrial = false,
                    showingFeedback = false,
                    inLearningMode = false
                )

                // Auto-play next chord pair
                playCurrentChordPair()
            }
        },
        onAbortSession = onAbortSession
    )
}
