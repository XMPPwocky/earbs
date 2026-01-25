package net.xmppwocky.earbs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.data.db.CardSessionAccuracy
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.fsrs.CardPhase
import org.junit.Test

/**
 * UI tests for CardDetailsScreen.
 *
 * Note: Since CardDetailsScreen requires a repository, these tests verify
 * the internal composable components used by the screen rather than the
 * full screen with database integration.
 */
class CardDetailsScreenTest : ComposeTestBase() {

    private fun createCardWithFsrs(
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

    private fun createSessionAccuracy(
        sessionId: Long,
        sessionDate: Long = System.currentTimeMillis(),
        trialsInSession: Int,
        correctInSession: Int
    ): CardSessionAccuracy {
        return CardSessionAccuracy(
            sessionId = sessionId,
            sessionDate = sessionDate,
            trialsInSession = trialsInSession,
            correctInSession = correctInSession
        )
    }

    // ========== Card Header Tests ==========

    @Test
    fun cardHeader_displaysCardName() {
        val card = createCardWithFsrs(chordType = "MINOR", octave = 5)

        composeTestRule.setContent {
            CardHeader(card)
        }

        composeTestRule.onNodeWithText("MINOR @ Octave 5").assertIsDisplayed()
    }

    @Test
    fun cardHeader_displaysPlaybackMode() {
        val card = createCardWithFsrs(playbackMode = "ARPEGGIATED")

        composeTestRule.setContent {
            CardHeader(card)
        }

        composeTestRule.onNodeWithText("Arpeggiated").assertIsDisplayed()
    }

    @Test
    fun cardHeader_displaysBlockPlaybackMode() {
        val card = createCardWithFsrs(playbackMode = "BLOCK")

        composeTestRule.setContent {
            CardHeader(card)
        }

        composeTestRule.onNodeWithText("Block").assertIsDisplayed()
    }

    // ========== FSRS Parameters Tests ==========

    @Test
    fun fsrsParameters_displaysStability() {
        val card = createCardWithFsrs(stability = 4.52)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Stability").assertIsDisplayed()
        composeTestRule.onNodeWithText("4.52").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysDifficulty() {
        val card = createCardWithFsrs(difficulty = 2.85)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Difficulty").assertIsDisplayed()
        composeTestRule.onNodeWithText("2.85").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysInterval() {
        val card = createCardWithFsrs(interval = 7)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Interval").assertIsDisplayed()
        composeTestRule.onNodeWithText("7 days").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysReviewCount() {
        val card = createCardWithFsrs(reviewCount = 15)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Reviews").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysPhaseReview() {
        val card = createCardWithFsrs(phase = CardPhase.Review.value)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Phase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Review").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysPhaseNew() {
        val card = createCardWithFsrs(phase = CardPhase.Added.value)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Phase").assertIsDisplayed()
        composeTestRule.onNodeWithText("New").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysPhaseReLearning() {
        val card = createCardWithFsrs(phase = CardPhase.ReLearning.value)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Phase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Re-learning").assertIsDisplayed()
    }

    @Test
    fun fsrsParameters_displaysLapses() {
        val card = createCardWithFsrs(lapses = 2)

        composeTestRule.setContent {
            FsrsParametersSection(card)
        }

        composeTestRule.onNodeWithText("Lapses").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
    }

    // ========== Lifetime Stats Tests ==========

    @Test
    fun lifetimeStats_displaysTotalTrials() {
        composeTestRule.setContent {
            LifetimeStatsSection(total = 47, correct = 38)
        }

        composeTestRule.onNodeWithText("LIFETIME STATS").assertIsDisplayed()
        composeTestRule.onNodeWithText("47").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
    }

    @Test
    fun lifetimeStats_displaysCorrectTrials() {
        composeTestRule.setContent {
            LifetimeStatsSection(total = 47, correct = 38)
        }

        composeTestRule.onNodeWithText("38").assertIsDisplayed()
        composeTestRule.onNodeWithText("Correct").assertIsDisplayed()
    }

    @Test
    fun lifetimeStats_displaysAccuracy() {
        composeTestRule.setContent {
            LifetimeStatsSection(total = 100, correct = 81)  // 81%
        }

        composeTestRule.onNodeWithText("81%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Accuracy").assertIsDisplayed()
    }

    // ========== Last Session Stats Tests ==========

    @Test
    fun lastSessionStats_displaysCorrectCount() {
        val lastSession = createSessionAccuracy(
            sessionId = 1,
            trialsInSession = 4,
            correctInSession = 3
        )

        composeTestRule.setContent {
            LastSessionSection(lastSession)
        }

        composeTestRule.onNodeWithText("LAST SESSION").assertIsDisplayed()
        composeTestRule.onNodeWithText("3/4").assertIsDisplayed()
        composeTestRule.onNodeWithText("correct").assertIsDisplayed()
    }

    @Test
    fun lastSessionStats_displaysAccuracy() {
        val lastSession = createSessionAccuracy(
            sessionId = 1,
            trialsInSession = 4,
            correctInSession = 3  // 75%
        )

        composeTestRule.setContent {
            LastSessionSection(lastSession)
        }

        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
        composeTestRule.onNodeWithText("accuracy").assertIsDisplayed()
    }

    // ========== Accuracy Over Time Tests ==========

    @Test
    fun accuracyOverTime_displaysNoDataMessage_whenEmpty() {
        composeTestRule.setContent {
            AccuracyOverTimeSection(sessions = emptyList())
        }

        composeTestRule.onNodeWithText("ACCURACY OVER TIME").assertIsDisplayed()
        composeTestRule.onNodeWithText("No review history yet").assertIsDisplayed()
    }

    @Test
    fun accuracyOverTime_displaysChart_whenHasData() {
        val sessions = listOf(
            createSessionAccuracy(sessionId = 1, trialsInSession = 4, correctInSession = 3),
            createSessionAccuracy(sessionId = 2, trialsInSession = 5, correctInSession = 5)
        )

        composeTestRule.setContent {
            AccuracyOverTimeSection(sessions = sessions)
        }

        composeTestRule.onNodeWithText("ACCURACY OVER TIME").assertIsDisplayed()
        // Chart should be displayed, no "No review history yet" message
        composeTestRule.onNodeWithText("No review history yet").assertDoesNotExist()
    }
}

