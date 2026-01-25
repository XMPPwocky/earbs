package net.xmppwocky.earbs.data.repository

import kotlinx.coroutines.test.runTest
import net.xmppwocky.earbs.data.DatabaseTestBase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CardSelectionTest : DatabaseTestBase() {

    private lateinit var repository: EarbsRepository

    @Before
    fun setupRepository() {
        // Set session size to 5 for easier testing
        prefs.edit().putInt("session_size", 5).apply()

        repository = EarbsRepository(
            cardDao = cardDao,
            functionCardDao = functionCardDao,
            fsrsStateDao = fsrsStateDao,
            reviewSessionDao = reviewSessionDao,
            trialDao = trialDao,
            historyDao = historyDao,
            prefs = prefs
        )
    }

    // ========== Basic Selection Tests ==========

    @Test
    fun `selectCardsForReview returns empty when no cards`() = runTest {
        val cards = repository.selectCardsForReview()

        assertTrue(cards.isEmpty())
    }

    @Test
    fun `selectCardsForReview returns all cards when fewer than session size`() = runTest {
        val now = System.currentTimeMillis()
        // Create 3 due cards
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2"), now - HOUR_MS)

        val cards = repository.selectCardsForReview()

        assertEquals(3, cards.size)
    }

    @Test
    fun `selectCardsForReview returns session size cards when more available`() = runTest {
        val now = System.currentTimeMillis()
        // Create 10 due cards
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2", "SUS4", "DOM7", "MAJ7", "MIN7", "DIM7"), now - HOUR_MS)

        val cards = repository.selectCardsForReview()

        assertEquals(5, cards.size)  // Session size is 5
    }

    @Test
    fun `selectCardsForReview result is shuffled`() = runTest {
        val now = System.currentTimeMillis()
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2", "SUS4", "DOM7"), now - HOUR_MS)

        // Run multiple times and check that order varies
        val orderings = mutableSetOf<List<String>>()
        repeat(10) {
            val cards = repository.selectCardsForReview()
            orderings.add(cards.map { it.chordType.name })
        }

        // With 5 cards, there are 120 possible orderings
        // After 10 runs, we should see multiple different orderings
        assertTrue("Selection should be shuffled", orderings.size > 1)
    }

    // ========== Group Preference Tests ==========

    @Test
    fun `selectCardsForReview prefers group with most due cards`() = runTest {
        val now = System.currentTimeMillis()

        // Group A: 4 due cards (octave 4, arpeggiated)
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2", "SUS4"), now - HOUR_MS)

        // Group B: 2 due cards (octave 4, block)
        createCardsInGroup("BLOCK", 4, listOf("DOM7", "MAJ7"), now - HOUR_MS)

        // Group C: 1 due card (octave 5, arpeggiated)
        createCardsInGroup("ARPEGGIATED", 5, listOf("MIN7"), now - HOUR_MS)

        val cards = repository.selectCardsForReview()

        // Should prefer group A with 4 due cards
        val groupACards = cards.filter { it.octave == 4 && it.playbackMode.name == "ARPEGGIATED" }
        assertTrue("Should have at least 4 cards from best group", groupACards.size >= 4)
    }

    @Test
    fun `selectCardsForReview pads with non-due from same group`() = runTest {
        val now = System.currentTimeMillis()

        // 3 due cards in group
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2"), now - HOUR_MS)

        // 2 non-due cards in same group (session size = 5, so need padding)
        createCardsInGroup("ARPEGGIATED", 4, listOf("SUS4", "DOM7"), now + HOUR_MS)

        val cards = repository.selectCardsForReview()

        assertEquals(5, cards.size)
        assertTrue("All cards should be from same group", cards.all { it.octave == 4 && it.playbackMode.name == "ARPEGGIATED" })
    }

    @Test
    fun `selectCardsForReview pads from other groups when needed`() = runTest {
        val now = System.currentTimeMillis()

        // 2 due cards in group A
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR"), now - HOUR_MS)

        // 0 non-due in group A (only 2 cards total)

        // 3 due cards in group B
        createCardsInGroup("BLOCK", 4, listOf("SUS2", "SUS4", "DOM7"), now - HOUR_MS)

        val cards = repository.selectCardsForReview()

        assertEquals(5, cards.size)
    }

    @Test
    fun `selectCardsForReview falls back to non-due from any group`() = runTest {
        val now = System.currentTimeMillis()

        // 2 due cards in group A
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR"), now - HOUR_MS)

        // 2 non-due cards in group B (not the best group, but needed for padding)
        createCardsInGroup("BLOCK", 5, listOf("SUS2", "SUS4"), now + DAY_MS)

        val cards = repository.selectCardsForReview()

        // Should get 2 due from A, plus non-due from B
        assertTrue("Should include cards from multiple groups when needed", cards.size >= 2)
    }

    // ========== Due Date Ordering Tests ==========

    @Test
    fun `selectCardsForReview prefers most overdue cards`() = runTest {
        val now = System.currentTimeMillis()

        // Create cards with different overdue amounts
        createCardWithDue("MAJOR", 4, "ARPEGGIATED", now - 3 * HOUR_MS)  // Most overdue
        createCardWithDue("MINOR", 4, "ARPEGGIATED", now - 1 * HOUR_MS)  // Least overdue
        createCardWithDue("SUS2", 4, "ARPEGGIATED", now - 2 * HOUR_MS)   // Middle

        // Use small session size
        prefs.edit().putInt("session_size", 2).apply()

        // Run selection multiple times - most overdue should always be included
        repeat(5) {
            val cards = repository.selectCardsForReview()

            // MAJOR (most overdue) should be selected
            assertTrue("Most overdue card should be selected",
                cards.any { it.chordType.name == "MAJOR" })
        }
    }

    // ========== No Due Cards Tests ==========

    @Test
    fun `selectCardsForReview returns non-due cards when no cards are due`() = runTest {
        val now = System.currentTimeMillis()

        // All cards non-due
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2", "SUS4", "DOM7"), now + HOUR_MS)

        val cards = repository.selectCardsForReview()

        assertEquals(5, cards.size)
    }

    @Test
    fun `selectCardsForReview prefers group with most cards when all non-due`() = runTest {
        val now = System.currentTimeMillis()

        // Group A: 4 non-due cards
        createCardsInGroup("ARPEGGIATED", 4, listOf("MAJOR", "MINOR", "SUS2", "SUS4"), now + HOUR_MS)

        // Group B: 2 non-due cards
        createCardsInGroup("BLOCK", 4, listOf("DOM7", "MAJ7"), now + HOUR_MS)

        val cards = repository.selectCardsForReview()

        val groupACards = cards.filter { it.playbackMode.name == "ARPEGGIATED" }
        assertTrue("Should prefer larger group", groupACards.size >= 4)
    }

    // ========== Function Card Selection Tests ==========

    @Test
    fun `selectFunctionCardsForReview returns empty when no cards`() = runTest {
        val cards = repository.selectFunctionCardsForReview()

        assertTrue(cards.isEmpty())
    }

    @Test
    fun `selectFunctionCardsForReview groups by keyQuality octave mode`() = runTest {
        val now = System.currentTimeMillis()

        // Group A: 3 due cards (MAJOR, octave 4, arpeggiated)
        createFunctionCardsInGroup("MAJOR", 4, "ARPEGGIATED", listOf("IV", "V", "vi"), now - HOUR_MS)

        // Group B: 2 due cards (MINOR, octave 4, arpeggiated)
        createFunctionCardsInGroup("MINOR", 4, "ARPEGGIATED", listOf("iv", "v"), now - HOUR_MS)

        val cards = repository.selectFunctionCardsForReview()

        // Should prefer major group with more cards
        val majorCards = cards.filter { it.keyQuality.name == "MAJOR" }
        assertTrue("Should prefer group with more due cards", majorCards.size >= 3)
    }

    @Test
    fun `selectFunctionCardsForReview pads with non-due from same group`() = runTest {
        val now = System.currentTimeMillis()

        // 2 due cards
        createFunctionCardsInGroup("MAJOR", 4, "ARPEGGIATED", listOf("IV", "V"), now - HOUR_MS)

        // 3 non-due cards in same group
        createFunctionCardsInGroup("MAJOR", 4, "ARPEGGIATED", listOf("vi", "ii", "iii"), now + HOUR_MS)

        val cards = repository.selectFunctionCardsForReview()

        assertEquals(5, cards.size)
        assertTrue("All cards should be from same group",
            cards.all { it.keyQuality.name == "MAJOR" && it.octave == 4 && it.playbackMode.name == "ARPEGGIATED" })
    }

    @Test
    fun `selectFunctionCardsForReview result is shuffled`() = runTest {
        val now = System.currentTimeMillis()
        createFunctionCardsInGroup("MAJOR", 4, "ARPEGGIATED", listOf("IV", "V", "vi", "ii", "iii"), now - HOUR_MS)

        val orderings = mutableSetOf<List<String>>()
        repeat(10) {
            val cards = repository.selectFunctionCardsForReview()
            orderings.add(cards.map { it.function.name })
        }

        assertTrue("Selection should be shuffled", orderings.size > 1)
    }

    // ========== Helper Methods ==========

    private suspend fun createCardsInGroup(
        playbackMode: String,
        octave: Int,
        chordTypes: List<String>,
        dueDate: Long
    ) {
        chordTypes.forEach { type ->
            createCardWithDue(type, octave, playbackMode, dueDate)
        }
    }

    private suspend fun createCardWithDue(
        chordType: String,
        octave: Int,
        playbackMode: String,
        dueDate: Long
    ) {
        val cardId = "${chordType}_${octave}_${playbackMode}"
        cardDao.insert(CardEntity(cardId, chordType, octave, playbackMode))
        fsrsStateDao.insert(FsrsStateEntity(
            cardId = cardId,
            gameType = GameType.CHORD_TYPE.name,
            dueDate = dueDate
        ))
    }

    private suspend fun createFunctionCardsInGroup(
        keyQuality: String,
        octave: Int,
        playbackMode: String,
        functions: List<String>,
        dueDate: Long
    ) {
        functions.forEach { func ->
            val cardId = "${func}_${keyQuality}_${octave}_${playbackMode}"
            functionCardDao.insert(FunctionCardEntity(cardId, func, keyQuality, octave, playbackMode))
            fsrsStateDao.insert(FsrsStateEntity(
                cardId = cardId,
                gameType = GameType.CHORD_FUNCTION.name,
                dueDate = dueDate
            ))
        }
    }
}
