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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.xmppwocky.earbs.model.CardScore
import net.xmppwocky.earbs.model.Grade

@Composable
fun ResultsScreen(
    results: List<CardScore>,
    onDoneClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Session Complete",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Summary stats
        val totalCorrect = results.sumOf { it.correct }
        val totalTrials = results.sumOf { it.total }
        Text(
            text = "Overall: $totalCorrect / $totalTrials correct",
            fontSize = 18.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Results cards
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            results.forEach { score ->
                ResultCard(score = score)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Done button
        Button(
            onClick = onDoneClicked,
            modifier = Modifier
                .fillMaxWidth()
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

@Composable
private fun ResultCard(score: CardScore) {
    val gradeColor = when (score.grade) {
        Grade.EASY -> Color(0xFF4CAF50)  // Green
        Grade.GOOD -> Color(0xFF8BC34A)  // Light green
        Grade.HARD -> Color(0xFFFFC107)  // Amber
        Grade.AGAIN -> Color(0xFFF44336) // Red
    }

    val gradeBackgroundColor = when (score.grade) {
        Grade.EASY -> Color(0xFFE8F5E9)  // Light green background
        Grade.GOOD -> Color(0xFFF1F8E9)  // Lighter green background
        Grade.HARD -> Color(0xFFFFF8E1)  // Light amber background
        Grade.AGAIN -> Color(0xFFFFEBEE) // Light red background
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = gradeBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card name
            Column {
                Text(
                    text = score.card.chordType.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Octave ${score.card.octave}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Score
            Text(
                text = "${score.correct}/${score.total}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Grade badge
            Box(
                modifier = Modifier
                    .background(
                        color = gradeColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = score.grade.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
