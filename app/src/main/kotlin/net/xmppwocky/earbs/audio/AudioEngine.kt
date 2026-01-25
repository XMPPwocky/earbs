package net.xmppwocky.earbs.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.sign

private const val TAG = "AudioEngine"
private const val SAMPLE_RATE = 44100

enum class PlaybackMode {
    BLOCK,
    ARPEGGIATED
}

object AudioEngine {

    /**
     * PolyBLEP correction for band-limited waveform generation.
     * Reduces aliasing by smoothing discontinuities in square waves.
     *
     * @param t Current phase position (0 to 1)
     * @param dt Phase increment per sample (frequency / sampleRate)
     * @return Correction value to add to the naive waveform
     */
    private fun polyBlep(t: Float, dt: Float): Float {
        return when {
            t < dt -> {
                val x = t / dt
                x + x - x * x - 1f
            }
            t > 1f - dt -> {
                val x = (t - 1f) / dt
                x * x + x + x + 1f
            }
            else -> 0f
        }
    }

    /**
     * Calculate how many samples each note in an arpeggiated chord should get.
     * Ensures total samples are preserved exactly (no truncation from integer division).
     *
     * @param totalDurationMs Total duration for the arpeggio in milliseconds
     * @param noteCount Number of notes in the chord
     * @param sampleRate Sample rate in Hz (default 44100)
     * @return IntArray with sample count for each note (remainder goes to last note)
     */
    fun calculateArpeggioSampleDistribution(
        totalDurationMs: Int,
        noteCount: Int,
        sampleRate: Int = SAMPLE_RATE
    ): IntArray {
        // Calculate total samples directly from duration to avoid truncation
        val totalSamples = (sampleRate * totalDurationMs / 1000.0).toInt()
        val samplesPerNote = totalSamples / noteCount
        val remainderSamples = totalSamples % noteCount

        return IntArray(noteCount) { index ->
            if (index == noteCount - 1) {
                samplesPerNote + remainderSamples  // Last note gets remainder
            } else {
                samplesPerNote
            }
        }
    }

    /**
     * Generate a square wave for a given frequency and duration.
     */
    fun generateSquareWave(frequency: Float, durationMs: Int, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        generateSquareWaveInto(samples, 0, numSamples, frequency, sampleRate)
        return samples
    }

    /**
     * Generate a band-limited square wave directly into a pre-allocated buffer.
     * Uses PolyBLEP to reduce aliasing artifacts at high frequencies.
     *
     * @param buffer The buffer to write samples into
     * @param offset Starting index in the buffer
     * @param numSamples Number of samples to generate
     * @param frequency Frequency in Hz
     * @param sampleRate Sample rate in Hz (default 44100)
     */
    fun generateSquareWaveInto(
        buffer: ShortArray,
        offset: Int,
        numSamples: Int,
        frequency: Float,
        sampleRate: Int = SAMPLE_RATE
    ) {
        val dt = frequency / sampleRate  // Phase increment per sample
        val amplitude = 8000f  // Keep low to allow mixing without clipping
        var phase = 0f

        for (i in 0 until numSamples) {
            // Naive square wave: +1 for first half, -1 for second half
            var value = if (phase < 0.5f) 1f else -1f

            // Apply PolyBLEP correction at discontinuities
            value += polyBlep(phase, dt)                    // Rising edge at phase 0
            value -= polyBlep((phase + 0.5f) % 1f, dt)      // Falling edge at phase 0.5

            buffer[offset + i] = (value * amplitude).toInt().toShort()

            // Advance phase
            phase += dt
            if (phase >= 1f) phase -= 1f
        }
    }

    /**
     * Mix multiple waves together by averaging samples to prevent clipping.
     */
    fun mixWaves(waves: List<ShortArray>): ShortArray {
        if (waves.isEmpty()) return ShortArray(0)

        val maxLength = waves.maxOf { it.size }
        val mixed = ShortArray(maxLength)

        for (i in 0 until maxLength) {
            var sum = 0
            var count = 0
            for (wave in waves) {
                if (i < wave.size) {
                    sum += wave[i]
                    count++
                }
            }
            // Average to prevent clipping
            mixed[i] = if (count > 0) (sum / count).toShort() else 0
        }

        return mixed
    }

    /**
     * Play a chord with the given frequencies.
     *
     * @param frequencies List of frequencies in Hz to play
     * @param mode BLOCK plays all notes simultaneously, ARPEGGIATED plays them sequentially
     * @param durationMs Duration of each note (or total chord in BLOCK mode)
     * @param chordType For logging purposes
     * @param rootSemitones For logging purposes
     */
    suspend fun playChord(
        frequencies: List<Float>,
        mode: PlaybackMode,
        durationMs: Int = 500,
        chordType: String = "unknown",
        rootSemitones: Int = 0
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // Log synthesis details before playing
        Log.i(TAG, "=== CHORD SYNTHESIS ===")
        Log.i(TAG, "Timestamp: $timestamp")
        Log.i(TAG, "Chord type: $chordType")
        Log.i(TAG, "Root semitones from A4: $rootSemitones")
        Log.i(TAG, "Frequencies: ${frequencies.map { "%.2f Hz".format(it) }}")
        Log.i(TAG, "Mode: $mode")
        Log.i(TAG, "Duration: ${durationMs}ms")

        when (mode) {
            PlaybackMode.BLOCK -> playBlockChord(frequencies, durationMs)
            PlaybackMode.ARPEGGIATED -> playArpeggiatedChord(frequencies, durationMs)
        }

        Log.i(TAG, "Playback complete at ${System.currentTimeMillis()}")
        Log.i(TAG, "======================")
    }

    /**
     * Play a chord pair: reference (tonic) chord followed by target chord.
     * Used for chord function game.
     *
     * @param referenceFreqs Frequencies for the reference/tonic chord
     * @param targetFreqs Frequencies for the target chord
     * @param mode BLOCK plays notes simultaneously, ARPEGGIATED plays sequentially
     * @param durationMs Duration of each chord
     * @param pauseMs Pause between reference and target chords
     * @param keyQuality For logging purposes (MAJOR or MINOR)
     * @param function For logging purposes (the function being played)
     * @param rootSemitones For logging purposes
     */
    suspend fun playChordPair(
        referenceFreqs: List<Float>,
        targetFreqs: List<Float>,
        mode: PlaybackMode,
        durationMs: Int = 500,
        pauseMs: Int = 300,
        keyQuality: String = "unknown",
        function: String = "unknown",
        rootSemitones: Int = 0
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // Log synthesis details
        Log.i(TAG, "=== CHORD PAIR SYNTHESIS ===")
        Log.i(TAG, "Timestamp: $timestamp")
        Log.i(TAG, "Key quality: $keyQuality")
        Log.i(TAG, "Function: $function")
        Log.i(TAG, "Root semitones from A4: $rootSemitones")
        Log.i(TAG, "Reference frequencies: ${referenceFreqs.map { "%.2f Hz".format(it) }}")
        Log.i(TAG, "Target frequencies: ${targetFreqs.map { "%.2f Hz".format(it) }}")
        Log.i(TAG, "Mode: $mode")
        Log.i(TAG, "Duration: ${durationMs}ms, Pause: ${pauseMs}ms")

        // Play reference chord
        Log.i(TAG, "Playing reference chord...")
        when (mode) {
            PlaybackMode.BLOCK -> playBlockChord(referenceFreqs, durationMs)
            PlaybackMode.ARPEGGIATED -> playArpeggiatedChord(referenceFreqs, durationMs)
        }

        // Pause between chords
        Log.i(TAG, "Pausing for ${pauseMs}ms...")
        Thread.sleep(pauseMs.toLong())

        // Play target chord
        Log.i(TAG, "Playing target chord...")
        when (mode) {
            PlaybackMode.BLOCK -> playBlockChord(targetFreqs, durationMs)
            PlaybackMode.ARPEGGIATED -> playArpeggiatedChord(targetFreqs, durationMs)
        }

        Log.i(TAG, "Chord pair playback complete at ${System.currentTimeMillis()}")
        Log.i(TAG, "============================")
    }

    /**
     * Play a chord progression: a sequence of chords with pauses between them.
     * Used for the chord progression recognition game.
     *
     * @param chords List of chords, each chord being a list of frequencies
     * @param mode BLOCK plays notes simultaneously, ARPEGGIATED plays sequentially
     * @param chordDurationMs Duration of each chord
     * @param pauseMs Pause between chords
     * @param progressionName For logging purposes
     * @param keyQuality For logging purposes
     * @param rootSemitones For logging purposes
     */
    suspend fun playProgression(
        chords: List<List<Float>>,
        mode: PlaybackMode,
        chordDurationMs: Int = 400,
        pauseMs: Int = 200,
        progressionName: String = "unknown",
        keyQuality: String = "unknown",
        rootSemitones: Int = 0
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // Log synthesis details
        Log.i(TAG, "=== PROGRESSION SYNTHESIS ===")
        Log.i(TAG, "Timestamp: $timestamp")
        Log.i(TAG, "Progression: $progressionName")
        Log.i(TAG, "Key quality: $keyQuality")
        Log.i(TAG, "Root semitones from A4: $rootSemitones")
        Log.i(TAG, "Mode: $mode")
        Log.i(TAG, "Chord duration: ${chordDurationMs}ms, Pause: ${pauseMs}ms")
        Log.i(TAG, "Number of chords: ${chords.size}")
        chords.forEachIndexed { index, freqs ->
            Log.i(TAG, "  Chord $index: ${freqs.map { "%.2f Hz".format(it) }}")
        }

        // Play each chord in sequence
        chords.forEachIndexed { index, frequencies ->
            Log.i(TAG, "Playing chord ${index + 1}/${chords.size}...")
            when (mode) {
                PlaybackMode.BLOCK -> playBlockChord(frequencies, chordDurationMs)
                PlaybackMode.ARPEGGIATED -> playArpeggiatedChord(frequencies, chordDurationMs)
            }

            // Pause between chords (but not after the last one)
            if (index < chords.size - 1) {
                Log.i(TAG, "Pausing for ${pauseMs}ms...")
                Thread.sleep(pauseMs.toLong())
            }
        }

        Log.i(TAG, "Progression playback complete at ${System.currentTimeMillis()}")
        Log.i(TAG, "=============================")
    }

    private fun playBlockChord(frequencies: List<Float>, durationMs: Int) {
        Log.d(TAG, "Playing block chord with ${frequencies.size} notes")

        // Generate waves for all frequencies
        val waves = frequencies.map { freq ->
            generateSquareWave(freq, durationMs)
        }

        // Mix them together
        val mixed = mixWaves(waves)

        // Play the mixed audio
        playAudio(mixed)
    }

    private fun playArpeggiatedChord(frequencies: List<Float>, totalDurationMs: Int) {
        // Calculate sample distribution with no truncation
        val sampleDistribution = calculateArpeggioSampleDistribution(totalDurationMs, frequencies.size)
        val totalSamples = sampleDistribution.sum()

        Log.d(TAG, "Playing arpeggiated chord: ${frequencies.size} notes, " +
                "samples per note: ${sampleDistribution.toList()}, total: $totalSamples")

        // Pre-allocate the entire buffer (zero allocations during generation)
        val allSamples = ShortArray(totalSamples)

        // Generate each note directly into the buffer
        var offset = 0
        for (i in frequencies.indices) {
            generateSquareWaveInto(allSamples, offset, sampleDistribution[i], frequencies[i])
            offset += sampleDistribution[i]
        }

        playAudio(allSamples)
    }

    private fun playAudio(samples: ShortArray) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        // Use CountDownLatch to wait for actual playback completion
        val completionLatch = CountDownLatch(1)

        try {
            audioTrack.write(samples, 0, samples.size)

            // Set marker at last sample position to detect playback end
            audioTrack.notificationMarkerPosition = samples.size
            audioTrack.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack?) {
                        Log.d(TAG, "Playback marker reached at sample ${samples.size}")
                        completionLatch.countDown()
                    }
                    override fun onPeriodicNotification(track: AudioTrack?) {}
                },
                Handler(Looper.getMainLooper())
            )

            audioTrack.play()

            // Wait for marker callback, with timeout as fallback
            val expectedDurationMs = (samples.size * 1000L) / SAMPLE_RATE
            val timeoutMs = expectedDurationMs + 500  // 500ms safety margin
            val completed = completionLatch.await(timeoutMs, TimeUnit.MILLISECONDS)

            if (!completed) {
                Log.w(TAG, "Playback completion timeout after ${timeoutMs}ms, proceeding anyway")
            }
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }
}
