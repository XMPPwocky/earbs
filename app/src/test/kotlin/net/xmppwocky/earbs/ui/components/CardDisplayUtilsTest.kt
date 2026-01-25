package net.xmppwocky.earbs.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CardDisplayUtilsTest {

    // ========== Chord Type Card Formatting Tests ==========

    @Test
    fun `formatCardId formats Major chord correctly`() {
        val result = formatCardId("MAJOR_4_ARPEGGIATED", "CHORD_TYPE")
        assertEquals("Major @ Oct 4 (arp)", result)
    }

    @Test
    fun `formatCardId formats Minor chord correctly`() {
        val result = formatCardId("MINOR_4_BLOCK", "CHORD_TYPE")
        assertEquals("Minor @ Oct 4 (blk)", result)
    }

    @Test
    fun `formatCardId formats Sus2 chord correctly`() {
        val result = formatCardId("SUS2_3_ARPEGGIATED", "CHORD_TYPE")
        assertEquals("Sus2 @ Oct 3 (arp)", result)
    }

    @Test
    fun `formatCardId formats Sus4 chord correctly`() {
        val result = formatCardId("SUS4_5_BLOCK", "CHORD_TYPE")
        assertEquals("Sus4 @ Oct 5 (blk)", result)
    }

    @Test
    fun `formatCardId formats Dom7 chord correctly`() {
        val result = formatCardId("DOM7_4_ARPEGGIATED", "CHORD_TYPE")
        assertEquals("Dom7 @ Oct 4 (arp)", result)
    }

    @Test
    fun `formatCardId formats Maj7 chord correctly`() {
        val result = formatCardId("MAJ7_4_BLOCK", "CHORD_TYPE")
        assertEquals("Maj7 @ Oct 4 (blk)", result)
    }

    @Test
    fun `formatCardId formats Min7 chord correctly`() {
        val result = formatCardId("MIN7_4_ARPEGGIATED", "CHORD_TYPE")
        assertEquals("Min7 @ Oct 4 (arp)", result)
    }

    @Test
    fun `formatCardId formats Dim7 chord correctly`() {
        val result = formatCardId("DIM7_4_BLOCK", "CHORD_TYPE")
        assertEquals("Dim7 @ Oct 4 (blk)", result)
    }

    // ========== Function Card Formatting Tests ==========

    @Test
    fun `formatCardId formats V function in major key correctly`() {
        val result = formatCardId("V_MAJOR_4_ARPEGGIATED", "CHORD_FUNCTION")
        assertEquals("V (major) @ Oct 4 (arp)", result)
    }

    @Test
    fun `formatCardId formats IV function in minor key correctly`() {
        val result = formatCardId("IV_MINOR_4_BLOCK", "CHORD_FUNCTION")
        assertEquals("IV (minor) @ Oct 4 (blk)", result)
    }

    @Test
    fun `formatCardId formats vi function correctly`() {
        val result = formatCardId("vi_MAJOR_3_ARPEGGIATED", "CHORD_FUNCTION")
        assertEquals("vi (major) @ Oct 3 (arp)", result)
    }

    // ========== Edge Cases ==========

    @Test
    fun `formatCardId returns original for malformed chord type card`() {
        val result = formatCardId("INVALID", "CHORD_TYPE")
        assertEquals("INVALID", result)
    }

    @Test
    fun `formatCardId returns original for malformed function card`() {
        val result = formatCardId("INVALID", "CHORD_FUNCTION")
        assertEquals("INVALID", result)
    }

    @Test
    fun `formatCardId handles two-part cardId`() {
        val result = formatCardId("MAJOR_4", "CHORD_TYPE")
        assertEquals("MAJOR_4", result)
    }
}
