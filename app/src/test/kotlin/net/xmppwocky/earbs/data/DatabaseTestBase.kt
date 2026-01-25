package net.xmppwocky.earbs.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import net.xmppwocky.earbs.data.db.CardDao
import net.xmppwocky.earbs.data.db.EarbsDatabase
import net.xmppwocky.earbs.data.db.FsrsStateDao
import net.xmppwocky.earbs.data.db.FunctionCardDao
import net.xmppwocky.earbs.data.db.HistoryDao
import net.xmppwocky.earbs.data.db.ReviewSessionDao
import net.xmppwocky.earbs.data.db.TrialDao
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.GameType
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base class for database integration tests using Robolectric.
 * Provides an in-memory Room database and all DAOs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
abstract class DatabaseTestBase {
    protected lateinit var db: EarbsDatabase
    protected lateinit var cardDao: CardDao
    protected lateinit var functionCardDao: FunctionCardDao
    protected lateinit var fsrsStateDao: FsrsStateDao
    protected lateinit var reviewSessionDao: ReviewSessionDao
    protected lateinit var trialDao: TrialDao
    protected lateinit var historyDao: HistoryDao
    protected lateinit var context: Context
    protected lateinit var prefs: SharedPreferences

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EarbsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cardDao = db.cardDao()
        functionCardDao = db.functionCardDao()
        fsrsStateDao = db.fsrsStateDao()
        reviewSessionDao = db.reviewSessionDao()
        trialDao = db.trialDao()
        historyDao = db.historyDao()
        prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
    }

    @After
    fun closeDb() {
        db.close()
        prefs.edit().clear().apply()
    }

    // ========== Test Data Factories ==========

    /**
     * Create a test CardEntity with optional FSRS state.
     */
    protected suspend fun createCard(
        chordType: String = "MAJOR",
        octave: Int = 4,
        playbackMode: String = "ARPEGGIATED",
        unlocked: Boolean = true,
        withFsrsState: Boolean = true,
        dueDate: Long = System.currentTimeMillis()
    ): CardEntity {
        val cardId = "${chordType}_${octave}_${playbackMode}"
        val card = CardEntity(
            id = cardId,
            chordType = chordType,
            octave = octave,
            playbackMode = playbackMode,
            unlocked = unlocked
        )
        cardDao.insert(card)

        if (withFsrsState) {
            fsrsStateDao.insert(
                FsrsStateEntity(
                    cardId = cardId,
                    gameType = GameType.CHORD_TYPE.name,
                    dueDate = dueDate
                )
            )
        }

        return card
    }

    /**
     * Create a test FunctionCardEntity with optional FSRS state.
     */
    protected suspend fun createFunctionCard(
        function: String = "V",
        keyQuality: String = "MAJOR",
        octave: Int = 4,
        playbackMode: String = "ARPEGGIATED",
        unlocked: Boolean = true,
        withFsrsState: Boolean = true,
        dueDate: Long = System.currentTimeMillis()
    ): FunctionCardEntity {
        val cardId = "${function}_${keyQuality}_${octave}_${playbackMode}"
        val card = FunctionCardEntity(
            id = cardId,
            function = function,
            keyQuality = keyQuality,
            octave = octave,
            playbackMode = playbackMode,
            unlocked = unlocked
        )
        functionCardDao.insert(card)

        if (withFsrsState) {
            fsrsStateDao.insert(
                FsrsStateEntity(
                    cardId = cardId,
                    gameType = GameType.CHORD_FUNCTION.name,
                    dueDate = dueDate
                )
            )
        }

        return card
    }

    /**
     * Create multiple cards with FSRS state for testing.
     */
    protected suspend fun createMultipleCards(count: Int, baseOctave: Int = 4): List<CardEntity> {
        val chordTypes = listOf("MAJOR", "MINOR", "SUS2", "SUS4", "DOM7", "MAJ7", "MIN7", "DIM7")
        val playbackModes = listOf("ARPEGGIATED", "BLOCK")
        val cards = mutableListOf<CardEntity>()
        var created = 0

        outer@ for (octave in baseOctave..(baseOctave + 2)) {
            for (mode in playbackModes) {
                for (type in chordTypes) {
                    if (created >= count) break@outer
                    cards.add(createCard(type, octave, mode))
                    created++
                }
            }
        }

        return cards
    }

    /**
     * Create cards with specific due dates for testing selection logic.
     */
    protected suspend fun createCardsWithDueDates(
        vararg dueDateOffsets: Long
    ): List<CardEntity> {
        val now = System.currentTimeMillis()
        val chordTypes = listOf("MAJOR", "MINOR", "SUS2", "SUS4", "DOM7", "MAJ7", "MIN7", "DIM7")

        return dueDateOffsets.mapIndexed { index, offset ->
            createCard(
                chordType = chordTypes[index % chordTypes.size],
                octave = 4 + (index / chordTypes.size),
                dueDate = now + offset
            )
        }
    }

    companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val DAY_MS = 24 * HOUR_MS
    }
}
