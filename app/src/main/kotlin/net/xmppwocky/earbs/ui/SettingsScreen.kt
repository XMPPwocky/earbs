package net.xmppwocky.earbs.ui

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
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

// Default values
const val DEFAULT_PLAYBACK_DURATION = 500
const val DEFAULT_SESSION_SIZE = 20
const val DEFAULT_TARGET_RETENTION = 0.9f

// Ranges
const val PLAYBACK_DURATION_MIN = 300
const val PLAYBACK_DURATION_MAX = 1000
const val TARGET_RETENTION_MIN = 0.7f
const val TARGET_RETENTION_MAX = 0.95f

val SESSION_SIZE_OPTIONS = listOf(10, 20, 30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    onBackClicked: () -> Unit
) {
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
    var sessionSizeExpanded by remember { mutableStateOf(false) }

    Log.d(TAG, "SettingsScreen composing with playbackDuration=$playbackDuration, sessionSize=$sessionSize, targetRetention=$targetRetention")

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

            Spacer(modifier = Modifier.weight(1f))

            // Reset to Defaults button
            OutlinedButton(
                onClick = {
                    Log.i(TAG, "Resetting to defaults")
                    playbackDuration = DEFAULT_PLAYBACK_DURATION
                    sessionSize = DEFAULT_SESSION_SIZE
                    targetRetention = DEFAULT_TARGET_RETENTION
                    prefs.edit()
                        .putInt(PREF_KEY_PLAYBACK_DURATION, DEFAULT_PLAYBACK_DURATION)
                        .putInt(PREF_KEY_SESSION_SIZE, DEFAULT_SESSION_SIZE)
                        .putFloat(PREF_KEY_TARGET_RETENTION, DEFAULT_TARGET_RETENTION)
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
