package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.launch
import net.xmppwocky.earbs.data.db.CardSessionAccuracy
import net.xmppwocky.earbs.data.db.GenericCardWithFsrs
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.repository.EarbsRepository
import net.xmppwocky.earbs.fsrs.CardPhase
import net.xmppwocky.earbs.ui.theme.AccuracyThresholds
import net.xmppwocky.earbs.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CardDetailsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailsScreen(
    cardId: String,
    gameType: GameType,
    repository: EarbsRepository,
    onBackClicked: () -> Unit,
    onUnlockToggled: (suspend (String, Boolean) -> Unit)? = null
) {
    BackHandler { onBackClicked() }

    var card by remember { mutableStateOf<GenericCardWithFsrs?>(null) }
    var sessionAccuracy by remember { mutableStateOf<List<CardSessionAccuracy>>(emptyList()) }
    var lifetimeStats by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Load data
    LaunchedEffect(cardId) {
        Log.d(TAG, "Loading card details for $cardId (gameType=${gameType.name})")
        card = repository.getCardWithFsrs(cardId, gameType)
        sessionAccuracy = repository.getCardSessionAccuracy(cardId)
        lifetimeStats = repository.getCardLifetimeStats(cardId)
        isLoading = false
        Log.d(TAG, "Loaded: card=${card != null}, sessions=${sessionAccuracy.size}, lifetime=$lifetimeStats")
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset FSRS State?") },
            text = {
                Text("This will reset scheduling for this card to initial values. Review history will be preserved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            Log.i(TAG, "Resetting FSRS state for card $cardId")
                            repository.resetFsrsState(cardId, gameType)
                            // Reload card data
                            card = repository.getCardWithFsrs(cardId, gameType)
                        }
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val currentCard = card
        if (currentCard == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Card not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card header with unlock toggle
            CardHeader(
                card = currentCard,
                onUnlockToggled = if (onUnlockToggled != null) {
                    { unlocked ->
                        coroutineScope.launch {
                            onUnlockToggled(cardId, unlocked)
                            // Reload card data after toggle
                            card = repository.getCardWithFsrs(cardId, gameType)
                        }
                    }
                } else null
            )

            // Show limited info for locked cards
            if (!currentCard.unlocked) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "This card is locked",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unlock it to start practicing and track FSRS progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Accuracy over time graph (only for unlocked cards)
                AccuracyOverTimeSection(sessionAccuracy)

            // Last session stats
            val lastSession = sessionAccuracy.lastOrNull()
            if (lastSession != null) {
                LastSessionSection(lastSession)
            }

            // FSRS Parameters
            FsrsParametersSection(currentCard)

            // Lifetime stats
            lifetimeStats?.let { (total, correct) ->
                LifetimeStatsSection(total, correct)
            }

                // Reset button (only for unlocked cards)
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset FSRS State")
                }
            }
        }
    }
}

@Composable
internal fun CardHeader(
    card: GenericCardWithFsrs,
    onUnlockToggled: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (card.unlocked)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${card.displayName} @ Octave ${card.octave}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (card.unlocked)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Unlock toggle switch
                if (onUnlockToggled != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (card.unlocked) "Unlocked" else "Locked",
                            fontSize = 12.sp,
                            color = if (card.unlocked)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Switch(
                            checked = card.unlocked,
                            onCheckedChange = { onUnlockToggled(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun AccuracyOverTimeSection(sessions: List<CardSessionAccuracy>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ACCURACY OVER TIME",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = "No review history yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                AccuracyChart(sessions)
            }
        }
    }
}

@Composable
internal fun AccuracyChart(sessions: List<CardSessionAccuracy>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(sessions) {
        val accuracies = sessions.map { session ->
            if (session.trialsInSession > 0) {
                (session.correctInSession.toFloat() / session.trialsInSession) * 100f
            } else {
                0f
            }
        }
        modelProducer.runTransaction {
            lineSeries {
                series(accuracies)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}

@Composable
internal fun LastSessionSection(lastSession: CardSessionAccuracy) {
    val accuracy = if (lastSession.trialsInSession > 0) {
        (lastSession.correctInSession.toFloat() / lastSession.trialsInSession * 100).toInt()
    } else {
        0
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LAST SESSION",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${lastSession.correctInSession}/${lastSession.trialsInSession}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "correct",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val color = when {
                        accuracy >= AccuracyThresholds.EXCELLENT -> AppColors.Success
                        accuracy >= AccuracyThresholds.GOOD -> AppColors.SuccessLight
                        accuracy >= AccuracyThresholds.FAIR -> AppColors.Warning
                        else -> AppColors.Error
                    }
                    Text(
                        text = "$accuracy%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = color
                    )
                    Text(
                        text = "accuracy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun FsrsParametersSection(card: GenericCardWithFsrs) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dueDate = dateFormat.format(Date(card.dueDate))
    val lastReviewDate = card.lastReview?.let { dateFormat.format(Date(it)) } ?: "Never"

    val phaseName = when (card.phase) {
        CardPhase.Added.value -> "New"
        CardPhase.ReLearning.value -> "Re-learning"
        CardPhase.Review.value -> "Review"
        else -> "Unknown"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "FSRS PARAMETERS",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Stability, Difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterItem("Stability", String.format("%.2f", card.stability))
                ParameterItem("Difficulty", String.format("%.2f", card.difficulty))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Interval, Due
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterItem("Interval", "${card.interval} days")
                ParameterItem("Due", dueDate)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Reviews, Last
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterItem("Reviews", card.reviewCount.toString())
                ParameterItem("Last", lastReviewDate)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Phase, Lapses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ParameterItem("Phase", phaseName)
                ParameterItem("Lapses", card.lapses.toString())
            }
        }
    }
}

@Composable
internal fun ParameterItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
internal fun LifetimeStatsSection(total: Int, correct: Int) {
    val accuracy = if (total > 0) (correct.toFloat() / total * 100).toInt() else 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "LIFETIME STATS",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = total.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Total",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = correct.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppColors.Success
                    )
                    Text(
                        text = "Correct",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val color = when {
                        accuracy >= AccuracyThresholds.EXCELLENT -> AppColors.Success
                        accuracy >= AccuracyThresholds.GOOD -> AppColors.SuccessLight
                        accuracy >= AccuracyThresholds.FAIR -> AppColors.Warning
                        else -> AppColors.Error
                    }
                    Text(
                        text = "$accuracy%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = color
                    )
                    Text(
                        text = "Accuracy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
