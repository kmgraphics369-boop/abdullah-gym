package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "gym_records",
    primaryKeys = ["ownerId", "collection", "id"]
)
data class GymRecordEntity(
    val ownerId: String,
    val collection: String,
    val id: String,
    val sortKey: Long = System.currentTimeMillis(),
    val jsonPayload: String
)

@Dao
interface GymRecordDao {
    @Query("SELECT * FROM gym_records WHERE ownerId = :ownerId AND collection = :collection ORDER BY sortKey DESC")
    fun observeCollection(ownerId: String, collection: String): Flow<List<GymRecordEntity>>

    @Query("SELECT * FROM gym_records WHERE ownerId = :ownerId AND collection = :collection")
    suspend fun getCollectionOnce(ownerId: String, collection: String): List<GymRecordEntity>

    @Query("SELECT * FROM gym_records WHERE ownerId = :ownerId AND collection = :collection AND id = :id LIMIT 1")
    suspend fun getOne(ownerId: String, collection: String, id: String): GymRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: GymRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<GymRecordEntity>)

    @Query("DELETE FROM gym_records WHERE ownerId = :ownerId AND collection = :collection AND id = :id")
    suspend fun deleteOne(ownerId: String, collection: String, id: String)
}

@Database(entities = [GymRecordEntity::class], version = 1, exportSchema = false)
abstract class GymDatabase : RoomDatabase() {
    abstract fun gymRecordDao(): GymRecordDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "gym_fee_manager_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
