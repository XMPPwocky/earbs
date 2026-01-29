package net.xmppwocky.earbs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.displayName
import net.xmppwocky.earbs.model.GameStats

/**
 * Helper to get stats for a specific game type from the list.
 * Returns null if the game type is not in the list (which would indicate a bug).
 */
private fun List<GameStats>.forGameType(gameType: GameType): GameStats? =
    find { it.gameType == gameType }

@Composable
fun HomeScreen(
    // Game mode selection
    selectedGameMode: GameType = GameType.CHORD_TYPE,
    onGameModeChanged: (GameType) -> Unit = {},
    // Game stats - a list ensures exhaustive handling of game types at call site
    gameStats: List<GameStats>,
    // Actions
    onStartReviewClicked: () -> Unit,
    onHistoryClicked: (GameType) -> Unit = {},
    onSettingsClicked: () -> Unit = {}
) {
    // Get stats for selected mode - asserts that all game types have stats
    val currentStats = gameStats.forGameType(selectedGameMode)
        ?: error("GameStats missing for $selectedGameMode - ensure all GameType values have stats")
    val dueCount = currentStats.dueCount
    val unlockedCount = currentStats.unlockedCount
    val totalCards = currentStats.totalCards

    // Get stats for each game type for the tabs
    val chordTypeStats = gameStats.forGameType(GameType.CHORD_TYPE)
    val functionStats = gameStats.forGameType(GameType.CHORD_FUNCTION)
    val progressionStats = gameStats.forGameType(GameType.CHORD_PROGRESSION)
    val intervalStats = gameStats.forGameType(GameType.INTERVAL)
    val scaleStats = gameStats.forGameType(GameType.SCALE)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title
        Text(
            text = "Earbs",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Subtitle
        Text(
            text = "Ear Training",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Game mode tabs - use ScrollableTabRow for 5 tabs
        // The selectedTabIndex when expression is exhaustive
        ScrollableTabRow(
            selectedTabIndex = when (selectedGameMode) {
                GameType.CHORD_TYPE -> 0
                GameType.CHORD_FUNCTION -> 1
                GameType.CHORD_PROGRESSION -> 2
                GameType.INTERVAL -> 3
                GameType.SCALE -> 4
            },
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 0.dp
        ) {
            Tab(
                selected = selectedGameMode == GameType.CHORD_TYPE,
                onClick = { onGameModeChanged(GameType.CHORD_TYPE) },
                text = { Text("Types (${chordTypeStats?.dueCount ?: 0})") }
            )
            Tab(
                selected = selectedGameMode == GameType.CHORD_FUNCTION,
                onClick = { onGameModeChanged(GameType.CHORD_FUNCTION) },
                text = { Text("Functions (${functionStats?.dueCount ?: 0})") }
            )
            Tab(
                selected = selectedGameMode == GameType.CHORD_PROGRESSION,
                onClick = { onGameModeChanged(GameType.CHORD_PROGRESSION) },
                text = { Text("Prog. (${progressionStats?.dueCount ?: 0})") }
            )
            Tab(
                selected = selectedGameMode == GameType.INTERVAL,
                onClick = { onGameModeChanged(GameType.INTERVAL) },
                text = { Text("Intervals (${intervalStats?.dueCount ?: 0})") }
            )
            Tab(
                selected = selectedGameMode == GameType.SCALE,
                onClick = { onGameModeChanged(GameType.SCALE) },
                text = { Text("Scales (${scaleStats?.dueCount ?: 0})") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Unlock progress
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "$unlockedCount / $totalCards cards unlocked",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Due count badge
        if (dueCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "$dueCount cards due",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (unlockedCount == 0) "Unlock cards in History > Cards" else "No cards due",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Start Review button
        Button(
            onClick = onStartReviewClicked,
            enabled = unlockedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Text(
                text = if (dueCount > 0) "Start Review" else "Practice Early",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History button
        OutlinedButton(
            onClick = { onHistoryClicked(selectedGameMode) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "History",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings button
        OutlinedButton(
            onClick = onSettingsClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info text - exhaustive when ensures compile error if new game type added
        Text(
            text = when (selectedGameMode) {
                GameType.CHORD_TYPE -> "Identify chord quality (Major, Minor, etc.)"
                GameType.CHORD_FUNCTION -> "Identify chord function (IV, V, vi, etc.)"
                GameType.CHORD_PROGRESSION -> "Identify chord progressions (I-IV-V-I, etc.)"
                GameType.INTERVAL -> "Identify intervals (Minor 2nd, Perfect 5th, etc.)"
                GameType.SCALE -> "Identify scales (Major, Dorian, Pentatonic, etc.)"
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
