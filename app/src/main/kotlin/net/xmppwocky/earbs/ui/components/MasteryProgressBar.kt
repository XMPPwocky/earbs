package net.xmppwocky.earbs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.model.MasteryDistribution
import net.xmppwocky.earbs.model.MasteryLevel
import net.xmppwocky.earbs.ui.theme.MasteryColors

/**
 * Displays a horizontal stacked bar chart showing mastery distribution with legend.
 */
@Composable
fun MasteryProgressBar(
    distribution: MasteryDistribution,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    if (distribution.total == 0) return

    Column(modifier = modifier.fillMaxWidth()) {
        // Title with card count
        if (title != null) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            MasteryLevel.entries.forEach { level ->
                val count = distribution.countFor(level)
                if (count > 0) {
                    val weight = count.toFloat() / distribution.total
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(MasteryColors.forLevel(level))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MasteryLevel.entries.forEach { level ->
                val count = distribution.countFor(level)
                val percentage = distribution.percentageFor(level)
                LegendItem(
                    level = level,
                    count = count,
                    percentage = percentage
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    level: MasteryLevel,
    count: Int,
    percentage: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Color swatch
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MasteryColors.forLevel(level))
        )
        // Label with count and percentage
        Column {
            Text(
                text = level.displayName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$count (${percentage.toInt()}%)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
