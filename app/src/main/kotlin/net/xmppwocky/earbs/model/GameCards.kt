package net.xmppwocky.earbs.model

import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.FunctionCardWithFsrs
import net.xmppwocky.earbs.data.db.IntervalCardWithFsrs
import net.xmppwocky.earbs.data.db.ProgressionCardWithFsrs
import net.xmppwocky.earbs.data.db.ScaleCardWithFsrs
import net.xmppwocky.earbs.data.entity.GameType

/**
 * Sealed class representing card data for a specific game type.
 * Using a sealed class ensures exhaustive handling when adding new game types.
 *
 * When a new game type is added, the compiler will require a new sealed class variant,
 * and any `when` expression on GameCards will fail to compile until the new case is handled.
 */
sealed class GameCards {
    abstract val gameType: GameType

    data class ChordType(
        val active: List<CardWithFsrs>,
        val deprecated: List<CardWithFsrs>
    ) : GameCards() {
        override val gameType = GameType.CHORD_TYPE
    }

    data class Function(
        val active: List<FunctionCardWithFsrs>,
        val deprecated: List<FunctionCardWithFsrs>
    ) : GameCards() {
        override val gameType = GameType.CHORD_FUNCTION
    }

    data class Progression(
        val active: List<ProgressionCardWithFsrs>,
        val deprecated: List<ProgressionCardWithFsrs>
    ) : GameCards() {
        override val gameType = GameType.CHORD_PROGRESSION
    }

    data class Interval(
        val active: List<IntervalCardWithFsrs>,
        val deprecated: List<IntervalCardWithFsrs>
    ) : GameCards() {
        override val gameType = GameType.INTERVAL
    }

    data class Scale(
        val active: List<ScaleCardWithFsrs>,
        val deprecated: List<ScaleCardWithFsrs>
    ) : GameCards() {
        override val gameType = GameType.SCALE
    }

    /** Number of active (non-deprecated) cards. Exhaustive when ensures compile error if new type added. */
    val activeCount: Int get() = when (this) {
        is ChordType -> active.size
        is Function -> active.size
        is Progression -> active.size
        is Interval -> active.size
        is Scale -> active.size
    }

    /** Number of deprecated cards. Exhaustive when ensures compile error if new type added. */
    val deprecatedCount: Int get() = when (this) {
        is ChordType -> deprecated.size
        is Function -> deprecated.size
        is Progression -> deprecated.size
        is Interval -> deprecated.size
        is Scale -> deprecated.size
    }
}
