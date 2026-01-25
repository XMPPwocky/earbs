package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.data.db.SessionCardStats
import net.xmppwocky.earbs.data.repository.EarbsRepository
import net.xmppwocky.earbs.ui.components.CardStatsRow
import net.xmppwocky.earbs.ui.components.formatCardId
import net.xmppwocky.earbs.ui.theme.AccuracyThresholds
import net.xmppwocky.earbs.ui.theme.AppColors

private const val TAG = "ResultsScreen"

/**
 * ResultsScreen with repository for loading card stats.
 */
@Composable
fun ResultsScreen(
    result: SessionResult,
    repository: EarbsRepository,
    onDoneClicked: () -> Unit
) {
    BackHandler { onDoneClicked() }

    // Load per-card stats for this session
    var cardStats by remember { mutableStateOf<List<SessionCardStats>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(result.sessionId) {
        Log.d(TAG, "Loading card stats for session ${result.sessionId}")
        cardStats = repository.getSessionCardStats(result.sessionId)
        Log.d(TAG, "Loaded ${cardStats.size} card stats")
        isLoading = false
    }

    ResultsScreenContent(
        result = result,
        cardStats = cardStats,
        isLoading = isLoading,
        onDoneClicked = onDoneClicked
    )
}

/**
 * Pure UI composable for the results screen content.
 * Separated for testing purposes.
 */
@Composable
fun ResultsScreenContent(
    result: SessionResult,
    cardStats: List<SessionCardStats>,
    isLoading: Boolean = false,
    onDoneClicked: () -> Unit
) {
    val accuracy = if (result.totalTrials > 0) {
        result.correctCount.toFloat() / result.totalTrials
    } else {
        0f
    }
    val accuracyPercent = (accuracy * 100).toInt()

    // Determine color based on accuracy
    val (backgroundColor, textColor) = when {
        accuracyPercent >= AccuracyThresholds.EXCELLENT -> AppColors.SuccessBackground to AppColors.Success
        accuracyPercent >= AccuracyThresholds.GOOD - 5 -> AppColors.WarningBackground to AppColors.Warning
        else -> AppColors.ErrorBackground to AppColors.Error
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Session Complete",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Result card (summary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score
                Text(
                    text = "${result.correctCount} / ${result.totalTrials}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "correct",
                    fontSize = 18.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Accuracy percentage
                Text(
                    text = "$accuracyPercent%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Card Breakdown Section
        Text(
            text = "CARD BREAKDOWN",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Card stats list (scrollable)
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cardStats) { stats ->
                    CardStatsRow(
                        displayName = formatCardId(stats.cardId, stats.gameType),
                        correct = stats.correctInSession,
                        total = stats.trialsInSession
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Done button
        Button(
            onClick = onDoneClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp)
        ) {
            Text(
                text = "Done",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
