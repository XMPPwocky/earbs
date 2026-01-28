package net.xmppwocky.earbs.data.repository

import net.xmppwocky.earbs.audio.ChordType
import net.xmppwocky.earbs.audio.PlaybackMode
import net.xmppwocky.earbs.data.db.CardDao
import net.xmppwocky.earbs.data.db.CardWithFsrs
import net.xmppwocky.earbs.data.db.FunctionCardDao
import net.xmppwocky.earbs.data.db.FunctionCardWithFsrs
import net.xmppwocky.earbs.data.db.IntervalCardDao
import net.xmppwocky.earbs.data.db.IntervalCardWithFsrs
import net.xmppwocky.earbs.data.db.ProgressionCardDao
import net.xmppwocky.earbs.data.db.ProgressionCardWithFsrs
import net.xmppwocky.earbs.data.db.ScaleCardDao
import net.xmppwocky.earbs.data.db.ScaleCardWithFsrs
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.model.Card
import net.xmppwocky.earbs.model.ChordFunction
import net.xmppwocky.earbs.model.FunctionCard
import net.xmppwocky.earbs.model.GameCard
import net.xmppwocky.earbs.model.IntervalCard
import net.xmppwocky.earbs.model.KeyQuality
import net.xmppwocky.earbs.model.ProgressionCard
import net.xmppwocky.earbs.model.ScaleCard

/**
 * Common data structure for cards with FSRS state used in selection algorithm.
 * Wraps game-specific WithFsrs types into a common format.
 */
data class CardWithFsrsData(
    val id: String,
    val octave: Int,
    val playbackMode: String,
    val dueDate: Long,
    /** Additional grouping key (keyQuality for function game, null for chord type game) */
    val groupKey: String?
)

/**
 * Generic interface for card database operations.
 * Adapts game-specific DAOs to a common interface for the selection algorithm.
 *
 * @param C The domain card type (Card or FunctionCard)
 */
interface GameCardOperations<C : GameCard> {
    /** Get count of cards in database */
    suspend fun count(): Int

    /** Get all due cards (dueDate <= now) with FSRS data */
    suspend fun getDueCards(now: Long): List<CardWithFsrsData>

    /** Get all unlocked cards with FSRS data */
    suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrsData>

    /** Get non-due cards for a specific group */
    suspend fun getNonDueCardsByGroup(
        now: Long,
        groupKey: String?,
        octave: Int,
        mode: String,
        limit: Int
    ): List<CardWithFsrsData>

    /** Get non-due cards from any group */
    suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrsData>

    /** Convert CardWithFsrsData back to domain card type */
    fun toDomainCard(data: CardWithFsrsData): C

    /**
     * Get grouping key for selection algorithm.
     * Returns (groupKey, octave, playbackMode) tuple as a composite key.
     */
    fun getGroupingKey(data: CardWithFsrsData): Triple<String?, Int, String> {
        return Triple(data.groupKey, data.octave, data.playbackMode)
    }
}

/**
 * Adapter wrapping CardDao for chord type game.
 */
class ChordTypeCardOperations(
    private val cardDao: CardDao
) : GameCardOperations<Card> {

    override suspend fun count(): Int = cardDao.count()

    override suspend fun getDueCards(now: Long): List<CardWithFsrsData> {
        return cardDao.getDueCards(now).map { it.toCardWithFsrsData() }
    }

    override suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrsData> {
        return cardDao.getAllUnlockedWithFsrs().map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCardsByGroup(
        now: Long,
        groupKey: String?,
        octave: Int,
        mode: String,
        limit: Int
    ): List<CardWithFsrsData> {
        // groupKey is ignored for chord type game (grouping is octave+mode only)
        return cardDao.getNonDueCardsByGroup(now, octave, mode, limit)
            .map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrsData> {
        return cardDao.getNonDueCards(now, limit).map { it.toCardWithFsrsData() }
    }

    override fun toDomainCard(data: CardWithFsrsData): Card {
        // Parse card ID: {chordType}_{octave}_{playbackMode}
        val parts = data.id.split("_")
        return Card(
            chordType = ChordType.valueOf(parts[0]),
            octave = data.octave,
            playbackMode = PlaybackMode.valueOf(data.playbackMode)
        )
    }

    private fun CardWithFsrs.toCardWithFsrsData() = CardWithFsrsData(
        id = id,
        octave = octave,
        playbackMode = playbackMode,
        dueDate = dueDate,
        groupKey = null  // No additional grouping for chord type game
    )
}

/**
 * Adapter wrapping FunctionCardDao for function game.
 */
class FunctionCardOperations(
    private val functionCardDao: FunctionCardDao
) : GameCardOperations<FunctionCard> {

    override suspend fun count(): Int = functionCardDao.count()

    override suspend fun getDueCards(now: Long): List<CardWithFsrsData> {
        return functionCardDao.getDueCards(now).map { it.toCardWithFsrsData() }
    }

    override suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrsData> {
        return functionCardDao.getAllUnlockedWithFsrs().map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCardsByGroup(
        now: Long,
        groupKey: String?,
        octave: Int,
        mode: String,
        limit: Int
    ): List<CardWithFsrsData> {
        // groupKey is keyQuality for function game
        val keyQuality = groupKey ?: return emptyList()
        return functionCardDao.getNonDueCardsByGroup(now, keyQuality, octave, mode, limit)
            .map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrsData> {
        return functionCardDao.getNonDueCards(now, limit).map { it.toCardWithFsrsData() }
    }

    override fun toDomainCard(data: CardWithFsrsData): FunctionCard {
        // Parse from card ID format: {function}_{keyQuality}_{octave}_{playbackMode}
        // Note: function names can contain underscores (e.g., vii_dim)
        return FunctionCard.fromId(data.id)
            ?: throw IllegalArgumentException("Invalid function card ID: ${data.id}")
    }

    private fun FunctionCardWithFsrs.toCardWithFsrsData() = CardWithFsrsData(
        id = id,
        octave = octave,
        playbackMode = playbackMode,
        dueDate = dueDate,
        groupKey = keyQuality  // keyQuality is the additional grouping key
    )
}

/**
 * Adapter wrapping ProgressionCardDao for progression game.
 */
class ProgressionCardOperations(
    private val progressionCardDao: ProgressionCardDao
) : GameCardOperations<ProgressionCard> {

    override suspend fun count(): Int = progressionCardDao.count()

    override suspend fun getDueCards(now: Long): List<CardWithFsrsData> {
        return progressionCardDao.getDueCards(now).map { it.toCardWithFsrsData() }
    }

    override suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrsData> {
        return progressionCardDao.getAllUnlockedWithFsrs().map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCardsByGroup(
        now: Long,
        groupKey: String?,
        octave: Int,
        mode: String,
        limit: Int
    ): List<CardWithFsrsData> {
        // groupKey is ignored for progression game (grouping is octave+mode only, like chord type)
        return progressionCardDao.getNonDueCardsByGroup(now, octave, mode, limit)
            .map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrsData> {
        return progressionCardDao.getNonDueCards(now, limit).map { it.toCardWithFsrsData() }
    }

    override fun toDomainCard(data: CardWithFsrsData): ProgressionCard {
        // Parse from card ID format
        return ProgressionCard.fromId(data.id)
            ?: throw IllegalArgumentException("Invalid progression card ID: ${data.id}")
    }

    private fun ProgressionCardWithFsrs.toCardWithFsrsData() = CardWithFsrsData(
        id = id,
        octave = octave,
        playbackMode = playbackMode,
        dueDate = dueDate,
        groupKey = null  // No additional grouping for progression game
    )
}

/**
 * Adapter wrapping IntervalCardDao for interval game.
 */
class IntervalCardOperations(
    private val intervalCardDao: IntervalCardDao
) : GameCardOperations<IntervalCard> {

    override suspend fun count(): Int = intervalCardDao.count()

    override suspend fun getDueCards(now: Long): List<CardWithFsrsData> {
        return intervalCardDao.getDueCards(now).map { it.toCardWithFsrsData() }
    }

    override suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrsData> {
        return intervalCardDao.getAllUnlockedWithFsrs().map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCardsByGroup(
        now: Long,
        groupKey: String?,
        octave: Int,
        mode: String,
        limit: Int
    ): List<CardWithFsrsData> {
        // groupKey is interval name, mode is direction for interval game
        val interval = groupKey ?: return emptyList()
        return intervalCardDao.getNonDueCardsByGroup(now, interval, octave, mode, limit)
            .map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrsData> {
        return intervalCardDao.getNonDueCards(now, limit).map { it.toCardWithFsrsData() }
    }

    override fun toDomainCard(data: CardWithFsrsData): IntervalCard {
        // Parse from card ID format: {interval}_{octave}_{direction}
        return IntervalCard.fromId(data.id)
    }

    /**
     * Override grouping to use interval type and direction instead of octave and playbackMode.
     * For intervals, we want to group by interval type (e.g., PERFECT_5TH) and direction.
     */
    override fun getGroupingKey(data: CardWithFsrsData): Triple<String?, Int, String> {
        // groupKey = interval type, mode = direction
        return Triple(data.groupKey, data.octave, data.playbackMode)
    }

    private fun IntervalCardWithFsrs.toCardWithFsrsData() = CardWithFsrsData(
        id = id,
        octave = octave,
        playbackMode = direction,  // Use direction as "mode" for grouping
        dueDate = dueDate,
        groupKey = interval  // Use interval type as grouping key
    )
}

/**
 * Adapter wrapping ScaleCardDao for scale game.
 */
class ScaleCardOperations(
    private val scaleCardDao: ScaleCardDao
) : GameCardOperations<ScaleCard> {

    override suspend fun count(): Int = scaleCardDao.count()

    override suspend fun getDueCards(now: Long): List<CardWithFsrsData> {
        return scaleCardDao.getDueCards(now).map { it.toCardWithFsrsData() }
    }

    override suspend fun getAllUnlockedWithFsrs(): List<CardWithFsrsData> {
        return scaleCardDao.getAllUnlockedWithFsrs().map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCardsByGroup(
        now: Long,
        groupKey: String?,
        octave: Int,
        mode: String,
        limit: Int
    ): List<CardWithFsrsData> {
        // groupKey is scale name, mode is direction for scale game
        val scale = groupKey ?: return emptyList()
        return scaleCardDao.getNonDueCardsByGroup(now, scale, octave, mode, limit)
            .map { it.toCardWithFsrsData() }
    }

    override suspend fun getNonDueCards(now: Long, limit: Int): List<CardWithFsrsData> {
        return scaleCardDao.getNonDueCards(now, limit).map { it.toCardWithFsrsData() }
    }

    override fun toDomainCard(data: CardWithFsrsData): ScaleCard {
        // Parse from card ID format: {scale}_{octave}_{direction}
        return ScaleCard.fromId(data.id)
    }

    /**
     * Override grouping to use scale type and direction instead of octave and playbackMode.
     * For scales, we want to group by scale type (e.g., MAJOR) and direction.
     */
    override fun getGroupingKey(data: CardWithFsrsData): Triple<String?, Int, String> {
        // groupKey = scale type, mode = direction
        return Triple(data.groupKey, data.octave, data.playbackMode)
    }

    private fun ScaleCardWithFsrs.toCardWithFsrsData() = CardWithFsrsData(
        id = id,
        octave = octave,
        playbackMode = direction,  // Use direction as "mode" for grouping
        dueDate = dueDate,
        groupKey = scale  // Use scale type as grouping key
    )
}
