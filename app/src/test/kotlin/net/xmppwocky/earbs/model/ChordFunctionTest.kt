package net.xmppwocky.earbs.model

import org.junit.Assert.*
import org.junit.Test

class ChordFunctionTest {

    @Test
    fun `all functions have valid music theory properties`() {
        // Expected: (function, semitoneOffset, quality, keyQuality)
        val expected = listOf(
            // Major key functions
            Triple(ChordFunction.ii, 2, ChordQuality.MINOR) to KeyQuality.MAJOR,
            Triple(ChordFunction.iii, 4, ChordQuality.MINOR) to KeyQuality.MAJOR,
            Triple(ChordFunction.IV, 5, ChordQuality.MAJOR) to KeyQuality.MAJOR,
            Triple(ChordFunction.V, 7, ChordQuality.MAJOR) to KeyQuality.MAJOR,
            Triple(ChordFunction.vi, 9, ChordQuality.MINOR) to KeyQuality.MAJOR,
            Triple(ChordFunction.vii_dim, 11, ChordQuality.DIMINISHED) to KeyQuality.MAJOR,
            // Minor key functions
            Triple(ChordFunction.ii_dim, 2, ChordQuality.DIMINISHED) to KeyQuality.MINOR,
            Triple(ChordFunction.III, 3, ChordQuality.MAJOR) to KeyQuality.MINOR,
            Triple(ChordFunction.iv, 5, ChordQuality.MINOR) to KeyQuality.MINOR,
            Triple(ChordFunction.v, 7, ChordQuality.MINOR) to KeyQuality.MINOR,
            Triple(ChordFunction.VI, 8, ChordQuality.MAJOR) to KeyQuality.MINOR,
            Triple(ChordFunction.VII, 10, ChordQuality.MAJOR) to KeyQuality.MINOR,
        )

        expected.forEach { (props, keyQuality) ->
            val (function, semitone, quality) = props
            assertEquals("${function.name} semitone", semitone, function.semitoneOffset)
            assertEquals("${function.name} quality", quality, function.quality)
            assertEquals("${function.name} keyQuality", keyQuality, function.keyQuality)
        }
    }

    @Test
    fun `forKeyQuality returns correct functions for each key`() {
        val majorFunctions = ChordFunction.forKeyQuality(KeyQuality.MAJOR)
        assertEquals(6, majorFunctions.size)
        assertEquals(
            setOf(ChordFunction.ii, ChordFunction.iii, ChordFunction.IV,
                  ChordFunction.V, ChordFunction.vi, ChordFunction.vii_dim),
            majorFunctions.toSet()
        )
        assertEquals(ChordFunction.MAJOR_FUNCTIONS.toSet(), majorFunctions.toSet())

        val minorFunctions = ChordFunction.forKeyQuality(KeyQuality.MINOR)
        assertEquals(6, minorFunctions.size)
        assertEquals(
            setOf(ChordFunction.ii_dim, ChordFunction.III, ChordFunction.iv,
                  ChordFunction.v, ChordFunction.VI, ChordFunction.VII),
            minorFunctions.toSet()
        )
        assertEquals(ChordFunction.MINOR_FUNCTIONS.toSet(), minorFunctions.toSet())
    }

    @Test
    fun `ChordQuality intervals are correct`() {
        assertEquals(listOf(0, 4, 7), ChordQuality.MAJOR.intervals)
        assertEquals(listOf(0, 3, 7), ChordQuality.MINOR.intervals)
        assertEquals(listOf(0, 3, 6), ChordQuality.DIMINISHED.intervals)
    }

    @Test
    fun `total ChordFunction entries is 12`() {
        assertEquals(12, ChordFunction.entries.size)
    }

    @Test
    fun `diminished functions have degree symbol in display name`() {
        assertTrue(ChordFunction.vii_dim.displayName.contains("\u00B0"))
        assertTrue(ChordFunction.ii_dim.displayName.contains("\u00B0"))
    }

    @Test
    fun `major and minor case convention in display names`() {
        // Major quality functions use uppercase Roman numerals
        ChordFunction.entries.filter { it.quality == ChordQuality.MAJOR }.forEach { function ->
            assertTrue("${function.name} should be uppercase",
                function.displayName.replace("\u00B0", "").all { it.isUpperCase() || !it.isLetter() })
        }
        // Minor/diminished quality functions use lowercase Roman numerals
        ChordFunction.entries.filter { it.quality != ChordQuality.MAJOR }.forEach { function ->
            assertTrue("${function.name} should be lowercase",
                function.displayName.replace("\u00B0", "").all { it.isLowerCase() || !it.isLetter() })
        }
    }
}
