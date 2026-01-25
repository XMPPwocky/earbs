package net.xmppwocky.earbs.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.xmppwocky.earbs.ComposeTestBase
import net.xmppwocky.earbs.data.db.CardStatsView
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.SessionOverview
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryScreenTest : ComposeTestBase() {

    private fun createSessionOverview(
        id: Long = 1,
        startedAt: Long = System.currentTimeMillis(),
        completedAt: Long? = startedAt + 5 * 60 * 1000,
        gameType: String = GameType.CHORD_TYPE.name,
        totalTrials: Int = 20,
        correctTrials: Int = 15
    ): SessionOverview {
        return SessionOverview(
            id = id,
            startedAt = startedAt,
            completedAt = completedAt,
            gameType = gameType,
            octave = 4,
            totalTrials = totalTrials,
            correctTrials = correctTrials
        )
    }

    private fun createCardWithFsrs(
        id: String = "MAJOR_4_ARPEGGIATED",
        chordType: String = "MAJOR",
        octave: Int = 4,
        playbackMode: String = "ARPEGGIATED",
        dueDate: Long = System.currentTimeMillis(),
        reviewCount: Int = 5,
        interval: Int = 3,
        stability: Double = 4.5
    ): CardWithFsrs {
        return CardWithFsrs(
            id = id,
            chordType = chordType,
            octave = octave,
            playbackMode = playbackMode,
            unlocked = true,
            stability = stability,
            difficulty = 2.5,
            interval = interval,
            dueDate = dueDate,
            reviewCount = reviewCount,
            lastReview = System.currentTimeMillis() - DAY_MS,
            phase = 2,
            lapses = 0
        )
    }

    private fun createCardStats(
        cardId: String = "MAJOR_4_ARPEGGIATED",
        gameType: String = GameType.CHORD_TYPE.name,
        totalTrials: Int = 10,
        correctTrials: Int = 8
    ): CardStatsView {
        return CardStatsView(
            cardId = cardId,
            gameType = gameType,
            totalTrials = totalTrials,
            correctTrials = correctTrials
        )
    }

    // ========== Tab Tests ==========

    @Test
    fun displaysSessionsTab() {
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Sessions").assertIsDisplayed()
    }

    @Test
    fun displaysCardsTab() {
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Cards").assertIsDisplayed()
    }

    @Test
    fun displaysStatsTab() {
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Stats").assertIsDisplayed()
    }

    // ========== Sessions Tab Tests ==========

    @Test
    fun sessionsTab_showsEmptyMessage_whenNoSessions() {
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("No sessions yet").assertIsDisplayed()
    }

    @Test
    fun sessionsTab_showsSessionInfo() {
        val session = createSessionOverview(totalTrials = 20, correctTrials = 18)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = listOf(session),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("18/20 correct").assertIsDisplayed()
    }

    @Test
    fun sessionsTab_showsAccuracyPercentage() {
        val session = createSessionOverview(totalTrials = 20, correctTrials = 16)  // 80%

        composeTestRule.setContent {
            HistoryScreen(
                sessions = listOf(session),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("80%").assertIsDisplayed()
    }

    @Test
    fun sessionsTab_showsChordTypeBadge() {
        val session = createSessionOverview(gameType = GameType.CHORD_TYPE.name)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = listOf(session),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Chord").assertIsDisplayed()
    }

    @Test
    fun sessionsTab_showsFunctionBadge() {
        val session = createSessionOverview(gameType = GameType.CHORD_FUNCTION.name)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = listOf(session),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Function").assertIsDisplayed()
    }

    @Test
    fun sessionsTab_showsIncomplete_whenNoCompletedAt() {
        val session = createSessionOverview(completedAt = null)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = listOf(session),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Incomplete").assertIsDisplayed()
    }

    // ========== Cards Tab Tests ==========

    @Test
    fun cardsTab_showsEmptyMessage_whenNoCards() {
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Cards").performClick()

        composeTestRule.onNodeWithText("No cards yet").assertIsDisplayed()
    }

    @Test
    fun cardsTab_showsCardInfo() {
        val card = createCardWithFsrs(chordType = "MAJOR", octave = 4)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = listOf(card),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Cards").performClick()

        composeTestRule.onNodeWithText("MAJOR @ Oct 4").assertIsDisplayed()
    }

    @Test
    fun cardsTab_showsReviewCount() {
        val card = createCardWithFsrs(reviewCount = 7)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = listOf(card),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Cards").performClick()

        composeTestRule.onNodeWithText("Reviews: 7").assertIsDisplayed()
    }

    @Test
    fun cardsTab_showsInterval() {
        val card = createCardWithFsrs(interval = 5)

        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = listOf(card),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Cards").performClick()

        composeTestRule.onNodeWithText("Interval: 5d").assertIsDisplayed()
    }

    @Test
    fun cardsTab_showsDueBadge_whenDue() {
        val card = createCardWithFsrs(dueDate = System.currentTimeMillis() - HOUR_MS)  // Due in past

        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = listOf(card),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Cards").performClick()

        composeTestRule.onNodeWithText("DUE").assertIsDisplayed()
    }

    // ========== Stats Tab Tests ==========

    @Test
    fun statsTab_showsEmptyMessage_whenNoStats() {
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Stats").performClick()

        composeTestRule.onNodeWithText("No stats yet - complete some reviews!").assertIsDisplayed()
    }

    @Test
    fun statsTab_showsOverallStats() {
        val stats = listOf(
            createCardStats(cardId = "MAJOR_4_ARPEGGIATED", totalTrials = 10, correctTrials = 8),
            createCardStats(cardId = "MINOR_4_ARPEGGIATED", totalTrials = 10, correctTrials = 9)
        )  // Total: 20 trials, 17 correct = 85%

        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = stats,
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Stats").performClick()

        composeTestRule.onNodeWithText("Overall").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        composeTestRule.onNodeWithText("17 / 20 trials").assertIsDisplayed()
    }

    @Test
    fun statsTab_showsPerCardStats() {
        val stats = listOf(
            createCardStats(cardId = "MAJOR_4_ARPEGGIATED", totalTrials = 10, correctTrials = 8)
        )

        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = stats,
                onBackClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Stats").performClick()

        composeTestRule.onNodeWithText("MAJOR @ Oct 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("8/10 trials").assertIsDisplayed()
    }

    // ========== Navigation Tests ==========

    @Test
    fun backButton_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            HistoryScreen(
                sessions = emptyList(),
                cards = emptyList(),
                cardStats = emptyList(),
                onBackClicked = { clicked = true }
            )
        }

        // Click back arrow (accessibility label)
        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue(clicked)
    }
}
