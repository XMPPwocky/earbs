package net.xmppwocky.earbs.audio

/**
 * Direction of interval playback.
 */
enum class IntervalDirection(val displayName: String) {
    ASCENDING("Ascending"),    // Sequential: root first, then interval note (going up)
    DESCENDING("Descending"),  // Sequential: interval note first, then root (going down)
    HARMONIC("Harmonic");      // Simultaneous: both notes together
}
