# Earbs - Chord Ear Training App

Android app (Kotlin, Jetpack Compose) for ear training chord recognition using FSRS spaced repetition.

## Important

- **Authoritative spec**: `docs/spec.md` — refer to it for detailed requirements
- **Commit often**: Make a git commit after each code change
- **Testing audio**: Claude cannot hear audio output. The synthesis engine should log what it's synthesizing (frequencies, chord type, timestamps) so correctness can be verified from logs.
- **Verbose logging**: Log generously throughout the app for testing/debugging. Exception: avoid logging inside the realtime audio synthesis loop to maintain performance.
- **Submodules**: ALWAYS ask before editing any code in submodules (e.g., `lib/fsrs-kotlin`). These are external dependencies.
- **Bug fixes**: When fixing a bug, first add tests that reproduce the bug and verify they fail. Only then implement the fix. If you can't think of tests that reproduce the bug, say so and ask for input.

## Environment

- Android SDK: `~/android-sdk`
- Package name: `net.xmppwocky.earbs`

## Core Concepts

- **Card**: A `(chord_type, octave, playback_mode)` tuple. User answers chord type only.
- **Octaves**: 3, 4, 5 (octave 4 is starting point)
- **Review session**: 20 cards, 1 trial each, shuffled randomly

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
| Dim7   | 0, 3, 6, 9     |

## Per-Trial FSRS Grading

Each trial immediately updates FSRS for that card:
- **Correct** → Good rating (interval extends)
- **Wrong** → Again rating (card becomes due soon)

## Card Selection Algorithm

1. Get due cards (next_review ≤ now)
2. If ≥20 due, take the 20 most overdue
3. If <20 due, pad with non-due cards (reviewing early)
4. Shuffle randomly
5. Run session with those 20 cards

## Starting Deck & Unlock Order

Cards unlock in groups of 4:
1. Major, Minor, Sus2, Sus4 @ octave 4, arpeggiated *(starting deck)*
2. Major, Minor, Sus2, Sus4 @ octave 4, block
3. Major, Minor, Sus2, Sus4 @ octave 3, arpeggiated
4. (continues expanding octaves and modes, then 7th chords)

Total: 48 cards (8 types × 3 octaves × 2 modes)

## Audio

- Square wave synthesis via AudioTrack
- Block (simultaneous) or arpeggiated playback modes (per-card property)
- Root note randomized within octave (prevents memorizing absolute pitches)
- Reference: A4 = 440Hz, frequency = `440 * 2^(semitones_from_A4 / 12)`
