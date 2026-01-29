# Plan: Scale Recognition Game

## Overview

Add a fifth game type for recognizing scales and modes. This trains the ear to identify the characteristic "color" of different scales, essential for improvisation and understanding modal harmony.

**Card tuple:** `(scale, octave, direction)`
**Total cards:** 90 (10 scales × 3 octaves × 3 directions)
**Answer strategy:** Session-based (like Chord Type game)

## Design Decisions

### Scales (10 total)

Organized from most common to more exotic:

| Name | Intervals (semitones from root) | Display Name |
|------|--------------------------------|--------------|
| MAJOR | 0, 2, 4, 5, 7, 9, 11, 12 | Major |
| NATURAL_MINOR | 0, 2, 3, 5, 7, 8, 10, 12 | Natural Minor |
| HARMONIC_MINOR | 0, 2, 3, 5, 7, 8, 11, 12 | Harmonic Minor |
| MELODIC_MINOR | 0, 2, 3, 5, 7, 9, 11, 12 | Melodic Minor |
| DORIAN | 0, 2, 3, 5, 7, 9, 10, 12 | Dorian |
| MIXOLYDIAN | 0, 2, 4, 5, 7, 9, 10, 12 | Mixolydian |
| PHRYGIAN | 0, 1, 3, 5, 7, 8, 10, 12 | Phrygian |
| LYDIAN | 0, 2, 4, 6, 7, 9, 11, 12 | Lydian |
| MAJOR_PENTATONIC | 0, 2, 4, 7, 9, 12 | Major Pentatonic |
| MINOR_PENTATONIC | 0, 3, 5, 7, 10, 12 | Minor Pentatonic |

**Not included (for now):** Locrian (rarely used melodically), Blues scale (could add later), exotic scales.

### Directions (3)

- **ASCENDING**: Scale played from root up to octave
- **DESCENDING**: Scale played from octave down to root
- **BOTH**: Ascending then descending (full round trip)

This mirrors how scales are practiced and tested in traditional music education.

### Card ID Format

`{scale}_{octave}_{direction}`

Examples:
- `MAJOR_4_ASCENDING` - Major scale at octave 4, ascending
- `DORIAN_3_BOTH` - Dorian mode at octave 3, up and down
- `MINOR_PENTATONIC_5_DESCENDING` - Minor pentatonic at octave 5, descending

### Audio Playback

**Ascending:**
- Play each scale degree sequentially from root to octave
- Note duration: ~200ms each (faster than chord arpeggios)
- Brief pause between notes: ~50ms

**Descending:**
- Play each scale degree from octave down to root
- Same timing as ascending

**Both:**
- Play ascending, brief pause (~150ms), then descending
- Gives the full "character" of the scale

Root note is randomized within the octave (like other games).

### Unlock Progression (10 groups of 9 cards)

Each group contains one scale across all octaves and directions (1 scale × 3 octaves × 3 directions = 9 cards).

Groups organized by familiarity and difficulty:

| Group | Scale | Rationale |
|-------|-------|-----------|
| 0 | Major | Most familiar, reference point |
| 1 | Natural Minor | Second most common |
| 2 | Major Pentatonic | 5 notes, very distinctive |
| 3 | Minor Pentatonic | 5 notes, blues/rock foundation |
| 4 | Dorian | Common in jazz/rock, minor with bright 6th |
| 5 | Mixolydian | Dominant sound, classic rock |
| 6 | Harmonic Minor | Distinctive augmented 2nd |
| 7 | Melodic Minor | Jazz minor, smooth |
| 8 | Phrygian | Spanish/flamenco flavor |
| 9 | Lydian | Bright, dreamy, raised 4th |

**Starting deck:** Major scale @ octave 4, all 3 directions (3 cards)

### Answer Options

Session-based: show distinct scales present in the current session's cards.

## Implementation

### Phase 1: Model Layer

#### 1.1 Add ScaleType enum

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/ScaleType.kt` (new)

```kotlin
package net.xmppwocky.earbs.audio

enum class ScaleType(
    val intervals: List<Int>,  // Semitones from root, including octave
    val displayName: String
) {
    MAJOR(listOf(0, 2, 4, 5, 7, 9, 11, 12), "Major"),
    NATURAL_MINOR(listOf(0, 2, 3, 5, 7, 8, 10, 12), "Natural Minor"),
    HARMONIC_MINOR(listOf(0, 2, 3, 5, 7, 8, 11, 12), "Harmonic Minor"),
    MELODIC_MINOR(listOf(0, 2, 3, 5, 7, 9, 11, 12), "Melodic Minor"),
    DORIAN(listOf(0, 2, 3, 5, 7, 9, 10, 12), "Dorian"),
    MIXOLYDIAN(listOf(0, 2, 4, 5, 7, 9, 10, 12), "Mixolydian"),
    PHRYGIAN(listOf(0, 1, 3, 5, 7, 8, 10, 12), "Phrygian"),
    LYDIAN(listOf(0, 2, 4, 6, 7, 9, 11, 12), "Lydian"),
    MAJOR_PENTATONIC(listOf(0, 2, 4, 7, 9, 12), "Major Pentatonic"),
    MINOR_PENTATONIC(listOf(0, 3, 5, 7, 10, 12), "Minor Pentatonic");

    val noteCount: Int get() = intervals.size
}
```

#### 1.2 Add ScaleDirection enum

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/ScaleDirection.kt` (new)

```kotlin
package net.xmppwocky.earbs.audio

enum class ScaleDirection(val displayName: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending"),
    BOTH("Both");  // Up then down
}
```

#### 1.3 Add ScaleCard model

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/ScaleCard.kt` (new)

```kotlin
package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType

data class ScaleCard(
    val scale: ScaleType,
    override val octave: Int,
    val direction: ScaleDirection
) : GameCard {
    override val id: String
        get() = "${scale.name}_${octave}_${direction.name}"

    override val playbackMode: PlaybackMode
        get() = PlaybackMode.ARPEGGIATED  // Scales are always sequential

    override val displayName: String
        get() = scale.displayName

    companion object {
        fun fromId(id: String): ScaleCard {
            val parts = id.split("_")
            // Format: SCALE_TYPE_OCTAVE_DIRECTION
            // Scale names may have underscores (e.g., NATURAL_MINOR)
            val direction = ScaleDirection.valueOf(parts.last())
            val octave = parts[parts.size - 2].toInt()
            val scaleName = parts.dropLast(2).joinToString("_")
            val scale = ScaleType.valueOf(scaleName)
            return ScaleCard(scale, octave, direction)
        }
    }
}
```

#### 1.4 Add GameAnswer.ScaleAnswer

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/GameAnswer.kt` (modify)

```kotlin
data class ScaleAnswer(val scale: ScaleType) : GameAnswer {
    override val displayName: String get() = scale.displayName
}
```

#### 1.5 Add GameType.SCALE

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/entity/GameType.kt` (modify)

```kotlin
enum class GameType {
    CHORD_TYPE,
    CHORD_FUNCTION,
    CHORD_PROGRESSION,
    INTERVAL,
    SCALE  // Add this
}
```

#### 1.6 Add ScaleGame to GameTypeConfig

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/GameTypeConfig.kt` (modify)

```kotlin
data class ScaleGame(
    override val gameType: GameType = GameType.SCALE
) : GameTypeConfig<ScaleCard, GameAnswer.ScaleAnswer>() {

    override val totalCards: Int = 90  // 10 scales × 3 octaves × 3 directions
    override val cardsPerUnlock: Int = 9  // 1 scale × 3 octaves × 3 directions
    override val maxUnlockLevel: Int = 9

    override fun getAnswerOptions(
        card: ScaleCard,
        session: GenericReviewSession<ScaleCard>
    ): List<GameAnswer.ScaleAnswer> {
        // Session-based: distinct scales in session
        return session.cards
            .map { it.scale }
            .distinct()
            .sorted()
            .map { GameAnswer.ScaleAnswer(it) }
    }

    override fun isCorrectAnswer(
        card: ScaleCard,
        answer: GameAnswer.ScaleAnswer
    ): Boolean = card.scale == answer.scale

    override fun getCorrectAnswer(card: ScaleCard): GameAnswer.ScaleAnswer =
        GameAnswer.ScaleAnswer(card.scale)

    override fun getUnlockGroupIndex(card: ScaleCard): Int =
        ScaleDeck.getGroupIndex(card)

    override fun getUnlockGroupName(groupIndex: Int): String =
        ScaleDeck.getGroupName(groupIndex)
}
```

#### 1.7 Add ScaleDeck

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/model/ScaleDeck.kt` (new)

```kotlin
package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.audio.ScaleDirection
import net.xmppwocky.earbs.audio.ScaleType

object ScaleDeck {
    // 10 groups, one per scale, ordered by familiarity/difficulty
    // Each group has 9 cards (3 octaves × 3 directions)

    private val unlockOrder = listOf(
        ScaleType.MAJOR,            // Group 0: Reference scale
        ScaleType.NATURAL_MINOR,    // Group 1: Most common minor
        ScaleType.MAJOR_PENTATONIC, // Group 2: Simple, distinctive
        ScaleType.MINOR_PENTATONIC, // Group 3: Blues/rock foundation
        ScaleType.DORIAN,           // Group 4: Jazz/rock minor variant
        ScaleType.MIXOLYDIAN,       // Group 5: Dominant/rock sound
        ScaleType.HARMONIC_MINOR,   // Group 6: Distinctive aug 2nd
        ScaleType.MELODIC_MINOR,    // Group 7: Jazz minor
        ScaleType.PHRYGIAN,         // Group 8: Spanish/exotic
        ScaleType.LYDIAN            // Group 9: Dreamy, bright
    )

    fun getGroupIndex(card: ScaleCard): Int {
        return unlockOrder.indexOf(card.scale)
    }

    fun getGroupName(groupIndex: Int): String {
        return unlockOrder.getOrNull(groupIndex)?.displayName ?: "Unknown"
    }

    fun getAllCards(): List<ScaleCard> {
        return ScaleType.entries.flatMap { scale ->
            listOf(3, 4, 5).flatMap { octave ->
                ScaleDirection.entries.map { direction ->
                    ScaleCard(scale, octave, direction)
                }
            }
        }
    }

    fun getStartingCards(): List<ScaleCard> {
        // Major scale at octave 4, all 3 directions
        return listOf(
            ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.ASCENDING),
            ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.DESCENDING),
            ScaleCard(ScaleType.MAJOR, 4, ScaleDirection.BOTH)
        )
    }
}
```

### Phase 2: Data Layer

#### 2.1 Add ScaleCardEntity

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/entity/ScaleCardEntity.kt` (new)

```kotlin
package net.xmppwocky.earbs.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scale_cards")
data class ScaleCardEntity(
    @PrimaryKey val id: String,
    val scale: String,
    val octave: Int,
    val direction: String,
    val unlocked: Boolean = true,
    val deprecated: Boolean = false
)
```

#### 2.2 Add ScaleCardDao

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/db/ScaleCardDao.kt` (new)

Follow the pattern from `CardDao.kt` with:
- `ScaleCardWithFsrs` data class
- Standard queries: `getDueCards`, `getNonDueCards`, `countDue`, `countUnlocked`, etc.
- `setDeprecated`, `setUnlocked` mutations
- Join with `fsrs_state` table using `gameType = 'SCALE'`

#### 2.3 Add ScaleCardOperations

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/repository/ScaleCardOperations.kt` (new)

```kotlin
class ScaleCardOperations(
    private val scaleCardDao: ScaleCardDao,
    private val fsrsStateDao: FsrsStateDao
) : GameCardOperations<ScaleCard> {
    // Implement interface methods following ChordTypeCardOperations pattern
    // Group by (octave, direction) - no additional grouping key
}
```

#### 2.4 Update EarbsDatabase

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/db/EarbsDatabase.kt` (modify)

- Add `ScaleCardEntity` to `@Database(entities = [...])`
- Add `abstract fun scaleCardDao(): ScaleCardDao`
- Add migration to create `scale_cards` table

#### 2.5 Update EarbsRepository

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/data/repository/EarbsRepository.kt` (modify)

- Add `scaleCardDao` parameter
- Add `ScaleCardOperations` instance
- Add `initializeScaleCards()` method
- Add `getScaleCardsForSession()` method
- Add `recordScaleTrialAndUpdateFsrs()` method

### Phase 3: Audio Layer

#### 3.1 Extend ChordBuilder for scales

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/ChordBuilder.kt` (modify)

```kotlin
/**
 * Build frequencies for a scale.
 * Returns list of frequencies from root through all scale degrees to octave.
 */
fun buildScale(rootSemitones: Int, scale: ScaleType): List<Float> {
    return scale.intervals.map { interval ->
        noteFrequency(rootSemitones + interval)
    }
}
```

#### 3.2 Add scale playback to AudioEngine

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/AudioEngine.kt` (modify)

```kotlin
/**
 * Play a scale in the specified direction.
 * @param frequencies List of scale degree frequencies (root to octave)
 */
suspend fun playScale(
    frequencies: List<Float>,
    direction: ScaleDirection,
    noteDurationMs: Int = 200,
    pauseBetweenNotesMs: Int = 50,
    pauseBetweenDirectionsMs: Int = 150,
    scaleName: String,
    rootSemitones: Int
) {
    Log.d(TAG, "Playing scale: $scaleName, direction=$direction, root=$rootSemitones, notes=${frequencies.size}")

    when (direction) {
        ScaleDirection.ASCENDING -> {
            playNoteSequence(frequencies, noteDurationMs, pauseBetweenNotesMs)
        }
        ScaleDirection.DESCENDING -> {
            playNoteSequence(frequencies.reversed(), noteDurationMs, pauseBetweenNotesMs)
        }
        ScaleDirection.BOTH -> {
            // Ascending
            playNoteSequence(frequencies, noteDurationMs, pauseBetweenNotesMs)
            delay(pauseBetweenDirectionsMs.toLong())
            // Descending (skip the top note to avoid playing it twice)
            playNoteSequence(frequencies.reversed().drop(1), noteDurationMs, pauseBetweenNotesMs)
        }
    }
}

private suspend fun playNoteSequence(
    frequencies: List<Float>,
    noteDurationMs: Int,
    pauseMs: Int
) {
    frequencies.forEachIndexed { index, freq ->
        playSingleNote(freq, noteDurationMs)
        if (index < frequencies.size - 1) {
            delay(pauseMs.toLong())
        }
    }
}
```

#### 3.3 Add ScalePlaybackStrategy

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/AudioPlaybackStrategy.kt` (modify)

```kotlin
data class ScaleStrategy(
    private val audioEngine: AudioEngine,
    private val chordBuilder: ChordBuilder
) : AudioPlaybackStrategy<ScaleCard, GameAnswer.ScaleAnswer> {

    override suspend fun playCard(card: ScaleCard, rootSemitones: Int, durationMs: Int) {
        val frequencies = chordBuilder.buildScale(rootSemitones, card.scale)
        audioEngine.playScale(
            frequencies = frequencies,
            direction = card.direction,
            noteDurationMs = durationMs / 3,  // Faster for scales
            scaleName = card.scale.displayName,
            rootSemitones = rootSemitones
        )
    }

    override suspend fun playAnswer(
        answer: GameAnswer.ScaleAnswer,
        card: ScaleCard,
        rootSemitones: Int,
        durationMs: Int
    ) {
        val frequencies = chordBuilder.buildScale(rootSemitones, answer.scale)
        audioEngine.playScale(
            frequencies = frequencies,
            direction = card.direction,
            noteDurationMs = durationMs / 3,
            scaleName = answer.scale.displayName,
            rootSemitones = rootSemitones
        )
    }
}
```

### Phase 4: UI Layer

#### 4.1 Add ScaleReviewScreen

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/review/ScaleReviewScreen.kt` (new)

Follow the pattern from `ReviewScreen.kt`:
- Use `GenericReviewScreen` with scale-specific slots
- Show octave and direction (Ascending/Descending/Both) as mode indicator
- Answer buttons for scales in session

#### 4.2 Add ScaleReviewViewModel

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/review/ScaleReviewViewModel.kt` (new)

Follow `ReviewViewModel.kt` pattern with `ScaleCard` type parameter.

#### 4.3 Update HomeScreen

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/home/HomeScreen.kt` (modify)

- Add "Scale" tab to game type tabs
- Show scale due count
- Navigate to `ScaleReviewScreen`

#### 4.4 Update HistoryScreen

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/ui/history/HistoryScreen.kt` (modify)

- Support `GameType.SCALE` in all three tabs
- Cards tab shows scale cards grouped by scale type
- Stats tab shows scale confusion matrix (actual vs answered)

### Phase 5: Testing

#### 5.1 Unit Tests

- `ScaleCardTest` - Card ID parsing, display name
- `ScaleDeckTest` - Unlock group assignment, starting cards
- `ScaleTypeTest` - Verify interval definitions are correct
- `ScaleCardDaoTest` - DAO operations (follow CardDaoTest pattern)
- `ScaleCardDaoDeprecationTest` - Use `DeprecationTestHelper` with adapter
- `ChordBuilderTest` - Add scale building tests
- `AudioEngineTest` - Add scale playback tests

#### 5.2 Integration Tests

- `ScaleCardSelectionTest` - Card selection algorithm
- `ScaleSessionLifecycleTest` - Full session flow

## New Abstraction: NoteSequencePlayer

Both intervals and scales play sequences of single notes. We could extract a shared abstraction:

**File:** `app/src/main/kotlin/net/xmppwocky/earbs/audio/NoteSequencePlayer.kt` (new)

```kotlin
/**
 * Plays sequences of single notes. Used by interval and scale playback.
 */
class NoteSequencePlayer(private val audioEngine: AudioEngine) {

    suspend fun playNote(frequency: Float, durationMs: Int) {
        // Single note playback
    }

    suspend fun playSequence(
        frequencies: List<Float>,
        noteDurationMs: Int,
        pauseMs: Int
    ) {
        frequencies.forEachIndexed { index, freq ->
            playNote(freq, noteDurationMs)
            if (index < frequencies.size - 1) {
                delay(pauseMs.toLong())
            }
        }
    }

    suspend fun playPair(
        freq1: Float,
        freq2: Float,
        durationMs: Int,
        pauseMs: Int
    ) {
        playNote(freq1, durationMs)
        delay(pauseMs.toLong())
        playNote(freq2, durationMs)
    }

    suspend fun playSimultaneous(
        frequencies: List<Float>,
        durationMs: Int
    ) {
        // Reuse existing chord playback
    }
}
```

This would be used by both `IntervalStrategy` and `ScaleStrategy`, keeping `AudioEngine` focused on low-level synthesis.

## Files to Create

| File | Description |
|------|-------------|
| `audio/ScaleType.kt` | Scale enum with intervals |
| `audio/ScaleDirection.kt` | Ascending/Descending/Both enum |
| `model/ScaleCard.kt` | Card model implementing GameCard |
| `model/ScaleDeck.kt` | Unlock progression logic |
| `data/entity/ScaleCardEntity.kt` | Room entity |
| `data/db/ScaleCardDao.kt` | DAO with queries |
| `data/repository/ScaleCardOperations.kt` | GameCardOperations impl |
| `ui/review/ScaleReviewScreen.kt` | Review UI |
| `ui/review/ScaleReviewViewModel.kt` | ViewModel |
| `audio/NoteSequencePlayer.kt` | Shared single-note playback (optional) |

## Files to Modify

| File | Change |
|------|--------|
| `data/entity/GameType.kt` | Add `SCALE` |
| `model/GameAnswer.kt` | Add `ScaleAnswer` |
| `model/GameTypeConfig.kt` | Add `ScaleGame` sealed subclass |
| `audio/ChordBuilder.kt` | Add `buildScale()` |
| `audio/AudioEngine.kt` | Add `playScale()`, `playNoteSequence()` |
| `audio/AudioPlaybackStrategy.kt` | Add `ScaleStrategy` |
| `data/db/EarbsDatabase.kt` | Add entity, DAO, migration |
| `data/repository/EarbsRepository.kt` | Add scale methods |
| `ui/home/HomeScreen.kt` | Add Scale tab |
| `ui/history/HistoryScreen.kt` | Support Scale game type |

## Database Migration

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS scale_cards (
                id TEXT PRIMARY KEY NOT NULL,
                scale TEXT NOT NULL,
                octave INTEGER NOT NULL,
                direction TEXT NOT NULL,
                unlocked INTEGER NOT NULL DEFAULT 1,
                deprecated INTEGER NOT NULL DEFAULT 0
            )
        """)

        database.execSQL("CREATE INDEX IF NOT EXISTS index_scale_cards_unlocked ON scale_cards(unlocked)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_scale_cards_deprecated ON scale_cards(deprecated)")
    }
}
```

## Estimated Impact

- ~15 new files (+ 1 optional shared abstraction)
- ~10 modified files
- ~1800 lines of new code
- ~200 lines of modified code
- Follows existing patterns exactly - low risk

## Open Questions

1. Should "Both" direction skip the repeated top note? (Current plan: yes, to sound more natural)
2. Note duration for scales - 200ms feels right for 7-8 note scales. Should pentatonic (5 notes) be slower?
3. Should we include the Blues scale? It's popular but adds complexity (it's pentatonic + chromatic passing tone)
4. Melodic minor traditionally descends as natural minor - should we follow this convention or always use the ascending form?

## Potential Future Extensions

- **Locrian mode** - Rarely used melodically, but completes the church modes
- **Blues scale** - Minor pentatonic + b5, very distinctive
- **Whole tone scale** - Symmetrical, dreamy sound
- **Chromatic scale** - All 12 notes, useful reference
- **Bebop scales** - 8-note scales used in jazz
