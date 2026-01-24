package net.xmppwocky.earbs.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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

        fun getDatabase(context: Context): EarbsDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarbsDatabase::class.java,
                    "earbs_database"
                ).build()
                INSTANCE = instance
                Log.i(TAG, "Database created successfully")
                instance
            }
        }
    }
}
