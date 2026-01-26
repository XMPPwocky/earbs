package net.xmppwocky.earbs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.xmppwocky.earbs.audio.AudioEngine
import net.xmppwocky.earbs.audio.ChordBuilder
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.ProgressionStrategy
import net.xmppwocky.earbs.data.backup.DatabaseBackupManager
import net.xmppwocky.earbs.data.db.EarbsDatabase
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.repository.EarbsRepository
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GenericReviewSession
import net.xmppwocky.earbs.model.ProgressionCard
import net.xmppwocky.earbs.model.ProgressionType
import net.xmppwocky.earbs.ui.AnswerResult
import net.xmppwocky.earbs.ui.CardDetailsScreen
import net.xmppwocky.earbs.ui.DEFAULT_AUTO_ADVANCE_DELAY
import net.xmppwocky.earbs.ui.DEFAULT_LEARN_FROM_MISTAKES
import net.xmppwocky.earbs.ui.DEFAULT_PLAYBACK_DURATION
import net.xmppwocky.earbs.ui.FunctionAnswerResult
import net.xmppwocky.earbs.ui.FunctionReviewScreen
import net.xmppwocky.earbs.ui.FunctionReviewScreenState
import net.xmppwocky.earbs.ui.GenericAnswerResult
import net.xmppwocky.earbs.ui.HistoryScreen
import net.xmppwocky.earbs.ui.ProgressionReviewScreen
import net.xmppwocky.earbs.ui.ProgressionReviewState
import net.xmppwocky.earbs.ui.HomeScreen
import net.xmppwocky.earbs.ui.PREF_KEY_AUTO_ADVANCE_DELAY
import net.xmppwocky.earbs.ui.PREF_KEY_LEARN_FROM_MISTAKES
import net.xmppwocky.earbs.ui.PREF_KEY_PLAYBACK_DURATION
import net.xmppwocky.earbs.ui.ResultsScreen
import net.xmppwocky.earbs.ui.ReviewScreen
import net.xmppwocky.earbs.ui.ReviewScreenState
import net.xmppwocky.earbs.ui.SessionResult
import net.xmppwocky.earbs.ui.SettingsScreen

private const val PREFS_NAME = "earbs_prefs"

private const val TAG = "Earbs"

/**
 * Navigation state for the app.
 */
enum class Screen {
    HOME,
    REVIEW,
    FUNCTION_REVIEW,
    PROGRESSION_REVIEW,
    RESULTS,
    HISTORY,
    CARD_DETAILS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private lateinit var repository: EarbsRepository
    private lateinit var backupManager: DatabaseBackupManager
    private lateinit var prefs: SharedPreferences

    // Mutable state for restore confirmation dialog
    private var showRestoreConfirmation = mutableStateOf(false)

    // SAF launcher for creating backup file
    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            Log.i(TAG, "Backup location selected: $uri")
            performBackup(uri)
        } else {
            Log.i(TAG, "Backup cancelled by user")
        }
    }

    // SAF launcher for opening backup file to restore
    private val openBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.i(TAG, "Restore file selected: $uri")
            performRestore(uri)
        } else {
            Log.i(TAG, "Restore cancelled by user")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity.onCreate() - app starting")

        // Initialize backup manager
        backupManager = DatabaseBackupManager(applicationContext)

        // Initialize database and repository
        val database = EarbsDatabase.getDatabase(applicationContext)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repository = EarbsRepository(
            cardDao = database.cardDao(),
            functionCardDao = database.functionCardDao(),
            progressionCardDao = database.progressionCardDao(),
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
                    EarbsApp(
                        repository = repository,
                        prefs = prefs,
                        showRestoreConfirmation = showRestoreConfirmation.value,
                        onBackupClicked = { startBackup() },
                        onRestoreClicked = { showRestoreConfirmation.value = true },
                        onRestoreConfirmed = {
                            showRestoreConfirmation.value = false
                            startRestore()
                        },
                        onRestoreCancelled = { showRestoreConfirmation.value = false }
                    )
                }
            }
        }
    }

    private fun startBackup() {
        Log.i(TAG, "Starting backup process")
        val filename = backupManager.generateBackupFilename()
        createBackupLauncher.launch(filename)
    }

    private fun performBackup(uri: Uri) {
        lifecycleScope.launch {
            Log.i(TAG, "Performing backup to $uri")
            when (val result = backupManager.createBackup(uri)) {
                is DatabaseBackupManager.Result.Success -> {
                    Log.i(TAG, "Backup completed successfully")
                    Toast.makeText(this@MainActivity, "Backup saved successfully", Toast.LENGTH_SHORT).show()
                    // Re-initialize repository after database was closed for backup
                    reinitializeRepository()
                }
                is DatabaseBackupManager.Result.Error -> {
                    Log.e(TAG, "Backup failed: ${result.message}")
                    Toast.makeText(this@MainActivity, "Backup failed: ${result.message}", Toast.LENGTH_LONG).show()
                    // Re-initialize repository to ensure database is accessible
                    reinitializeRepository()
                }
            }
        }
    }

    private fun startRestore() {
        Log.i(TAG, "Starting restore process")
        openBackupLauncher.launch(arrayOf("application/octet-stream", "*/*"))
    }

    private fun performRestore(uri: Uri) {
        lifecycleScope.launch {
            Log.i(TAG, "Performing restore from $uri")
            when (val result = backupManager.restoreBackup(uri)) {
                is DatabaseBackupManager.Result.Success -> {
                    Log.i(TAG, "Restore completed successfully, restarting app")
                    Toast.makeText(this@MainActivity, "Restore successful, restarting...", Toast.LENGTH_SHORT).show()
                    restartApp()
                }
                is DatabaseBackupManager.Result.Error -> {
                    Log.e(TAG, "Restore failed: ${result.message}")
                    Toast.makeText(this@MainActivity, "Restore failed: ${result.message}", Toast.LENGTH_LONG).show()
                    // Re-initialize repository to ensure database is accessible
                    reinitializeRepository()
                }
            }
        }
    }

    private fun reinitializeRepository() {
        Log.i(TAG, "Re-initializing repository after backup operation")
        val database = EarbsDatabase.getDatabase(applicationContext)
        repository = EarbsRepository(
            cardDao = database.cardDao(),
            functionCardDao = database.functionCardDao(),
            progressionCardDao = database.progressionCardDao(),
            fsrsStateDao = database.fsrsStateDao(),
            reviewSessionDao = database.reviewSessionDao(),
            trialDao = database.trialDao(),
            historyDao = database.historyDao(),
            prefs = prefs
        )
    }

    private fun restartApp() {
        Log.i(TAG, "Restarting app for clean state after restore")
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
        // Force process termination to ensure clean restart
        Runtime.getRuntime().exit(0)
    }
}

@Composable
private fun EarbsApp(
    repository: EarbsRepository,
    prefs: SharedPreferences,
    showRestoreConfirmation: Boolean = false,
    onBackupClicked: () -> Unit = {},
    onRestoreClicked: () -> Unit = {},
    onRestoreConfirmed: () -> Unit = {},
    onRestoreCancelled: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedGameMode by remember { mutableStateOf(GameType.CHORD_TYPE) }

    // Chord type game state
    var chordTypeSession by remember { mutableStateOf<GenericReviewSession<Card>?>(null) }
    var chordTypeDueCount by remember { mutableIntStateOf(0) }
    var chordTypeUnlockedCount by remember { mutableIntStateOf(4) }

    // Function game state
    var functionSession by remember { mutableStateOf<GenericReviewSession<FunctionCard>?>(null) }
    var functionDueCount by remember { mutableIntStateOf(0) }
    var functionUnlockedCount by remember { mutableIntStateOf(0) }

    // Progression game state
    var progressionSession by remember { mutableStateOf<GenericReviewSession<ProgressionCard>?>(null) }
    var progressionDueCount by remember { mutableIntStateOf(0) }
    var progressionUnlockedCount by remember { mutableIntStateOf(0) }

    // Shared state
    var dbSessionId by remember { mutableStateOf<Long?>(null) }
    var sessionResult by remember { mutableStateOf<SessionResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCardId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize on first composition
    LaunchedEffect(Unit) {
        Log.i(TAG, "Initializing app...")

        // Initialize chord type game (verify FSRS state exists for all cards)
        repository.initializeStartingDeck()
        chordTypeDueCount = repository.getDueCount()
        chordTypeUnlockedCount = repository.getUnlockedCount()

        // Initialize function game (verify FSRS state exists for all cards)
        repository.initializeFunctionStartingDeck()
        functionDueCount = repository.getFunctionDueCount()
        functionUnlockedCount = repository.getFunctionUnlockedCount()

        // Initialize progression game (verify FSRS state exists for all cards)
        repository.initializeProgressionStartingDeck()
        progressionDueCount = repository.getProgressionDueCount()
        progressionUnlockedCount = repository.getProgressionUnlockedCount()

        isLoading = false
        Log.i(TAG, "Initialization complete")
        Log.i(TAG, "  Chord type: $chordTypeDueCount due, $chordTypeUnlockedCount unlocked")
        Log.i(TAG, "  Function: $functionDueCount due, $functionUnlockedCount unlocked")
        Log.i(TAG, "  Progression: $progressionDueCount due, $progressionUnlockedCount unlocked")
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
                functionDueCount = functionDueCount,
                functionUnlockedCount = functionUnlockedCount,
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
                                chordTypeSession = GenericReviewSession(cards, "chord type")
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
                                functionSession = GenericReviewSession(cards, "function")
                                dbSessionId = repository.startSession(GameType.CHORD_FUNCTION)
                                currentScreen = Screen.FUNCTION_REVIEW
                            }
                            GameType.CHORD_PROGRESSION -> {
                                Log.i(TAG, "Starting progression review session")
                                val cards = repository.selectProgressionCardsForReview()
                                if (cards.isEmpty()) {
                                    Log.w(TAG, "No progression cards available for session")
                                    return@launch
                                }
                                progressionSession = GenericReviewSession(cards, "progression")
                                dbSessionId = repository.startSession(GameType.CHORD_PROGRESSION)
                                currentScreen = Screen.PROGRESSION_REVIEW
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

        Screen.PROGRESSION_REVIEW -> {
            progressionSession?.let { activeSession ->
                ProgressionReviewSessionScreen(
                    session = activeSession,
                    sessionId = dbSessionId ?: 0L,
                    repository = repository,
                    prefs = prefs,
                    onSessionComplete = { result ->
                        coroutineScope.launch {
                            Log.i(TAG, "Progression session complete: ${result.correctCount}/${result.totalTrials}")
                            dbSessionId?.let { repository.completeSession(it) }
                            progressionDueCount = repository.getProgressionDueCount()
                            sessionResult = result
                            currentScreen = Screen.RESULTS
                        }
                    },
                    onAbortSession = {
                        Log.i(TAG, "Progression session aborted by user")
                        progressionSession = null
                        dbSessionId = null
                        currentScreen = Screen.HOME
                    }
                )
            } ?: run {
                Log.w(TAG, "Progression review screen but no session, returning to home")
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
                        progressionSession = null
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
            // Use the new flow that includes all cards (locked and unlocked)
            val cards by repository.getAllCardsForUnlockScreen().collectAsState(initial = emptyList())
            val functionCards by repository.getAllFunctionCardsForUnlockScreen().collectAsState(initial = emptyList())
            val cardStats by repository.getCardStats().collectAsState(initial = emptyList())

            HistoryScreen(
                sessions = sessions,
                cards = cards,
                functionCards = functionCards,
                cardStats = cardStats,
                onBackClicked = {
                    coroutineScope.launch {
                        // Refresh counts after potential card unlock changes
                        chordTypeDueCount = repository.getDueCount()
                        chordTypeUnlockedCount = repository.getUnlockedCount()
                        functionDueCount = repository.getFunctionDueCount()
                        functionUnlockedCount = repository.getFunctionUnlockedCount()
                        progressionDueCount = repository.getProgressionDueCount()
                        progressionUnlockedCount = repository.getProgressionUnlockedCount()
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
                },
                onCardUnlockToggled = { cardId, unlocked ->
                    Log.i(TAG, "Card unlock toggled: $cardId -> $unlocked")
                    repository.setCardUnlocked(cardId, unlocked)
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
                    },
                    onUnlockToggled = { id, unlocked ->
                        Log.i(TAG, "Card unlock toggled from details: $id -> $unlocked")
                        repository.setCardUnlocked(id, unlocked)
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
                },
                onBackupClicked = onBackupClicked,
                onRestoreClicked = onRestoreClicked
            )
        }
    }

    // Restore confirmation dialog
    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = onRestoreCancelled,
            title = { Text("Restore Database?") },
            text = { Text("This will replace all current progress with the backup. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onRestoreConfirmed) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onRestoreCancelled) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChordTypeReviewSessionScreen(
    session: GenericReviewSession<Card>,
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
                AnswerResult.Wrong(actualType = currentCard.chordType, selectedType = answeredType)
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
    session: GenericReviewSession<FunctionCard>,
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
                FunctionAnswerResult.Wrong(currentCard.function, answeredFunction)
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

/**
 * Progression Review Session Screen - manages state and audio for progression game.
 */
@Composable
private fun ProgressionReviewSessionScreen(
    session: GenericReviewSession<ProgressionCard>,
    sessionId: Long,
    repository: EarbsRepository,
    prefs: SharedPreferences,
    onSessionComplete: (SessionResult) -> Unit,
    onAbortSession: () -> Unit
) {
    val initialCard = session.getCurrentCard()
    var reviewState by remember {
        mutableStateOf(
            ProgressionReviewState(
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

    // Play arbitrary progression (for learning mode)
    fun playProgression(progression: ProgressionType) {
        val currentCard = reviewState.currentCard ?: return
        val rootSemitones = reviewState.currentRootSemitones ?: return

        Log.i(TAG, "Playing progression ${progression.displayName} for learning mode")
        reviewState = reviewState.copy(isPlaying = true)

        coroutineScope.launch {
            try {
                ProgressionStrategy.playAnswer(
                    answer = GameAnswer.ProgressionAnswer(progression),
                    card = currentCard,
                    rootSemitones = rootSemitones,
                    durationMs = playbackDuration
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error playing progression", e)
            } finally {
                reviewState = reviewState.copy(isPlaying = false)
            }
        }
    }

    // Shared play function for both manual play and auto-play
    fun playCurrentProgression() {
        val currentCard = reviewState.currentCard ?: return
        val rootSemitones = reviewState.currentRootSemitones ?: return

        Log.i(TAG, "Playing progression for trial ${session.currentTrial + 1}")
        Log.i(TAG, "Playing progression ${currentCard.displayName}, root: $rootSemitones, mode: ${currentCard.playbackMode}")

        reviewState = reviewState.copy(
            isPlaying = true,
            lastAnswer = null
        )

        coroutineScope.launch {
            try {
                ProgressionStrategy.playCard(
                    card = currentCard,
                    rootSemitones = rootSemitones,
                    durationMs = playbackDuration
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error playing progression", e)
            } finally {
                reviewState = reviewState.copy(
                    isPlaying = false,
                    hasPlayedThisTrial = true
                )
                Log.i(TAG, "Progression playback finished, ready for answer")
            }
        }
    }

    ProgressionReviewScreen(
        state = reviewState,
        autoAdvanceDelayMs = autoAdvanceDelayMs,
        onPlayClicked = {
            Log.i(TAG, "Play button clicked for progression trial ${session.currentTrial + 1}")
            playCurrentProgression()
        },
        onAnswerClicked = { answeredProgression ->
            val currentCard = reviewState.currentCard ?: return@ProgressionReviewScreen

            Log.i(TAG, "Progression answer clicked: ${answeredProgression.displayName}")

            val isCorrect = answeredProgression == currentCard.progression

            val result = if (isCorrect) {
                Log.i(TAG, "CORRECT! User answered ${answeredProgression.displayName}")
                GenericAnswerResult.Correct<GameAnswer.ProgressionAnswer>()
            } else {
                Log.i(TAG, "WRONG! User answered ${answeredProgression.displayName}, actual was ${currentCard.progression.displayName}")
                GenericAnswerResult.Wrong(
                    actualAnswer = GameAnswer.ProgressionAnswer(currentCard.progression),
                    selectedAnswer = GameAnswer.ProgressionAnswer(answeredProgression)
                )
            }

            coroutineScope.launch {
                repository.recordProgressionTrialAndUpdateFsrs(sessionId, currentCard, isCorrect, answeredProgression)
            }

            session.recordAnswer(isCorrect)

            val enterLearningMode = learnFromMistakesEnabled && !isCorrect
            reviewState = reviewState.copy(
                lastAnswer = result,
                showingFeedback = true,
                inLearningMode = enterLearningMode
            )

            // Auto-play incorrect progression if entering learning mode
            if (enterLearningMode) {
                Log.i(TAG, "Entering learning mode - playing incorrect progression: ${answeredProgression.displayName}")
                playProgression(answeredProgression)
            }
        },
        onTrialComplete = {
            val nextCard = session.getCurrentCard()
            val nextRootSemitones = nextCard?.let { ChordBuilder.randomRootInOctave(it.octave) }
            Log.i(TAG, "Progression trial complete, next card: ${nextCard?.displayName}, root: $nextRootSemitones")

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
            Log.i(TAG, "Auto-playing next progression")
            playCurrentProgression()
        },
        onSessionComplete = {
            onSessionComplete(SessionResult(
                correctCount = session.correctCount,
                totalTrials = session.totalTrials,
                sessionId = sessionId,
                gameType = GameType.CHORD_PROGRESSION.name
            ))
        },
        onPlayProgression = { progression ->
            Log.i(TAG, "Learning mode: playing progression ${progression.displayName}")
            playProgression(progression)
        },
        onNextClicked = {
            Log.i(TAG, "Learning mode: Next clicked")
            if (session.isComplete()) {
                Log.i(TAG, "Progression session complete after learning mode, navigating to results")
                onSessionComplete(SessionResult(
                    correctCount = session.correctCount,
                    totalTrials = session.totalTrials,
                    sessionId = sessionId,
                    gameType = GameType.CHORD_PROGRESSION.name
                ))
            } else {
                Log.i(TAG, "Advancing to next progression trial")
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

                // Auto-play next progression
                playCurrentProgression()
            }
        },
        onAbortSession = onAbortSession
    )
}
