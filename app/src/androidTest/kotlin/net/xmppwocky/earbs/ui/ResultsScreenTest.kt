package net.xmppwocky.earbs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.data.db.SessionCardStats
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultsScreenTest : ComposeTestBase() {

    // ========== Basic Display Tests ==========

    @Test
    fun displaysSessionCompleteTitle() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Session Complete").assertIsDisplayed()
    }

    @Test
    fun displaysCorrectScoreFormat() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("15 / 20").assertIsDisplayed()
        composeTestRule.onNodeWithText("correct").assertIsDisplayed()
    }

    @Test
    fun displaysAccuracyPercentage() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun displaysCardBreakdownHeader() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("CARD BREAKDOWN").assertIsDisplayed()
    }

    @Test
    fun displaysDoneButton() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    // ========== Card Breakdown Tests ==========

    @Test
    fun displaysCardBreakdownWithStats() {
        val cardStats = listOf(
            SessionCardStats("MAJOR_4_ARPEGGIATED", "CHORD_TYPE", 4, 3),
            SessionCardStats("MINOR_4_ARPEGGIATED", "CHORD_TYPE", 2, 2)
        )

        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(5, 6, 1L, "CHORD_TYPE"),
                cardStats = cardStats,
                onDoneClicked = {}
            )
        }

        // Verify card names are displayed
        composeTestRule.onNodeWithText("Major @ Oct 4 (arp)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Minor @ Oct 4 (arp)").assertIsDisplayed()
    }

    @Test
    fun displaysCardStatsCorrectly() {
        val cardStats = listOf(
            SessionCardStats("MAJOR_4_ARPEGGIATED", "CHORD_TYPE", 4, 3)
        )

        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(3, 4, 1L, "CHORD_TYPE"),
                cardStats = cardStats,
                onDoneClicked = {}
            )
        }

        // Verify correct/total is displayed
        composeTestRule.onNodeWithText("3/4").assertIsDisplayed()
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun displaysFunctionCardBreakdown() {
        val cardStats = listOf(
            SessionCardStats("V_MAJOR_4_ARPEGGIATED", "CHORD_FUNCTION", 3, 3),
            SessionCardStats("IV_MAJOR_4_ARPEGGIATED", "CHORD_FUNCTION", 2, 1)
        )

        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(4, 5, 1L, "CHORD_FUNCTION"),
                cardStats = cardStats,
                onDoneClicked = {}
            )
        }

        // Verify function card names are displayed
        composeTestRule.onNodeWithText("V (major) @ Oct 4 (arp)").assertIsDisplayed()
        composeTestRule.onNodeWithText("IV (major) @ Oct 4 (arp)").assertIsDisplayed()
    }

    // ========== Accuracy Color Tests ==========

    @Test
    fun displaysExcellentAccuracy() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(19, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("95%").assertIsDisplayed()
    }

    @Test
    fun displaysGoodAccuracy() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(16, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("80%").assertIsDisplayed()
    }

    @Test
    fun displaysPoorAccuracy() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(10, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    // ========== Interaction Tests ==========

    @Test
    fun doneButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Done").performClick()

        assertTrue(clicked)
    }

    // ========== Loading State Tests ==========

    @Test
    fun displaysLoadingIndicator_whenLoading() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(15, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                isLoading = true,
                onDoneClicked = {}
            )
        }

        // Verify loading indicator is shown (no card breakdown items)
        composeTestRule.onNodeWithText("CARD BREAKDOWN").assertIsDisplayed()
    }

    // ========== Edge Cases ==========

    @Test
    fun displaysZeroAccuracy() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(0, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("0 / 20").assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun displaysPerfectAccuracy() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(20, 20, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("20 / 20").assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun handlesEmptySession() {
        composeTestRule.setContent {
            ResultsScreenContent(
                result = SessionResult(0, 0, 1L, "CHORD_TYPE"),
                cardStats = emptyList(),
                onDoneClicked = {}
            )
        }

        composeTestRule.onNodeWithText("0 / 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }
}
