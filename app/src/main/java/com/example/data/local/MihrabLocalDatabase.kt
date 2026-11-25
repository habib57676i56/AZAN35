package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mushaf_pages")
data class MushafPageEntity(
    @PrimaryKey val pageNumber: Int,
    val juzNumber: Int,
    val hizbQuarter: Int,
    val ayahsJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quran_ayah_index")
data class QuranAyahIndexEntity(
    @PrimaryKey val globalAyahNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbQuarter: Int,
    val surahNameAr: String,
    val surahNameEn: String,
    val uthmaniText: String,
    val normalizedText: String,
    val sajdah: Boolean = false
)

@Entity(tableName = "reciters")
data class ReciterEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val styleAr: String,
    val styleEn: String,
    val editionId: String,
    val everyAyahFolder: String,
    val sortOrder: Int,
    val active: Boolean = true,
    val cloudAudioBaseUrl: String = ""
)

@Entity(tableName = "cached_audio")
data class CachedAudioEntity(
    @PrimaryKey val cacheKey: String,
    val reciterId: String,
    val globalAyahNumber: Int,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_user_prefs")
data class LocalUserPrefsEntity(
    @PrimaryKey val userId: String,
    val preferredLanguage: String = "ar",
    val themeMode: String = "SYSTEM",
    val mushafPaper: String = "WHITE",
    val fontScale: Float = 1.0f,
    val lastReadPage: Int = 1,
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val preferredReciterId: String = "ar.alafasy",
    val adhanEnabled: Boolean = true,
    val adhanVoiceId: String = "makkah",
    val customAdhanName: String = "",
    val customAdhanLocalPath: String = "",
    val perPrayerAdhanJson: String = "{}",
    val khatmahPagesRead: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "adhan_voices")
data class AdhanVoiceEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val muezzin: String,
    val audioUrl: String,
    val active: Boolean = true
)

@Entity(tableName = "adhkar_items")
data class AdhkarEntity(
    @PrimaryKey val id: String,
    val category: String,
    val titleAr: String,
    val titleEn: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val targetCount: Int,
    val referenceAr: String,
    val referenceEn: String,
    val iconName: String = "",
    val sortOrder: Int,
    val active: Boolean = true
)

@Dao
interface MushafDao {
    @Query("SELECT * FROM mushaf_pages WHERE pageNumber = :page")
    suspend fun getPage(page: Int): MushafPageEntity?

    @Query("SELECT COUNT(*) FROM mushaf_pages")
    fun observeCachedPageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mushaf_pages")
    suspend fun getCachedPageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: MushafPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<MushafPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahIndexBatch(ayahs: List<QuranAyahIndexEntity>)

    @Query(
        "SELECT * FROM quran_ayah_index WHERE normalizedText LIKE '%' || :normalizedQuery || '%' " +
            "OR surahNameAr LIKE '%' || :rawQuery || '%' " +
            "OR surahNameEn LIKE '%' || :rawQuery || '%' " +
            "ORDER BY globalAyahNumber ASC LIMIT 60"
    )
    suspend fun searchAyahs(normalizedQuery: String, rawQuery: String): List<QuranAyahIndexEntity>

    @Query("SELECT * FROM quran_ayah_index WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getAyah(surahNumber: Int, ayahNumber: Int): QuranAyahIndexEntity?
}

@Dao
interface ReciterDao {
    @Query("SELECT * FROM reciters WHERE active = 1 ORDER BY sortOrder ASC")
    fun observeActiveReciters(): Flow<List<ReciterEntity>>

    @Query("SELECT * FROM reciters WHERE active = 1 ORDER BY sortOrder ASC")
    suspend fun getActiveReciters(): List<ReciterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reciters: List<ReciterEntity>)
}

@Dao
interface AudioCacheDao {
    @Query("SELECT * FROM cached_audio WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getCachedAudio(cacheKey: String): CachedAudioEntity?

    @Query("SELECT COUNT(*) FROM cached_audio WHERE reciterId = :reciterId")
    fun observeCachedCountForReciter(reciterId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM cached_audio WHERE reciterId = :reciterId")
    fun observeCachedBytesForReciter(reciterId: String): Flow<Long>

    @Query("SELECT reciterId FROM cached_audio GROUP BY reciterId")
    fun observeRecitersWithCache(): Flow<List<String>>

    @Query("DELETE FROM cached_audio WHERE reciterId = :reciterId")
    suspend fun deleteForReciter(reciterId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAudio(entry: CachedAudioEntity)
}

@Dao
interface LocalPrefsDao {
    @Query("SELECT * FROM local_user_prefs WHERE userId = :userId LIMIT 1")
    fun observePrefs(userId: String): Flow<LocalUserPrefsEntity?>

    @Query("SELECT * FROM local_user_prefs WHERE userId = :userId LIMIT 1")
    suspend fun getPrefs(userId: String): LocalUserPrefsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrefs(prefs: LocalUserPrefsEntity)

    @Query("DELETE FROM local_user_prefs WHERE userId = :userId")
    suspend fun deletePrefs(userId: String)
}

@Dao
interface AdhanVoiceDao {
    @Query("SELECT * FROM adhan_voices WHERE active = 1")
    fun observeActiveVoices(): Flow<List<AdhanVoiceEntity>>

    @Query("SELECT * FROM adhan_voices WHERE active = 1")
    suspend fun getActiveVoices(): List<AdhanVoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(voices: List<AdhanVoiceEntity>)
}

@Dao
interface AdhkarDao {
    @Query("SELECT * FROM adhkar_items WHERE active = 1 ORDER BY sortOrder ASC")
    fun observeAllActive(): Flow<List<AdhkarEntity>>

    @Query("SELECT * FROM adhkar_items WHERE active = 1 ORDER BY sortOrder ASC")
    suspend fun getAllActive(): List<AdhkarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AdhkarEntity>)
}

@Database(
    entities = [
        MushafPageEntity::class,
        QuranAyahIndexEntity::class,
        ReciterEntity::class,
        CachedAudioEntity::class,
        LocalUserPrefsEntity::class,
        AdhanVoiceEntity::class,
        AdhkarEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MihrabLocalDatabase : RoomDatabase() {
    abstract fun mushafDao(): MushafDao
    abstract fun reciterDao(): ReciterDao
    abstract fun audioCacheDao(): AudioCacheDao
    abstract fun localPrefsDao(): LocalPrefsDao
    abstract fun adhanVoiceDao(): AdhanVoiceDao
    abstract fun adhkarDao(): AdhkarDao

    companion object {
        @Volatile
        private var INSTANCE: MihrabLocalDatabase? = null

        fun getInstance(context: Context): MihrabLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MihrabLocalDatabase::class.java,
                    "mihrab_local_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
