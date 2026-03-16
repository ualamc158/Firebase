package com.alberto.firebase.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "favorites")
data class FavoriteSong(
    @PrimaryKey val title: String,
    val artist: String,
    val imageUrl: String,
    val audioUrl: String?
)


@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: FavoriteSong)

    @Delete
    suspend fun deleteFavorite(song: FavoriteSong)
}


@Database(entities = [FavoriteSong::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundconnect_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}