# Earbs Architecture

Android chord ear training app using FSRS spaced repetition.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Database:** Room (SQLite)
- **Audio:** AudioTrack (PCM synthesis)
- **Async:** Kotlin Coroutines
- **Spaced Repetition:** FSRS (git submodule)

## Project Structure

```
app/src/main/kotlin/net/xmppwocky/earbs/
├── MainActivity.kt          # Entry point & navigation
├── audio/
│   ├── AudioEngine.kt       # Square wave synthesis, playback
│   └── ChordBuilder.kt      # ChordType enum, frequency calculation
├── data/
│   ├── db/
│   │   ├── EarbsDatabase.kt # Room database, migrations
│   │   ├── CardDao.kt       # Card FSRS state queries
│   │   ├── ReviewSessionDao.kt
│   │   ├── TrialDao.kt
│   │   └── HistoryDao.kt    # Aggregated statistics
│   ├── entity/
│   │   ├── CardEntity.kt    # FSRS state per card
│   │   ├── ReviewSessionEntity.kt
│   │   └── TrialEntity.kt   # Individual answer records
│   └── repository/
│       └── EarbsRepository.kt  # Business logic coordinator
├── model/
│   ├── Card.kt              # (chord_type, octave, playback_mode) tuple
│   ├── ReviewSession.kt     # In-memory session state
│   └── Deck.kt              # Unlock progression
└── ui/
    ├── HomeScreen.kt        # Main hub, deck status
    ├── ReviewScreen.kt      # Active review session
    ├── ResultsScreen.kt     # Session results
    ├── HistoryScreen.kt     # Statistics (3 tabs)
    └── SettingsScreen.kt    # Playback, session, FSRS settings

lib/fsrs-kotlin/             # FSRS submodule (external dependency)
```

## Architecture Layers

### UI Layer

Single-activity architecture with enum-based navigation:

```kotlin
enum class Screen { HOME, REVIEW, RESULTS, HISTORY, SETTINGS }
```

State is managed via Compose `remember`/`mutableStateOf`. Screens are pure functions that receive state and emit callbacks.

**Navigation Flow:**
```
HOME ──→ REVIEW ──→ RESULTS ──→ HOME
  │                               ↑
  ├──→ HISTORY ───────────────────┤
  └──→ SETTINGS ──────────────────┘
```

### Domain Layer

**Card** - A unique (chord_type, octave, playback_mode) tuple:
```kotlin
data class Card(
    val chordType: ChordType,   // MAJOR, MINOR, SUS2, SUS4, DOM7, MAJ7, MIN7, DIM7
    val octave: Int,            // 3, 4, or 5
    val playbackMode: PlaybackMode  // BLOCK or ARPEGGIATED
)
```

**ReviewSession** - Tracks 20-card session progress in memory:
```kotlin
class ReviewSession(val cards: List<Card>) {
    var currentTrial: Int = 0
    var correctCount: Int = 0
    fun getCurrentCard(): Card?
    fun recordAnswer(correct: Boolean)
    fun isComplete(): Boolean
}
```

**Deck** - Defines unlock progression (12 groups of 4 cards):
```kotlin
object Deck {
    val STARTING_CARDS: List<Card>  // 4 triads @ octave 4, arpeggiated
    val UNLOCK_ORDER: List<UnlockGroup>
    const val TOTAL_CARDS = 48  // 8 types × 3 octaves × 2 modes
}
```

### Data Layer

**Repository Pattern** - `EarbsRepository` coordinates all data access:
- `initializeStartingDeck()` - First launch setup
- `selectCardsForReview()` - Card selection algorithm
- `recordTrialAndUpdateFsrs()` - Per-trial FSRS updates
- `unlockNextGroup()` - Progression system

**Room Database** - 3 tables with typed DAOs:

| Table | Purpose |
|-------|---------|
| `cards` | FSRS state per card (stability, difficulty, interval, dueDate, phase) |
| `review_sessions` | Session metadata (timestamps) |
| `trials` | Individual answer records (card, correct/wrong) |

**Settings** - SharedPreferences for:
- Playback duration (300-1000ms)
- Session size (10, 20, or 30 cards)
- Target retention (0.7-0.95)

### Audio Layer

**ChordBuilder** - Frequency calculation:
```kotlin
// A4 = 440Hz, freq = 440 * 2^(semitones/12)
fun noteFrequency(semitonesFromA4: Int): Float
fun buildChord(chordType: ChordType, rootSemitones: Int): List<Float>
fun randomRootInOctave(octave: Int): Int  // Prevents pitch memorization
```

**AudioEngine** - Square wave synthesis at 44.1 kHz:
- **BLOCK mode:** Mix all frequencies simultaneously
- **ARPEGGIATED mode:** Play notes sequentially

Logs synthesis details (frequencies, chord type) for headless testing.

## Key Flows

### Review Session Flow

```
1. User taps "Start Review"
   └─ selectCardsForReview():
      ├─ Get due cards (dueDate ≤ now)
      ├─ If < 20 due, pad with non-due cards
      └─ Shuffle randomly

2. For each of 20 trials:
   ├─ User taps Play → AudioEngine.playChord()
   ├─ User taps answer
   └─ recordTrialAndUpdateFsrs():
      ├─ Insert TrialEntity
      ├─ Calculate FSRS grade (Good or Again)
      └─ Update CardEntity (stability, difficulty, dueDate)

3. Session complete → Show results
```

### FSRS Integration

Per-trial grading with immediate updates:

| Answer | Rating | Effect |
|--------|--------|--------|
| Correct | Good | Interval extends based on stability |
| Wrong | Again | Card becomes due soon, enters ReLearning |

**Phases:**
- **Added (0):** New card, fixed short intervals
- **ReLearning (1):** Failed card, short intervals
- **Review (2):** Mature card, full FSRS calculation

### Card Unlock Progression

```
Group 0: Major, Minor, Sus2, Sus4 @ octave 4, arpeggiated (starting deck)
Group 1: Major, Minor, Sus2, Sus4 @ octave 4, block
Group 2: Major, Minor, Sus2, Sus4 @ octave 3, arpeggiated
...
Group 11: Dom7, Maj7, Min7, Dim7 @ octave 5, block (full deck)
```

User unlocks 4 cards at a time via "Add 4 Cards" button.

## Database Schema

### CardEntity
```kotlin
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,  // "MAJOR_4_ARPEGGIATED"
    val chordType: String,
    val octave: Int,
    val playbackMode: String,
    val stability: Double,       // FSRS parameter
    val difficulty: Double,      // FSRS parameter
    val interval: Int,           // Days until next review
    val dueDate: Long,           // Epoch millis
    val phase: Int,              // 0=Added, 1=ReLearning, 2=Review
    val lapses: Int,             // Count of "Again" ratings
    val unlocked: Boolean
)
```

### TrialEntity
```kotlin
@Entity(tableName = "trials")
data class TrialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val sessionId: Long,         // FK to review_sessions
    val cardId: String,          // FK to cards
    val timestamp: Long,
    val wasCorrect: Boolean
)
```

## Concurrency Model

- **Coroutines** with appropriate dispatchers
- `withContext(Dispatchers.IO)` for database and audio operations
- `LaunchedEffect` for Compose side effects
- `Flow` for reactive UI updates from database

## Testing Notes

Audio synthesis cannot be verified by listening in CI. The engine logs:
- Chord type being synthesized
- Root note (semitones from A4)
- All frequencies in Hz
- Playback mode and duration

Verify correctness by checking logs match expected intervals.
