package net.xmppwocky.earbs.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.IntervalType
import net.xmppwocky.earbs.audio.ScaleType
import net.xmppwocky.earbs.data.db.CardStatsView
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.ConfusionEntry
import net.xmppwocky.earbs.data.db.FunctionCardWithFsrs
import net.xmppwocky.earbs.data.db.IntervalCardWithFsrs
import net.xmppwocky.earbs.data.db.ProgressionCardWithFsrs
import net.xmppwocky.earbs.data.db.ScaleCardWithFsrs
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.entity.GameType
import net.xmppwocky.earbs.data.entity.TrialEntity
import net.xmppwocky.earbs.data.entity.displayName
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.GameCards
import net.xmppwocky.earbs.model.computeFunctionMasteryDistribution
import net.xmppwocky.earbs.model.computeMasteryDistribution
import net.xmppwocky.earbs.model.computeProgressionMasteryDistribution
import net.xmppwocky.earbs.ui.components.ConfusionMatrix
import net.xmppwocky.earbs.ui.components.ConfusionMatrixData
import net.xmppwocky.earbs.ui.components.FilterChipRow
import net.xmppwocky.earbs.ui.components.FilterOption
import net.xmppwocky.earbs.ui.components.MasteryProgressBar
import net.xmppwocky.earbs.ui.components.buildConfusionMatrix
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
    cards: GameCards,
    sessions: List<SessionOverview>,
    cardStats: List<CardStatsView>,
    onBackClicked: () -> Unit,
    onLoadTrials: (suspend (Long) -> List<TrialEntity>)? = null,
    onLoadChordConfusion: (suspend (Int?) -> List<ConfusionEntry>)? = null,
    onLoadFunctionConfusion: (suspend (String) -> List<ConfusionEntry>)? = null,
    onResetFsrs: (suspend (String) -> Unit)? = null,
    onCardClicked: ((String) -> Unit)? = null,
    onCardUnlockToggled: (suspend (String, Boolean) -> Unit)? = null
) {
    BackHandler { onBackClicked() }

    var selectedTab by remember { mutableStateOf(HistoryTab.SESSIONS) }

    val gameType = cards.gameType
    Log.d(TAG, "HistoryScreen composing: ${sessions.size} sessions, ${cards.activeCount} cards (gameType=${gameType.name})")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${gameType.displayName} History") },
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
                HistoryTab.CARDS -> CardsTab(
                    cards = cards,
                    onResetFsrs = onResetFsrs,
                    onCardClicked = onCardClicked,
                    onCardUnlockToggled = onCardUnlockToggled
                )
                HistoryTab.STATS -> StatsTab(
                    cards = cards,
                    cardStats = cardStats,
                    onLoadChordConfusion = onLoadChordConfusion,
                    onLoadFunctionConfusion = onLoadFunctionConfusion
                )
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
                // Game type badge - convert string to enum for comparison
                val sessionGameType = runCatching { GameType.valueOf(session.gameType) }.getOrNull()
                Surface(
                    color = if (sessionGameType == GameType.CHORD_FUNCTION)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = sessionGameType?.displayName ?: session.gameType,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = if (sessionGameType == GameType.CHORD_FUNCTION)
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
    // Convert string to enum for comparison
    val trialGameType = runCatching { GameType.valueOf(trial.gameType) }.getOrNull()
    val isFunction = trialGameType == GameType.CHORD_FUNCTION

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

/**
 * Generic card item for display in the Cards tab.
 * Abstracts over the different card types.
 */
private data class CardDisplayItem(
    val id: String,
    val displayName: String,
    val unlocked: Boolean,
    val deprecated: Boolean,
    val dueDate: Long,
    val stability: Double,
    val groupKey: String
)

@Composable
private fun CardsTab(
    cards: GameCards,
    onResetFsrs: (suspend (String) -> Unit)? = null,
    onCardClicked: ((String) -> Unit)? = null,
    onCardUnlockToggled: (suspend (String, Boolean) -> Unit)? = null
) {
    // Convert cards to generic display items based on game type
    // Using exhaustive when on sealed class ensures compile error if new game type added
    val cardItems = remember(cards) {
        when (cards) {
            is GameCards.ChordType -> cards.active.map { card ->
                val chordType = ChordType.valueOf(card.chordType)
                val isTriad = chordType in ChordType.TRIADS
                val category = if (isTriad) "Triads" else "7ths"
                val mode = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = card.chordType,
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "$category @ Octave ${card.octave}, $mode"
                )
            }
            is GameCards.Function -> cards.active.map { card ->
                val mode = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() }
                val keyQualityDisplay = card.keyQuality.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = "${card.function} ($keyQualityDisplay)",
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "$keyQualityDisplay @ Octave ${card.octave}, $mode"
                )
            }
            is GameCards.Progression -> cards.active.map { card ->
                val mode = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() }
                // Determine key quality from progression name (ends with _MAJOR or _MINOR equivalent)
                val keyQuality = if (card.progression.contains("MAJOR", ignoreCase = true) ||
                    card.progression in listOf("I_IV_V_I", "I_V_vi_IV", "I_vi_IV_V", "ii_V_I", "I_IV_vi_V", "vi_IV_I_V", "I_V_IV_I", "IV_I_V_vi")
                ) "Major" else "Minor"
                CardDisplayItem(
                    id = card.id,
                    displayName = card.progression.replace("_", "-"),
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "$keyQuality @ Octave ${card.octave}, $mode"
                )
            }
            is GameCards.Interval -> cards.active.map { card ->
                val intervalType = IntervalType.valueOf(card.interval)
                val direction = card.direction.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = intervalType.displayName,
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "$direction @ Octave ${card.octave}"
                )
            }
            is GameCards.Scale -> cards.active.map { card ->
                val scaleType = ScaleType.valueOf(card.scale)
                val direction = card.direction.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = scaleType.displayName,
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "$direction @ Octave ${card.octave}"
                )
            }
        }
    }

    // Convert deprecated cards to display items
    // Using exhaustive when on sealed class ensures compile error if new game type added
    val deprecatedItems = remember(cards) {
        when (cards) {
            is GameCards.ChordType -> cards.deprecated.map { card ->
                val chordType = ChordType.valueOf(card.chordType)
                val isTriad = chordType in ChordType.TRIADS
                val category = if (isTriad) "Triads" else "7ths"
                val mode = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = card.chordType,
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "Archived: $category @ Octave ${card.octave}, $mode"
                )
            }
            is GameCards.Function -> cards.deprecated.map { card ->
                val mode = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() }
                val keyQualityDisplay = card.keyQuality.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = "${card.function} ($keyQualityDisplay)",
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "Archived: $keyQualityDisplay @ Octave ${card.octave}, $mode"
                )
            }
            is GameCards.Progression -> cards.deprecated.map { card ->
                val mode = card.playbackMode.lowercase().replaceFirstChar { it.uppercase() }
                val keyQuality = if (card.progression.contains("MAJOR", ignoreCase = true) ||
                    card.progression in listOf("I_IV_V_I", "I_V_vi_IV", "I_vi_IV_V", "ii_V_I", "I_IV_vi_V", "vi_IV_I_V", "I_V_IV_I", "IV_I_V_vi")
                ) "Major" else "Minor"
                CardDisplayItem(
                    id = card.id,
                    displayName = card.progression.replace("_", "-"),
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "Archived: $keyQuality @ Octave ${card.octave}, $mode"
                )
            }
            is GameCards.Interval -> cards.deprecated.map { card ->
                val intervalType = IntervalType.valueOf(card.interval)
                val direction = card.direction.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = intervalType.displayName,
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "Archived: $direction @ Octave ${card.octave}"
                )
            }
            is GameCards.Scale -> cards.deprecated.map { card ->
                val scaleType = ScaleType.valueOf(card.scale)
                val direction = card.direction.lowercase().replaceFirstChar { it.uppercase() }
                CardDisplayItem(
                    id = card.id,
                    displayName = scaleType.displayName,
                    unlocked = card.unlocked,
                    deprecated = card.deprecated,
                    dueDate = card.dueDate,
                    stability = card.stability,
                    groupKey = "Archived: $direction @ Octave ${card.octave}"
                )
            }
        }
    }

    if (cardItems.isEmpty() && deprecatedItems.isEmpty()) {
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

    val coroutineScope = rememberCoroutineScope()

    // Group cards by their group key
    val groupedCards = remember(cardItems) {
        cardItems.groupBy { it.groupKey }.toSortedMap(compareBy(
            // Sort groups: for chord type, triads before 7ths; for others, major before minor
            { group ->
                when {
                    group.contains("Triads") -> 0
                    group.contains("7ths") -> 1
                    group.contains("Major") -> 0
                    group.contains("Minor") -> 1
                    else -> 2
                }
            },
            // Arpeggiated before Block
            { if (it.contains("Arpeggiated")) 0 else 1 },
            // Then by octave (extract octave number)
            { group ->
                Regex("Octave (\\d)").find(group)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
        ))
    }

    // Group deprecated cards
    val groupedDeprecatedCards = remember(deprecatedItems) {
        deprecatedItems.groupBy { it.groupKey }.toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active cards
        groupedCards.forEach { (groupName, groupCards) ->
            // Group header
            item(key = "header_$groupName") {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Cards in this group
            items(groupCards, key = { it.id }) { cardItem ->
                GenericCardRow(
                    cardItem = cardItem,
                    onUnlockToggled = if (onCardUnlockToggled != null) {
                        { unlocked -> coroutineScope.launch { onCardUnlockToggled(cardItem.id, unlocked) } }
                    } else null,
                    onCardClicked = onCardClicked
                )
            }
        }

        // Archived (deprecated) cards section
        if (deprecatedItems.isNotEmpty()) {
            item(key = "archived_section_header") {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ARCHIVED CARDS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "These cards are no longer included in reviews but their history is preserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            groupedDeprecatedCards.forEach { (groupName, groupCards) ->
                // Group header (remove "Archived: " prefix for cleaner display)
                val displayGroupName = groupName.removePrefix("Archived: ")
                item(key = "header_deprecated_$groupName") {
                    Text(
                        text = displayGroupName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Deprecated cards in this group (no unlock toggle, just view history)
                items(groupCards, key = { "deprecated_${it.id}" }) { cardItem ->
                    GenericCardRow(
                        cardItem = cardItem,
                        onUnlockToggled = null,  // Can't toggle unlock for deprecated cards
                        onCardClicked = onCardClicked,
                        isArchived = true
                    )
                }
            }
        }
    }
}

/**
 * Generic card row that works with any card type.
 */
@Composable
private fun GenericCardRow(
    cardItem: CardDisplayItem,
    onUnlockToggled: ((Boolean) -> Unit)? = null,
    onCardClicked: ((String) -> Unit)? = null,
    isArchived: Boolean = false
) {
    val isLocked = !cardItem.unlocked
    val alpha = if (isLocked || isArchived) 0.6f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .then(
                if (onCardClicked != null) {
                    Modifier.clickable { onCardClicked(cardItem.id) }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unlock checkbox
            if (onUnlockToggled != null) {
                Checkbox(
                    checked = cardItem.unlocked,
                    onCheckedChange = { onUnlockToggled(it) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Card info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cardItem.displayName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )

                when {
                    isArchived -> {
                        // Show archived card info (just stability, no due date)
                        Text(
                            text = "Stability: ${String.format("%.1f", cardItem.stability)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    cardItem.unlocked -> {
                        // Show FSRS info for unlocked cards
                        val now = System.currentTimeMillis()
                        val isDue = cardItem.dueDate <= now
                        val dueText = if (isDue) {
                            "Due now"
                        } else {
                            val daysUntilDue = ((cardItem.dueDate - now) / (24 * 60 * 60 * 1000)).toInt()
                            when {
                                daysUntilDue == 0 -> "Due today"
                                daysUntilDue == 1 -> "Due tomorrow"
                                else -> "Due in ${daysUntilDue}d"
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = dueText,
                                fontSize = 12.sp,
                                color = if (isDue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Stability: ${String.format("%.1f", cardItem.stability)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        // Minimal info for locked cards
                        Text(
                            text = "(locked)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Badge: ARCHIVED, DUE, or nothing
            when {
                isArchived -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "ARCHIVED",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                cardItem.unlocked -> {
                    val now = System.currentTimeMillis()
                    val isDue = cardItem.dueDate <= now
                    if (isDue) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "DUE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTab(
    cards: GameCards,
    cardStats: List<CardStatsView>,
    onLoadChordConfusion: (suspend (Int?) -> List<ConfusionEntry>)? = null,
    onLoadFunctionConfusion: (suspend (String) -> List<ConfusionEntry>)? = null
) {
    val gameType = cards.gameType

    // Filter state
    var octaveFilter by remember { mutableStateOf<Int?>(null) }
    var keyQualityFilter by remember { mutableStateOf("MAJOR") }

    // Confusion matrix data (only for chord type and function games)
    var chordConfusion by remember { mutableStateOf<ConfusionMatrixData?>(null) }
    var functionConfusion by remember { mutableStateOf<ConfusionMatrixData?>(null) }

    // Load chord confusion data when filter changes (only for CHORD_TYPE game)
    LaunchedEffect(octaveFilter, onLoadChordConfusion, cards) {
        if (cards is GameCards.ChordType && onLoadChordConfusion != null) {
            val entries = onLoadChordConfusion(octaveFilter)
            chordConfusion = buildConfusionMatrix(entries, ChordType.entries.map { it.name })
        }
    }

    // Load function confusion data when filter changes (only for CHORD_FUNCTION game)
    LaunchedEffect(keyQualityFilter, onLoadFunctionConfusion, cards) {
        if (cards is GameCards.Function && onLoadFunctionConfusion != null) {
            val entries = onLoadFunctionConfusion(keyQualityFilter)
            val functions = if (keyQualityFilter == "MAJOR")
                ChordFunction.MAJOR_FUNCTIONS else ChordFunction.MINOR_FUNCTIONS
            functionConfusion = buildConfusionMatrix(entries, functions.map { it.name })
        }
    }

    // Check if there's any data to show - exhaustive when on sealed class
    val hasConfusionData = when (cards) {
        is GameCards.ChordType -> chordConfusion?.isNotEmpty() == true
        is GameCards.Function -> functionConfusion?.isNotEmpty() == true
        is GameCards.Progression -> false // No confusion matrix for progressions yet
        is GameCards.Interval -> false  // TODO: Add interval confusion matrix
        is GameCards.Scale -> false  // TODO: Add scale confusion matrix
    }

    if (cardStats.isEmpty() && !hasConfusionData) {
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

    // Compute mastery distribution - exhaustive when on sealed class
    val masteryDist = remember(cards) {
        when (cards) {
            is GameCards.ChordType -> computeMasteryDistribution(cards.active)
            is GameCards.Function -> computeFunctionMasteryDistribution(cards.active)
            is GameCards.Progression -> computeProgressionMasteryDistribution(cards.active)
            is GameCards.Interval -> net.xmppwocky.earbs.model.MasteryDistribution(0, 0, 0, 0)  // TODO: Add interval mastery
            is GameCards.Scale -> net.xmppwocky.earbs.model.MasteryDistribution(0, 0, 0, 0)  // TODO: Add scale mastery
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mastery progress section
        if (masteryDist.total > 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MASTERY PROGRESS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        MasteryProgressBar(
                            distribution = masteryDist,
                            title = "${gameType.displayName} (${masteryDist.total} cards)"
                        )
                    }
                }
            }
        }

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

        // Chord type confusion matrix (only for CHORD_TYPE game)
        if (cards is GameCards.ChordType && onLoadChordConfusion != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Confusion Matrix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterChipRow(
                            label = "Octave:",
                            options = listOf(
                                FilterOption(null, "All", isAll = true),
                                FilterOption(3, "3"),
                                FilterOption(4, "4"),
                                FilterOption(5, "5")
                            ),
                            selected = octaveFilter,
                            onSelect = { octaveFilter = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        chordConfusion?.let { data ->
                            ConfusionMatrix(
                                data = data,
                                labelTransform = { name ->
                                    ChordType.entries.find { it.name == name }?.displayName ?: name
                                }
                            )
                        }
                    }
                }
            }
        }

        // Function confusion matrix (only for CHORD_FUNCTION game)
        if (cards is GameCards.Function && onLoadFunctionConfusion != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Confusion Matrix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterChipRow(
                            label = "Key:",
                            options = listOf(
                                FilterOption("MAJOR", "Major"),
                                FilterOption("MINOR", "Minor")
                            ),
                            selected = keyQualityFilter,
                            onSelect = { keyQualityFilter = it ?: "MAJOR" }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        functionConfusion?.let { data ->
                            ConfusionMatrix(
                                data = data,
                                labelTransform = { name ->
                                    ChordFunction.entries.find { it.name == name }?.displayName ?: name
                                }
                            )
                        }
                    }
                }
            }
        }

        // Note: CHORD_PROGRESSION doesn't have a confusion matrix yet

        // Per-card stats header
        if (cardStats.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Per-Card Statistics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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

    // Convert string to enum for comparison
    val statGameType = runCatching { GameType.valueOf(stat.gameType) }.getOrNull()
    val isFunction = statGameType == GameType.CHORD_FUNCTION

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
