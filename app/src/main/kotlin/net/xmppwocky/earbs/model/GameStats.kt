package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.data.entity.GameType

/**
 * Stats for a single game type. Used by HomeScreen to display due/unlocked counts.
 *
 * Using this data class with a list of all game types ensures exhaustive handling
 * when iterating or mapping over the stats.
 */
data class GameStats(
    val gameType: GameType,
    val dueCount: Int,
    val unlockedCount: Int,
    val totalCards: Int
)
