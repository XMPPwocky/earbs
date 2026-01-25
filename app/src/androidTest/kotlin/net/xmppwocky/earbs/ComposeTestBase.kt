package net.xmppwocky.earbs

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

/**
 * Base class for Compose UI integration tests.
 * Provides Compose test rule and common utilities.
 *
 * Note: These tests focus on pure UI testing without database/repository
 * dependencies. The UI components receive their state as parameters,
 * making them easy to test in isolation.
 */
abstract class ComposeTestBase {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val DAY_MS = 24 * HOUR_MS
    }
}
