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
import net.xmppwocky.earbs.data.entity.ProgressionCardEntity
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity

private const val TAG = "EarbsDatabase"

@Database(
    entities = [
        CardEntity::class,
        FunctionCardEntity::class,
        ProgressionCardEntity::class,
        FsrsStateEntity::class,
        ReviewSessionEntity::class,
        TrialEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class EarbsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun functionCardDao(): FunctionCardDao
    abstract fun progressionCardDao(): ProgressionCardDao
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

        /**
         * Migration from version 7 to 8:
         * - Create progression_cards table for chord progression game
         * - Pre-create all 96 progression cards (16 progressions × 3 octaves × 2 modes)
         * - Create FSRS state for all progression cards
         * - Add answeredProgression column to trials table
         */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 7 to 8: adding chord progression game")

                // 1. Create progression_cards table
                Log.i(TAG, "Creating progression_cards table")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS progression_cards (
                        id TEXT NOT NULL PRIMARY KEY,
                        progression TEXT NOT NULL,
                        octave INTEGER NOT NULL,
                        playbackMode TEXT NOT NULL,
                        unlocked INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())

                // 2. Add answeredProgression column to trials table
                Log.i(TAG, "Adding answeredProgression column to trials table")
                db.execSQL("ALTER TABLE trials ADD COLUMN answeredProgression TEXT")

                // 3. Pre-create all 96 progression cards
                // 16 progressions × 3 octaves × 2 modes = 96 cards
                // Group 0 (starting deck): 3-chord major @ octave 4, arpeggiated (unlocked)
                Log.i(TAG, "Pre-creating progression cards")

                // 8 major progressions
                val threeChordMajor = listOf("I_IV_I_MAJOR", "I_V_I_MAJOR")
                val fourChordMajor = listOf("I_IV_V_I_MAJOR", "I_ii_V_I_MAJOR")
                val fiveChordMajor = listOf("I_vi_ii_V_I_MAJOR", "I_vi_IV_V_I_MAJOR")
                val loopsMajor = listOf("I_V_vi_IV_MAJOR", "I_vi_IV_V_MAJOR")

                // 8 minor progressions
                val threeChordMinor = listOf("i_iv_i_MINOR", "i_v_i_MINOR")
                val fourChordMinor = listOf("i_iv_v_i_MINOR", "i_iio_v_i_MINOR")
                val fiveChordMinor = listOf("i_VI_iio_v_i_MINOR", "i_VI_iv_v_i_MINOR")
                val loopsMinor = listOf("i_v_VI_iv_MINOR", "i_VI_iv_v_MINOR")

                val octaves = listOf(4, 3, 5)
                val playbackModes = listOf("ARPEGGIATED", "BLOCK")

                fun insertProgressionCard(progression: String, octave: Int, mode: String, unlocked: Int) {
                    val id = "${progression}_${octave}_$mode"
                    db.execSQL("""
                        INSERT OR IGNORE INTO progression_cards (id, progression, octave, playbackMode, unlocked)
                        VALUES ('$id', '$progression', $octave, '$mode', $unlocked)
                    """.trimIndent())
                }

                // Insert cards by complexity group
                for (octave in octaves) {
                    for (mode in playbackModes) {
                        // 3-chord major: starting deck is oct 4, arpeggiated
                        for (prog in threeChordMajor) {
                            val unlocked = if (octave == 4 && mode == "ARPEGGIATED") 1 else 0
                            insertProgressionCard(prog, octave, mode, unlocked)
                        }
                        // All other progressions are locked
                        for (prog in threeChordMinor + fourChordMajor + fourChordMinor +
                                     fiveChordMajor + fiveChordMinor + loopsMajor + loopsMinor) {
                            insertProgressionCard(prog, octave, mode, 0)
                        }
                    }
                }

                Log.i(TAG, "Inserted 96 progression cards")

                // 4. Create FSRS state for all progression cards
                val now = System.currentTimeMillis()
                db.execSQL("""
                    INSERT OR IGNORE INTO fsrs_state (cardId, gameType, stability, difficulty, `interval`, dueDate, reviewCount, lastReview, phase, lapses)
                    SELECT id, 'CHORD_PROGRESSION', 2.5, 2.5, 0, $now, 0, NULL, 0, 0
                    FROM progression_cards
                    WHERE id NOT IN (SELECT cardId FROM fsrs_state WHERE gameType = 'CHORD_PROGRESSION')
                """.trimIndent())

                Log.i(TAG, "Created FSRS state for progression cards")
                Log.i(TAG, "Migration 7->8 complete: added chord progression game support")
            }
        }

        /**
         * Migration from version 8 to 9:
         * - Add deprecated column to all card tables for app-level card deprecation
         * - Deprecated cards are excluded from reviews but historical data is preserved
         */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 8 to 9: adding card deprecation support")

                // Add deprecated column to cards table (default false = not deprecated)
                db.execSQL("ALTER TABLE cards ADD COLUMN deprecated INTEGER NOT NULL DEFAULT 0")
                Log.i(TAG, "Added deprecated column to cards table")

                // Add deprecated column to function_cards table
                db.execSQL("ALTER TABLE function_cards ADD COLUMN deprecated INTEGER NOT NULL DEFAULT 0")
                Log.i(TAG, "Added deprecated column to function_cards table")

                // Add deprecated column to progression_cards table
                db.execSQL("ALTER TABLE progression_cards ADD COLUMN deprecated INTEGER NOT NULL DEFAULT 0")
                Log.i(TAG, "Added deprecated column to progression_cards table")

                Log.i(TAG, "Migration 8->9 complete: added card deprecation support")
            }
        }

        /**
         * Migration from version 9 to 10:
         * - Deprecate all 5-chord progression cards (4 progressions × 6 variants = 24 cards)
         * - Create new 4-chord progression cards (2 progressions × 6 variants = 12 cards)
         * - The 5-chord versions I-vi-IV-V-I and i-VI-iv-v-i already have 4-chord
         *   equivalents in the LOOPS (I-vi-IV-V and i-VI-iv-v), so only 2 new progressions
         *   need to be created: I-vi-ii-V and i-VI-ii°-v
         */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 9 to 10: deprecating 5-chord progressions")

                // 1. Deprecate all 5-chord progression cards (4 progressions × 6 variants = 24 cards)
                val fiveChordProgressions = listOf(
                    "I_vi_ii_V_I_MAJOR",
                    "I_vi_IV_V_I_MAJOR",
                    "i_VI_iio_v_i_MINOR",
                    "i_VI_iv_v_i_MINOR"
                )
                for (progression in fiveChordProgressions) {
                    db.execSQL("UPDATE progression_cards SET deprecated = 1 WHERE progression = '$progression'")
                }
                Log.i(TAG, "Deprecated 24 five-chord progression cards")

                // 2. Insert new 4-chord progression cards (2 progressions × 6 variants = 12 cards)
                val newProgressions = listOf("I_vi_ii_V_MAJOR", "i_VI_iio_v_MINOR")
                val octaves = listOf(4, 3, 5)
                val modes = listOf("ARPEGGIATED", "BLOCK")
                val now = System.currentTimeMillis()

                for (progression in newProgressions) {
                    for (octave in octaves) {
                        for (mode in modes) {
                            val id = "${progression}_${octave}_$mode"
                            // Cards locked by default (unlocked = 0)
                            db.execSQL("""
                                INSERT OR IGNORE INTO progression_cards (id, progression, octave, playbackMode, unlocked, deprecated)
                                VALUES ('$id', '$progression', $octave, '$mode', 0, 0)
                            """.trimIndent())
                            // Create FSRS state
                            db.execSQL("""
                                INSERT OR IGNORE INTO fsrs_state (cardId, gameType, stability, difficulty, `interval`, dueDate, reviewCount, lastReview, phase, lapses)
                                VALUES ('$id', 'CHORD_PROGRESSION', 2.5, 2.5, 0, $now, 0, NULL, 0, 0)
                            """.trimIndent())
                        }
                    }
                }
                Log.i(TAG, "Created 12 new 4-chord progression cards")

                Log.i(TAG, "Migration 9->10 complete: deprecated 5-chord, added new 4-chord progressions")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
