package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.data.db.CardStatsView
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.entity.TrialEntity
import net.xmppwocky.earbs.ui.theme.AccuracyThresholds
import net.xmppwocky.earbs.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "HistoryScreen"

enum class HistoryTab {
    SESSIONS,
    CARDS,
    STATS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessions: List<SessionOverview>,
    cards: List<CardWithFsrs>,
    cardStats: List<CardStatsView>,
    onBackClicked: () -> Unit,
    onLoadTrials: (suspend (Long) -> List<TrialEntity>)? = null
) {
    var selectedTab by remember { mutableStateOf(HistoryTab.SESSIONS) }

    Log.d(TAG, "HistoryScreen composing: ${sessions.size} sessions, ${cards.size} cards")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == HistoryTab.SESSIONS,
                    onClick = { selectedTab = HistoryTab.SESSIONS },
                    text = { Text("Sessions") }
                )
                Tab(
                    selected = selectedTab == HistoryTab.CARDS,
                    onClick = { selectedTab = HistoryTab.CARDS },
                    text = { Text("Cards") }
                )
                Tab(
                    selected = selectedTab == HistoryTab.STATS,
                    onClick = { selectedTab = HistoryTab.STATS },
                    text = { Text("Stats") }
                )
            }

            when (selectedTab) {
                HistoryTab.SESSIONS -> SessionsTab(sessions, onLoadTrials)
                HistoryTab.CARDS -> CardsTab(cards)
                HistoryTab.STATS -> StatsTab(cardStats)
            }
        }
    }
}

@Composable
private fun SessionsTab(
    sessions: List<SessionOverview>,
    onLoadTrials: (suspend (Long) -> List<TrialEntity>)? = null
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No sessions yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions) { session ->
            SessionCard(session, onLoadTrials)
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionOverview,
    onLoadTrials: (suspend (Long) -> List<TrialEntity>)? = null
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val date = dateFormat.format(Date(session.startedAt))
    var expanded by remember { mutableStateOf(false) }
    var trials by remember { mutableStateOf<List<TrialEntity>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Load trials when expanded
    LaunchedEffect(expanded) {
        if (expanded && trials == null && onLoadTrials != null && !isLoading) {
            isLoading = true
            trials = onLoadTrials(session.id)
            isLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onLoadTrials != null) { expanded = !expanded }
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
                    text = date,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val accuracy = (session.accuracy * 100).toInt()
                    val color = when {
                        accuracy >= AccuracyThresholds.EXCELLENT -> AppColors.Success
                        accuracy >= AccuracyThresholds.GOOD -> AppColors.SuccessLight
                        accuracy >= AccuracyThresholds.FAIR -> AppColors.Warning
                        else -> AppColors.Error
                    }

                    Surface(
                        color = color.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "$accuracy%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (onLoadTrials != null) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${session.correctTrials}/${session.totalTrials} correct",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Game type badge
                Surface(
                    color = if (session.gameType == "CHORD_FUNCTION")
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = if (session.gameType == "CHORD_FUNCTION") "Function" else "Chord",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = if (session.gameType == "CHORD_FUNCTION")
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (session.completedAt == null) {
                Text(
                    text = "Incomplete",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            // Expandable trials list
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    } else {
                        trials?.forEach { trial ->
                            TrialRow(trial)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialRow(trial: TrialEntity) {
    val isFunction = trial.gameType == "CHORD_FUNCTION"

    // Parse card ID based on game type
    // Chord type: "MAJOR_4_ARPEGGIATED"
    // Function: "IV_MAJOR_4_ARPEGGIATED"
    val cardParts = trial.cardId.split("_")
    val (displayText, octave, mode) = if (isFunction && cardParts.size >= 4) {
        val function = cardParts[0]
        val keyQuality = cardParts[1].lowercase()
        Triple("$function ($keyQuality)", cardParts[2], cardParts[3].lowercase().take(3))
    } else if (cardParts.size >= 3) {
        Triple(cardParts[0], cardParts[1], cardParts[2].lowercase().take(3))
    } else {
        Triple(trial.cardId, "?", "")
    }

    // Get what user answered (wrong answers only)
    val userAnswer = if (isFunction) trial.answeredFunction else trial.answeredChordType

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (trial.wasCorrect) {
            Text(
                text = "$displayText @ $octave ($mode)",
                fontSize = 13.sp,
                color = AppColors.Success
            )
            Text(
                text = "✓",
                color = AppColors.Success,
                fontWeight = FontWeight.Bold
            )
        } else {
            Column {
                Text(
                    text = "$displayText @ $octave ($mode)",
                    fontSize = 13.sp,
                    color = AppColors.Error
                )
                userAnswer?.let { answered ->
                    Text(
                        text = "You said: $answered",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "✗",
                color = AppColors.Error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CardsTab(cards: List<CardWithFsrs>) {
    if (cards.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No cards yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards) { card ->
            CardWithFsrsRow(card)
        }
    }
}

@Composable
private fun CardWithFsrsRow(card: CardWithFsrs) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dueDate = dateFormat.format(Date(card.dueDate))
    val now = System.currentTimeMillis()
    val isDue = card.dueDate <= now

    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "${card.chordType} @ Oct ${card.octave}",
                    fontWeight = FontWeight.Medium
                )

                if (isDue) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "DUE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Reviews: ${card.reviewCount}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Interval: ${card.interval}d",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Due: $dueDate",
                        fontSize = 12.sp,
                        color = if (isDue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stability: ${String.format("%.1f", card.stability)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsTab(cardStats: List<CardStatsView>) {
    if (cardStats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No stats yet - complete some reviews!",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalTrials = cardStats.sumOf { it.totalTrials }
    val totalCorrect = cardStats.sumOf { it.correctTrials }
    val overallAccuracy = if (totalTrials > 0) (totalCorrect.toFloat() / totalTrials * 100).toInt() else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Overall stats
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overall",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$overallAccuracy%",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$totalCorrect / $totalTrials trials",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Per-card stats
        items(cardStats.sortedByDescending { it.accuracy }) { stat ->
            CardStatRow(stat)
        }
    }
}

@Composable
private fun CardStatRow(stat: CardStatsView) {
    val accuracy = (stat.accuracy * 100).toInt()
    val color = when {
        accuracy >= AccuracyThresholds.EXCELLENT -> AppColors.Success
        accuracy >= AccuracyThresholds.GOOD -> AppColors.SuccessLight
        accuracy >= AccuracyThresholds.FAIR -> AppColors.Warning
        else -> AppColors.Error
    }

    val isFunction = stat.gameType == "CHORD_FUNCTION"

    // Parse card ID based on game type
    val cardParts = stat.cardId.split("_")
    val displayName = if (isFunction && cardParts.size >= 4) {
        // Function: "IV_MAJOR_4_ARPEGGIATED"
        "${cardParts[0]} (${cardParts[1].lowercase()}) @ Oct ${cardParts[2]}"
    } else if (cardParts.size >= 3) {
        // Chord type: "MAJOR_4_ARPEGGIATED"
        "${cardParts[0]} @ Oct ${cardParts[1]}"
    } else {
        stat.cardId
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Medium
                    )
                    if (isFunction) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "F",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = "${stat.correctTrials}/${stat.totalTrials} trials",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = color.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "$accuracy%",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
