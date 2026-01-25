package net.xmppwocky.earbs.model

import org.junit.Assert.*
import org.junit.Test

class ChordFunctionTest {

    // ========== Major key semitone offset tests ==========

    @Test
    fun `ii has semitone offset 2`() {
        assertEquals(2, ChordFunction.ii.semitoneOffset)
    }

    @Test
    fun `iii has semitone offset 4`() {
        assertEquals(4, ChordFunction.iii.semitoneOffset)
    }

    @Test
    fun `IV has semitone offset 5`() {
        assertEquals(5, ChordFunction.IV.semitoneOffset)
    }

    @Test
    fun `V has semitone offset 7`() {
        assertEquals(7, ChordFunction.V.semitoneOffset)
    }

    @Test
    fun `vi has semitone offset 9`() {
        assertEquals(9, ChordFunction.vi.semitoneOffset)
    }

    @Test
    fun `vii_dim has semitone offset 11`() {
        assertEquals(11, ChordFunction.vii_dim.semitoneOffset)
    }

    // ========== Minor key semitone offset tests ==========

    @Test
    fun `ii_dim has semitone offset 2`() {
        assertEquals(2, ChordFunction.ii_dim.semitoneOffset)
    }

    @Test
    fun `III has semitone offset 3`() {
        assertEquals(3, ChordFunction.III.semitoneOffset)
    }

    @Test
    fun `iv has semitone offset 5`() {
        assertEquals(5, ChordFunction.iv.semitoneOffset)
    }

    @Test
    fun `v has semitone offset 7`() {
        assertEquals(7, ChordFunction.v.semitoneOffset)
    }

    @Test
    fun `VI has semitone offset 8`() {
        assertEquals(8, ChordFunction.VI.semitoneOffset)
    }

    @Test
    fun `VII has semitone offset 10`() {
        assertEquals(10, ChordFunction.VII.semitoneOffset)
    }

    // ========== Major key chord quality tests ==========

    @Test
    fun `ii has minor quality`() {
        assertEquals(ChordQuality.MINOR, ChordFunction.ii.quality)
    }

    @Test
    fun `iii has minor quality`() {
        assertEquals(ChordQuality.MINOR, ChordFunction.iii.quality)
    }

    @Test
    fun `IV has major quality`() {
        assertEquals(ChordQuality.MAJOR, ChordFunction.IV.quality)
    }

    @Test
    fun `V has major quality`() {
        assertEquals(ChordQuality.MAJOR, ChordFunction.V.quality)
    }

    @Test
    fun `vi has minor quality`() {
        assertEquals(ChordQuality.MINOR, ChordFunction.vi.quality)
    }

    @Test
    fun `vii_dim has diminished quality`() {
        assertEquals(ChordQuality.DIMINISHED, ChordFunction.vii_dim.quality)
    }

    // ========== Minor key chord quality tests ==========

    @Test
    fun `ii_dim has diminished quality`() {
        assertEquals(ChordQuality.DIMINISHED, ChordFunction.ii_dim.quality)
    }

    @Test
    fun `III has major quality`() {
        assertEquals(ChordQuality.MAJOR, ChordFunction.III.quality)
    }

    @Test
    fun `iv has minor quality`() {
        assertEquals(ChordQuality.MINOR, ChordFunction.iv.quality)
    }

    @Test
    fun `v has minor quality`() {
        assertEquals(ChordQuality.MINOR, ChordFunction.v.quality)
    }

    @Test
    fun `VI has major quality`() {
        assertEquals(ChordQuality.MAJOR, ChordFunction.VI.quality)
    }

    @Test
    fun `VII has major quality`() {
        assertEquals(ChordQuality.MAJOR, ChordFunction.VII.quality)
    }

    // ========== Key quality filtering tests ==========

    @Test
    fun `forKeyQuality MAJOR returns 6 functions`() {
        val majorFunctions = ChordFunction.forKeyQuality(KeyQuality.MAJOR)
        assertEquals(6, majorFunctions.size)
    }

    @Test
    fun `forKeyQuality MINOR returns 6 functions`() {
        val minorFunctions = ChordFunction.forKeyQuality(KeyQuality.MINOR)
        assertEquals(6, minorFunctions.size)
    }

    @Test
    fun `forKeyQuality MAJOR returns correct functions`() {
        val majorFunctions = ChordFunction.forKeyQuality(KeyQuality.MAJOR)
        assertTrue(majorFunctions.contains(ChordFunction.ii))
        assertTrue(majorFunctions.contains(ChordFunction.iii))
        assertTrue(majorFunctions.contains(ChordFunction.IV))
        assertTrue(majorFunctions.contains(ChordFunction.V))
        assertTrue(majorFunctions.contains(ChordFunction.vi))
        assertTrue(majorFunctions.contains(ChordFunction.vii_dim))
    }

    @Test
    fun `forKeyQuality MINOR returns correct functions`() {
        val minorFunctions = ChordFunction.forKeyQuality(KeyQuality.MINOR)
        assertTrue(minorFunctions.contains(ChordFunction.ii_dim))
        assertTrue(minorFunctions.contains(ChordFunction.III))
        assertTrue(minorFunctions.contains(ChordFunction.iv))
        assertTrue(minorFunctions.contains(ChordFunction.v))
        assertTrue(minorFunctions.contains(ChordFunction.VI))
        assertTrue(minorFunctions.contains(ChordFunction.VII))
    }

    @Test
    fun `MAJOR_FUNCTIONS matches forKeyQuality MAJOR`() {
        assertEquals(
            ChordFunction.forKeyQuality(KeyQuality.MAJOR).toSet(),
            ChordFunction.MAJOR_FUNCTIONS.toSet()
        )
    }

    @Test
    fun `MINOR_FUNCTIONS matches forKeyQuality MINOR`() {
        assertEquals(
            ChordFunction.forKeyQuality(KeyQuality.MINOR).toSet(),
            ChordFunction.MINOR_FUNCTIONS.toSet()
        )
    }

    // ========== Display name tests ==========

    @Test
    fun `ii display name is lowercase ii`() {
        assertEquals("ii", ChordFunction.ii.displayName)
    }

    @Test
    fun `IV display name is uppercase IV`() {
        assertEquals("IV", ChordFunction.IV.displayName)
    }

    @Test
    fun `V display name is uppercase V`() {
        assertEquals("V", ChordFunction.V.displayName)
    }

    @Test
    fun `vi display name is lowercase vi`() {
        assertEquals("vi", ChordFunction.vi.displayName)
    }

    @Test
    fun `vii_dim display name has degree symbol`() {
        assertEquals("vii\u00B0", ChordFunction.vii_dim.displayName)
    }

    @Test
    fun `ii_dim display name has degree symbol`() {
        assertEquals("ii\u00B0", ChordFunction.ii_dim.displayName)
    }

    @Test
    fun `III display name is uppercase III`() {
        assertEquals("III", ChordFunction.III.displayName)
    }

    @Test
    fun `VII display name is uppercase VII`() {
        assertEquals("VII", ChordFunction.VII.displayName)
    }

    // ========== ChordQuality interval tests ==========

    @Test
    fun `MAJOR quality has intervals 0, 4, 7`() {
        assertEquals(listOf(0, 4, 7), ChordQuality.MAJOR.intervals)
    }

    @Test
    fun `MINOR quality has intervals 0, 3, 7`() {
        assertEquals(listOf(0, 3, 7), ChordQuality.MINOR.intervals)
    }

    @Test
    fun `DIMINISHED quality has intervals 0, 3, 6`() {
        assertEquals(listOf(0, 3, 6), ChordQuality.DIMINISHED.intervals)
    }

    // ========== Total functions test ==========

    @Test
    fun `total ChordFunction entries is 12`() {
        assertEquals(12, ChordFunction.entries.size)
    }

    @Test
    fun `each function has correct keyQuality property`() {
        ChordFunction.MAJOR_FUNCTIONS.forEach { function ->
            assertEquals(KeyQuality.MAJOR, function.keyQuality)
        }
        ChordFunction.MINOR_FUNCTIONS.forEach { function ->
            assertEquals(KeyQuality.MINOR, function.keyQuality)
        }
    }
}
