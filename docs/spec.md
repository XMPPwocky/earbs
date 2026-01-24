# Chord Ear Training App — Claude Code Prompt

## Overview

Build an Android app (Kotlin, Jetpack Compose) for ear training focused on chord recognition. The user hears a chord and identifies its type (major, minor, sus2, etc.). The app uses FSRS (Free Spaced Repetition Scheduler) to optimize review scheduling.

## Core Concepts

### Cards

A **card** is a `(chord_type, octave)` pair. Examples:
- Major @ octave 4
- Minor @ octave 3
- Sus4 @ octave 5

The user only answers the **chord type** — octave affects the sound but isn't part of the answer. This trains recognition of chord *quality* across different registers.

### Chord Types (in unlock order)

1. Major
2. Minor
3. Sus2
4. Sus4
5. Dominant 7th
6. Major 7th
7. Minor 7th
8. (More can be added later: 9ths, diminished, augmented, etc.)

### Octave Range

Octaves 3, 4, and 5. Octave 4 is the starting point (most familiar register).

### Review Sessions

A **review session** consists of:
- 4 cards, all from the **same octave** (to prevent guessing by register)
- 40 trials total, interleaved randomly (~10 per card)
- After all trials, each card is graded based on hit rate:
  - 10/10 correct → Easy
  - 9/10 correct → Good
  - 8/10 correct → Hard
  - ≤7/10 correct → Again

### Card Selection for Reviews

1. Query all cards, compute which are "due" (next_review ≤ now)
2. Group due cards by octave
3. Pick the octave with the most due cards
4. If that octave has <4 due cards, pad with non-due cards from the same octave (reviewing early is fine for FSRS)
5. Run the review session with those 4 cards

### Progression / Unlocking

The user starts with a minimal deck and can tap "Add 2 cards" to expand.

**Unlock order:**
1. Major, Minor, Sus2, Sus4 @ octave 4 *(starting deck)*
2. Major @ octave 3, Minor @ octave 3
3. Sus2 @ octave 3, Sus4 @ octave 3
4. Major @ octave 5, Minor @ octave 5
5. Sus2 @ octave 5, Sus4 @ octave 5
6. Dom7 @ octave 4, Maj7 @ octave 4
7. Min7 @ octave 4, Dom7 @ octave 3
8. ...continue interleaving new chord types with octave expansion

This keeps things interesting — the user gets new chord types before fully grinding all octaves.

### Audio Synthesis

- **Square wave synthesis** (simple, distinctive tone)
- Sum square waves at the appropriate frequencies for each chord tone
- Support two playback modes:
  - **Block**: all notes simultaneously
  - **Arpeggiated**: notes played in sequence (e.g., 100ms apart)
- User can toggle between modes

### Chord Construction (for synthesis)

All chords are built from intervals above the root. Semitone offsets from root:

| Chord Type | Intervals (semitones) |
|------------|----------------------|
| Major | 0, 4, 7 |
| Minor | 0, 3, 7 |
| Sus2 | 0, 2, 7 |
| Sus4 | 0, 5, 7 |
| Dom7 | 0, 4, 7, 10 |
| Maj7 | 0, 4, 7, 11 |
| Min7 | 0, 3, 7, 10 |

The root note should be randomized within the octave (any of the 12 semitones) so the user can't memorize absolute pitches.

### UX Flow

**Main screen:**
- "Start Review" button (if cards are due or available)
- "Add 2 Cards" button (to unlock next cards in progression)
- Shows deck overview (how many cards, how many due)

**Review screen:**
- "Play" button (plays current chord)
- Can replay as many times as desired
- Answer buttons for each chord type in the current deck
- After tapping answer: brief feedback (correct/incorrect), then next trial
- After 40 trials: show summary (per-card results), then return to main screen

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

### Epic 4: Progression + Unlock

**Goal:** User can expand their deck gradually.

**Tasks:**
1. Define full unlock order (see spec above)
2. Track unlock progress (persisted)
3. "Add 2 Cards" button:
   - Unlocks next 2 cards in progression
   - Initializes their FSRS state as "new" (due immediately)
4. Handle edge case: what if no cards are due?
   - Show "No cards due — come back later" or allow early review
5. Update answer buttons to only show chord types that exist in the user's deck

**Milestone:** User starts with 4 cards, can grow deck over time. Full app loop works.

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
