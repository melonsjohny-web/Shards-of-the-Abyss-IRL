package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import kotlin.math.min
import kotlin.random.Random

// Sealed state for the overall game structure
sealed interface GameUiState {
    object LaunchSelection : GameUiState
    data class Loaded(
        val profile: GameProfileEntity,
        val heroes: List<AwakenedHero>,
        val inventory: List<GearItem>,
        val pois: List<PointOfInterest>,
        val guilds: List<GuildEntity>,
        val weather: WeatherCondition,
        val isNightMode: Boolean,
        val activeBattle: ActiveBattleState?,
        val message: String? = null
    ) : GameUiState
}

// Full Combat State representation
data class ActiveBattleState(
    val poiId: String,
    val poiName: String,
    val poiType: PoiType,
    val element: Element,
    val playerUnits: List<BattleUnit>,
    val enemyUnits: List<BattleUnit>,
    val turnOrder: List<String>, // list of BattleUnit uid
    val activeUnitUid: String?,
    val combatLog: List<String>,
    val qteState: CombatQteState,
    val isFinished: Boolean = false,
    val isVictory: Boolean = false,
    val xpReward: Int = 0,
    val goldReward: Int = 0,
    val shardReward: Int = 0,
    val gearReward: GearItem? = null
) : Serializable

class GameViewModel(
    private val context: Context,
    private val repository: GameRepository
) : ViewModel() {

    // Current menu screen: "MAP", "PARTY", "FORGE", "SUMMON", "GUILD", "SETTINGS"
    private val _currentTab = MutableStateFlow("MAP")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Sub-viewmodels split for modularity
    val mapViewModel = MapViewModel(context, repository, viewModelScope, ::showToast)
    val combatViewModel = CombatViewModel(repository, viewModelScope, ::showToast)
    val forgeViewModel = ForgeViewModel(repository, viewModelScope, ::showToast)

    // Delegated state flows
    val isRealGpsEnabled: StateFlow<Boolean> = mapViewModel.isRealGpsEnabled
    val currentWeather: StateFlow<WeatherCondition> = mapViewModel.currentWeather
    val isNight: StateFlow<Boolean> = mapViewModel.isNight
    val activeBattle: StateFlow<ActiveBattleState?> = combatViewModel.activeBattle

    // Feedback toast messages
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    // Standard hourly etheric count for offline dungeons
    private val _remainingEtherDungeons = MutableStateFlow(10)
    val remainingEtherDungeons: StateFlow<Int> = _remainingEtherDungeons.asStateFlow()

    // Combined overall state
    val uiState: StateFlow<GameUiState> = combine(
        repository.profileFlow,
        repository.allHeroesFlow,
        repository.allGearFlow,
        repository.allPOIsFlow,
        repository.allGuildsFlow,
        currentWeather,
        isNight,
        activeBattle,
        _feedbackMessage
    ) { array ->
        val profile = array[0] as? GameProfileEntity
        @Suppress("UNCHECKED_CAST")
        val heroes = array[1] as? List<AwakenedHero> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val gear = array[2] as? List<GearItem> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val pois = array[3] as? List<PointOfInterest> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val guilds = array[4] as? List<GuildEntity> ?: emptyList()
        val weather = array[5] as? WeatherCondition ?: WeatherCondition.CLEAR
        val isNight = array[6] as? Boolean ?: false
        val battle = array[7] as? ActiveBattleState
        val msg = array[8] as? String

        if (profile == null) {
            GameUiState.LaunchSelection
        } else {
            GameUiState.Loaded(
                profile = profile,
                heroes = heroes,
                inventory = gear,
                pois = pois,
                guilds = guilds,
                weather = weather,
                isNightMode = isNight,
                activeBattle = battle,
                message = msg
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameUiState.LaunchSelection)

    init {
        // Start Combat Battle Timer thread with weather-boosting check callback
        combatViewModel.startTicker { elem ->
            val cw = mapViewModel.currentWeather.value
            when (cw) {
                WeatherCondition.CLEAR -> elem == Element.AETHER
                WeatherCondition.SNOWY -> elem == Element.ICE
                WeatherCondition.RAIN -> elem == Element.ICE || elem == Element.BLOOM
                WeatherCondition.HOT_WAVE -> elem == Element.BLAZE
                WeatherCondition.FOGGY -> elem == Element.MIST
                WeatherCondition.WINDY -> elem == Element.MIST
            }
        }
    }

    // Tab control
    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // Feedback toast mechanism
    fun showToast(msg: String) {
        _feedbackMessage.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_feedbackMessage.value == msg) {
                _feedbackMessage.value = null
            }
        }
    }

    // --- CHARACTER SELECT COVENANT ---
    fun selectStartingCovenant(element: Element) {
        viewModelScope.launch {
            val initialProfile = GameProfileEntity(
                selectedElement = element,
                level = 1,
                xp = 0,
                gold = 1000,
                abyssalShards = 50,
                currentLatitude = 55.7558,
                currentLongitude = 37.6173,
                activeGuildName = "Первые Пробуждённые",
                dailyDungeonsCleared = 0,
                lastClearedDate = System.currentTimeMillis(),
                completedQuestsCount = 0
            )
            repository.saveProfile(initialProfile)

            val starterHero1 = AwakenedHero(
                id = "hero_lead_${element.name.lowercase()}",
                name = "${element.god} (Аватар)",
                element = element,
                currentLevel = 5,
                starRating = 2,
                xp = 120,
                maxHp = 150,
                attack = 30,
                defense = 15,
                speed = 18,
                activeSkillId = "elemental_blast"
            )

            val starterHero2 = AwakenedHero(
                id = "hero_aether_scout",
                name = "Эфирный Кадет",
                element = Element.AETHER,
                currentLevel = 4,
                starRating = 1,
                xp = 80,
                maxHp = 130,
                attack = 25,
                defense = 12,
                speed = 20,
                activeSkillId = "swift_cut"
            )

            repository.saveHeroes(listOf(starterHero1, starterHero2))

            val weapon = GearItem(
                id = "gear_init_weapon",
                name = "Меч Инициации",
                slot = GearSlot.WEAPON,
                rarity = Rarity.COMMON,
                levelReq = 1,
                basePower = 15,
                affixes = listOf(GearAffix("Сила", 5)),
                equippedHeroId = starterHero1.id
            )

            val armor = GearItem(
                id = "gear_init_armor",
                name = "Плащ Новичка",
                slot = GearSlot.ARMOR,
                rarity = Rarity.COMMON,
                levelReq = 1,
                basePower = 20,
                affixes = listOf(GearAffix("ОЗ", 25)),
                equippedHeroId = starterHero1.id
            )

            val ring = GearItem(
                id = "gear_init_ring",
                name = "Кольцо Бездны",
                slot = GearSlot.RING_1,
                rarity = Rarity.RARE,
                levelReq = 1,
                basePower = 10,
                affixes = listOf(GearAffix("Крит Шанс", 4, isPercent = true)),
                equippedHeroId = starterHero2.id
            )

            repository.saveGearItems(listOf(weapon, armor, ring))

            mapViewModel.loadPOIsForPosition(initialProfile.currentLatitude, initialProfile.currentLongitude)

            repository.saveGuild(GuildEntity("Вороны Смерти", 12, 18, 50000, 600, 3))
            repository.saveGuild(GuildEntity("Первые Пробуждённые", 8, 14, 25000, 250, 1))
            repository.saveGuild(GuildEntity("Орден Творцов", 15, 34, 120000, 2000, 8))

            showToast("Вы присягнули ковенанту: ${element.god}!")
        }
    }

    // --- DELEGATED MAP METHODS ---
    fun toggleNightMode() = mapViewModel.toggleNightMode()
    fun toggleWeatherSimulation() = mapViewModel.toggleWeatherSimulation()
    fun toggleRealGpsTracker(enabled: Boolean) = mapViewModel.toggleRealGpsTracker(enabled)
    fun triggerVirtualMove(dir: String) = mapViewModel.triggerVirtualMove(dir)
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return mapViewModel.calculateDistance(lat1, lon1, lat2, lon2)
    }

    // --- DELEGATED COMBAT METHODS ---
    fun initiatePOICombat(poi: PointOfInterest) {
        // Prevent starting combat if there is already an active battle
        if (combatViewModel.activeBattle.value != null) return
        combatViewModel.initiatePOICombat(poi)
        selectTab("COMBAT_SCREEN")
    }

    fun playerTriggerSkillAttack() = combatViewModel.playerTriggerSkillAttack()
    fun playerFinishQTEHit(successRatio: Float) = combatViewModel.playerFinishQTEHit(successRatio)
    fun feedSwipeResult(correct: Boolean) = combatViewModel.feedSwipeResult(correct)
    fun playerDefenseGuard() = combatViewModel.playerDefenseGuard()
    fun runFromBattle() = combatViewModel.runFromBattle { selectTab("MAP") }
    fun claimBattleRewardsExit() = combatViewModel.claimBattleRewardsExit { selectTab("MAP") }

    // --- DELEGATED FORGE & INVENTORY METHODS ---
    fun dismantleItem(gear: GearItem) = forgeViewModel.dismantleItem(gear)
    fun craftNewItem(slot: GearSlot) = forgeViewModel.craftNewItem(slot)
    fun reforgeEquipmentAffixes(gear: GearItem) = forgeViewModel.reforgeEquipmentAffixes(gear)
    fun equipItemToHero(gear: GearItem, heroId: String?) = forgeViewModel.equipItemToHero(gear, heroId)

    // --- HERO PROGRESSION ---
    fun levelUpHeroThroughShards(hero: AwakenedHero) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val costCoins = hero.currentLevel * 100
            val costShards = hero.currentLevel + 2

            if (prof.gold < costCoins || prof.abyssalShards < costShards) {
                showToast("Недостаточно ресурсов! Нужно: $costCoins Золота + $costShards Осколков.")
                return@launch
            }

            val upHero = hero.copy(
                currentLevel = hero.currentLevel + 1,
                maxHp = hero.maxHp + 15,
                attack = hero.attack + 4,
                defense = hero.defense + 2,
                speed = hero.speed + 1
            )
            repository.saveHero(upHero)

            val updatedProf = prof.copy(
                gold = prof.gold - costCoins,
                abyssalShards = prof.abyssalShards - costShards
            )
            repository.saveProfile(updatedProf)

            showToast("🎉 Герой ${hero.name} успешно повысил уровень до ${upHero.currentLevel}!")
        }
    }

    fun ascendHeroStars(hero: AwakenedHero) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val costShards = 100
            if (prof.abyssalShards < costShards) {
                showToast("Для Возвышения Звёзд требуется 100 Осколков Силы!")
                return@launch
            }

            val upHero = hero.copy(
                starRating = min(5, hero.starRating + 1),
                attack = hero.attack + 12,
                maxHp = hero.maxHp + 45
            )
            repository.saveHero(upHero)

            val updatedProf = prof.copy(abyssalShards = prof.abyssalShards - costShards)
            repository.saveProfile(updatedProf)

            showToast("⭐️ ${hero.name} возвышен до ${upHero.starRating}-звёздного ранга!")
        }
    }

    fun triggerOfflineEtherDungeon() {
        viewModelScope.launch {
            val limit = _remainingEtherDungeons.value
            if (limit <= 0) {
                showToast("Вы исчерпали лимит Эфирных данжей сегодня (макс 10/день)!")
                return@launch
            }

            val dummyPoi = PointOfInterest(
                id = "ether_dungeon_${System.currentTimeMillis()}",
                name = "Эфирный Астрал Бездны (Оффлайн)",
                type = PoiType.RIFT,
                element = Element.AETHER,
                latitude = 0.0,
                longitude = 0.0,
                minLevel = 5,
                maxLevel = 15
            )

            _remainingEtherDungeons.value = limit - 1
            initiatePOICombat(dummyPoi)
        }
    }

    // --- PORTAL GACHA SUMMONS ---
    fun executeVoidPortalSummon() {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val summonCost = 15
            if (prof.abyssalShards < summonCost) {
                showToast("Недостаточно осколков! Требуется 15 ед. для открытия Врат.")
                return@launch
            }

            val generatorNames = listOf(
                "Громовой Странник", "Адепт Вечной Мерзлоты", "Жрец Живого Леса",
                "Разрушитель Магмы", "Вестник Звёздного Моста", "Паладин Светлого Домена",
                "Монах Срединных Путей", "Тень Безликого"
            )
            val summonElem = Element.entries.random()
            val summonName = generatorNames.random()

            val newHeroId = "hero_${System.currentTimeMillis()}"
            val newlySummoned = AwakenedHero(
                id = newHeroId,
                name = summonName,
                element = summonElem,
                currentLevel = 1,
                starRating = Random.nextInt(1, 4),
                maxHp = 100 + Random.nextInt(10, 50),
                attack = 18 + Random.nextInt(2, 10),
                defense = 10 + Random.nextInt(1, 6),
                speed = 12 + Random.nextInt(2, 10)
            )

            repository.saveHero(newlySummoned)

            val updatedProf = prof.copy(abyssalShards = prof.abyssalShards - summonCost)
            repository.saveProfile(updatedProf)

            showToast("🌌 ПРИЗЫВ! Из Врат Хаоса явился: ${newlySummoned.name} (${newlySummoned.element.title})!")
        }
    }

    // --- SOCIETAL COVENANTS ---
    fun donateToCovenantGuild(guild: GuildEntity) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            if (prof.gold < 200) {
                showToast("Требуется минимум 200 золота для взноса!")
                return@launch
            }

            val updatedProf = prof.copy(gold = prof.gold - 200)
            repository.saveProfile(updatedProf)

            val updatedGuild = guild.copy(
                treasuryGold = guild.treasuryGold + 200,
                totalContributors = guild.totalContributors + 1
            )
            repository.saveGuild(updatedGuild)

            showToast("Вклад внесен! Вы укрепили доминирование ковенанта ${guild.name}!")
        }
    }

    fun captureNexusTerritory(poi: PointOfInterest) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val guildName = prof.activeGuildName ?: "Авангард Эфира"

            val updatedPoi = poi.copy(
                isCapturedByGuild = true,
                capturedGuildName = guildName
            )
            repository.savePOIs(listOf(updatedPoi))
            showToast("🎉 БРАВО! Вы захватили Узел Силы для гильдии '$guildName'!")
        }
    }

    // --- IMPORT / EXPORT BACKUP ---
    suspend fun exportBackupJson(): String {
        val rootObj = JSONObject()
        try {
            val prof = repository.getProfileSync() ?: return ""
            val profJson = JSONObject().apply {
                put("element", prof.selectedElement.name)
                put("level", prof.level)
                put("xp", prof.xp)
                put("gold", prof.gold)
                put("shards", prof.abyssalShards)
                put("lat", prof.currentLatitude)
                put("lon", prof.currentLongitude)
                put("guild", prof.activeGuildName)
            }
            rootObj.put("profile", profJson)

            val heroesArr = JSONArray()
            val heroes = repository.getAllHeroesSync()
            for (h in heroes) {
                val hJson = JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("element", h.element.name)
                    put("level", h.currentLevel)
                    put("star", h.starRating)
                    put("xp", h.xp)
                    put("maxHp", h.maxHp)
                    put("attack", h.attack)
                    put("defense", h.defense)
                    put("speed", h.speed)
                }
                heroesArr.put(hJson)
            }
            rootObj.put("heroes", heroesArr)

            val gearArr = JSONArray()
            val gear = repository.getAllGearSync()
            for (g in gear) {
                val gJson = JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("slot", g.slot.name)
                    put("rarity", g.rarity.name)
                    put("lvlReq", g.levelReq)
                    put("power", g.basePower)
                    put("equipped", g.equippedHeroId)
                }
                gearArr.put(gJson)
            }
            rootObj.put("gear", gearArr)
            rootObj.put("exportTime", System.currentTimeMillis())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rootObj.toString()
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonString)
                val prof = root.getJSONObject("profile")

                val updatedProf = GameProfileEntity(
                    selectedElement = Element.fromString(prof.getString("element")),
                    level = prof.getInt("level"),
                    xp = prof.getOptInt("xp", 0),
                    gold = prof.getInt("gold"),
                    abyssalShards = prof.getInt("shards"),
                    currentLatitude = prof.getDouble("lat"),
                    currentLongitude = prof.getDouble("lon"),
                    activeGuildName = if (prof.isNull("guild")) null else prof.optString("guild"),
                    dailyDungeonsCleared = 2,
                    lastClearedDate = System.currentTimeMillis(),
                    completedQuestsCount = 1
                )
                repository.saveProfile(updatedProf)

                val heroesArr = root.getJSONArray("heroes")
                val importedHeroes = mutableListOf<AwakenedHero>()
                for (i in 0 until heroesArr.length()) {
                    val h = heroesArr.getJSONObject(i)
                    importedHeroes.add(
                        AwakenedHero(
                            id = h.getString("id"),
                            name = h.getString("name"),
                            element = Element.fromString(h.getString("element")),
                            currentLevel = h.getInt("level"),
                            starRating = h.getInt("star"),
                            xp = h.getOptInt("xp", 0),
                            maxHp = h.getInt("maxHp"),
                            attack = h.getInt("attack"),
                            defense = h.getInt("defense"),
                            speed = h.getInt("speed")
                        )
                    )
                }
                repository.saveHeroes(importedHeroes)

                val gearArr = root.getJSONArray("gear")
                val importedGear = mutableListOf<GearItem>()
                for (i in 0 until gearArr.length()) {
                    val g = gearArr.getJSONObject(i)
                    importedGear.add(
                        GearItem(
                            id = g.getString("id"),
                            name = g.getString("name"),
                            slot = GearSlot.valueOf(g.getString("slot")),
                            rarity = Rarity.fromString(g.getString("rarity")),
                            levelReq = g.getInt("lvlReq"),
                            basePower = g.getInt("power"),
                            affixes = emptyList(),
                            equippedHeroId = if (g.isNull("equipped")) null else g.getString("equipped")
                        )
                    )
                }
                repository.saveGearItems(importedGear)

                mapViewModel.loadPOIsForPosition(updatedProf.currentLatitude, updatedProf.currentLongitude)
                showToast("Синхронизация успешно выполнена! Локальный сейв обновлен.")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Ошибка импорта! Неверный формат файла бекапа.")
            }
        }
    }

    private fun JSONObject.getOptInt(key: String, fallback: Int): Int {
        return if (has(key)) getInt(key) else fallback
    }

    override fun onCleared() {
        super.onCleared()
        mapViewModel.clear()
        combatViewModel.clear()
        forgeViewModel.clear()
    }
}
