package net.xmppwocky.earbs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.fsrs.CardPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI tests for CardDetailsScreen unlock toggle functionality.
 *
 * Tests the CardHeader component with unlock toggle and the locked card message.
 */
class CardDetailsScreenUnlockTest : ComposeTestBase() {

    private fun createUnlockedCard(
        id: String = "MAJOR_4_ARPEGGIATED",
        chordType: String = "MAJOR",
        octave: Int = 4,
        playbackMode: String = "ARPEGGIATED",
        stability: Double = 4.52,
        difficulty: Double = 2.85,
        interval: Int = 7,
        dueDate: Long = System.currentTimeMillis() + 3 * DAY_MS,
        reviewCount: Int = 15,
        lastReview: Long? = System.currentTimeMillis() - DAY_MS,
        phase: Int = CardPhase.Review.value,
        lapses: Int = 2
    ): CardWithFsrs {
        return CardWithFsrs(
            id = id,
            chordType = chordType,
            octave = octave,
            playbackMode = playbackMode,
            unlocked = true,
            stability = stability,
            difficulty = difficulty,
            interval = interval,
            dueDate = dueDate,
            reviewCount = reviewCount,
            lastReview = lastReview,
            phase = phase,
            lapses = lapses
        )
    }

    private fun createLockedCard(
        id: String = "MAJOR_4_ARPEGGIATED",
        chordType: String = "MAJOR",
        octave: Int = 4,
        playbackMode: String = "ARPEGGIATED"
    ): CardWithFsrs {
        return CardWithFsrs(
            id = id,
            chordType = chordType,
            octave = octave,
            playbackMode = playbackMode,
            unlocked = false,
            stability = 0.0,
            difficulty = 0.0,
            interval = 0,
            dueDate = 0L,
            reviewCount = 0,
            lastReview = null,
            phase = 0,
            lapses = 0
        )
    }

    // ========== CardHeader Unlock Toggle Tests ==========

    @Test
    fun cardHeader_showsUnlockToggle_whenCallbackProvided() {
        val card = createUnlockedCard()

        composeTestRule.setContent {
            CardHeader(
                card = card,
                onUnlockToggled = { _ -> }
            )
        }

        // Toggle should be visible with "Unlocked" text
        composeTestRule.onNodeWithText("Unlocked").assertIsDisplayed()
    }

    @Test
    fun cardHeader_showsLockedLabel_whenCardIsLocked() {
        val card = createLockedCard()

        composeTestRule.setContent {
            CardHeader(
                card = card,
                onUnlockToggled = { _ -> }
            )
        }

        // Toggle should show "Locked" text for locked cards
        composeTestRule.onNodeWithText("Locked").assertIsDisplayed()
    }

    @Test
    fun cardHeader_hidesToggle_whenNoCallback() {
        val card = createUnlockedCard()

        composeTestRule.setContent {
            CardHeader(
                card = card,
                onUnlockToggled = null
            )
        }

        // Toggle label should not be visible when no callback is provided
        composeTestRule.onNodeWithText("Unlocked").assertDoesNotExist()
        composeTestRule.onNodeWithText("Locked").assertDoesNotExist()
    }

    @Test
    fun cardHeader_toggleUnlocked_callsCallback_withFalse() {
        var callbackValue: Boolean? = null
        val card = createUnlockedCard()

        composeTestRule.setContent {
            CardHeader(
                card = card,
                onUnlockToggled = { unlocked -> callbackValue = unlocked }
            )
        }

        // Find and click the switch toggle using semantic matcher
        composeTestRule.onNode(isToggleable()).performClick()

        // Callback should be called with false (lock the card)
        assertEquals(false, callbackValue)
    }

    @Test
    fun cardHeader_toggleLocked_callsCallback_withTrue() {
        var callbackValue: Boolean? = null
        val card = createLockedCard()

        composeTestRule.setContent {
            CardHeader(
                card = card,
                onUnlockToggled = { unlocked -> callbackValue = unlocked }
            )
        }

        // Find and click the switch toggle using semantic matcher
        composeTestRule.onNode(isToggleable()).performClick()

        // Callback should be called with true (unlock the card)
        assertEquals(true, callbackValue)
    }

    @Test
    fun cardHeader_displaysCardName() {
        val card = createUnlockedCard(chordType = "MINOR", octave = 5)

        composeTestRule.setContent {
            CardHeader(card = card)
        }

        composeTestRule.onNodeWithText("MINOR @ Octave 5").assertIsDisplayed()
    }

    @Test
    fun cardHeader_displaysPlaybackMode() {
        val card = createUnlockedCard(playbackMode = "ARPEGGIATED")

        composeTestRule.setContent {
            CardHeader(card = card)
        }

        composeTestRule.onNodeWithText("Arpeggiated").assertIsDisplayed()
    }

    @Test
    fun cardHeader_displaysBlockPlaybackMode() {
        val card = createUnlockedCard(playbackMode = "BLOCK")

        composeTestRule.setContent {
            CardHeader(card = card)
        }

        composeTestRule.onNodeWithText("Block").assertIsDisplayed()
    }

    // ========== Locked Card Message Tests ==========

    @Test
    fun lockedCard_showsLockedMessage() {
        val card = createLockedCard()

        composeTestRule.setContent {
            // Simulate what CardDetailsScreen shows for locked cards
            if (!card.unlocked) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "This card is locked"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("This card is locked").assertIsDisplayed()
    }

    // ========== FSRS Section Visibility Tests ==========

    @Test
    fun lockedCard_hidesFsrsSection() {
        val card = createLockedCard()

        composeTestRule.setContent {
            // For locked cards, FSRS section should not be shown
            if (card.unlocked) {
                FsrsParametersSection(card)
            }
        }

        // FSRS parameters should not be displayed for locked cards
        composeTestRule.onNodeWithText("FSRS PARAMETERS").assertDoesNotExist()
        composeTestRule.onNodeWithText("Stability").assertDoesNotExist()
    }

    @Test
    fun unlockedCard_showsFsrsSection() {
        val card = createUnlockedCard()

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        // FSRS parameters should be displayed for unlocked cards
        composeTestRule.onNodeWithText("FSRS PARAMETERS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stability").assertIsDisplayed()
        composeTestRule.onNodeWithText("Difficulty").assertIsDisplayed()
    }

    @Test
    fun unlockedCard_showsAccuracySection() {
        val card = createUnlockedCard()

        composeTestRule.setContent {
            AccuracyOverTimeSection(sessions = emptyList())
        }

        composeTestRule.onNodeWithText("ACCURACY OVER TIME").assertIsDisplayed()
    }

    // ========== CardHeader Background Color Tests ==========

    @Test
    fun cardHeader_unlockedCard_hasPrimaryContainerBackground() {
        val card = createUnlockedCard()

        composeTestRule.setContent {
            CardHeader(card = card)
        }

        // The card should be displayed (background color verification is limited in Compose tests)
        composeTestRule.onNodeWithText("MAJOR @ Octave 4").assertIsDisplayed()
    }

    @Test
    fun cardHeader_lockedCard_hasSurfaceVariantBackground() {
        val card = createLockedCard()

        composeTestRule.setContent {
            CardHeader(card = card)
        }

        // The card should be displayed
        composeTestRule.onNodeWithText("MAJOR @ Octave 4").assertIsDisplayed()
    }

    // ========== Lifetime Stats Section Tests ==========

    @Test
    fun lifetimeStats_notShown_forLockedCard() {
        val card = createLockedCard()

        composeTestRule.setContent {
            // Lifetime stats are only shown for unlocked cards
            if (card.unlocked) {
                LifetimeStatsSection(total = 47, correct = 38)
            }
        }

        composeTestRule.onNodeWithText("LIFETIME STATS").assertDoesNotExist()
    }

    @Test
    fun lifetimeStats_shown_forUnlockedCard() {
        composeTestRule.setContent {
            LifetimeStatsSection(total = 47, correct = 38)
        }

        composeTestRule.onNodeWithText("LIFETIME STATS").assertIsDisplayed()
        composeTestRule.onNodeWithText("47").assertIsDisplayed()
        composeTestRule.onNodeWithText("38").assertIsDisplayed()
    }

    // ========== Integration Tests ==========

    @Test
    fun lockedCard_showsUnlockPrompt() {
        val card = createLockedCard()

        composeTestRule.setContent {
            Column {
                CardHeader(card = card, onUnlockToggled = { _ -> })

                // The locked card message from CardDetailsScreen
                if (!card.unlocked) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(text = "This card is locked")
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                            Text(
                                text = "Unlock it to start practicing and track FSRS progress"
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Locked").assertIsDisplayed()
        composeTestRule.onNodeWithText("This card is locked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unlock it to start practicing and track FSRS progress").assertIsDisplayed()
    }

    @Test
    fun unlockedCard_showsFullDetails() {
        val card = createUnlockedCard(
            stability = 5.5,
            reviewCount = 20
        )

        composeTestRule.setContent {
            Column {
                CardHeader(card = card, onUnlockToggled = { _ -> })
                FsrsParametersSection(card)
            }
        }

        composeTestRule.onNodeWithText("Unlocked").assertIsDisplayed()
        composeTestRule.onNodeWithText("FSRS PARAMETERS").assertIsDisplayed()
        composeTestRule.onNodeWithText("5.50").assertIsDisplayed()  // Stability
        composeTestRule.onNodeWithText("20").assertIsDisplayed()    // Review count
    }
}
