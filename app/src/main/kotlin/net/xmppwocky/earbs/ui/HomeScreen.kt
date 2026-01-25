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
import net.xmppwocky.earbs.model.Deck
import net.xmppwocky.earbs.model.FunctionDeck

@Composable
fun HomeScreen(
    // Game mode selection
    selectedGameMode: GameType = GameType.CHORD_TYPE,
    onGameModeChanged: (GameType) -> Unit = {},
    // Chord type game stats
    chordTypeDueCount: Int = 0,
    chordTypeUnlockedCount: Int = 4,
    canUnlockMoreChordTypes: Boolean = true,
    // Function game stats
    functionDueCount: Int = 0,
    functionUnlockedCount: Int = 0,
    canUnlockMoreFunctions: Boolean = true,
    // Actions
    onStartReviewClicked: () -> Unit,
    onAddCardsClicked: () -> Unit = {},
    onHistoryClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {}
) {
    // Current game stats based on selected mode
    val dueCount = if (selectedGameMode == GameType.CHORD_TYPE) chordTypeDueCount else functionDueCount
    val unlockedCount = if (selectedGameMode == GameType.CHORD_TYPE) chordTypeUnlockedCount else functionUnlockedCount
    val totalCards = if (selectedGameMode == GameType.CHORD_TYPE) Deck.TOTAL_CARDS else FunctionDeck.TOTAL_CARDS
    val canUnlockMore = if (selectedGameMode == GameType.CHORD_TYPE) canUnlockMoreChordTypes else canUnlockMoreFunctions
    val cardsPerUnlock = if (selectedGameMode == GameType.CHORD_TYPE) 4 else FunctionDeck.CARDS_PER_GROUP

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
            text = "Chord Ear Training",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Game mode tabs
        TabRow(
            selectedTabIndex = if (selectedGameMode == GameType.CHORD_TYPE) 0 else 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedGameMode == GameType.CHORD_TYPE,
                onClick = { onGameModeChanged(GameType.CHORD_TYPE) },
                text = { Text("Chord Type") }
            )
            Tab(
                selected = selectedGameMode == GameType.CHORD_FUNCTION,
                onClick = { onGameModeChanged(GameType.CHORD_FUNCTION) },
                text = { Text("Function") }
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
                    text = if (unlockedCount == 0) "Tap 'Add Cards' to start!" else "No cards due",
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
            onClick = onHistoryClicked,
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

        Spacer(modifier = Modifier.height(16.dp))

        // Add Cards button
        if (canUnlockMore) {
            Button(
                onClick = onAddCardsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "Add $cardsPerUnlock Cards",
                    fontSize = 18.sp
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "All cards unlocked!",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info text
        Text(
            text = if (selectedGameMode == GameType.CHORD_TYPE) {
                "Identify chord quality (Major, Minor, etc.)"
            } else {
                "Identify chord function (IV, V, vi, etc.)"
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
