package net.xmppwocky.earbs.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic filter option for statistics.
 * Extensible: add new filter dimensions as needed.
 */
data class FilterOption<T>(
    val value: T,
    val label: String,
    val isAll: Boolean = false  // True for "All" option that doesn't filter
)

/**
 * Current filter state. Null means "All" (no filtering).
 */
data class StatsFilters(
    val octave: Int? = null,        // Game 1: filter by octave (3, 4, 5, or null=all)
    val keyQuality: String? = null  // Game 2: filter by key quality (MAJOR, MINOR)
    // Future: playbackMode, dateRange, etc.
)

/**
 * Composable for filter chips.
 * Reusable for other statistics views.
 */
@Composable
fun <T> FilterChipRow(
    label: String,
    options: List<FilterOption<T>>,
    selected: T?,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(8.dp))
        options.forEach { option ->
            FilterChip(
                selected = if (option.isAll) selected == null else selected == option.value,
                onClick = { onSelect(if (option.isAll) null else option.value) },
                label = { Text(option.label) }
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}
