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

1. Query all cards, compute which are "due" (next_review ≤ now)
2. If ≥session_size due cards: select the most overdue
3. If <session_size due cards: pad with non-due cards (reviewing early is fine for FSRS)
4. Shuffle the cards randomly
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

**Home screen:**
- "Start Review" / "Practice Early" button (label depends on whether cards are due)
- "Add 4 Cards" button (to unlock next 4 cards in progression)
- Shows deck overview (X/48 cards unlocked, how many due)
- "History" button (view past sessions and card stats)
- "Settings" button (configure playback, session, and FSRS settings)

**Review screen:**
- Progress indicator (e.g., "Trial 5 / 20")
- Current card info (octave)
- Playback mode indicator (shows Block or Arpeggiated, based on current card)
- "Play" / "Replay" button (plays current chord using the card's playback mode)
- Can replay as many times as desired (same root note on replay)
- Answer buttons for each chord type in the session's cards
- After tapping answer: brief feedback (correct/incorrect with actual answer if wrong), FSRS update, auto-advance after 500ms
- After all trials: navigate to results screen

**Results screen:**
- Shows total correct / total trials (e.g., "15 / 20 correct")
- Shows accuracy percentage
- Color-coded based on performance (green ≥90%, amber ≥70%, red <70%)
- "Done" button returns to home screen

**History screen (3 tabs):**
- **Sessions tab:** List of past sessions with accuracy, expandable to show individual trials (including wrong answers given)
- **Cards tab:** All unlocked cards with FSRS state (stability, difficulty, interval, due date)
- **Stats tab:** Overall accuracy and per-card lifetime accuracy

**Settings screen:**
- Playback duration (300-1000ms slider)
- Session size (10, 20, or 30 cards)
- Target retention (0.70-0.95 slider, default 0.90)

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
   - Get due cards (dueDate ≤ now), ordered by most overdue
   - If ≥session_size due, take the most overdue
   - If <session_size due, pad with non-due cards
   - Shuffle and return mixed cards
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
