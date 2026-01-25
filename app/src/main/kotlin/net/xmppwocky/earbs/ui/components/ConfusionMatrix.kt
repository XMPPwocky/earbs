package net.xmppwocky.earbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.data.db.ConfusionEntry
import net.xmppwocky.earbs.ui.theme.AppColors

/**
 * Data for a confusion matrix with labels and counts.
 */
data class ConfusionMatrixData(
    val labels: List<String>,
    val counts: Array<IntArray>  // counts[row][col] = raw count
) {
    val size: Int get() = labels.size
    fun rowTotal(row: Int): Int = counts[row].sum()
    fun total(): Int = counts.sumOf { it.sum() }
    fun isEmpty(): Boolean = total() == 0

    /**
     * Get percentage for a cell (row-normalized).
     * "When actual=row, what % of the time was answered=col?"
     */
    fun percentage(row: Int, col: Int): Float {
        val rowSum = rowTotal(row)
        return if (rowSum > 0) counts[row][col].toFloat() / rowSum * 100 else 0f
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ConfusionMatrixData
        if (labels != other.labels) return false
        if (!counts.contentDeepEquals(other.counts)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = labels.hashCode()
        result = 31 * result + counts.contentDeepHashCode()
        return result
    }
}

/**
 * Build a confusion matrix from ConfusionEntry list.
 *
 * @param entries List of (actual, answered, count) entries from the database
 * @param orderedLabels List of labels in display order (row/column headers)
 * @return ConfusionMatrixData with counts populated
 */
fun buildConfusionMatrix(
    entries: List<ConfusionEntry>,
    orderedLabels: List<String>
): ConfusionMatrixData {
    val size = orderedLabels.size
    val labelIndex = orderedLabels.withIndex().associate { it.value to it.index }
    val matrix = Array(size) { IntArray(size) { 0 } }

    for (entry in entries) {
        val row = labelIndex[entry.actual] ?: continue
        val col = labelIndex[entry.answered] ?: continue
        matrix[row][col] = entry.count
    }
    return ConfusionMatrixData(orderedLabels, matrix)
}

/**
 * Display a confusion matrix as a grid.
 *
 * Rows = actual chord type/function
 * Columns = what user answered
 * Cells show row-normalized percentages
 * Diagonal (correct) = green, off-diagonal (confusion) = red gradient
 */
@Composable
fun ConfusionMatrix(
    data: ConfusionMatrixData,
    modifier: Modifier = Modifier,
    labelTransform: (String) -> String = { it }
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data for selected filter",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row with column labels
        Row(modifier = Modifier.fillMaxWidth()) {
            // Empty corner cell
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
            )
            // Column headers (answered labels)
            data.labels.forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelTransform(label),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // Data rows
        data.labels.forEachIndexed { rowIndex, rowLabel ->
            Row(modifier = Modifier.fillMaxWidth()) {
                // Row header (actual label)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelTransform(rowLabel),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // Data cells
                data.labels.forEachIndexed { colIndex, _ ->
                    val count = data.counts[rowIndex][colIndex]
                    val percentage = data.percentage(rowIndex, colIndex)
                    val isDiagonal = rowIndex == colIndex

                    val backgroundColor = when {
                        count == 0 -> Color.Transparent
                        isDiagonal -> AppColors.Success.copy(alpha = (percentage / 100f).coerceIn(0.1f, 0.6f))
                        else -> AppColors.Error.copy(alpha = (percentage / 100f).coerceIn(0.1f, 0.6f))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(backgroundColor)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (count > 0) {
                            Text(
                                text = "${percentage.toInt()}%",
                                fontSize = 8.sp,
                                fontWeight = if (isDiagonal) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDiagonal) AppColors.Success else AppColors.Error
                            )
                        }
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(AppColors.Success.copy(alpha = 0.4f))
            )
            Text(
                text = " Correct",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(AppColors.Error.copy(alpha = 0.4f))
            )
            Text(
                text = " Confusion",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
