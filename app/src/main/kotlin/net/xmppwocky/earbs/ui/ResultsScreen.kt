package net.xmppwocky.earbs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import net.xmppwocky.earbs.ui.theme.AccuracyThresholds
import net.xmppwocky.earbs.ui.theme.AppColors
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultsScreen(
    result: SessionResult,
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Session Complete",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Result card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score
                Text(
                    text = "${result.correctCount} / ${result.totalTrials}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "correct",
                    fontSize = 20.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Accuracy percentage
                Text(
                    text = "$accuracyPercent%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Done button
        Button(
            onClick = onDoneClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
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
