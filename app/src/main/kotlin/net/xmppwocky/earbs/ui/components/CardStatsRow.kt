package net.xmppwocky.earbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.ui.theme.AccuracyThresholds
import net.xmppwocky.earbs.ui.theme.AppColors

/**
 * A reusable row component for displaying per-card statistics.
 * Used by ResultsScreen and HistoryScreen (session details).
 *
 * Shows: Card name | correct/total | accuracy%
 * Background color is based on accuracy.
 *
 * @param displayName Human-readable card name (e.g., "Major @ Oct 4 (arp)")
 * @param correct Number of correct answers for this card
 * @param total Total number of trials for this card
 * @param modifier Optional modifier for the row
 */
@Composable
fun CardStatsRow(
    displayName: String,
    correct: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val accuracy = if (total > 0) (correct.toFloat() / total * 100).toInt() else 0

    // Determine background color based on accuracy
    val backgroundColor = when {
        accuracy >= AccuracyThresholds.EXCELLENT -> AppColors.SuccessBackground
        accuracy >= AccuracyThresholds.GOOD -> AppColors.WarningBackground
        else -> AppColors.ErrorBackground
    }

    // Determine text color for accuracy percentage
    val accuracyColor = when {
        accuracy >= AccuracyThresholds.EXCELLENT -> AppColors.Success
        accuracy >= AccuracyThresholds.GOOD -> AppColors.Warning
        else -> AppColors.Error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Card name (left side)
        Text(
            text = displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Correct/Total (middle)
        Text(
            text = "$correct/$total",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Accuracy percentage (right side)
        Text(
            text = "$accuracy%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accuracyColor
        )
    }
}
