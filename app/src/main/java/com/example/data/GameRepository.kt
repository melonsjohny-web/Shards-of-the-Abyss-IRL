package com.example.data

import com.example.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    // --- PROFILE ---
    val profileFlow: Flow<GameProfileEntity?> = gameDao.getProfile()

    suspend fun getProfileSync(): GameProfileEntity? {
        val profile = gameDao.getProfileSync() ?: return null
        val now = System.currentTimeMillis()
        if (profile.lastClearedDate / 86400000L != now / 86400000L) {
            val resetProfile = profile.copy(
                dailyDungeonsCleared = 0,
                lastClearedDate = now
            )
            gameDao.saveProfile(resetProfile)
            return resetProfile
        }
        return profile
    }

    suspend fun saveProfile(profile: GameProfileEntity) {
        gameDao.saveProfile(profile)
    }

    // --- HEROES ---
    val allHeroesFlow: Flow<List<AwakenedHero>> = gameDao.getAllHeroes().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllHeroesSync(): List<AwakenedHero> =
        gameDao.getAllHeroesSync().map { it.toDomain() }

    suspend fun saveHero(hero: AwakenedHero) {
        gameDao.insertHero(hero.toEntity())
    }

    suspend fun saveHeroes(heroes: List<AwakenedHero>) {
        gameDao.insertHeroes(heroes.map { it.toEntity() })
    }

    suspend fun deleteHero(id: String) {
        gameDao.unequipGearForHero(id)
        gameDao.deleteHero(id)
    }

    // --- GEAR ITEMS ---
    val allGearFlow: Flow<List<GearItem>> = gameDao.getAllGear().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllGearSync(): List<GearItem> =
        gameDao.getAllGearSync().map { it.toDomain() }

    suspend fun saveGearItem(gear: GearItem) {
        gameDao.insertGear(gear.toEntity())
    }

    suspend fun saveGearItems(gearItems: List<GearItem>) {
        gameDao.insertGearItems(gearItems.map { it.toEntity() })
    }

    suspend fun deleteGearItem(id: String) {
        gameDao.deleteGearItem(id)
    }

    // --- POINTS OF INTEREST ---
    val allPOIsFlow: Flow<List<PointOfInterest>> = gameDao.getAllPOIs().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getAllPOIsSync(): List<PointOfInterest> =
        gameDao.getAllPOIsSync().map { it.toDomain() }

    suspend fun savePOIs(pois: List<PointOfInterest>) {
        gameDao.insertPOIs(pois.map { it.toEntity() })
    }

    suspend fun clearPOIs() {
        gameDao.clearPOIs()
    }

    // --- GUILDS ---
    val allGuildsFlow: Flow<List<GuildEntity>> = gameDao.getGuilds()

    suspend fun getAllGuildsSync(): List<GuildEntity> = gameDao.getGuildsSync()

    suspend fun saveGuild(guild: GuildEntity) {
        gameDao.saveGuild(guild)
    }

    // --- MAPPING HELPERS ---
    private fun HeroEntity.toDomain() = AwakenedHero(
        id = id,
        name = name,
        element = element,
        currentLevel = currentLevel,
        starRating = starRating,
        xp = xp,
        maxHp = maxHp,
        attack = attack,
        defense = defense,
        speed = speed,
        isAscended = isAscended,
        activeSkillId = activeSkillId,
        unlockedSkillTreeNodes = unlockedSkillTreeNodes
    )

    private fun AwakenedHero.toEntity() = HeroEntity(
        id = id,
        name = name,
        element = element,
        currentLevel = currentLevel,
        starRating = starRating,
        xp = xp,
        maxHp = maxHp,
        attack = attack,
        defense = defense,
        speed = speed,
        isAscended = isAscended,
        activeSkillId = activeSkillId,
        unlockedSkillTreeNodes = unlockedSkillTreeNodes
    )

    private fun GearItemEntity.toDomain() = GearItem(
        id = id,
        name = name,
        slot = slot,
        rarity = rarity,
        levelReq = levelReq,
        basePower = basePower,
        affixes = affixes,
        socketedShardId = socketedShardId,
        socketedShardName = socketedShardName,
        equippedHeroId = equippedHeroId
    )

    private fun GearItem.toEntity() = GearItemEntity(
        id = id,
        name = name,
        slot = slot,
        rarity = rarity,
        levelReq = levelReq,
        basePower = basePower,
        affixes = affixes,
        socketedShardId = socketedShardId,
        socketedShardName = socketedShardName,
        equippedHeroId = equippedHeroId
    )

    private fun PoiEntity.toDomain() = PointOfInterest(
        id = id,
        name = name,
        type = type,
        element = element,
        latitude = latitude,
        longitude = longitude,
        minLevel = minLevel,
        maxLevel = maxLevel,
        lastVisitedTimestamp = lastVisitedTimestamp,
        isCapturedByGuild = isCapturedByGuild,
        capturedGuildName = capturedGuildName,
        osmTags = osmTags,
        realName = realName,
        cooldownUntil = cooldownUntil
    )

    private fun PointOfInterest.toEntity() = PoiEntity(
        id = id,
        name = name,
        type = type,
        element = element,
        latitude = latitude,
        longitude = longitude,
        minLevel = minLevel,
        maxLevel = maxLevel,
        lastVisitedTimestamp = lastVisitedTimestamp,
        isCapturedByGuild = isCapturedByGuild,
        capturedGuildName = capturedGuildName,
        osmTags = osmTags,
        realName = realName,
        cooldownUntil = cooldownUntil
    )
}
