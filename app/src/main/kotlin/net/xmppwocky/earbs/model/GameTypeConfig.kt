package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.data.entity.GameType

/**
 * Sealed class defining configuration and behavior for each game type.
 * Encapsulates game-specific constants, answer generation, and correctness checking.
 *
 * @param C The card type for this game
 * @param A The answer type for this game
 */
sealed class GameTypeConfig<C : GameCard, A : GameAnswer> {
    /** The GameType enum value for database operations */
    abstract val gameType: GameType

    /** Total number of cards when fully unlocked */
    abstract val totalCards: Int

    /** Number of cards per unlock group */
    abstract val cardsPerUnlock: Int

    /** Maximum unlock level (0-indexed) */
    abstract val maxUnlockLevel: Int

    /** Human-readable name for display */
    abstract val displayName: String

    /** Short description of this game mode */
    abstract val description: String

    /**
     * Get the list of possible answers for this session.
     * For chord type game: distinct chord types in the session
     * For function game: all functions for the current key quality
     */
    abstract fun getAnswerOptions(session: GenericReviewSession<C>): List<A>

    /**
     * Check if the given answer is correct for the card.
     */
    abstract fun isCorrectAnswer(card: C, answer: A): Boolean

    /**
     * Get the correct answer for a card.
     */
    abstract fun getCorrectAnswer(card: C): A

    /**
     * Get the unlock group index for a card.
     * Returns the group index or -1 if not found.
     */
    abstract fun getUnlockGroupIndex(card: C): Int

    /**
     * Get a human-readable name for an unlock group.
     */
    abstract fun getUnlockGroupName(groupIndex: Int): String

    /**
     * Total number of unlock groups.
     */
    abstract val unlockGroupCount: Int

    /**
     * Configuration for the Chord Type recognition game.
     * User hears a chord and identifies its quality (Major, Minor, Sus2, etc.)
     */
    data object ChordTypeGame : GameTypeConfig<Card, GameAnswer.ChordTypeAnswer>() {
        override val gameType: GameType = GameType.CHORD_TYPE
        override val totalCards: Int = Deck.TOTAL_CARDS
        override val cardsPerUnlock: Int = 4
        override val maxUnlockLevel: Int = Deck.MAX_UNLOCK_LEVEL
        override val displayName: String = "Chord Type"
        override val description: String = "Identify chord quality (Major, Minor, etc.)"
        override val unlockGroupCount: Int = Deck.UNLOCK_ORDER.size

        override fun getAnswerOptions(session: GenericReviewSession<Card>): List<GameAnswer.ChordTypeAnswer> {
            return session.cards
                .map { it.chordType }
                .distinct()
                .sortedBy { it.ordinal }
                .map { GameAnswer.ChordTypeAnswer(it) }
        }

        override fun isCorrectAnswer(card: Card, answer: GameAnswer.ChordTypeAnswer): Boolean {
            return card.chordType == answer.chordType
        }

        override fun getCorrectAnswer(card: Card): GameAnswer.ChordTypeAnswer {
            return GameAnswer.ChordTypeAnswer(card.chordType)
        }

        override fun getUnlockGroupIndex(card: Card): Int {
            return Deck.getGroupIndex(card)
        }

        override fun getUnlockGroupName(groupIndex: Int): String {
            return Deck.getGroupName(groupIndex)
        }
    }

    /**
     * Configuration for the Chord Function recognition game.
     * User hears tonic chord, then target chord, and identifies the function.
     */
    data object FunctionGame : GameTypeConfig<FunctionCard, GameAnswer.FunctionAnswer>() {
        override val gameType: GameType = GameType.CHORD_FUNCTION
        override val totalCards: Int = FunctionDeck.TOTAL_CARDS
        override val cardsPerUnlock: Int = FunctionDeck.CARDS_PER_GROUP
        override val maxUnlockLevel: Int = FunctionDeck.MAX_UNLOCK_LEVEL
        override val displayName: String = "Chord Function"
        override val description: String = "Identify chord function (ii, IV, V, etc.)"
        override val unlockGroupCount: Int = FunctionDeck.UNLOCK_ORDER.size

        override fun getAnswerOptions(session: GenericReviewSession<FunctionCard>): List<GameAnswer.FunctionAnswer> {
            // For function game, show all functions for the key quality (not just those in session)
            val keyQuality = session.cards.firstOrNull()?.keyQuality ?: return emptyList()
            return ChordFunction.forKeyQuality(keyQuality)
                .map { GameAnswer.FunctionAnswer(it) }
        }

        override fun isCorrectAnswer(card: FunctionCard, answer: GameAnswer.FunctionAnswer): Boolean {
            return card.function == answer.function
        }

        override fun getCorrectAnswer(card: FunctionCard): GameAnswer.FunctionAnswer {
            return GameAnswer.FunctionAnswer(card.function)
        }

        override fun getUnlockGroupIndex(card: FunctionCard): Int {
            return FunctionDeck.getGroupIndex(card)
        }

        override fun getUnlockGroupName(groupIndex: Int): String {
            return FunctionDeck.getGroupName(groupIndex)
        }
    }

    /**
     * Configuration for the Chord Progression recognition game.
     * User hears a sequence of chords and identifies the progression.
     * Key quality is randomized at playback and hidden from user.
     */
    data object ProgressionGame : GameTypeConfig<ProgressionCard, GameAnswer.ProgressionAnswer>() {
        override val gameType: GameType = GameType.CHORD_PROGRESSION
        override val totalCards: Int = ProgressionDeck.TOTAL_CARDS
        override val cardsPerUnlock: Int = ProgressionDeck.CARDS_PER_GROUP
        override val maxUnlockLevel: Int = ProgressionDeck.MAX_UNLOCK_LEVEL
        override val displayName: String = "Chord Progressions"
        override val description: String = "Identify chord progression (I-IV-V-I, etc.)"
        override val unlockGroupCount: Int = ProgressionDeck.UNLOCK_ORDER.size

        override fun getAnswerOptions(session: GenericReviewSession<ProgressionCard>): List<GameAnswer.ProgressionAnswer> {
            // Show only progressions in the current session (like chord type game)
            return session.cards
                .map { it.progression }
                .distinct()
                .sortedBy { it.ordinal }
                .map { GameAnswer.ProgressionAnswer(it) }
        }

        override fun isCorrectAnswer(card: ProgressionCard, answer: GameAnswer.ProgressionAnswer): Boolean {
            return card.progression == answer.progression
        }

        override fun getCorrectAnswer(card: ProgressionCard): GameAnswer.ProgressionAnswer {
            return GameAnswer.ProgressionAnswer(card.progression)
        }

        override fun getUnlockGroupIndex(card: ProgressionCard): Int {
            return ProgressionDeck.getGroupIndex(card)
        }

        override fun getUnlockGroupName(groupIndex: Int): String {
            return ProgressionDeck.getGroupName(groupIndex)
        }
    }

    companion object {
        /**
         * Get the config for a given game type.
         */
        fun forGameType(gameType: GameType): GameTypeConfig<*, *> = when (gameType) {
            GameType.CHORD_TYPE -> ChordTypeGame
            GameType.CHORD_FUNCTION -> FunctionGame
            GameType.CHORD_PROGRESSION -> ProgressionGame
        }
    }
}
