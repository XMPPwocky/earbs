# Chord Ear Training App — Claude Code Prompt

## Overview

Build an Android app (Kotlin, Jetpack Compose) for ear training focused on chord recognition. The app uses FSRS (Free Spaced Repetition Scheduler) to optimize review scheduling.

## Game Modes

The app has two training games:

### Chord Type Game (Game 1)
The original game mode. The user hears a chord and identifies its type (major, minor, sus2, etc.).
- **48 total cards** (8 types × 3 octaves × 2 playback modes)
- User answers chord quality only

### Chord Function Game (Game 2)
A second game mode focused on recognizing chord functions within a key. The user hears a tonic chord followed by a target chord and identifies the roman numeral function.
- **72 total cards** (12 functions × 3 octaves × 2 playback modes)
- User answers the chord function (roman numeral)
- Audio plays tonic chord first to establish key, then target chord

## Core Concepts

### Cards

A **card** is a `(chord_type, octave, playback_mode)` tuple. Examples:
- Major @ octave 4, arpeggiated
- Minor @ octave 3, block
- Sus4 @ octave 5, arpeggiated

The user only answers the **chord type** — octave and playback mode affect the sound but aren't part of the answer. This trains recognition of chord *quality* across different registers and playback styles.

**Playback mode is a card property, not a user toggle.** This prevents users from "cheesing" by identifying the playback mode instead of the chord quality.

### Chord Types (8 total)

**Triads:**
1. Major
2. Minor
3. Sus2
4. Sus4

**7th Chords:**
5. Dominant 7th (Dom7)
6. Major 7th (Maj7)
7. Minor 7th (Min7)
8. Diminished 7th (Dim7)

### Octave Range

Octaves 3, 4, and 5. Octave 4 is the starting point (most familiar register).

### Review Sessions

A **review session** consists of:
- Configurable session size (10, 20, or 30 cards; default 20)
- 1 trial per card
- Cards are mixed (can include different octaves and playback modes)
- Order is randomized at session start
- Answer buttons show the chord types present in the session's cards

**Per-trial FSRS updates:**
Each answer immediately updates FSRS for that card:
- **Correct answer** → Good rating (extends interval)
- **Wrong answer** → Again rating (card becomes due soon), wrong answer is recorded

This gives FSRS accurate per-recall feedback rather than aggregated batch scores.

### Card Selection for Reviews

Sessions prefer cards from the same `(octave, playbackMode)` group to keep practice focused:

1. Query all due cards (next_review ≤ now)
2. Group by `(octave, playbackMode)`
3. Pick the group with the most due cards (tie-break: most overdue)
4. If that group has ≥session_size due cards: take session_size from that group
5. If that group has <session_size due cards:
   a. Take all due from that group
   b. Pad with non-due cards from the SAME group first
   c. If still <session_size, pad with cards from other groups
6. Shuffle the cards randomly
7. Run the review session

This keeps sessions focused on one octave and playback mode when possible, improving the learning experience.

### Card Unlock System

All 48 chord type cards (and 72 function cards) are pre-created in the database. Users can unlock or lock any card at any time through the History > Cards tab.

**Key features:**
- **Per-card toggle**: Each card has an individual lock/unlock toggle
- **Flexible progression**: Users can unlock cards in any order
- **FSRS preservation**: FSRS state is preserved when locking/unlocking cards
- **Visual grouping**: Cards are displayed grouped by type category, octave, and playback mode

**Total cards:** 8 chord types × 3 octaves × 2 playback modes = 48 cards

**Starting deck (unlocked by default):**
- Major, Minor, Sus2, Sus4 @ octave 4, arpeggiated (Chord Type game)
- IV, V, vi @ major key, octave 4, arpeggiated (Chord Function game)

**Card groups (12 groups for Chord Type):**

| # | Cards | Octave | Mode |
|---|-------|--------|------|
| 1 | Major, Minor, Sus2, Sus4 | 4 | Arpeggiated |
| 2 | Major, Minor, Sus2, Sus4 | 4 | Block |
| 3 | Major, Minor, Sus2, Sus4 | 3 | Arpeggiated |
| 4 | Major, Minor, Sus2, Sus4 | 3 | Block |
| 5 | Major, Minor, Sus2, Sus4 | 5 | Arpeggiated |
| 6 | Major, Minor, Sus2, Sus4 | 5 | Block |
| 7 | Dom7, Maj7, Min7, Dim7 | 4 | Arpeggiated |
| 8 | Dom7, Maj7, Min7, Dim7 | 4 | Block |
| 9 | Dom7, Maj7, Min7, Dim7 | 3 | Arpeggiated |
| 10 | Dom7, Maj7, Min7, Dim7 | 3 | Block |
| 11 | Dom7, Maj7, Min7, Dim7 | 5 | Arpeggiated |
| 12 | Dom7, Maj7, Min7, Dim7 | 5 | Block |

Only unlocked cards appear in review sessions. Locked cards are shown in the Cards tab but grayed out.

### Chord Function Game Details

**Card model:** `(function, key_quality, octave, playback_mode)`

**Functions (12 total):**
- **Major key (6):** ii, iii, IV, V, vi, vii°
- **Minor key (6):** ii°, III, iv, v, VI, VII

Note: The tonic (I/i) is not included as it's always played as the reference chord.

**Audio playback:**
1. Play tonic chord (I or i depending on key quality)
2. Brief pause
3. Play target chord (the function being tested)

The key's root note is randomized within the octave to prevent memorizing absolute pitches.

**Unlock order (24 groups of 3 cards each):**
Cards unlock in groups of 3 functions. Major key functions are unlocked first, followed by minor key functions. Within each key quality, octave 4 arpeggiated comes first, then block, then other octaves.

| # | Functions | Key | Octave | Mode |
|---|-----------|-----|--------|------|
| 1 | ii, iii, IV | Major | 4 | Arpeggiated | **Starting deck** |
| 2 | V, vi, vii° | Major | 4 | Arpeggiated |
| 3 | ii, iii, IV | Major | 4 | Block |
| 4 | V, vi, vii° | Major | 4 | Block |
| ... | (continues for octaves 3, 5) | | | |
| 13-24 | (minor key functions follow same pattern) | | | |

### Audio Synthesis

- **Square wave synthesis** (simple, distinctive tone)
- **PolyBLEP anti-aliasing** for reduced aliasing artifacts and smoother sound
- Sum square waves at the appropriate frequencies for each chord tone
- Two playback modes (determined by the card, not user choice):
  - **Block**: all notes simultaneously
  - **Arpeggiated**: notes played in sequence

### Chord Construction (for synthesis)

All chords are built from intervals above the root. Semitone offsets from root:

**Triads:**
| Chord Type | Intervals (semitones) |
|------------|----------------------|
| Major | 0, 4, 7 |
| Minor | 0, 3, 7 |
| Sus2 | 0, 2, 7 |
| Sus4 | 0, 5, 7 |

**7th Chords:**
| Chord Type | Intervals (semitones) |
|------------|----------------------|
| Dom7 | 0, 4, 7, 10 |
| Maj7 | 0, 4, 7, 11 |
| Min7 | 0, 3, 7, 10 |
| Dim7 | 0, 3, 6, 9 |

The root note should be randomized within the octave (any of the 12 semitones) so the user can't memorize absolute pitches.

### UX Flow

**Home screen:**
- **Game mode tabs**: Switch between Chord Type and Chord Function games
  - Tab titles show due counts (e.g., "Chord Type (5)")
  - Each game has separate unlock status
- "Start Review" / "Practice Early" button (label depends on whether cards are due)
- Shows deck overview (cards unlocked, how many due)
- "History" button (view past sessions, manage card unlocks, and view stats)
- "Settings" button (configure playback, session, and FSRS settings)

**Review screen:**
- Progress indicator (e.g., "Trial 5 / 20")
- Current card info (octave, and key quality for Chord Function game)
- Playback mode indicator (shows Block or Arpeggiated, based on current card)
- "Play" / "Replay" button (plays current chord using the card's playback mode)
- Can replay as many times as desired (same root note on replay)
- Answer buttons for each chord type/function in the session's cards
- After tapping answer: brief feedback (correct/incorrect with actual answer if wrong), FSRS update

**Auto-advance behavior:**
- On correct answer: auto-advances after configurable delay (300-2000ms, default 750ms)
- On wrong answer with Learn From Mistakes OFF: auto-advances after delay
- On wrong answer with Learn From Mistakes ON: enters learning mode (see below)

**Learn From Mistakes mode** (when enabled in settings):
- After a wrong answer, the chord the user selected is played automatically
- User can tap any answer button to hear that chord for comparison
- "Next" button appears to manually advance to next trial
- Helps user understand the difference between confused chords
- After all trials: navigate to results screen

**Results screen:**
- Shows total correct / total trials (e.g., "15 / 20 correct")
- Shows accuracy percentage
- Color-coded based on performance (green ≥90%, amber ≥70%, red <70%)
- **Per-card breakdown**: Expandable section showing each card's result
  - Shows card details (chord type, octave, playback mode)
  - Indicates correct/incorrect with color coding
  - If wrong, shows what user answered
- "Done" button returns to home screen

**History screen (3 tabs):**
- **Sessions tab:** List of past sessions with accuracy, expandable to show individual trials (including wrong answers given)
- **Cards tab:** All cards (locked and unlocked) for managing card unlocks
  - Cards grouped by type category (Triads/7ths), octave, and playback mode
  - Each card has a checkbox to toggle lock/unlock status
  - Locked cards appear grayed out with minimal info
  - Unlocked cards show due date and stability
  - Tap a card row (not checkbox) to open Card Details screen
- **Stats tab:**
  - Overall accuracy and per-card lifetime accuracy
  - **Confusion matrices**: Visual heatmap showing which chord types are confused with each other
    - Rows = actual chord type, Columns = user's answer
    - Filter by octave and key quality (for Chord Function game)
    - Color intensity indicates frequency of confusion

**Card Details screen** (accessed from Cards tab):
- Card header with lock/unlock toggle switch
- For locked cards: Shows message that card is locked with instructions
- For unlocked cards:
  - Accuracy over time chart (line graph of recent performance)
  - FSRS parameters display (stability, difficulty, interval, due date, phase)
  - Lifetime statistics (total reviews, correct count, accuracy %)
  - **Reset FSRS button**: Resets card to initial FSRS state (new card)
- Back button returns to Cards tab

**Settings screen:**
- Playback duration (300-1000ms slider)
- Session size (10, 20, or 30 cards)
- Target retention (0.70-0.95 slider, default 0.90)
- Auto-advance delay (300-2000ms slider, default 750ms)
- Learn from mistakes toggle (default ON)
- **Database backup button**: Export database to file via SAF (Storage Access Framework)
- **Database restore button**: Import database from file via SAF

---

## Implementation Epics

Work through these in order. Complete each epic before moving to the next.

---

### Epic 1: Minimal Sound + UI

**Goal:** Play a chord and allow the user to answer. No scoring, no persistence.

**Tasks:**
1. Set up Android project with Kotlin and Jetpack Compose
2. Implement square wave synthesis using AudioTrack
   - Function to generate a square wave at a given frequency and duration
   - Function to mix multiple frequencies into a chord
3. Implement chord construction
   - Given a chord type and root frequency, return the list of frequencies
   - Use A4 = 440Hz as reference, calculate other notes with equal temperament
4. Build basic UI:
   - "Play" button that plays a hardcoded chord (e.g., C Major @ octave 4)
   - Toggle for block vs arpeggiated playback
   - Answer buttons: Major, Minor, Sus2, Sus4
   - On answer tap: show "Correct!" or "Wrong — it was X"
5. Randomize the chord type on each play (from the 4 basic types)
6. Randomize the root note within the octave

**Milestone:** User can tap play, hear a random chord, tap an answer, see if correct. No state persists.

---

### Epic 2: Card System + Session Tracking

**Goal:** Introduce the card abstraction and track performance within a session.

**Tasks:**
1. Define data classes:
   ```kotlin
   data class Card(val chordType: ChordType, val octave: Int)
   enum class ChordType { MAJOR, MINOR, SUS2, SUS4, DOM7, MAJ7, MIN7, DIM7 }
   ```
2. Create initial deck: Major, Minor, Sus2, Sus4 @ octave 4
3. Implement trial logic:
   - Select cards for session, shuffle randomly
   - 1 trial per card, track correct/incorrect
4. Implement review session flow:
   - 20 cards, 1 trial each
   - Show progress during session
   - After all trials, show accuracy

**Milestone:** Full review session with starting cards, shows results at end. Still no persistence.

---

### Epic 3: FSRS Integration

**Goal:** Real spaced repetition with persistence.

**Tasks:**
1. Integrate FSRS (use fsrs-kotlin submodule)
2. Define persistent card state in Room:
   ```kotlin
   data class CardEntity(
       val id: String,  // "MAJOR_4_ARPEGGIATED"
       val chordType: String,
       val octave: Int,
       val playbackMode: String,
       val stability: Double,
       val difficulty: Double,
       val interval: Int,
       val dueDate: Long,
       val phase: Int,  // Added, ReLearning, Review
       val lapses: Int
   )
   ```
3. Set up Room database for card state persistence
4. Implement card selection algorithm:
   - Get due cards (dueDate ≤ now), group by (octave, playbackMode)
   - Pick the group with the most due cards
   - Pad from same group first, then other groups if needed
   - Shuffle and return cards
5. Per-trial FSRS updates (not batch):
   - Correct → Good rating
   - Wrong → Again rating (record what user answered)
6. Update home screen to show due count

**Milestone:** Spaced repetition fully functional. Cards come due at appropriate intervals.

---

### Epic 4: Progression + Unlock with Playback Mode as Card Property

**Goal:** User can expand their deck gradually. Playback mode is a card property.

**Key Changes:**
- Card model: `(chord_type, octave, playback_mode)` tuple
- Total cards: 48 (8 types × 3 octaves × 2 modes)
- Sessions mix cards across octaves and playback modes
- Dim7 chord type: (0, 3, 6, 9)

**Tasks:**
1. Add Dim7 chord type and playbackMode to Card model
2. Update CardEntity with playbackMode column, database migration
3. Define 12-group unlock order (4 cards per group)
4. "Add 4 Cards" button with unlock progress display
5. Replace playback mode toggle with read-only mode indicator

**Milestone:** User starts with 4 arpeggiated triads @ octave 4, can grow deck via "Add 4 Cards" button. Full app loop works with playback mode per card.

---

### Epic 5: Polish

**Goal:** Make it feel like a real app.

**Tasks:**
1. Settings screen:
   - Playback duration (300-1000ms)
   - Session size (10, 20, 30 cards)
   - Target retention (0.70-0.95)
   - (Future: volume, audio waveform choice)
2. History screen with 3 tabs:
   - Sessions: past sessions with accuracy, expandable to show trials
   - Cards: all unlocked cards with FSRS state
   - Stats: overall and per-card accuracy
3. Track wrong answers (what user said when incorrect)
4. UI polish:
   - Visual feedback on correct/incorrect with actual answer shown
   - Progress indicators during review
   - Color-coded accuracy display
5. Replay uses same root note (doesn't re-randomize)

**Milestone:** App is pleasant to use and robust.

---

## Technical Notes

### Square Wave Generation

```kotlin
fun generateSquareWave(frequency: Float, durationMs: Int, sampleRate: Int = 44100): ShortArray {
    val numSamples = (sampleRate * durationMs / 1000)
    val samples = ShortArray(numSamples)
    val period = sampleRate / frequency
    for (i in 0 until numSamples) {
        val phase = (i % period) / period
        samples[i] = if (phase < 0.5) Short.MAX_VALUE else Short.MIN_VALUE
    }
    return samples
}
```

Mix multiple waves by averaging their samples (watch for clipping — divide by number of notes).

### Frequency Calculation

```kotlin
fun noteFrequency(semitonesFromA4: Int): Float {
    return 440f * 2f.pow(semitonesFromA4 / 12f)
}
```

Octave 4 contains A4 (440Hz). C4 is 9 semitones below A4.

### FSRS Core

If implementing from scratch, the key formula for interval calculation:

```
new_interval = stability * ln(requested_retention) / ln(0.9)
```

Where stability is updated based on difficulty and grade. See the FSRS paper/repo for full formulas.

---

## Getting Started

Begin with Epic 1. Create a new Android project, get audio working, build the basic UI. Don't worry about architecture perfection — get it working first, refactor as needed in later epics.
