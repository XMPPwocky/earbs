package net.xmppwocky.earbs.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.FunctionCardEntity
import net.xmppwocky.earbs.data.entity.FsrsStateEntity
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity

private const val TAG = "EarbsDatabase"

@Database(
    entities = [
        CardEntity::class,
        FunctionCardEntity::class,
        FsrsStateEntity::class,
        ReviewSessionEntity::class,
        TrialEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class EarbsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun functionCardDao(): FunctionCardDao
    abstract fun fsrsStateDao(): FsrsStateDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun trialDao(): TrialDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: EarbsDatabase? = null

        /**
         * Migration from version 1 to 2:
         * - Add playbackMode column to cards table
         * - Update card IDs to include playback mode
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 1 to 2")
                // Drop and recreate is simpler for this schema change
                db.execSQL("DROP TABLE IF EXISTS cards")
                db.execSQL("DROP TABLE IF EXISTS trials")
                db.execSQL("DROP TABLE IF EXISTS session_card_summaries")
                db.execSQL("DROP TABLE IF EXISTS review_sessions")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cards (
                        id TEXT NOT NULL PRIMARY KEY,
                        chordType TEXT NOT NULL,
                        octave INTEGER NOT NULL,
                        playbackMode TEXT NOT NULL,
                        stability REAL NOT NULL DEFAULT 2.5,
                        difficulty REAL NOT NULL DEFAULT 2.5,
                        `interval` INTEGER NOT NULL DEFAULT 0,
                        dueDate INTEGER NOT NULL,
                        reviewCount INTEGER NOT NULL DEFAULT 0,
                        lastReview INTEGER,
                        phase INTEGER NOT NULL DEFAULT 0,
                        lapses INTEGER NOT NULL DEFAULT 0,
                        unlocked INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS review_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        octave INTEGER NOT NULL,
                        playbackMode TEXT NOT NULL DEFAULT 'ARPEGGIATED'
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS trials (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        cardId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        wasCorrect INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS session_card_summaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        cardId TEXT NOT NULL,
                        trialsCount INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL,
                        grade TEXT NOT NULL
                    )
                """.trimIndent())

                Log.i(TAG, "Migration 1->2 complete: recreated tables with playbackMode")
            }
        }

        /**
         * Migration from version 2 to 3:
         * - Remove session_card_summaries table (no longer needed with per-trial FSRS)
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 2 to 3")
                db.execSQL("DROP TABLE IF EXISTS session_card_summaries")
                Log.i(TAG, "Migration 2->3 complete: removed session_card_summaries table")
            }
        }

        /**
         * Migration from version 3 to 4:
         * - Fix cards with stability=0.0 which cause NaN crashes
         * - Reset them to default values (2.5) and Added phase for proper re-initialization
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 3 to 4")
                db.execSQL("""
                    UPDATE cards
                    SET stability = 2.5, difficulty = 2.5, phase = 0
                    WHERE stability = 0.0 OR stability IS NULL
                """.trimIndent())
                Log.i(TAG, "Migration 3->4 complete: fixed cards with zero stability")
            }
        }

        /**
         * Migration from version 4 to 5:
         * - Add answeredChordType column to trials table to track wrong answers
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 4 to 5")
                db.execSQL("ALTER TABLE trials ADD COLUMN answeredChordType TEXT")
                Log.i(TAG, "Migration 4->5 complete: added answeredChordType column")
            }
        }

        /**
         * Migration from version 5 to 6:
         * - Create fsrs_state table and extract FSRS fields from cards
         * - Recreate cards table without FSRS fields
         * - Create function_cards table for chord function game
         * - Add gameType to review_sessions
         * - Recreate trials table with gameType and answeredFunction, remove FK on cardId
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 5 to 6")

                // 1. Create fsrs_state table
                Log.i(TAG, "Creating fsrs_state table")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fsrs_state (
                        cardId TEXT NOT NULL PRIMARY KEY,
                        gameType TEXT NOT NULL,
                        stability REAL NOT NULL DEFAULT 2.5,
                        difficulty REAL NOT NULL DEFAULT 2.5,
                        `interval` INTEGER NOT NULL DEFAULT 0,
                        dueDate INTEGER NOT NULL,
                        reviewCount INTEGER NOT NULL DEFAULT 0,
                        lastReview INTEGER,
                        phase INTEGER NOT NULL DEFAULT 0,
                        lapses INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fsrs_state_gameType ON fsrs_state (gameType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fsrs_state_dueDate ON fsrs_state (dueDate)")

                // 2. Copy FSRS data from cards to fsrs_state
                Log.i(TAG, "Copying FSRS data from cards to fsrs_state")
                db.execSQL("""
                    INSERT INTO fsrs_state (cardId, gameType, stability, difficulty, `interval`, dueDate, reviewCount, lastReview, phase, lapses)
                    SELECT id, 'CHORD_TYPE', stability, difficulty, `interval`, dueDate, reviewCount, lastReview, phase, lapses
                    FROM cards
                """.trimIndent())

                // 3. Recreate cards table without FSRS fields
                Log.i(TAG, "Recreating cards table without FSRS fields")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cards_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        chordType TEXT NOT NULL,
                        octave INTEGER NOT NULL,
                        playbackMode TEXT NOT NULL,
                        unlocked INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO cards_new (id, chordType, octave, playbackMode, unlocked)
                    SELECT id, chordType, octave, playbackMode, unlocked
                    FROM cards
                """.trimIndent())
                db.execSQL("DROP TABLE cards")
                db.execSQL("ALTER TABLE cards_new RENAME TO cards")

                // 4. Create function_cards table
                Log.i(TAG, "Creating function_cards table")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS function_cards (
                        id TEXT NOT NULL PRIMARY KEY,
                        function TEXT NOT NULL,
                        keyQuality TEXT NOT NULL,
                        octave INTEGER NOT NULL,
                        playbackMode TEXT NOT NULL,
                        unlocked INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                // 5. Add gameType to review_sessions (recreate to add column with default)
                Log.i(TAG, "Adding gameType to review_sessions")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS review_sessions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        gameType TEXT NOT NULL DEFAULT 'CHORD_TYPE',
                        octave INTEGER NOT NULL DEFAULT 0,
                        playbackMode TEXT NOT NULL DEFAULT 'MIXED'
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO review_sessions_new (id, startedAt, completedAt, gameType, octave, playbackMode)
                    SELECT id, startedAt, completedAt, 'CHORD_TYPE', octave, playbackMode
                    FROM review_sessions
                """.trimIndent())
                db.execSQL("DROP TABLE review_sessions")
                db.execSQL("ALTER TABLE review_sessions_new RENAME TO review_sessions")

                // 6. Recreate trials table with new columns and without FK on cardId
                Log.i(TAG, "Recreating trials table with gameType and answeredFunction")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS trials_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        cardId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        wasCorrect INTEGER NOT NULL,
                        gameType TEXT NOT NULL DEFAULT 'CHORD_TYPE',
                        answeredChordType TEXT,
                        answeredFunction TEXT,
                        FOREIGN KEY (sessionId) REFERENCES review_sessions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO trials_new (id, sessionId, cardId, timestamp, wasCorrect, gameType, answeredChordType)
                    SELECT id, sessionId, cardId, timestamp, wasCorrect, 'CHORD_TYPE', answeredChordType
                    FROM trials
                """.trimIndent())
                db.execSQL("DROP TABLE trials")
                db.execSQL("ALTER TABLE trials_new RENAME TO trials")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trials_sessionId ON trials (sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trials_cardId ON trials (cardId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trials_gameType ON trials (gameType)")

                Log.i(TAG, "Migration 5->6 complete: extracted FSRS state, added function game support")
            }
        }

        fun getDatabase(context: Context): EarbsDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarbsDatabase::class.java,
                    "earbs_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                Log.i(TAG, "Database created successfully")
                instance
            }
        }
    }
}
