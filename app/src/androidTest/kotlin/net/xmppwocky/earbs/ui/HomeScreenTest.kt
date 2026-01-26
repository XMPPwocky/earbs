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
    fun gameModeToggle_displaysTypesTab() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_TYPE,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Types (0)").assertIsDisplayed()
    }

    @Test
    fun gameModeToggle_displaysFunctionsTab() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_TYPE,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Functions (0)").assertIsDisplayed()
    }

    @Test
    fun gameModeToggle_displaysProgressionsTab() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_TYPE,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Progressions (0)").assertIsDisplayed()
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

        composeTestRule.onNodeWithText("Functions (0)").performClick()

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

        composeTestRule.onNodeWithText("Types (0)").performClick()

        assertEquals(GameType.CHORD_TYPE, selectedMode)
    }

    @Test
    fun gameModeToggle_switchesToProgression() {
        var selectedMode = GameType.CHORD_TYPE
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = selectedMode,
                onGameModeChanged = { selectedMode = it },
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Progressions (0)").performClick()

        assertEquals(GameType.CHORD_PROGRESSION, selectedMode)
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

    // ========== Progression Mode Tests ==========

    @Test
    fun displaysProgressionStats_whenProgressionModeSelected() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_PROGRESSION,
                progressionDueCount = 8,
                progressionUnlockedCount = 12,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("8 cards due").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 / 96 cards unlocked").assertIsDisplayed()
    }

    @Test
    fun displaysProgressionInfo_whenProgressionModeSelected() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_PROGRESSION,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Identify chord progressions (I-IV-V-I, etc.)").assertIsDisplayed()
    }

    @Test
    fun displaysUnlockCardsMessage_whenNoProgressionCardsUnlocked() {
        composeTestRule.setContent {
            HomeScreen(
                selectedGameMode = GameType.CHORD_PROGRESSION,
                progressionDueCount = 0,
                progressionUnlockedCount = 0,
                onStartReviewClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Unlock cards in History > Cards").assertIsDisplayed()
    }

    // ========== Tab Due Count Tests ==========

    @Test
    fun tabTitle_showsTypesDueCount() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 5,
                onStartReviewClicked = {}
            )
        }
        composeTestRule.onNodeWithText("Types (5)").assertIsDisplayed()
    }

    @Test
    fun tabTitle_showsFunctionsDueCount() {
        composeTestRule.setContent {
            HomeScreen(
                functionDueCount = 3,
                onStartReviewClicked = {}
            )
        }
        composeTestRule.onNodeWithText("Functions (3)").assertIsDisplayed()
    }

    @Test
    fun tabTitle_showsProgressionsDueCount() {
        composeTestRule.setContent {
            HomeScreen(
                progressionDueCount = 7,
                onStartReviewClicked = {}
            )
        }
        composeTestRule.onNodeWithText("Progressions (7)").assertIsDisplayed()
    }

    @Test
    fun tabTitles_showZeroDueCounts() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 0,
                functionDueCount = 0,
                progressionDueCount = 0,
                onStartReviewClicked = {}
            )
        }
        composeTestRule.onNodeWithText("Types (0)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Functions (0)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progressions (0)").assertIsDisplayed()
    }

    @Test
    fun tabTitles_updateWithDifferentCounts() {
        composeTestRule.setContent {
            HomeScreen(
                chordTypeDueCount = 12,
                functionDueCount = 7,
                progressionDueCount = 4,
                onStartReviewClicked = {}
            )
        }
        composeTestRule.onNodeWithText("Types (12)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Functions (7)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progressions (4)").assertIsDisplayed()
    }
}
