package net.xmppwocky.earbs.audio

import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.GameAnswer
import net.xmppwocky.earbs.model.GameCard
import net.xmppwocky.earbs.model.ProgressionCard

/**
 * Strategy interface for game-specific audio playback.
 * Abstracts the differences between chord type and function game audio.
 */
sealed interface AudioPlaybackStrategy<C : GameCard, A : GameAnswer> {
    /**
     * Play audio for the given card.
     *
     * @param card The card to play
     * @param rootSemitones The root note in semitones from A4
     * @param durationMs Playback duration per chord
     */
    suspend fun playCard(card: C, rootSemitones: Int, durationMs: Int)

    /**
     * Play audio for a specific answer (for learning mode).
     *
     * @param answer The answer to play
     * @param card The current card context (for playback mode, key quality, etc.)
     * @param rootSemitones The root note in semitones from A4
     * @param durationMs Playback duration per chord
     */
    suspend fun playAnswer(answer: A, card: C, rootSemitones: Int, durationMs: Int)

    /**
     * Strategy for chord type game.
     * Plays a single chord based on the card's chord type.
     */
    object ChordTypeStrategy : AudioPlaybackStrategy<Card, GameAnswer.ChordTypeAnswer> {
        override suspend fun playCard(card: Card, rootSemitones: Int, durationMs: Int) {
            val frequencies = ChordBuilder.buildChord(card.chordType, rootSemitones)
            AudioEngine.playChord(
                frequencies = frequencies,
                mode = card.playbackMode,
                durationMs = durationMs,
                chordType = card.chordType.displayName,
                rootSemitones = rootSemitones
            )
        }

        override suspend fun playAnswer(
            answer: GameAnswer.ChordTypeAnswer,
            card: Card,
            rootSemitones: Int,
            durationMs: Int
        ) {
            val frequencies = ChordBuilder.buildChord(answer.chordType, rootSemitones)
            AudioEngine.playChord(
                frequencies = frequencies,
                mode = card.playbackMode,
                durationMs = durationMs,
                chordType = answer.chordType.displayName,
                rootSemitones = rootSemitones
            )
        }
    }

    /**
     * Strategy for function game.
     * Plays a chord pair: tonic chord followed by target chord.
     */
    object FunctionStrategy : AudioPlaybackStrategy<FunctionCard, GameAnswer.FunctionAnswer> {
        private const val PAUSE_MS = 300

        override suspend fun playCard(card: FunctionCard, rootSemitones: Int, durationMs: Int) {
            val referenceFreqs = ChordBuilder.buildTonicChord(rootSemitones, card.keyQuality)
            val targetFreqs = ChordBuilder.buildDiatonicChord(rootSemitones, card.function)
            AudioEngine.playChordPair(
                referenceFreqs = referenceFreqs,
                targetFreqs = targetFreqs,
                mode = card.playbackMode,
                durationMs = durationMs,
                pauseMs = PAUSE_MS,
                keyQuality = card.keyQuality.name,
                function = card.function.displayName,
                rootSemitones = rootSemitones
            )
        }

        override suspend fun playAnswer(
            answer: GameAnswer.FunctionAnswer,
            card: FunctionCard,
            rootSemitones: Int,
            durationMs: Int
        ) {
            val referenceFreqs = ChordBuilder.buildTonicChord(rootSemitones, card.keyQuality)
            val targetFreqs = ChordBuilder.buildDiatonicChord(rootSemitones, answer.function)
            AudioEngine.playChordPair(
                referenceFreqs = referenceFreqs,
                targetFreqs = targetFreqs,
                mode = card.playbackMode,
                durationMs = durationMs,
                pauseMs = PAUSE_MS,
                keyQuality = card.keyQuality.name,
                function = answer.function.displayName,
                rootSemitones = rootSemitones
            )
        }
    }
}

/**
 * Strategy for progression game.
 * Plays a sequence of chords as a progression.
 *
 * Each progression has a fixed key quality (major or minor) defined in ProgressionType,
 * so no key quality parameter is needed.
 */
object ProgressionStrategy : AudioPlaybackStrategy<ProgressionCard, GameAnswer.ProgressionAnswer> {
    private const val CHORD_DURATION_MS = 400
    private const val PAUSE_MS = 200

    /**
     * Play audio for a progression card.
     *
     * @param card The progression card to play
     * @param rootSemitones The root note in semitones from A4
     * @param durationMs Duration for each chord
     */
    override suspend fun playCard(
        card: ProgressionCard,
        rootSemitones: Int,
        durationMs: Int
    ) {
        val chords = ChordBuilder.buildProgression(
            keyRootSemitones = rootSemitones,
            progression = card.progression
        )
        AudioEngine.playProgression(
            chords = chords,
            mode = card.playbackMode,
            chordDurationMs = durationMs,
            pauseMs = PAUSE_MS,
            progressionName = card.progression.displayName,
            keyQuality = card.progression.keyQuality.name,
            rootSemitones = rootSemitones
        )
    }

    /**
     * Play audio for a specific progression answer (for learning mode).
     *
     * @param answer The progression answer to play
     * @param card The current card context (for playback mode)
     * @param rootSemitones The root note in semitones from A4
     * @param durationMs Duration for each chord
     */
    override suspend fun playAnswer(
        answer: GameAnswer.ProgressionAnswer,
        card: ProgressionCard,
        rootSemitones: Int,
        durationMs: Int
    ) {
        val chords = ChordBuilder.buildProgression(
            keyRootSemitones = rootSemitones,
            progression = answer.progression
        )
        AudioEngine.playProgression(
            chords = chords,
            mode = card.playbackMode,
            chordDurationMs = durationMs,
            pauseMs = PAUSE_MS,
            progressionName = answer.progression.displayName,
            keyQuality = answer.progression.keyQuality.name,
            rootSemitones = rootSemitones
        )
    }
}
