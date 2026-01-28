package net.xmppwocky.earbs.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Adapter interface for card-type-agnostic deprecation testing.
 * Each card DAO provides an implementation delegating to its specific methods.
 */
interface DeprecationTestAdapter<CardWithFsrs> {
    // Card creation
    suspend fun createActiveCard(dueDate: Long): String
    suspend fun createDeprecatedCard(dueDate: Long): String
    suspend fun createActiveCardWithFsrs(
        stability: Double,
        difficulty: Double,
        interval: Int,
        reviewCount: Int,
        phase: Int,
        lapses: Int,
        lastReview: Long
    ): String
    suspend fun createUnlockedCard(): String
    suspend fun createLockedCard(): String
    suspend fun createDeprecatedUnlockedCard(): String

    // DAO queries
    suspend fun getDueCards(now: Long): List<CardWithFsrs>
    suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrs>
    suspend fun countDue(now: Long): Int
    suspend fun countUnlocked(): Int
    fun countUnlockedFlow(): Flow<Int>
    suspend fun getAllUnlocked(): List<*>
    suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrs>
    fun getAllUnlockedWithFsrsFlow(): Flow<List<CardWithFsrs>>
    suspend fun getDeprecatedCardsWithFsrs(): List<CardWithFsrs>
    fun getDeprecatedCardsWithFsrsFlow(): Flow<List<CardWithFsrs>>
    suspend fun countDeprecated(): Int
    suspend fun setDeprecated(id: String, deprecated: Boolean)
    suspend fun getByIdWithFsrs(id: String): CardWithFsrs?
    suspend fun getAllCardsOrdered(): List<*>
    suspend fun getAllCardsWithFsrsOrdered(): List<CardWithFsrs>
    fun countDueFlow(now: Long): Flow<Int>

    // CardWithFsrs field accessors
    fun getId(card: CardWithFsrs): String
    fun isDeprecated(card: CardWithFsrs): Boolean
    fun getStability(card: CardWithFsrs): Double
    fun getDifficulty(card: CardWithFsrs): Double
    fun getInterval(card: CardWithFsrs): Int
    fun getReviewCount(card: CardWithFsrs): Int
    fun getPhase(card: CardWithFsrs): Int
    fun getLapses(card: CardWithFsrs): Int
}

/**
 * Shared deprecation test implementations that work with any card type via adapter.
 */
object DeprecationTestHelper {

    // ========== Review Exclusion Tests ==========

    suspend fun <T> testDueCardsExcludesDeprecated(
        adapter: DeprecationTestAdapter<T>,
        now: Long,
        hourMs: Long
    ) {
        val activeId = adapter.createActiveCard(now - hourMs)
        adapter.createDeprecatedCard(now - hourMs)

        val dueCards = adapter.getDueCards(now)

        assertEquals(1, dueCards.size)
        assertEquals(activeId, adapter.getId(dueCards[0]))
    }

    suspend fun <T> testNonDueCardsExcludesDeprecated(
        adapter: DeprecationTestAdapter<T>,
        now: Long,
        hourMs: Long
    ) {
        val activeId = adapter.createActiveCard(now + hourMs)
        adapter.createDeprecatedCard(now + hourMs)

        val nonDueCards = adapter.getNonDueCards(now, 10)

        assertEquals(1, nonDueCards.size)
        assertEquals(activeId, adapter.getId(nonDueCards[0]))
    }

    suspend fun <T> testCountDueExcludesDeprecated(
        adapter: DeprecationTestAdapter<T>,
        now: Long,
        hourMs: Long
    ) {
        adapter.createActiveCard(now - hourMs)
        adapter.createActiveCard(now - hourMs)
        adapter.createDeprecatedCard(now - hourMs)

        assertEquals(2, adapter.countDue(now))
    }

    suspend fun <T> testCountUnlockedExcludesDeprecated(adapter: DeprecationTestAdapter<T>) {
        adapter.createUnlockedCard()
        adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()
        adapter.createLockedCard()

        assertEquals(2, adapter.countUnlocked())
    }

    suspend fun <T> testCountUnlockedFlowExcludesDeprecated(adapter: DeprecationTestAdapter<T>) {
        adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val count = adapter.countUnlockedFlow().first()
        assertEquals(1, count)
    }

    suspend fun <T> testGetAllUnlockedExcludesDeprecated(adapter: DeprecationTestAdapter<T>) {
        val activeId = adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val unlocked = adapter.getAllUnlocked()

        assertEquals(1, unlocked.size)
    }

    suspend fun <T> testGetAllUnlockedWithFsrsExcludesDeprecated(adapter: DeprecationTestAdapter<T>) {
        val activeId = adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val unlockedWithFsrs = adapter.getAllUnlockedWithFsrs()

        assertEquals(1, unlockedWithFsrs.size)
        assertEquals(activeId, adapter.getId(unlockedWithFsrs[0]))
    }

    suspend fun <T> testGetAllUnlockedWithFsrsFlowExcludesDeprecated(adapter: DeprecationTestAdapter<T>) {
        val activeId = adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val cards = adapter.getAllUnlockedWithFsrsFlow().first()

        assertEquals(1, cards.size)
        assertEquals(activeId, adapter.getId(cards[0]))
    }

    // ========== Archived Cards Retrieval Tests ==========

    suspend fun <T> testGetDeprecatedCardsWithFsrsReturnsOnlyDeprecated(
        adapter: DeprecationTestAdapter<T>
    ) {
        // Active cards
        adapter.createUnlockedCard()
        adapter.createUnlockedCard()

        // Deprecated cards
        adapter.createDeprecatedUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val deprecatedCards = adapter.getDeprecatedCardsWithFsrs()

        assertEquals(2, deprecatedCards.size)
        assertTrue(deprecatedCards.all { adapter.isDeprecated(it) })
    }

    suspend fun <T> testGetDeprecatedCardsWithFsrsFlowEmitsDeprecated(
        adapter: DeprecationTestAdapter<T>
    ) {
        val deprecatedId = adapter.createDeprecatedUnlockedCard()
        adapter.createUnlockedCard()

        val deprecatedCards = adapter.getDeprecatedCardsWithFsrsFlow().first()

        assertEquals(1, deprecatedCards.size)
        assertEquals(deprecatedId, adapter.getId(deprecatedCards[0]))
    }

    suspend fun <T> testCountDeprecatedReturnsCorrectCount(adapter: DeprecationTestAdapter<T>) {
        adapter.createUnlockedCard()
        adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()
        adapter.createDeprecatedUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        assertEquals(3, adapter.countDeprecated())
    }

    suspend fun <T> testGetByIdWithFsrsReturnsDeprecatedCards(adapter: DeprecationTestAdapter<T>) {
        val cardId = adapter.createActiveCardWithFsrs(
            stability = 5.0,
            difficulty = 4.0,
            interval = 7,
            reviewCount = 5,
            phase = 1,
            lapses = 0,
            lastReview = System.currentTimeMillis()
        )
        adapter.setDeprecated(cardId, true)

        val card = adapter.getByIdWithFsrs(cardId)

        assertNotNull(card)
        assertTrue(adapter.isDeprecated(card!!))
        assertEquals(5.0, adapter.getStability(card), 0.01)
    }

    // ========== Data Preservation Tests ==========

    suspend fun <T> testSetDeprecatedPreservesFsrsState(
        adapter: DeprecationTestAdapter<T>,
        dayMs: Long
    ) {
        val now = System.currentTimeMillis()
        val cardId = adapter.createActiveCardWithFsrs(
            stability = 8.5,
            difficulty = 3.2,
            interval = 14,
            reviewCount = 10,
            phase = 2,
            lapses = 1,
            lastReview = now - dayMs
        )

        adapter.setDeprecated(cardId, true)

        val card = adapter.getByIdWithFsrs(cardId)!!
        assertEquals(8.5, adapter.getStability(card), 0.01)
        assertEquals(3.2, adapter.getDifficulty(card), 0.01)
        assertEquals(14, adapter.getInterval(card))
        assertEquals(10, adapter.getReviewCount(card))
        assertEquals(2, adapter.getPhase(card))
        assertEquals(1, adapter.getLapses(card))
    }

    // ========== Toggle Tests ==========

    suspend fun <T> testSetDeprecatedSetsFlag(adapter: DeprecationTestAdapter<T>) {
        val cardId = adapter.createUnlockedCard()

        adapter.setDeprecated(cardId, true)

        val card = adapter.getByIdWithFsrs(cardId)
        assertTrue(adapter.isDeprecated(card!!))
    }

    suspend fun <T> testSetDeprecatedFalseClearsFlag(adapter: DeprecationTestAdapter<T>) {
        val cardId = adapter.createDeprecatedUnlockedCard()

        adapter.setDeprecated(cardId, false)

        val card = adapter.getByIdWithFsrs(cardId)
        assertFalse(adapter.isDeprecated(card!!))
    }

    suspend fun <T> testToggleDeprecatedBackAndForth(adapter: DeprecationTestAdapter<T>) {
        val cardId = adapter.createUnlockedCard()

        // Deprecate
        adapter.setDeprecated(cardId, true)
        assertTrue(adapter.isDeprecated(adapter.getByIdWithFsrs(cardId)!!))

        // Un-deprecate
        adapter.setDeprecated(cardId, false)
        assertFalse(adapter.isDeprecated(adapter.getByIdWithFsrs(cardId)!!))

        // Deprecate again
        adapter.setDeprecated(cardId, true)
        assertTrue(adapter.isDeprecated(adapter.getByIdWithFsrs(cardId)!!))
    }

    // ========== Edge Cases ==========

    suspend fun <T> testAllCardsDeprecatedReturnsEmptyDueList(
        adapter: DeprecationTestAdapter<T>,
        now: Long,
        hourMs: Long
    ) {
        adapter.createDeprecatedCard(now - hourMs)
        adapter.createDeprecatedCard(now - hourMs)

        val dueCards = adapter.getDueCards(now)

        assertTrue(dueCards.isEmpty())
    }

    suspend fun <T> testAllCardsDeprecatedReturnsEmptyNonDueList(
        adapter: DeprecationTestAdapter<T>,
        now: Long,
        hourMs: Long
    ) {
        adapter.createDeprecatedCard(now + hourMs)
        adapter.createDeprecatedCard(now + hourMs)

        val nonDueCards = adapter.getNonDueCards(now, 10)

        assertTrue(nonDueCards.isEmpty())
    }

    suspend fun <T> testGetAllCardsOrderedExcludesDeprecated(adapter: DeprecationTestAdapter<T>) {
        adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val cards = adapter.getAllCardsOrdered()

        assertEquals(1, cards.size)
    }

    suspend fun <T> testGetAllCardsWithFsrsOrderedExcludesDeprecated(
        adapter: DeprecationTestAdapter<T>
    ) {
        val activeId = adapter.createUnlockedCard()
        adapter.createDeprecatedUnlockedCard()

        val cards = adapter.getAllCardsWithFsrsOrdered()

        assertEquals(1, cards.size)
        assertEquals(activeId, adapter.getId(cards[0]))
    }

    suspend fun <T> testCountDueFlowExcludesDeprecated(
        adapter: DeprecationTestAdapter<T>,
        now: Long,
        hourMs: Long
    ) {
        adapter.createActiveCard(now - hourMs)
        adapter.createDeprecatedCard(now - hourMs)

        val count = adapter.countDueFlow(now).first()
        assertEquals(1, count)
    }
}
