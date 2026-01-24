# Earbs - Chord Ear Training App

Android app (Kotlin, Jetpack Compose) for ear training chord recognition using FSRS spaced repetition.

## Important

- **Authoritative spec**: `docs/spec.md` — refer to it for detailed requirements
- **Commit often**: Make a git commit after each code change
- **Testing audio**: Claude cannot hear audio output. The synthesis engine should log what it's synthesizing (frequencies, chord type, timestamps) so correctness can be verified from logs.
- **Verbose logging**: Log generously throughout the app for testing/debugging. Exception: avoid logging inside the realtime audio synthesis loop to maintain performance.

## Environment

- Android SDK: `~/android-sdk`

## Core Concepts

- **Card**: A `(chord_type, octave)` pair. User answers chord type only.
- **Octaves**: 3, 4, 5 (octave 4 is starting point)
- **Review session**: 4 cards from same octave, 40 trials total (~10 per card)

## Chord Types & Intervals (semitones from root)

| Type   | Intervals      |
|--------|----------------|
| Major  | 0, 4, 7        |
| Minor  | 0, 3, 7        |
| Sus2   | 0, 2, 7        |
| Sus4   | 0, 5, 7        |
| Dom7   | 0, 4, 7, 10    |
| Maj7   | 0, 4, 7, 11    |
| Min7   | 0, 3, 7, 10    |

## Grading (per card after session)

- 10/10 correct → Easy
- 9/10 correct → Good
- 8/10 correct → Hard
- ≤7/10 correct → Again

## Card Selection Algorithm

1. Get due cards (next_review ≤ now)
2. Group by octave, pick octave with most due
3. If <4 due, pad with non-due cards from same octave
4. Run session with those 4 cards

## Starting Deck & Unlock Order

1. Major, Minor, Sus2, Sus4 @ octave 4 *(starting deck)*
2. Major, Minor @ octave 3
3. Sus2, Sus4 @ octave 3
4. Major, Minor @ octave 5
5. Sus2, Sus4 @ octave 5
6. Dom7, Maj7 @ octave 4
7. Min7 @ octave 4, Dom7 @ octave 3
8. (continues interleaving new types with octave expansion)

## Audio

- Square wave synthesis via AudioTrack
- Block (simultaneous) or arpeggiated playback modes
- Root note randomized within octave (prevents memorizing absolute pitches)
- Reference: A4 = 440Hz, frequency = `440 * 2^(semitones_from_A4 / 12)`
