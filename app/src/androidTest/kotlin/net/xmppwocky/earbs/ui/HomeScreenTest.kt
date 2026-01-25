package net.xmppwocky.earbs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenTest : ComposeTestBase() {

    @Test
    fun displaysAppTitle() {
        composeTestRule.setContent {
            HomeScreen(
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Earbs").assertIsDisplayed()
    }

    @Test
    fun displaysSubtitle() {
        composeTestRule.setContent {
            HomeScreen(
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Chord Ear Training").assertIsDisplayed()
    }

    @Test
    fun displaysCorrectDueCount() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 15,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("15 cards due").assertIsDisplayed()
    }

    @Test
    fun displaysNoDueMessage_whenZeroDue() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 0,
                chordTypeUnlockedCount = 4,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("No cards due").assertIsDisplayed()
    }

    @Test
    fun displaysUnlockedCount() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeUnlockedCount = 12,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("12 / 48 cards unlocked").assertIsDisplayed()
    }

    @Test
    fun showsStartReviewButton_whenCardsUnlocked() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 5,
                chordTypeUnlockedCount = 4,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Start Review").assertIsDisplayed()
    }

    @Test
    fun showsPracticeEarlyButton_whenNoDue() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 0,
                chordTypeUnlockedCount = 4,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Practice Early").assertIsDisplayed()
    }

    @Test
    fun startButton_disabled_whenNoCardsUnlocked() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeUnlockedCount = 0,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Practice Early").assertIsNotEnabled()
    }

    @Test
    fun startButton_enabled_whenCardsUnlocked() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeUnlockedCount = 4,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Practice Early").assertIsEnabled()
    }

    @Test
    fun showsAddCardsButton_whenMoreToUnlock() {
        composeTestRule.setContent {
            HomeScreen(
                canUnlockMoreChordTypes = true,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Add 4 Cards").assertIsDisplayed()
    }

    @Test
    fun hidesAddCardsButton_atMaxUnlock() {
        composeTestRule.setContent {
            HomeScreen(
                canUnlockMoreChordTypes = false,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("All cards unlocked!").assertIsDisplayed()
    }

    @Test
    fun startButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            HomeScreen(
                chordTypeUnlockedCount = 4,
                onStartReviewClicked = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Practice Early").performClick()

        assertTrue(clicked)
    }

    @Test
    fun addCardsButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            HomeScreen(
                canUnlockMoreChordTypes = true,
                onStartReviewClicked = {},
                onAddCardsClicked = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Add 4 Cards").performClick()

        assertTrue(clicked)
    }

    @Test
    fun historyButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            HomeScreen(
                onStartReviewClicked = {},
                onHistoryClicked = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("History").performClick()

        assertTrue(clicked)
    }

    @Test
    fun settingsButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            HomeScreen(
                onStartReviewClicked = {},
                onSettingsClicked = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Settings").performClick()

        assertTrue(clicked)
    }

    // ========== Game Mode Tab Tests ==========

    @Test
    fun gameModeToggle_displaysChordTypeTab() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_TYPE,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Chord Type").assertIsDisplayed()
    }

    @Test
    fun gameModeToggle_displaysFunctionTab() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_TYPE,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Function").assertIsDisplayed()
    }

    @Test
    fun gameModeToggle_switchesToFunction() {
        var selectedMode = GameType.CHORD_TYPE
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = selectedMode,
                onGameModeChanged = { selectedMode = it },
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Function").performClick()

        assertEquals(GameType.CHORD_FUNCTION, selectedMode)
    }

    @Test
    fun gameModeToggle_switchesToChordType() {
        var selectedMode = GameType.CHORD_FUNCTION
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = selectedMode,
                onGameModeChanged = { selectedMode = it },
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Chord Type").performClick()

        assertEquals(GameType.CHORD_TYPE, selectedMode)
    }

    @Test
    fun displaysFunctionStats_whenFunctionModeSelected() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_FUNCTION,
                functionDueCount = 5,
                functionUnlockedCount = 9,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("5 cards due").assertIsDisplayed()
        composeTestRule.onNodeWithText("9 / 72 cards unlocked").assertIsDisplayed()
    }

    @Test
    fun displaysChordTypeInfo_whenChordTypeModeSelected() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_TYPE,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Identify chord quality (Major, Minor, etc.)").assertIsDisplayed()
    }

    @Test
    fun displaysFunctionInfo_whenFunctionModeSelected() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_FUNCTION,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Identify chord function (IV, V, vi, etc.)").assertIsDisplayed()
    }

    @Test
    fun showsAdd3CardsButton_forFunctionMode() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_FUNCTION,
                canUnlockMoreFunctions = true,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Add 3 Cards").assertIsDisplayed()
    }
}
