package net.xmppwocky.earbs.ui

import android.content.SharedPreferences
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val TAG = "SettingsScreen"

// Settings keys
const val PREF_KEY_PLAYBACK_DURATION = "playback_duration"
const val PREF_KEY_SESSION_SIZE = "session_size"
const val PREF_KEY_TARGET_RETENTION = "target_retention"
const val PREF_KEY_AUTO_ADVANCE_DELAY = "auto_advance_delay"
const val PREF_KEY_LEARN_FROM_MISTAKES = "learn_from_mistakes"

// Default values
const val DEFAULT_PLAYBACK_DURATION = 500
const val DEFAULT_SESSION_SIZE = 20
const val DEFAULT_TARGET_RETENTION = 0.9f
const val DEFAULT_AUTO_ADVANCE_DELAY = 750  // ms
const val DEFAULT_LEARN_FROM_MISTAKES = true

// Ranges
const val PLAYBACK_DURATION_MIN = 300
const val PLAYBACK_DURATION_MAX = 1000
const val TARGET_RETENTION_MIN = 0.7f
const val TARGET_RETENTION_MAX = 0.95f
const val AUTO_ADVANCE_DELAY_MIN = 300
const val AUTO_ADVANCE_DELAY_MAX = 2000

val SESSION_SIZE_OPTIONS = listOf(10, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    onBackClicked: () -> Unit,
    onBackupClicked: () -> Unit = {},
    onRestoreClicked: () -> Unit = {}
) {
    BackHandler { onBackClicked() }

    // Load current values from prefs
    var playbackDuration by remember {
        mutableIntStateOf(prefs.getInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION))
    }
    var sessionSize by remember {
        mutableIntStateOf(prefs.getInt(PREF_KEY_SESSION_SIZE, DEFAULT_SESSION_SIZE))
    }
    var targetRetention by remember {
        mutableFloatStateOf(prefs.getFloat(PREF_KEY_TARGET_RETENTION, DEFAULT_TARGET_RETENTION))
    }
    var autoAdvanceDelay by remember {
        mutableIntStateOf(prefs.getInt(PREF_KEY_AUTO_ADVANCE_DELAY, DEFAULT_AUTO_ADVANCE_DELAY))
    }
    var learnFromMistakes by remember {
        mutableStateOf(prefs.getBoolean(PREF_KEY_LEARN_FROM_MISTAKES, DEFAULT_LEARN_FROM_MISTAKES))
    }
    var sessionSizeExpanded by remember { mutableStateOf(false) }

    Log.d(TAG, "SettingsScreen composing with playbackDuration=$playbackDuration, sessionSize=$sessionSize, targetRetention=$targetRetention, autoAdvanceDelay=$autoAdvanceDelay, learnFromMistakes=$learnFromMistakes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Playback Duration Setting
            SettingSection(title = "Playback Duration") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${playbackDuration}ms",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${PLAYBACK_DURATION_MIN}-${PLAYBACK_DURATION_MAX}ms",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = playbackDuration.toFloat(),
                        onValueChange = { newValue ->
                            playbackDuration = newValue.roundToInt()
                        },
                        onValueChangeFinished = {
                            Log.i(TAG, "Playback duration changed to $playbackDuration")
                            prefs.edit().putInt(PREF_KEY_PLAYBACK_DURATION, playbackDuration).apply()
                        },
                        valueRange = PLAYBACK_DURATION_MIN.toFloat()..PLAYBACK_DURATION_MAX.toFloat(),
                        steps = ((PLAYBACK_DURATION_MAX - PLAYBACK_DURATION_MIN) / 50) - 1
                    )
                    Text(
                        text = "How long each chord plays",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Auto-Advance Delay Setting
            SettingSection(title = "Auto-Advance Delay") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${autoAdvanceDelay}ms",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${AUTO_ADVANCE_DELAY_MIN}-${AUTO_ADVANCE_DELAY_MAX}ms",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = autoAdvanceDelay.toFloat(),
                        onValueChange = { newValue ->
                            autoAdvanceDelay = newValue.roundToInt()
                        },
                        onValueChangeFinished = {
                            Log.i(TAG, "Auto-advance delay changed to $autoAdvanceDelay")
                            prefs.edit().putInt(PREF_KEY_AUTO_ADVANCE_DELAY, autoAdvanceDelay).apply()
                        },
                        valueRange = AUTO_ADVANCE_DELAY_MIN.toFloat()..AUTO_ADVANCE_DELAY_MAX.toFloat(),
                        steps = ((AUTO_ADVANCE_DELAY_MAX - AUTO_ADVANCE_DELAY_MIN) / 100) - 1
                    )
                    Text(
                        text = "Feedback display time before auto-playing next chord",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Learn from Mistakes Setting
            SettingSection(title = "Learn from Mistakes") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (learnFromMistakes) "Enabled" else "Disabled",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = learnFromMistakes,
                            onCheckedChange = { newValue ->
                                learnFromMistakes = newValue
                                Log.i(TAG, "Learn from mistakes changed to $newValue")
                                prefs.edit().putBoolean(PREF_KEY_LEARN_FROM_MISTAKES, newValue).apply()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When wrong, hear your answer, explore chords, and manually advance",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Session Size Setting
            SettingSection(title = "Session Size") {
                Column {
                    ExposedDropdownMenuBox(
                        expanded = sessionSizeExpanded,
                        onExpandedChange = { sessionSizeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "$sessionSize cards",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionSizeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = sessionSizeExpanded,
                            onDismissRequest = { sessionSizeExpanded = false }
                        ) {
                            SESSION_SIZE_OPTIONS.forEach { size ->
                                DropdownMenuItem(
                                    text = { Text("$size cards") },
                                    onClick = {
                                        sessionSize = size
                                        sessionSizeExpanded = false
                                        Log.i(TAG, "Session size changed to $sessionSize")
                                        prefs.edit().putInt(PREF_KEY_SESSION_SIZE, sessionSize).apply()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Number of cards per review session",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target Retention Setting
            SettingSection(title = "FSRS Target Retention") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(targetRetention * 100).roundToInt()}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(TARGET_RETENTION_MIN * 100).roundToInt()}-${(TARGET_RETENTION_MAX * 100).roundToInt()}%",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = targetRetention,
                        onValueChange = { newValue ->
                            // Round to nearest 0.01
                            targetRetention = (newValue * 100).roundToInt() / 100f
                        },
                        onValueChangeFinished = {
                            Log.i(TAG, "Target retention changed to $targetRetention")
                            prefs.edit().putFloat(PREF_KEY_TARGET_RETENTION, targetRetention).apply()
                        },
                        valueRange = TARGET_RETENTION_MIN..TARGET_RETENTION_MAX,
                        steps = 24  // 0.70 to 0.95 in 0.01 increments = 25 steps, so 24 intermediate steps
                    )
                    Text(
                        text = "Higher = more frequent reviews, better retention",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Data Backup/Restore Section
            SettingSection(title = "Data") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            Log.i(TAG, "Backup button clicked")
                            onBackupClicked()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Backup Database")
                    }
                    Text(
                        text = "Save your progress to a file",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            Log.i(TAG, "Restore button clicked")
                            onRestoreClicked()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Restore Database")
                    }
                    Text(
                        text = "Replace current data with a backup",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reset to Defaults button
            OutlinedButton(
                onClick = {
                    Log.i(TAG, "Resetting to defaults")
                    playbackDuration = DEFAULT_PLAYBACK_DURATION
                    sessionSize = DEFAULT_SESSION_SIZE
                    targetRetention = DEFAULT_TARGET_RETENTION
                    autoAdvanceDelay = DEFAULT_AUTO_ADVANCE_DELAY
                    learnFromMistakes = DEFAULT_LEARN_FROM_MISTAKES
                    prefs.edit()
                        .putInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION)
                        .putInt(PREF_KEY_SESSION_SIZE, DEFAULT_SESSION_SIZE)
                        .putFloat(PREF_KEY_TARGET_RETENTION, DEFAULT_TARGET_RETENTION)
                        .putInt(PREF_KEY_AUTO_ADVANCE_DELAY, DEFAULT_AUTO_ADVANCE_DELAY)
                        .putBoolean(PREF_KEY_LEARN_FROM_MISTAKES, DEFAULT_LEARN_FROM_MISTAKES)
                        .apply()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}
