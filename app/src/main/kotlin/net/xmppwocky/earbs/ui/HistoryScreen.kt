package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import net.xmppwocky.earbs.data.db.CardStatsView
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.entity.CardEntity
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
    cards: List<CardEntity>,
    cardStats: List<CardStatsView>,
    onBackClicked: () -> Unit
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
                HistoryTab.SESSIONS -> SessionsTab(sessions)
                HistoryTab.CARDS -> CardsTab(cards)
                HistoryTab.STATS -> StatsTab(cardStats)
            }
        }
    }
}

@Composable
private fun SessionsTab(sessions: List<SessionOverview>) {
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
            SessionCard(session)
        }
    }
}

@Composable
private fun SessionCard(session: SessionOverview) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val date = dateFormat.format(Date(session.startedAt))

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
                    text = date,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Octave ${session.octave}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${session.correctTrials}/${session.totalTrials} correct",
                    fontSize = 14.sp
                )

                val accuracy = (session.accuracy * 100).toInt()
                val color = when {
                    accuracy >= 90 -> Color(0xFF4CAF50)  // Green
                    accuracy >= 75 -> Color(0xFF8BC34A)  // Light green
                    accuracy >= 60 -> Color(0xFFFFC107)  // Amber
                    else -> Color(0xFFF44336)            // Red
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
            }

            if (session.completedAt == null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Incomplete",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CardsTab(cards: List<CardEntity>) {
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
            CardEntityRow(card)
        }
    }
}

@Composable
private fun CardEntityRow(card: CardEntity) {
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
        accuracy >= 90 -> Color(0xFF4CAF50)
        accuracy >= 75 -> Color(0xFF8BC34A)
        accuracy >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
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
                Text(
                    text = stat.cardId.replace("_", " @ Oct "),
                    fontWeight = FontWeight.Medium
                )
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
