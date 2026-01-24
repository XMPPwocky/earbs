package net.xmppwocky.earbs.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.xmppwocky.earbs.data.entity.CardEntity
import net.xmppwocky.earbs.data.entity.ReviewSessionEntity
import net.xmppwocky.earbs.data.entity.TrialEntity

private const val TAG = "EarbsDatabase"

@Database(
    entities = [
        CardEntity::class,
        ReviewSessionEntity::class,
        TrialEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class EarbsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
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

        fun getDatabase(context: Context): EarbsDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarbsDatabase::class.java,
                    "earbs_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                Log.i(TAG, "Database created successfully")
                instance
            }
        }
    }
}
