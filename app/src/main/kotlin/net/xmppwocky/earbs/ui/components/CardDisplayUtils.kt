package net.xmppwocky.earbs.ui.components

/**
 * Utility functions for displaying card information.
 * Reusable by ResultsScreen, HistoryScreen, and other UI components.
 */

/**
 * Format a card ID into a human-readable display name.
 *
 * Chord type cards: "MAJOR_4_ARPEGGIATED" -> "Major @ Oct 4 (arp)"
 * Function cards: "IV_MAJOR_4_ARPEGGIATED" -> "IV (major) @ Oct 4 (arp)"
 *
 * @param cardId The card ID string (e.g., "MAJOR_4_ARPEGGIATED" or "IV_MAJOR_4_ARPEGGIATED")
 * @param gameType The game type ("CHORD_TYPE" or "CHORD_FUNCTION")
 * @return A human-readable display name
 */
fun formatCardId(cardId: String, gameType: String): String {
    val parts = cardId.split("_")

    return if (gameType == "CHORD_FUNCTION" && parts.size >= 4) {
        // Function card: "IV_MAJOR_4_ARPEGGIATED" -> "IV (major) @ Oct 4 (arp)"
        val function = parts[0]
        val keyQuality = parts[1].lowercase()
        val octave = parts[2]
        val mode = formatPlaybackMode(parts[3])
        "$function ($keyQuality) @ Oct $octave ($mode)"
    } else if (parts.size >= 3) {
        // Chord type card: "MAJOR_4_ARPEGGIATED" -> "Major @ Oct 4 (arp)"
        val chordType = formatChordType(parts[0])
        val octave = parts[1]
        val mode = formatPlaybackMode(parts[2])
        "$chordType @ Oct $octave ($mode)"
    } else {
        // Fallback: return as-is
        cardId
    }
}

/**
 * Format a chord type name for display.
 * "MAJOR" -> "Major", "DOM7" -> "Dom7", "MIN7" -> "Min7"
 */
private fun formatChordType(chordType: String): String {
    return when (chordType.uppercase()) {
        "DOM7" -> "Dom7"
        "MAJ7" -> "Maj7"
        "MIN7" -> "Min7"
        "DIM7" -> "Dim7"
        "SUS2" -> "Sus2"
        "SUS4" -> "Sus4"
        else -> chordType.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * Format a playback mode name for display.
 * "ARPEGGIATED" -> "arp", "BLOCK" -> "blk"
 */
private fun formatPlaybackMode(mode: String): String {
    return when (mode.uppercase()) {
        "ARPEGGIATED" -> "arp"
        "BLOCK" -> "blk"
        else -> mode.take(3).lowercase()
    }
}
