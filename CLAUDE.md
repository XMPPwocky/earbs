# Earbs - Chord Ear Training App

Android app (Kotlin, Jetpack Compose) for ear training chord recognition using FSRS spaced repetition.

## Important

- **Authoritative spec**: `docs/spec.md` — refer to it for detailed requirements
- **Commit often**: Make a git commit after each code change
- **Testing audio**: Claude cannot hear audio output. The synthesis engine should log what it's synthesizing (frequencies, chord type, timestamps) so correctness can be verified from logs.
- **Verbose logging**: Log generously throughout the app for testing/debugging. Exception: avoid logging inside the realtime audio synthesis loop to maintain performance.
- **Submodules**: ALWAYS ask before editing any code in submodules (e.g., `lib/fsrs-kotlin`). These are external dependencies.
- **Bug fixes**: When fixing a bug, first add tests that reproduce the bug and verify they fail. Only then implement the fix. If you can't think of tests that reproduce the bug, say so and ask for input.
- **Generic code**: Make code generic across game types when possible. Use shared abstractions (e.g., `GameTypeConfig`, `GameCardOperations`) rather than duplicating logic.

## Environment

- Android SDK: `~/android-sdk`
- Package name: `net.xmppwocky.earbs`
- Source directory: `app/src/main/kotlin/` (not `java/`)

## Game Types

Five ear training games, each with its own card type and deck:

| Game | Card tuple | Total cards | Answer options |
|------|------------|-------------|----------------|
| Chord Type | `(chord_type, octave, mode)` | 48 | Session-based (distinct types in session) |
| Chord Function | `(function, key_quality, octave, mode)` | 72 | Card-based (all functions for key) |
| Chord Progression | `(progression, octave, mode)` | 84 | Session-based (distinct progressions) |
| Interval | `(interval, octave, direction)` | 108 | Session-based (distinct intervals in session) |
| Scale | `(scale, octave, direction)` | 90 | Session-based (distinct scales in session) |

Chord games: octaves 3/4/5, playback modes arpeggiated/block.
Interval/Scale games: octaves 3/4/5, directions ascending/descending/harmonic (intervals) or both (scales).
All games share FSRS scheduling.

## Architecture

Key abstractions for game-agnostic code:
- **`GameCard`** - Interface for all card types (`id`, `octave`, `playbackMode`, `displayName`)
- **`GameTypeConfig<C, A>`** - Sealed class with game-specific behavior (answer options, correctness checking, unlock groups)
- **`GameCardOperations<C>`** - Interface for card database operations (due cards, unlock, FSRS updates)
- **`GenericReviewSession<C>`** - Type-parameterized session that works with any card type

Database: FSRS state stored separately from card data (`fsrs_state` table with `gameType` discriminator).

## Adding a New Game Type

When adding a new game type, search for all places that handle existing game types and update them. Checklist:

- [ ] `GameType` enum in `data/entity/GameType.kt`
- [ ] Card entity, DAO, and `*WithFsrs` view class
- [ ] Deck object (card generation, unlock order, starting cards)
- [ ] `GameTypeConfig` sealed class entry
- [ ] `GameCardOperations` implementation
- [ ] Repository methods (due cards, unlock, FSRS updates, flows)
- [ ] Database migration (create table, prepopulate)
- [ ] Audio synthesis for the new sound type
- [ ] **MainActivity**: home screen card, session state, history screen data loading
- [ ] **HistoryScreen**: card display items, deprecated items, stats tab
- [ ] Tests for all new components

## Card Deprecation

Cards can be deprecated at the app level (e.g., when removing a progression type):
- `deprecated` column on all card tables (default 0)
- Deprecated cards excluded from reviews but historical data preserved
- Migration adds new cards and marks old ones deprecated atomically

## Chord Types (for Chord Type game)

| Type   | Intervals      |
|--------|----------------|
| Major  | 0, 4, 7        |
| Minor  | 0, 3, 7        |
| Sus2   | 0, 2, 7        |
| Sus4   | 0, 5, 7        |
| Dom7   | 0, 4, 7, 10    |
| Maj7   | 0, 4, 7, 11    |
| Min7   | 0, 3, 7, 10    |
| Dim7   | 0, 3, 6, 9     |

## FSRS & Card Selection

Per-trial grading: Correct → Good, Wrong → Again.

Card selection (all games):
1. Get unlocked, non-deprecated, due cards
2. If ≥20 due, take 20 most overdue
3. If <20 due, pad with non-due cards
4. Shuffle randomly

## Audio

- Square wave synthesis via AudioTrack
- Block (simultaneous) or arpeggiated playback modes (per-card property)
- Root note randomized within octave (prevents memorizing absolute pitches)
- Reference: A4 = 440Hz, frequency = `440 * 2^(semitones_from_A4 / 12)`

## Testing

### Unit Tests (Robolectric)

Location: `app/src/test/kotlin/`

Fast JVM-based tests (~50x faster than instrumented). Uses Robolectric for Android framework APIs and in-memory Room database.

Run all unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

Run specific test class:
```bash
./gradlew :app:testDebugUnitTest --tests "*CardDaoTest*"
```

Run with `--rerun` to force re-execution of cached tests.

### Instrumented Tests (Android Emulator)

Location: `app/src/androidTest/kotlin/`

UI tests using Compose Testing and tests requiring real Android environment. Requires emulator or device.

Run all instrumented tests:
```bash
./gradlew :app:connectedDebugAndroidTest
```

Check emulator status: `adb devices`

### Test Categories

| Category | Location | Examples |
|----------|----------|----------|
| DAO tests | `data/db/` | CardDaoTest, FunctionCardDaoTest, *DaoDeprecationTest |
| Repository tests | `data/repository/` | EarbsRepositoryTest, CardSelectionTest |
| Model tests | `model/` | DeckTest, ProgressionDeckTest, ProgressionTypeTest |
| Audio tests | `audio/` | AudioEngineTest, ChordBuilderTest |
| UI tests (instrumented) | `ui/` | ReviewScreenTest, HistoryScreenTest |

### Test Base Classes

- `DatabaseTestBase` - In-memory Room database with all DAOs, helper methods for creating test cards
- `ComposeTestBase` - Compose UI test setup with test rule
