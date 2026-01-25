package net.xmppwocky.earbs.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Application color constants for consistent UI styling.
 */
object AppColors {
    val Success = Color(0xFF4CAF50)      // Green
    val SuccessLight = Color(0xFF8BC34A) // Light green
    val Error = Color(0xFFF44336)        // Red
    val Warning = Color(0xFFFFC107)      // Amber

    // Background colors for result cards
    val SuccessBackground = Color(0xFFE8F5E9)  // Light green background
    val WarningBackground = Color(0xFFFFF8E1)  // Light amber background
    val ErrorBackground = Color(0xFFFFEBEE)    // Light red background
}

/**
 * Accuracy percentage thresholds for color coding.
 */
object AccuracyThresholds {
    const val EXCELLENT = 90
    const val GOOD = 75
    const val FAIR = 60
}

/**
 * Colors for mastery level visualization.
 */
object MasteryColors {
    val Learning = Color(0xFF9E9E9E)   // Gray
    val Familiar = Color(0xFF2196F3)   // Blue
    val Confident = Color(0xFF8BC34A)  // Light Green
    val Mastered = Color(0xFF4CAF50)   // Green

    fun forLevel(level: net.xmppwocky.earbs.model.MasteryLevel): Color = when (level) {
        net.xmppwocky.earbs.model.MasteryLevel.LEARNING -> Learning
        net.xmppwocky.earbs.model.MasteryLevel.FAMILIAR -> Familiar
        net.xmppwocky.earbs.model.MasteryLevel.CONFIDENT -> Confident
        net.xmppwocky.earbs.model.MasteryLevel.MASTERED -> Mastered
    }
}

/**
 * Timing constants.
 */
object Timing {
    const val FEEDBACK_DELAY_MS = 500L
}
