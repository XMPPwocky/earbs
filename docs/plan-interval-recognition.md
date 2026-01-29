# Plan: Interval Recognition Game

## Overview

Add a fourth game type for recognizing musical intervals. This is the most fundamental ear training skill and provides a pedagogical foundation for chord recognition.

**Card tuple:** `(interval, octave, direction)`
**Total cards:** 108 (12 intervals × 3 octaves × 3 directions)
**Answer strategy:** Session-based (like Chord Type game)

## Design Decisions

### Intervals (12 total)

| Name | Semitones | Display Name |
|------|-----------|--------------|
| m2 | 1 | Minor 2nd |
| M2 | 2 | Major 2nd |
| m3 | 3 | Minor 3rd |
| M3 | 4 | Major 3rd |
| P4 | 5 | Perfect 4th |
| TT | 6 | Tritone |
| P5 | 7 | Perfect 5th |
| m6 | 8 | Minor 6th |
| M6 | 9 | Major 6th |
| m7 | 10 | Minor 7th |
| M7 | 11 | Major 7th |
| P8 | 12 | Octave |

### Directions (3)

- **ASCENDING**: Notes played sequentially, lower note first (root → interval note)
- **DESCENDING**: Notes played sequentially, higher note first (interval note → root)
- **HARMONIC**: Notes played simultaneously

This gives complete coverage of how intervals appear in real music. Descending intervals can feel different to identify than ascending ones, so training both builds stronger skills.

### Card ID Format

`{interval}_{octave}_{direction}`

Examples:
- `MAJOR_3RD_4_ASCENDING` - Major 3rd at octave 4, ascending
- `PERFECT_5TH_3_HARMONIC` - Perfect 5th at octave 3, harmonic
- `MINOR_2ND_5_DESCENDING` - Minor 2nd at octave 5, descending

### Audio Playback

**Ascending intervals:**
1. Play root note (randomized within octave)
2. Brief pause (200ms)
3. Play interval note (root + semitones)

**Descending intervals:**
1. Play interval note first (root + semitones)
2. Brief pause (200ms)
3. Play root note

**Harmonic intervals:**
1. Play both notes simultaneously (like a 2-note chord)

Duration matches existing chord playback settings.

### Unlock Progression (12 groups of 9 cards)

Each group contains one interval across all octaves and directions (1 interval × 3 octaves × 3 directions = 9 cards).

Groups organized by interval difficulty and musical importance:

| Group | Interval | Rationale |
|-------|----------|-----------|
| 0 | Perfect 5th | Most consonant, easiest to hear |
| 1 | Octave | Perfect consonance, very distinctive |
| 2 | Major 3rd | Foundation of major chords |
| 3 | Minor 3rd | Foundation of minor chords |
| 4 | Perfect 4th | Common melodic interval |
| 5 | Major 2nd | Whole step, common in melodies |
| 6 | Minor 2nd | Half step, distinctive tension |
| 7 | Major 6th | Wider interval, sweet sound |
| 8 | Minor 6th | Wider interval, darker sound |
| 9 | Minor 7th | Dominant 7th sound |
| 10 | Major 7th | Leading tone tension |
| 11 | Tritone | Most dissonant, hardest to identify |

**Starting deck:** Perfect 5th @ octave 4, all 3 directions (3 cards)

### Answer Options

Session-based: show distinct intervals present in the current session's cards.

## Implementation

### Phase 1: Model Layer

#### 1.1 Add IntervalType enum

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/IntervalType.kt` (new)

```kotlin
package net.xmppwocky.earbs.audio

enum class IntervalType(val semitones: Int, val displayName: String) {
    MINOR_2ND(1, "Minor 2nd"),
    MAJOR_2ND(2, "Major 2nd"),
    MINOR_3RD(3, "Minor 3rd"),
    MAJOR_3RD(4, "Major 3rd"),
    PERFECT_4TH(5, "Perfect 4th"),
    TRITONE(6, "Tritone"),
    PERFECT_5TH(7, "Perfect 5th"),
    MINOR_6TH(8, "Minor 6th"),
    MAJOR_6TH(9, "Major 6th"),
    MINOR_7TH(10, "Minor 7th"),
    MAJOR_7TH(11, "Major 7th"),
    OCTAVE(12, "Octave");

    companion object {
        fun fromSemitones(semitones: Int): IntervalType? =
            entries.find { it.semitones == semitones }
    }
}
```

#### 1.2 Add IntervalDirection enum

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/IntervalDirection.kt` (new)

```kotlin
package net.xmppwocky.earbs.audio

enum class IntervalDirection(val displayName: String) {
    ASCENDING("Ascending"),    // Sequential: root first, then interval note
    DESCENDING("Descending"),  // Sequential: interval note first, then root
    HARMONIC("Harmonic");      // Simultaneous: both notes together
}
```

#### 1.3 Add IntervalCard model

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/IntervalCard.kt` (new)

```kotlin
package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType
import net.xmppwocky.earbs.audio.PlaybackMode

data class IntervalCard(
    val interval: IntervalType,
    override val octave: Int,
    val direction: IntervalDirection
) : GameCard {
    override val id: String
        get() = "${interval.name}_${octave}_${direction.name}"

    override val playbackMode: PlaybackMode
        get() = PlaybackMode.ARPEGGIATED  // Not applicable, but required by interface

    override val displayName: String
        get() = interval.displayName

    companion object {
        fun fromId(id: String): IntervalCard {
            val parts = id.split("_")
            // Format: INTERVAL_TYPE_OCTAVE_DIRECTION (e.g., MAJOR_3RD_4_MELODIC)
            // IntervalType names have underscores, so we need to parse carefully
            val direction = IntervalDirection.valueOf(parts.last())
            val octave = parts[parts.size - 2].toInt()
            val intervalName = parts.dropLast(2).joinToString("_")
            val interval = IntervalType.valueOf(intervalName)
            return IntervalCard(interval, octave, direction)
        }
    }
}
```

#### 1.4 Add GameAnswer.IntervalAnswer

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/GameAnswer.kt` (modify)

```kotlin
data class IntervalAnswer(val interval: IntervalType) : GameAnswer {
    override val displayName: String get() = interval.displayName
}
```

#### 1.5 Add GameType.INTERVAL

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/entity/GameType.kt` (modify)

```kotlin
enum class GameType {
    CHORD_TYPE,
    CHORD_FUNCTION,
    CHORD_PROGRESSION,
    INTERVAL  // Add this
}
```

#### 1.6 Add IntervalGame to GameTypeConfig

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/GameTypeConfig.kt` (modify)

```kotlin
data class IntervalGame(
    override val gameType: GameType = GameType.INTERVAL
) : GameTypeConfig<IntervalCard, GameAnswer.IntervalAnswer>() {

    override val totalCards: Int = 108  // 12 intervals × 3 octaves × 3 directions
    override val cardsPerUnlock: Int = 9  // 1 interval × 3 octaves × 3 directions
    override val maxUnlockLevel: Int = 11

    override fun getAnswerOptions(
        card: IntervalCard,
        session: GenericReviewSession<IntervalCard>
    ): List<GameAnswer.IntervalAnswer> {
        // Session-based: distinct intervals in session
        return session.cards
            .map { it.interval }
            .distinct()
            .sorted()
            .map { GameAnswer.IntervalAnswer(it) }
    }

    override fun isCorrectAnswer(
        card: IntervalCard,
        answer: GameAnswer.IntervalAnswer
    ): Boolean = card.interval == answer.interval

    override fun getCorrectAnswer(card: IntervalCard): GameAnswer.IntervalAnswer =
        GameAnswer.IntervalAnswer(card.interval)

    override fun getUnlockGroupIndex(card: IntervalCard): Int =
        IntervalDeck.getGroupIndex(card)

    override fun getUnlockGroupName(groupIndex: Int): String =
        IntervalDeck.getGroupName(groupIndex)
}
```

#### 1.7 Add IntervalDeck

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/IntervalDeck.kt` (new)

```kotlin
package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.IntervalDirection
import net.xmppwocky.earbs.audio.IntervalType

object IntervalDeck {
    // 12 groups, one per interval, ordered by difficulty
    // Each group has 9 cards (3 octaves × 3 directions)

    private val unlockOrder = listOf(
        IntervalType.PERFECT_5TH,   // Group 0: Easiest - most consonant
        IntervalType.OCTAVE,        // Group 1: Very distinctive
        IntervalType.MAJOR_3RD,     // Group 2: Major chord foundation
        IntervalType.MINOR_3RD,     // Group 3: Minor chord foundation
        IntervalType.PERFECT_4TH,   // Group 4: Common melodic interval
        IntervalType.MAJOR_2ND,     // Group 5: Whole step
        IntervalType.MINOR_2ND,     // Group 6: Half step
        IntervalType.MAJOR_6TH,     // Group 7: Wider, sweet
        IntervalType.MINOR_6TH,     // Group 8: Wider, darker
        IntervalType.MINOR_7TH,     // Group 9: Dominant 7th sound
        IntervalType.MAJOR_7TH,     // Group 10: Leading tone tension
        IntervalType.TRITONE        // Group 11: Hardest - most dissonant
    )

    fun getGroupIndex(card: IntervalCard): Int {
        return unlockOrder.indexOf(card.interval)
    }

    fun getGroupName(groupIndex: Int): String {
        return unlockOrder.getOrNull(groupIndex)?.displayName ?: "Unknown"
    }

    fun getAllCards(): List<IntervalCard> {
        return IntervalType.entries.flatMap { interval ->
            listOf(3, 4, 5).flatMap { octave ->
                IntervalDirection.entries.map { direction ->
                    IntervalCard(interval, octave, direction)
                }
            }
        }
    }

    fun getStartingCards(): List<IntervalCard> {
        // Perfect 5th at octave 4, all 3 directions
        return listOf(
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.ASCENDING),
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.DESCENDING),
            IntervalCard(IntervalType.PERFECT_5TH, 4, IntervalDirection.HARMONIC)
        )
    }
}
```

### Phase 2: Data Layer

#### 2.1 Add IntervalCardEntity

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/entity/IntervalCardEntity.kt` (new)

```kotlin
package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interval_cards")
data class IntervalCardEntity(
    @PrimaryKey val id: String,
    val interval: String,
    val octave: Int,
    val direction: String,
    val unlocked: Boolean = true,
    val deprecated: Boolean = false
)
```

#### 2.2 Add IntervalCardDao

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/db/IntervalCardDao.kt` (new)

Follow the pattern from `CardDao.kt` with:
- `IntervalCardWithFsrs` data class
- Standard queries: `getDueCards`, `getNonDueCards`, `countDue`, `countUnlocked`, etc.
- `setDeprecated`, `setUnlocked` mutations
- Join with `fsrs_state` table using `gameType = 'INTERVAL'`

#### 2.3 Add IntervalCardOperations

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/repository/IntervalCardOperations.kt` (new)

```kotlin
class IntervalCardOperations(
    private val intervalCardDao: IntervalCardDao,
    private val fsrsStateDao: FsrsStateDao
) : GameCardOperations<IntervalCard> {
    // Implement interface methods following ChordTypeCardOperations pattern
    // Group by (octave, direction) - no additional grouping key
}
```

#### 2.4 Update EarbsDatabase

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/db/EarbsDatabase.kt` (modify)

- Add `IntervalCardEntity` to `@Database(entities = [...])`
- Add `abstract fun intervalCardDao(): IntervalCardDao`
- Add migration to create `interval_cards` table

#### 2.5 Update EarbsRepository

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/repository/EarbsRepository.kt` (modify)

- Add `intervalCardDao` parameter
- Add `IntervalCardOperations` instance
- Add `initializeIntervalCards()` method
- Add `getIntervalCardsForSession()` method
- Add `recordIntervalTrialAndUpdateFsrs()` method

### Phase 3: Audio Layer

#### 3.1 Extend ChordBuilder for intervals

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/ChordBuilder.kt` (modify)

```kotlin
/**
 * Build frequencies for an interval.
 * Returns [rootFreq, intervalFreq] for melodic, or same for harmonic (played together).
 */
fun buildInterval(rootSemitones: Int, interval: IntervalType): List<Float> {
    val rootFreq = noteFrequency(rootSemitones)
    val intervalFreq = noteFrequency(rootSemitones + interval.semitones)
    return listOf(rootFreq, intervalFreq)
}
```

#### 3.2 Add interval playback to AudioEngine

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/AudioEngine.kt` (modify)

```kotlin
/**
 * Play an interval in the specified direction.
 * @param frequencies [rootFreq, intervalFreq] where intervalFreq is always the higher note
 */
suspend fun playInterval(
    frequencies: List<Float>,  // [root, interval]
    direction: IntervalDirection,
    durationMs: Int = 500,
    pauseMs: Int = 200,
    intervalName: String,
    rootSemitones: Int
) {
    Log.d(TAG, "Playing interval: $intervalName, direction=$direction, root=$rootSemitones, freqs=$frequencies")

    when (direction) {
        IntervalDirection.HARMONIC -> {
            // Play both notes together (like a 2-note chord)
            playChord(frequencies, PlaybackMode.BLOCK, durationMs, intervalName, rootSemitones)
        }
        IntervalDirection.ASCENDING -> {
            // Play root (lower), pause, then interval note (higher)
            playSingleNote(frequencies[0], durationMs)
            delay(pauseMs.toLong())
            playSingleNote(frequencies[1], durationMs)
        }
        IntervalDirection.DESCENDING -> {
            // Play interval note (higher) first, pause, then root (lower)
            playSingleNote(frequencies[1], durationMs)
            delay(pauseMs.toLong())
            playSingleNote(frequencies[0], durationMs)
        }
    }
}

private suspend fun playSingleNote(frequency: Float, durationMs: Int) {
    // Generate and play a single square wave
    // (factor out from existing chord logic)
}
```

#### 3.3 Add IntervalPlaybackStrategy

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/AudioPlaybackStrategy.kt` (modify)

```kotlin
data class IntervalStrategy(
    private val audioEngine: AudioEngine,
    private val chordBuilder: ChordBuilder
) : AudioPlaybackStrategy<IntervalCard, GameAnswer.IntervalAnswer> {

    override suspend fun playCard(card: IntervalCard, rootSemitones: Int, durationMs: Int) {
        val frequencies = chordBuilder.buildInterval(rootSemitones, card.interval)
        audioEngine.playInterval(
            frequencies = frequencies,
            direction = card.direction,
            durationMs = durationMs,
            intervalName = card.interval.displayName,
            rootSemitones = rootSemitones
        )
    }

    override suspend fun playAnswer(
        answer: GameAnswer.IntervalAnswer,
        card: IntervalCard,
        rootSemitones: Int,
        durationMs: Int
    ) {
        val frequencies = chordBuilder.buildInterval(rootSemitones, answer.interval)
        audioEngine.playInterval(
            frequencies = frequencies,
            direction = card.direction,
            durationMs = durationMs,
            intervalName = answer.interval.displayName,
            rootSemitones = rootSemitones
        )
    }
}
```

### Phase 4: UI Layer

#### 4.1 Add IntervalReviewScreen

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/review/IntervalReviewScreen.kt` (new)

Follow the pattern from `ReviewScreen.kt`:
- Use `GenericReviewScreen` with interval-specific slots
- Show octave and direction (Melodic/Harmonic) as mode indicator
- Answer buttons for intervals in session

#### 4.2 Add IntervalReviewViewModel

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/review/IntervalReviewViewModel.kt` (new)

Follow `ReviewViewModel.kt` pattern with `IntervalCard` type parameter.

#### 4.3 Update HomeScreen

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/home/HomeScreen.kt` (modify)

- Add "Interval" tab to game type tabs
- Show interval due count
- Navigate to `IntervalReviewScreen`

#### 4.4 Update HistoryScreen

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/history/HistoryScreen.kt` (modify)

- Support `GameType.INTERVAL` in all three tabs
- Cards tab shows interval cards grouped by interval type
- Stats tab shows interval confusion matrix (actual vs answered)

### Phase 5: Testing

#### 5.1 Unit Tests

- `IntervalCardTest` - Card ID parsing, display name
- `IntervalDeckTest` - Unlock group assignment, starting cards
- `IntervalCardDaoTest` - DAO operations (follow CardDaoTest pattern)
- `IntervalCardDaoDeprecationTest` - Use `DeprecationTestHelper` with adapter
- `ChordBuilderTest` - Add interval building tests
- `AudioEngineTest` - Add interval playback tests

#### 5.2 Integration Tests

- `IntervalCardSelectionTest` - Card selection algorithm
- `IntervalSessionLifecycleTest` - Full session flow

## Files to Create

| File | Description |
|------|-------------|
| `audio/IntervalType.kt` | Interval enum with semitones |
| `audio/IntervalDirection.kt` | Melodic/Harmonic enum |
| `model/IntervalCard.kt` | Card model implementing GameCard |
| `model/IntervalDeck.kt` | Unlock progression logic |
| `data/entity/IntervalCardEntity.kt` | Room entity |
| `data/db/IntervalCardDao.kt` | DAO with queries |
| `data/repository/IntervalCardOperations.kt` | GameCardOperations impl |
| `ui/review/IntervalReviewScreen.kt` | Review UI |
| `ui/review/IntervalReviewViewModel.kt` | ViewModel |

## Files to Modify

| File | Change |
|------|--------|
| `data/entity/GameType.kt` | Add `INTERVAL` |
| `model/GameAnswer.kt` | Add `IntervalAnswer` |
| `model/GameTypeConfig.kt` | Add `IntervalGame` sealed subclass |
| `audio/ChordBuilder.kt` | Add `buildInterval()` |
| `audio/AudioEngine.kt` | Add `playInterval()`, `playSingleNote()` |
| `audio/AudioPlaybackStrategy.kt` | Add `IntervalStrategy` |
| `data/db/EarbsDatabase.kt` | Add entity, DAO, migration |
| `data/repository/EarbsRepository.kt` | Add interval methods |
| `ui/home/HomeScreen.kt` | Add Interval tab |
| `ui/history/HistoryScreen.kt` | Support Interval game type |

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS interval_cards (
                id TEXT PRIMARY KEY NOT NULL,
                interval TEXT NOT NULL,
                octave INTEGER NOT NULL,
                direction TEXT NOT NULL,
                unlocked INTEGER NOT NULL DEFAULT 1,
                deprecated INTEGER NOT NULL DEFAULT 0
            )
        """)

        database.execSQL("CREATE INDEX IF NOT EXISTS index_interval_cards_unlocked ON interval_cards(unlocked)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_interval_cards_deprecated ON interval_cards(deprecated)")
    }
}
```

## Estimated Impact

- ~15 new files
- ~10 modified files
- ~1800 lines of new code
- ~200 lines of modified code
- Follows existing patterns exactly - low risk

## Open Questions

1. Should the confusion matrix show direction as well, or just interval? (Suggest: just interval - direction is secondary)
2. Starting deck has 3 cards (1 interval × 3 directions) - should we start with 2 intervals (6 cards) for more variety?
3. Should we show direction in the card info display, or just in a mode indicator like the other games?
