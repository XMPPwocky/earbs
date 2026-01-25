package net.xmppwocky.earbs.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import net.xmppwocky.earbs.ComposeTestBase
import org.junit.Test

class CardStatsRowTest : ComposeTestBase() {

    @Test
    fun displaysCardName() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "Major @ Oct 4 (arp)",
                correct = 3,
                total = 4
            )
        }

        composeTestRule.onNodeWithText("Major @ Oct 4 (arp)").assertIsDisplayed()
    }

    @Test
    fun displaysCorrectOverTotal() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "Major @ Oct 4 (arp)",
                correct = 3,
                total = 4
            )
        }

        composeTestRule.onNodeWithText("3/4").assertIsDisplayed()
    }

    @Test
    fun displaysAccuracyPercentage() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "Major @ Oct 4 (arp)",
                correct = 3,
                total = 4
            )
        }

        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun displaysPerfectAccuracy() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "Minor @ Oct 4 (arp)",
                correct = 5,
                total = 5
            )
        }

        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun displaysZeroAccuracy() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "Sus2 @ Oct 4 (arp)",
                correct = 0,
                total = 3
            )
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun handlesZeroTrials() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "Sus4 @ Oct 4 (arp)",
                correct = 0,
                total = 0
            )
        }

        composeTestRule.onNodeWithText("0/0").assertIsDisplayed()
        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun displaysLongCardName() {
        composeTestRule.setContent {
            CardStatsRow(
                displayName = "V (major) @ Oct 4 (arp)",
                correct = 2,
                total = 3
            )
        }

        composeTestRule.onNodeWithText("V (major) @ Oct 4 (arp)").assertIsDisplayed()
    }
}
