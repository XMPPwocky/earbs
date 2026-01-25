package net.xmppwocky.earbs.audio

import org.junit.Assert.*
import org.junit.Test

class AudioEngineTest {

    companion object {
        private const val TEST_SAMPLE_RATE = 44100
        private const val AMPLITUDE = 8000
    }

    // ========== Square wave length tests ==========

    @Test
    fun `generateSquareWave output length matches expected samples`() {
        val durationMs = 100
        val samples = AudioEngine.generateSquareWave(440f, durationMs, TEST_SAMPLE_RATE)

        val expectedLength = (TEST_SAMPLE_RATE * durationMs / 1000.0).toInt()
        assertEquals(expectedLength, samples.size)
    }

    @Test
    fun `generateSquareWave 1 second produces sampleRate samples`() {
        val samples = AudioEngine.generateSquareWave(440f, 1000, TEST_SAMPLE_RATE)
        assertEquals(TEST_SAMPLE_RATE, samples.size)
    }

    @Test
    fun `generateSquareWave 500ms produces half sampleRate samples`() {
        val samples = AudioEngine.generateSquareWave(440f, 500, TEST_SAMPLE_RATE)
        assertEquals(TEST_SAMPLE_RATE / 2, samples.size)
    }

    @Test
    fun `generateSquareWave short duration works`() {
        val samples = AudioEngine.generateSquareWave(440f, 10, TEST_SAMPLE_RATE)
        assertEquals(441, samples.size) // 44100 * 10 / 1000
    }

    // ========== Square wave amplitude tests ==========

    @Test
    fun `generateSquareWave uses correct positive amplitude`() {
        val samples = AudioEngine.generateSquareWave(440f, 100, TEST_SAMPLE_RATE)
        val positiveMax = samples.maxOrNull()
        assertEquals(AMPLITUDE.toShort(), positiveMax)
    }

    @Test
    fun `generateSquareWave uses correct negative amplitude`() {
        val samples = AudioEngine.generateSquareWave(440f, 100, TEST_SAMPLE_RATE)
        val negativeMin = samples.minOrNull()
        assertEquals((-AMPLITUDE).toShort(), negativeMin)
    }

    // ========== Square wave shape tests ==========

    @Test
    fun `generateSquareWave has approximately equal positive and negative samples`() {
        // For a complete cycle, should have about 50% positive and 50% negative
        val samples = AudioEngine.generateSquareWave(440f, 1000, TEST_SAMPLE_RATE)

        val positiveCount = samples.count { it > 0 }
        val negativeCount = samples.count { it < 0 }

        // Allow some tolerance due to incomplete cycles
        val ratio = positiveCount.toFloat() / negativeCount.toFloat()
        assertTrue("Ratio should be close to 1.0, got $ratio", ratio in 0.95f..1.05f)
    }

    @Test
    fun `generateSquareWave transitions within a cycle`() {
        // At 440 Hz with 44100 sample rate, one cycle is about 100.23 samples
        // So within first 200 samples, we should see both positive and negative
        val samples = AudioEngine.generateSquareWave(440f, 100, TEST_SAMPLE_RATE)

        val first200 = samples.take(200)
        assertTrue(first200.any { it > 0 })
        assertTrue(first200.any { it < 0 })
    }

    // ========== Different frequency tests ==========

    @Test
    fun `generateSquareWave works at different frequencies`() {
        val lowFreq = AudioEngine.generateSquareWave(100f, 100, TEST_SAMPLE_RATE)
        val midFreq = AudioEngine.generateSquareWave(440f, 100, TEST_SAMPLE_RATE)
        val highFreq = AudioEngine.generateSquareWave(2000f, 100, TEST_SAMPLE_RATE)

        // All should have same length
        assertEquals(lowFreq.size, midFreq.size)
        assertEquals(midFreq.size, highFreq.size)

        // All should have same amplitude range
        assertEquals(AMPLITUDE.toShort(), lowFreq.maxOrNull())
        assertEquals(AMPLITUDE.toShort(), midFreq.maxOrNull())
        assertEquals(AMPLITUDE.toShort(), highFreq.maxOrNull())
    }

    // ========== Wave mixing tests ==========

    @Test
    fun `mixWaves single wave returns unchanged`() {
        val wave = shortArrayOf(100, 200, 300, 400)
        val mixed = AudioEngine.mixWaves(listOf(wave))

        assertArrayEquals(wave, mixed)
    }

    @Test
    fun `mixWaves empty list returns empty array`() {
        val mixed = AudioEngine.mixWaves(emptyList())
        assertEquals(0, mixed.size)
    }

    @Test
    fun `mixWaves two waves averaged correctly`() {
        val wave1 = shortArrayOf(100, 200, 300)
        val wave2 = shortArrayOf(200, 400, 600)
        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2))

        assertEquals(3, mixed.size)
        assertEquals(150.toShort(), mixed[0]) // (100 + 200) / 2
        assertEquals(300.toShort(), mixed[1]) // (200 + 400) / 2
        assertEquals(450.toShort(), mixed[2]) // (300 + 600) / 2
    }

    @Test
    fun `mixWaves three waves averaged correctly`() {
        val wave1 = shortArrayOf(300, 300, 300)
        val wave2 = shortArrayOf(300, 300, 300)
        val wave3 = shortArrayOf(300, 300, 300)
        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2, wave3))

        assertEquals(3, mixed.size)
        mixed.forEach { assertEquals(300.toShort(), it) }
    }

    @Test
    fun `mixWaves handles negative values`() {
        val wave1 = shortArrayOf(-100, 100)
        val wave2 = shortArrayOf(100, -100)
        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2))

        assertEquals(0.toShort(), mixed[0]) // (-100 + 100) / 2
        assertEquals(0.toShort(), mixed[1]) // (100 + -100) / 2
    }

    @Test
    fun `mixWaves different length waves uses max length`() {
        val wave1 = shortArrayOf(100, 200, 300)
        val wave2 = shortArrayOf(100, 200, 300, 400, 500)
        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2))

        assertEquals(5, mixed.size) // Max length
    }

    @Test
    fun `mixWaves shorter wave treated as having zeros at end`() {
        val wave1 = shortArrayOf(100, 200)
        val wave2 = shortArrayOf(100, 200, 300, 400)
        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2))

        assertEquals(4, mixed.size)
        assertEquals(100.toShort(), mixed[0]) // (100 + 100) / 2
        assertEquals(200.toShort(), mixed[1]) // (200 + 200) / 2
        // After wave1 ends, only wave2 contributes (count=1)
        assertEquals(300.toShort(), mixed[2]) // 300 / 1
        assertEquals(400.toShort(), mixed[3]) // 400 / 1
    }

    @Test
    fun `mixWaves prevents clipping for large amplitudes`() {
        val wave1 = shortArrayOf(8000, 8000, 8000)
        val wave2 = shortArrayOf(8000, 8000, 8000)
        val wave3 = shortArrayOf(8000, 8000, 8000)
        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2, wave3))

        // Averaging 3 waves of 8000 should give 8000, not 24000
        assertEquals(8000.toShort(), mixed[0])
        assertTrue(mixed.all { it <= 8000 })
    }

    // ========== Arpeggio sample distribution tests ==========

    @Test
    fun `calculateArpeggioSampleDistribution preserves total samples for triads`() {
        val distribution = AudioEngine.calculateArpeggioSampleDistribution(500, 3)
        assertEquals(22050, distribution.sum())  // 44100 * 0.5
    }

    @Test
    fun `calculateArpeggioSampleDistribution preserves total samples for 7th chords`() {
        val distribution = AudioEngine.calculateArpeggioSampleDistribution(500, 4)
        assertEquals(22050, distribution.sum())
    }

    @Test
    fun `calculateArpeggioSampleDistribution gives remainder to last note`() {
        // 22094 samples (501ms) / 3 notes = 7364 each with 2 remainder → last gets 7366
        val dist = AudioEngine.calculateArpeggioSampleDistribution(501, 3)
        assertEquals(3, dist.size)
        assertTrue(dist.last() >= dist.first())
        val expected = (44100 * 501 / 1000.0).toInt()
        assertEquals(expected, dist.sum())
    }

    @Test
    fun `calculateArpeggioSampleDistribution handles single note`() {
        val distribution = AudioEngine.calculateArpeggioSampleDistribution(100, 1)
        assertEquals(1, distribution.size)
        assertEquals(4410, distribution[0])  // 44100 * 0.1
    }

    @Test
    fun `calculateArpeggioSampleDistribution handles various durations`() {
        // Test that no samples are lost for various durations
        for (durationMs in listOf(100, 250, 333, 500, 750, 1000)) {
            for (noteCount in 1..4) {
                val dist = AudioEngine.calculateArpeggioSampleDistribution(durationMs, noteCount)
                val expected = (44100 * durationMs / 1000.0).toInt()
                assertEquals(
                    "Failed for duration=$durationMs, notes=$noteCount",
                    expected,
                    dist.sum()
                )
            }
        }
    }

    // ========== Integration tests ==========

    @Test
    fun `mixing generated square waves produces valid output`() {
        val wave1 = AudioEngine.generateSquareWave(440f, 100, TEST_SAMPLE_RATE)
        val wave2 = AudioEngine.generateSquareWave(554.37f, 100, TEST_SAMPLE_RATE) // C#5
        val wave3 = AudioEngine.generateSquareWave(659.25f, 100, TEST_SAMPLE_RATE) // E5

        val mixed = AudioEngine.mixWaves(listOf(wave1, wave2, wave3))

        // Should have same length as individual waves
        assertEquals(wave1.size, mixed.size)

        // Should not clip (all values within short range)
        assertTrue(mixed.all { it >= Short.MIN_VALUE && it <= Short.MAX_VALUE })

        // Should have reduced amplitude due to averaging
        val maxMixed = mixed.maxOrNull() ?: 0
        assertTrue("Max mixed amplitude should be <= individual amplitude",
            maxMixed <= AMPLITUDE)
    }
}
