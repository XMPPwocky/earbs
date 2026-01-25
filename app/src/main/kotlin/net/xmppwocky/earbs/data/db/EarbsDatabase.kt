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
    version = 7,
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

        /**
         * Migration from version 6 to 7:
         * - Pre-create all cards (48 chord type + 72 function) for the new unlock system
         * - Existing cards are preserved (INSERT OR IGNORE)
         * - New cards are created with unlocked = 0 (locked)
         * - Only group 0 cards (starting deck) are unlocked by default
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 6 to 7: pre-creating all cards")

                // ========== Chord Type Cards (48 total) ==========
                // Group 0: Triads @ Octave 4, Arpeggiated (starting deck - unlocked)
                val chordTypeStartingCards = listOf(
                    "MAJOR_4_ARPEGGIATED", "MINOR_4_ARPEGGIATED", "SUS2_4_ARPEGGIATED", "SUS4_4_ARPEGGIATED"
                )
                // All other chord type cards (locked by default)
                val chordTypeLockedCards = listOf(
                    // Group 1: Triads @ Octave 4, Block
                    "MAJOR_4_BLOCK", "MINOR_4_BLOCK", "SUS2_4_BLOCK", "SUS4_4_BLOCK",
                    // Group 2: Triads @ Octave 3, Arpeggiated
                    "MAJOR_3_ARPEGGIATED", "MINOR_3_ARPEGGIATED", "SUS2_3_ARPEGGIATED", "SUS4_3_ARPEGGIATED",
                    // Group 3: Triads @ Octave 3, Block
                    "MAJOR_3_BLOCK", "MINOR_3_BLOCK", "SUS2_3_BLOCK", "SUS4_3_BLOCK",
                    // Group 4: Triads @ Octave 5, Arpeggiated
                    "MAJOR_5_ARPEGGIATED", "MINOR_5_ARPEGGIATED", "SUS2_5_ARPEGGIATED", "SUS4_5_ARPEGGIATED",
                    // Group 5: Triads @ Octave 5, Block
                    "MAJOR_5_BLOCK", "MINOR_5_BLOCK", "SUS2_5_BLOCK", "SUS4_5_BLOCK",
                    // Group 6: 7ths @ Octave 4, Arpeggiated
                    "DOM7_4_ARPEGGIATED", "MAJ7_4_ARPEGGIATED", "MIN7_4_ARPEGGIATED", "DIM7_4_ARPEGGIATED",
                    // Group 7: 7ths @ Octave 4, Block
                    "DOM7_4_BLOCK", "MAJ7_4_BLOCK", "MIN7_4_BLOCK", "DIM7_4_BLOCK",
                    // Group 8: 7ths @ Octave 3, Arpeggiated
                    "DOM7_3_ARPEGGIATED", "MAJ7_3_ARPEGGIATED", "MIN7_3_ARPEGGIATED", "DIM7_3_ARPEGGIATED",
                    // Group 9: 7ths @ Octave 3, Block
                    "DOM7_3_BLOCK", "MAJ7_3_BLOCK", "MIN7_3_BLOCK", "DIM7_3_BLOCK",
                    // Group 10: 7ths @ Octave 5, Arpeggiated
                    "DOM7_5_ARPEGGIATED", "MAJ7_5_ARPEGGIATED", "MIN7_5_ARPEGGIATED", "DIM7_5_ARPEGGIATED",
                    // Group 11: 7ths @ Octave 5, Block
                    "DOM7_5_BLOCK", "MAJ7_5_BLOCK", "MIN7_5_BLOCK", "DIM7_5_BLOCK"
                )

                // Insert starting deck cards (unlocked = 1)
                for (cardId in chordTypeStartingCards) {
                    val parts = cardId.split("_")
                    val chordType = parts[0]
                    val octave = parts[1]
                    val playbackMode = parts[2]
                    db.execSQL("""
                        INSERT OR IGNORE INTO cards (id, chordType, octave, playbackMode, unlocked)
                        VALUES ('$cardId', '$chordType', $octave, '$playbackMode', 1)
                    """.trimIndent())
                }

                // Insert locked cards (unlocked = 0)
                for (cardId in chordTypeLockedCards) {
                    val parts = cardId.split("_")
                    val chordType = parts[0]
                    val octave = parts[1]
                    val playbackMode = parts[2]
                    db.execSQL("""
                        INSERT OR IGNORE INTO cards (id, chordType, octave, playbackMode, unlocked)
                        VALUES ('$cardId', '$chordType', $octave, '$playbackMode', 0)
                    """.trimIndent())
                }

                Log.i(TAG, "Inserted chord type cards")

                // ========== Function Cards (72 total) ==========
                // Use explicit tuples: (id, function, keyQuality, octave, playbackMode, unlocked)
                // Note: Function names like vii_dim contain underscores, so we can't parse the ID

                // Helper to insert function card
                fun insertFunctionCard(id: String, function: String, keyQuality: String, octave: Int, playbackMode: String, unlocked: Int) {
                    db.execSQL("""
                        INSERT OR IGNORE INTO function_cards (id, function, keyQuality, octave, playbackMode, unlocked)
                        VALUES ('$id', '$function', '$keyQuality', $octave, '$playbackMode', $unlocked)
                    """.trimIndent())
                }

                // Major key functions
                val majorFunctions = listOf("IV", "V", "vi", "ii", "iii", "vii_dim")
                val majorPrimary = listOf("IV", "V", "vi")
                val majorSecondary = listOf("ii", "iii", "vii_dim")

                // Minor key functions
                val minorFunctions = listOf("iv", "v", "VI", "ii_dim", "III", "VII")
                val minorPrimary = listOf("iv", "v", "VI")
                val minorSecondary = listOf("ii_dim", "III", "VII")

                val octaves = listOf(4, 3, 5)
                val playbackModes = listOf("ARPEGGIATED", "BLOCK")

                // Group 0 (starting deck) is Major Primary @ Octave 4, Arpeggiated
                for (octave in octaves) {
                    for (mode in playbackModes) {
                        for (func in majorPrimary) {
                            val id = "${func}_MAJOR_${octave}_$mode"
                            // Starting deck: octave 4, arpeggiated, major primary
                            val unlocked = if (octave == 4 && mode == "ARPEGGIATED") 1 else 0
                            insertFunctionCard(id, func, "MAJOR", octave, mode, unlocked)
                        }
                        for (func in majorSecondary) {
                            val id = "${func}_MAJOR_${octave}_$mode"
                            insertFunctionCard(id, func, "MAJOR", octave, mode, 0)
                        }
                    }
                }

                for (octave in octaves) {
                    for (mode in playbackModes) {
                        for (func in minorPrimary) {
                            val id = "${func}_MINOR_${octave}_$mode"
                            insertFunctionCard(id, func, "MINOR", octave, mode, 0)
                        }
                        for (func in minorSecondary) {
                            val id = "${func}_MINOR_${octave}_$mode"
                            insertFunctionCard(id, func, "MINOR", octave, mode, 0)
                        }
                    }
                }

                Log.i(TAG, "Inserted function cards")

                // ========== Create FSRS state for all new cards ==========
                val now = System.currentTimeMillis()

                // Create FSRS state for chord type cards (only for cards that don't have state yet)
                db.execSQL("""
                    INSERT OR IGNORE INTO fsrs_state (cardId, gameType, stability, difficulty, `interval`, dueDate, reviewCount, lastReview, phase, lapses)
                    SELECT id, 'CHORD_TYPE', 2.5, 2.5, 0, $now, 0, NULL, 0, 0
                    FROM cards
                    WHERE id NOT IN (SELECT cardId FROM fsrs_state WHERE gameType = 'CHORD_TYPE')
                """.trimIndent())

                // Create FSRS state for function cards (only for cards that don't have state yet)
                db.execSQL("""
                    INSERT OR IGNORE INTO fsrs_state (cardId, gameType, stability, difficulty, `interval`, dueDate, reviewCount, lastReview, phase, lapses)
                    SELECT id, 'CHORD_FUNCTION', 2.5, 2.5, 0, $now, 0, NULL, 0, 0
                    FROM function_cards
                    WHERE id NOT IN (SELECT cardId FROM fsrs_state WHERE gameType = 'CHORD_FUNCTION')
                """.trimIndent())

                Log.i(TAG, "Created FSRS state for new cards")
                Log.i(TAG, "Migration 6->7 complete: pre-created all cards for new unlock system")
            }
        }

        fun getDatabase(context: Context): EarbsDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarbsDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                Log.i(TAG, "Database created successfully")
                instance
            }
        }

        /**
         * Closes the database connection and clears the singleton instance.
         * This is needed for backup/restore operations to safely access the database file.
         */
        fun closeDatabase() {
            synchronized(this) {
                Log.i(TAG, "Closing database")
                INSTANCE?.close()
                INSTANCE = null
                Log.i(TAG, "Database closed and instance cleared")
            }
        }

        /**
         * Gets the database file path for backup/restore operations.
         */
        fun getDatabasePath(context: Context): java.io.File {
            return context.getDatabasePath(DATABASE_NAME)
        }

        const val DATABASE_NAME = "earbs_database"
    }
}
