package net.xmppwocky.earbs.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.xmppwocky.earbs.data.entity.FunctionCardEntity

@Dao
interface FunctionCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: FunctionCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<FunctionCardEntity>)

    @Query("SELECT * FROM function_cards WHERE id = :id")
    suspend fun getById(id: String): FunctionCardEntity?

    @Query("SELECT * FROM function_cards WHERE unlocked = 1")
    suspend fun getAllUnlocked(): List<FunctionCardEntity>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1")
    fun getAllUnlockedFlow(): Flow<List<FunctionCardEntity>>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND keyQuality = :keyQuality")
    suspend fun getByKeyQuality(keyQuality: String): List<FunctionCardEntity>

    @Query("SELECT * FROM function_cards WHERE unlocked = 1 AND octave = :octave AND playbackMode = :mode")
    suspend fun getByGroup(octave: Int, mode: String): List<FunctionCardEntity>

    @Query("SELECT COUNT(*) FROM function_cards")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM function_cards WHERE unlocked = 1")
    suspend fun countUnlocked(): Int

    @Query("SELECT COUNT(*) FROM function_cards WHERE unlocked = 1")
    fun countUnlockedFlow(): Flow<Int>
}
