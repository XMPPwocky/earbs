# Chord Ear Training App — Claude Code Prompt

## Overview

Build an Android app (Kotlin, Jetpack Compose) for ear training focused on chord recognition. The user hears a chord and identifies its type (major, minor, sus2, etc.). The app uses FSRS (Free Spaced Repetition Scheduler) to optimize review scheduling.

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
- 20 cards, 1 trial each
- Cards are mixed (can include different octaves and playback modes)
- Order is randomized at session start
- Answer buttons show the chord types present in the session's cards

**Per-trial FSRS updates:**
Each answer immediately updates FSRS for that card:
- **Correct answer** → Good rating (extends interval)
- **Wrong answer** → Again rating (card becomes due soon)

This gives FSRS accurate per-recall feedback rather than aggregated batch scores.

### Card Selection for Reviews

1. Query all cards, compute which are "due" (next_review ≤ now)
2. If ≥20 due cards: select the 20 most overdue
3. If <20 due cards: pad with non-due cards (reviewing early is fine for FSRS)
4. Shuffle the 20 cards randomly
5. Run the review session

### Progression / Unlocking

The user starts with a minimal deck and can tap "Add 4 Cards" to expand.

**Total cards:** 8 chord types × 3 octaves × 2 playback modes = 48 cards

**Unlock order (12 groups of 4 cards each):**

| # | Cards | Octave | Mode | Notes |
|---|-------|--------|------|-------|
| 1 | Major, Minor, Sus2, Sus4 | 4 | Arpeggiated | **Starting deck** |
| 2 | Major, Minor, Sus2, Sus4 | 4 | Block | |
| 3 | Major, Minor, Sus2, Sus4 | 3 | Arpeggiated | |
| 4 | Major, Minor, Sus2, Sus4 | 3 | Block | |
| 5 | Major, Minor, Sus2, Sus4 | 5 | Arpeggiated | |
| 6 | Major, Minor, Sus2, Sus4 | 5 | Block | |
| 7 | Dom7, Maj7, Min7, Dim7 | 4 | Arpeggiated | |
| 8 | Dom7, Maj7, Min7, Dim7 | 4 | Block | |
| 9 | Dom7, Maj7, Min7, Dim7 | 3 | Arpeggiated | |
| 10 | Dom7, Maj7, Min7, Dim7 | 3 | Block | |
| 11 | Dom7, Maj7, Min7, Dim7 | 5 | Arpeggiated | |
| 12 | Dom7, Maj7, Min7, Dim7 | 5 | Block | **Full deck** |

This order ensures:
- All triads are learned before 7th chords
- Each playback mode is practiced separately
- Octave expansion happens gradually

### Audio Synthesis

- **Square wave synthesis** (simple, distinctive tone)
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

**Main screen:**
- "Start Review" button (if cards are due or available)
- "Add 4 Cards" button (to unlock next 4 cards in progression)
- Shows deck overview (X/48 cards unlocked, how many due)

**Review screen:**
- Playback mode indicator (shows Block or Arpeggiated, based on current card)
- "Play" button (plays current chord using the card's playback mode)
- Can replay as many times as desired
- Answer buttons for each chord type in the session's cards
- After tapping answer: brief feedback (correct/incorrect), FSRS update, then next trial
- After 20 trials: show session summary (X/20 correct), then return to main screen

**Results screen:**
- Shows total correct / total trials (e.g., "15 / 20 correct")
- Shows accuracy percentage
- Color-coded based on performance

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
   enum class ChordType { MAJOR, MINOR, SUS2, SUS4, DOM7, MAJ7, MIN7 }
   ```
2. Create initial deck: Major, Minor, Sus2, Sus4 @ octave 4
3. Implement trial logic:
   - Randomly pick a card from the deck
   - Track correct/incorrect counts per card within session
4. Implement review session flow:
   - 40 trials
   - Interleave 4 cards randomly (for now, all 4 starting cards)
   - After 40 trials, show per-card hit rates
5. Compute grades from hit rates (just display, don't store yet):
   - 10/10 → Easy, 9/10 → Good, 8/10 → Hard, else → Again

**Milestone:** Full 40-trial review session with 4 cards, shows grades at end. Still no persistence.

---

### Epic 3: FSRS Integration

**Goal:** Real spaced repetition with persistence.

**Tasks:**
1. Integrate FSRS
   - Option A: Use existing library (search for `fsrs-kt` or JVM port)
   - Option B: Implement core FSRS algorithm directly (~50 lines of math)
2. Define persistent card state:
   ```kotlin
   data class CardState(
       val card: Card,
       val stability: Float,
       val difficulty: Float,
       val lastReview: Instant?,
       val nextReview: Instant,
       val reps: Int
   )
   ```
3. Set up Room database for card state persistence
4. Implement card selection algorithm:
   - Get all cards, filter to due (nextReview ≤ now)
   - Group by octave, pick octave with most due
   - If <4 due in that octave, pad with non-due cards from same octave
   - Return 4 cards for review
5. After review session, call FSRS update for each card with its grade
6. Update main screen to show due count

**Milestone:** Spaced repetition fully functional. Cards come due at appropriate intervals.

---

### Epic 4: Progression + Unlock with Playback Mode as Card Property

**Goal:** User can expand their deck gradually. Playback mode is now a card property.

**Key Changes:**
- Card model: `(chord_type, octave, playback_mode)` instead of `(chord_type, octave)`
- Total cards: 48 (8 types × 3 octaves × 2 modes)
- Session constraint: All 4 cards share the same (octave, playback_mode)
- New chord type: Dim7 (0, 3, 6, 9)

**Tasks:**
1. Add Dim7 chord type and playbackMode to Card model
2. Update CardEntity with playbackMode column, database migration
3. Define 12-group unlock order (4 cards per group)
4. Update card selection to group by (octave, playbackMode)
5. "Add 4 Cards" button with unlock progress display
6. Replace playback mode toggle with read-only mode indicator
7. Update spec.md

**Milestone:** User starts with 4 arpeggiated triads @ octave 4, can grow deck via "Add 4 Cards" button. Full app loop works with playback mode per card.

---

### Epic 5: Polish

**Goal:** Make it feel like a real app.

**Tasks:**
1. Settings screen:
   - Default playback mode (block/arpeggiated)
   - (Future: volume, audio waveform choice)
2. Session history / stats:
   - Track reviews completed, accuracy over time
3. UI polish:
   - Better visual feedback on correct/incorrect
   - Progress indicators during review
   - Nicer main screen layout
4. Handle app lifecycle properly (audio cleanup, state restoration)
5. Error handling and edge cases

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
