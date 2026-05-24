package com.example.data

import androidx.room.*
import com.example.domain.Element
import com.example.domain.Rarity
import com.example.domain.GearSlot
import com.example.domain.GearAffix
import com.example.domain.PoiType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

// --- TYPE CONVERTERS ---
class GameTypeConverters {
    @TypeConverter
    fun fromElement(value: Element): String = value.name

    @TypeConverter
    fun toElement(value: String): Element = Element.fromString(value)

    @TypeConverter
    fun fromRarity(value: Rarity): String = value.name

    @TypeConverter
    fun toRarity(value: String): Rarity = Rarity.fromString(value)

    @TypeConverter
    fun fromGearSlot(value: GearSlot): String = value.name

    @TypeConverter
    fun toGearSlot(value: String): GearSlot = GearSlot.valueOf(value)

    @TypeConverter
    fun fromPoiType(value: PoiType): String = value.name

    @TypeConverter
    fun toPoiType(value: String): PoiType = PoiType.valueOf(value)

    @TypeConverter
    fun fromAffixList(value: List<GearAffix>): String {
        val array = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("attribute", item.attribute)
            obj.put("value", item.value)
            obj.put("isPercent", item.isPercent)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toAffixList(value: String): List<GearAffix> {
        val list = mutableListOf<GearAffix>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GearAffix(
                        attribute = obj.getString("attribute"),
                        value = obj.getInt("value"),
                        isPercent = obj.optBoolean("isPercent", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(separator = ",")

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        return value.split(",")
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return JSONObject(value).toString()
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(value)
            obj.keys().forEach { key ->
                map[key] = obj.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }
}

// --- ENTITIES ---

@Entity(tableName = "game_profile")
data class GameProfileEntity(
    @PrimaryKey val id: Int = 1,
    val selectedElement: Element,
    val level: Int,
    val xp: Int,
    val gold: Int,
    val abyssalShards: Int,
    var currentLatitude: Double,
    var currentLongitude: Double,
    val activeGuildName: String?,
    val dailyDungeonsCleared: Int,
    val lastClearedDate: Long,
    val completedQuestsCount: Int
)

@Entity(tableName = "heroes")
data class HeroEntity(
    @PrimaryKey val id: String,
    val name: String,
    val element: Element,
    val currentLevel: Int,
    val starRating: Int,
    val xp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val isAscended: Boolean,
    val activeSkillId: String,
    val unlockedSkillTreeNodes: List<String>
)

@Entity(tableName = "gear")
data class GearItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slot: GearSlot,
    val rarity: Rarity,
    val levelReq: Int,
    val basePower: Int,
    val affixes: List<GearAffix>,
    val socketedShardId: String?,
    val socketedShardName: String?,
    val equippedHeroId: String? // null mean inventory
)

@Entity(tableName = "pois")
data class PoiEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: PoiType,
    val element: Element,
    val latitude: Double,
    val longitude: Double,
    val minLevel: Int,
    val maxLevel: Int,
    val lastVisitedTimestamp: Long,
    val isCapturedByGuild: Boolean,
    val capturedGuildName: String?,
    @ColumnInfo(defaultValue = "{}") val osmTags: Map<String, String>,
    val realName: String?,
    @ColumnInfo(defaultValue = "0") val cooldownUntil: Long
)

@Entity(tableName = "guild_info")
data class GuildEntity(
    @PrimaryKey val name: String,
    val level: Int,
    val totalContributors: Int,
    val treasuryGold: Int,
    val treasuryShards: Int,
    val territoryCount: Int
)


// --- DAO ---
@Dao
interface GameDao {
    @Query("SELECT * FROM game_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<GameProfileEntity?>

    @Query("SELECT * FROM game_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: GameProfileEntity)

    @Query("SELECT * FROM heroes")
    fun getAllHeroes(): Flow<List<HeroEntity>>

    @Query("SELECT * FROM heroes")
    suspend fun getAllHeroesSync(): List<HeroEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHero(hero: HeroEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeroes(heroes: List<HeroEntity>)

    @Query("DELETE FROM heroes WHERE id = :heroId")
    suspend fun deleteHero(heroId: String)

    @Query("UPDATE gear SET equippedHeroId = NULL WHERE equippedHeroId = :heroId")
    suspend fun unequipGearForHero(heroId: String)

    @Query("SELECT * FROM gear")
    fun getAllGear(): Flow<List<GearItemEntity>>

    @Query("SELECT * FROM gear")
    suspend fun getAllGearSync(): List<GearItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGear(gear: GearItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGearItems(gearItems: List<GearItemEntity>)

    @Query("DELETE FROM gear WHERE id = :gearId")
    suspend fun deleteGearItem(gearId: String)

    @Query("SELECT * FROM pois")
    fun getAllPOIs(): Flow<List<PoiEntity>>

    @Query("SELECT * FROM pois")
    suspend fun getAllPOIsSync(): List<PoiEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOIs(pois: List<PoiEntity>)

    @Query("DELETE FROM pois")
    suspend fun clearPOIs()

    @Query("SELECT * FROM guild_info")
    fun getGuilds(): Flow<List<GuildEntity>>

    @Query("SELECT * FROM guild_info")
    suspend fun getGuildsSync(): List<GuildEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGuild(guild: GuildEntity)

    @Transaction
    suspend fun clearAllData() {
        clearPOIs()
        // Other clearing
    }
}

// --- DATABASE ---
@Database(
    entities = [
        GameProfileEntity::class,
        HeroEntity::class,
        GearItemEntity::class,
        PoiEntity::class,
        GuildEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(GameTypeConverters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract val gameDao: GameDao
}
