package com.example

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.GameDatabase
import com.example.data.GameRepository

class AbyssShardsApplication : Application() {

    lateinit var database: GameDatabase
        private set

    lateinit var repository: GameRepository
        private set

    override fun onCreate() {
        super.onCreate()
        
        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE pois ADD COLUMN osmTags TEXT NOT NULL DEFAULT '{}'")
                } catch (e: Exception) {
                    android.util.Log.w("SHARDS_MIGRATION", "Column osmTags might already exist", e)
                }
                try {
                    db.execSQL("ALTER TABLE pois ADD COLUMN realName TEXT")
                } catch (e: Exception) {
                    android.util.Log.w("SHARDS_MIGRATION", "Column realName might already exist", e)
                }
                try {
                    db.execSQL("ALTER TABLE pois ADD COLUMN cooldownUntil INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    android.util.Log.w("SHARDS_MIGRATION", "Column cooldownUntil might already exist", e)
                }
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS poi_cooldowns (poiId TEXT NOT NULL PRIMARY KEY, cooldownUntil INTEGER NOT NULL)")
                } catch (e: Exception) {
                    android.util.Log.w("SHARDS_MIGRATION", "Table poi_cooldowns creation failed or already exists", e)
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pois_cooldownUntil ON pois(cooldownUntil)")
                } catch (e: Exception) {
                    android.util.Log.w("SHARDS_MIGRATION", "Index index_pois_cooldownUntil on pois failed", e)
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_poi_cooldowns_cooldownUntil ON poi_cooldowns(cooldownUntil)")
                } catch (e: Exception) {
                    android.util.Log.w("SHARDS_MIGRATION", "Index index_poi_cooldowns_cooldownUntil on poi_cooldowns failed", e)
                }
            }
        }

        try {
            database = Room.databaseBuilder(
                applicationContext,
                GameDatabase::class.java,
                "shards_of_abyss_db"
            )
            .addMigrations(migration1To2)
            .fallbackToDestructiveMigration()
            .build()
            
            // Force verify DB creation/migration is sane on launch
            database.openHelper.writableDatabase
        } catch (t: Throwable) {
            android.util.Log.e("SHARDS_INIT", "Database migration/validation failed, performing destructive recovery in App", t)
            try {
                applicationContext.deleteDatabase("shards_of_abyss_db")
            } catch (de: Exception) {
                android.util.Log.e("SHARDS_INIT", "Failed to clear database files", de)
            }
            database = Room.databaseBuilder(
                applicationContext,
                GameDatabase::class.java,
                "shards_of_abyss_db"
            )
            .addMigrations(migration1To2)
            .fallbackToDestructiveMigration()
            .build()
            database.openHelper.writableDatabase
        }

        repository = GameRepository(database.gameDao)
    }
}
