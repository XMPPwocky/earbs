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
import net.xmppwocky.earbs.data.entity.SessionCardSummaryEntity
import net.xmppwocky.earbs.data.entity.TrialEntity

private const val TAG = "EarbsDatabase"

@Database(
    entities = [
        CardEntity::class,
        ReviewSessionEntity::class,
        TrialEntity::class,
        SessionCardSummaryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EarbsDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun trialDao(): TrialDao
    abstract fun sessionCardSummaryDao(): SessionCardSummaryDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: EarbsDatabase? = null

        /**
         * Migration from version 1 to 2:
         * - Add playbackMode column to cards table
         * - Update card IDs to include playback mode
         *
         * Note: For simplicity, we use destructive migration which drops and recreates
         * the database. This is acceptable during development but would need proper
         * migration for production releases.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "Migrating database from version 1 to 2")
                // Drop and recreate is simpler for this schema change
                // Production would need proper data migration
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

        fun getDatabase(context: Context): EarbsDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarbsDatabase::class.java,
                    "earbs_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                Log.i(TAG, "Database created successfully")
                instance
            }
        }
    }
}
