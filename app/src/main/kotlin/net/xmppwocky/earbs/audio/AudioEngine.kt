package net.xmppwocky.earbs.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sign

private const val TAG = "AudioEngine"
private const val SAMPLE_RATE = 44100

enum class PlaybackMode {
    BLOCK,
    ARPEGGIATED
}

object AudioEngine {

    /**
     * Generate a square wave for a given frequency and duration.
     */
    fun generateSquareWave(frequency: Float, durationMs: Int, sampleRate: Int = SAMPLE_RATE): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000.0).toInt()
        val samples = ShortArray(numSamples)
        val samplesPerCycle = sampleRate / frequency
        val amplitude = 8000 // Keep low to allow mixing without clipping

        for (i in 0 until numSamples) {
            val cyclePosition = (i % samplesPerCycle) / samplesPerCycle
            // Square wave: positive for first half, negative for second half
            val value = if (cyclePosition < 0.5f) amplitude else -amplitude
            samples[i] = value.toShort()
        }

        return samples
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

    private fun playArpeggiatedChord(frequencies: List<Float>, noteDurationMs: Int) {
        val arpDuration = noteDurationMs / frequencies.size
        Log.d(TAG, "Playing arpeggiated chord: ${frequencies.size} notes @ ${arpDuration}ms each")

        // Generate and concatenate waves sequentially
        val allSamples = mutableListOf<Short>()
        for (freq in frequencies) {
            val wave = generateSquareWave(freq, arpDuration)
            allSamples.addAll(wave.toList())
        }

        playAudio(allSamples.toShortArray())
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

        try {
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Wait for playback to complete
            val durationMs = (samples.size * 1000L) / SAMPLE_RATE
            Thread.sleep(durationMs + 50) // Add small buffer for completion
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }
}
