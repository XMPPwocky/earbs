package net.xmppwocky.earbs.audio

/**
 * Direction for scale playback.
 */
enum class ScaleDirection(val displayName: String) {
    ASCENDING("Ascending"),   // Root to octave
    DESCENDING("Descending"), // Octave to root
    BOTH("Both");             // Ascending then descending
}
