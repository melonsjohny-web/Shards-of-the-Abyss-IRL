package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.domain.*
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import kotlin.math.*
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

    // Current menu screen: "MAP", "PARTY", "FORGE", "SUMMON", "GUILD", "CAMPAIGN", "SETTINGS"
    private val _currentTab = MutableStateFlow("MAP")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Real vs Virtual GPS Tracker
    private val _isRealGpsEnabled = MutableStateFlow(false)
    val isRealGpsEnabled: StateFlow<Boolean> = _isRealGpsEnabled.asStateFlow()

    // Environment/Weather properties
    private val _currentWeather = MutableStateFlow(WeatherCondition.CLEAR)
    val currentWeather: StateFlow<WeatherCondition> = _currentWeather.asStateFlow()

    private val _isNight = MutableStateFlow(false)
    val isNight: StateFlow<Boolean> = _isNight.asStateFlow()

    // Active screen feedback message
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    // Primary state flows merged from DB
    private val _activeBattle = MutableStateFlow<ActiveBattleState?>(null)
    val activeBattle: StateFlow<ActiveBattleState?> = _activeBattle.asStateFlow()

    // Standard hourly etheric count for offline dungeons
    private val _remainingEtherDungeons = MutableStateFlow(10)
    val remainingEtherDungeons: StateFlow<Int> = _remainingEtherDungeons.asStateFlow()

    // Timer jobs
    private val fuzzerJob: Job
    private val battleTickerJob: Job
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    // Main combined UI State
    val uiState: StateFlow<GameUiState> = combine(
        repository.profileFlow,
        repository.allHeroesFlow,
        repository.allGearFlow,
        repository.allPOIsFlow,
        repository.allGuildsFlow,
        _currentWeather,
        _isNight,
        _activeBattle,
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
        // Automatically check if day or night according to Android phone time
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        _isNight.value = hour < 6 || hour >= 18

        // Start weather auto-rotator simulating real changing outdoor factors
        fuzzerJob = viewModelScope.launch {
            while (true) {
                delay(300000) // change weather every 5 mins
                cycleWeather()
            }
        }

        // Action point battle incremental clock
        battleTickerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                tickBattleTimer()
            }
        }

        // Initialize GPS client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    private fun cycleWeather() {
        val nextWeather = WeatherCondition.entries.random()
        _currentWeather.value = nextWeather
        viewModelScope.launch {
            val prof = repository.getProfileSync()
            if (prof != null) {
                showToast("Погода за окном изменилась: ${nextWeather.title}!")
            }
        }
    }

    fun toggleNightMode() {
        _isNight.value = !_isNight.value
        showToast("Смена времени суток: ${if (_isNight.value) "🌙 Ночь Бездны" else "☀️ Солнечный День"}")
    }

    fun toggleWeatherSimulation() {
        val nextIndex = (currentWeather.value.ordinal + 1) % WeatherCondition.entries.size
        _currentWeather.value = WeatherCondition.entries[nextIndex]
        showToast("Новая аура погоды: ${currentWeather.value.title}")
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun showToast(msg: String) {
        _feedbackMessage.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_feedbackMessage.value == msg) {
                _feedbackMessage.value = null
            }
        }
    }

    // --- CHARACTER INCEPTION & COVENANT ---
    fun selectStartingCovenant(element: Element) {
        viewModelScope.launch {
            // Setup base profile
            val initialProfile = GameProfileEntity(
                selectedElement = element,
                level = 1,
                xp = 0,
                gold = 1000,
                abyssalShards = 50,
                currentLatitude = 55.7558,  // Default Moscow center coordinates
                currentLongitude = 37.6173,
                activeGuildName = "Первые Пробуждённые",
                dailyDungeonsCleared = 0,
                lastClearedDate = System.currentTimeMillis(),
                completedQuestsCount = 0
            )
            repository.saveProfile(initialProfile)

            // Setup 3 characters: Elemental leader + Aether recruit + alternate element
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

            // Provide starting equipment
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

            // Initialize default surrounding GPS points
            generateSurroundingPOIs(initialProfile.currentLatitude, initialProfile.currentLongitude)

            // Add standard guilds to SQLite
            repository.saveGuild(GuildEntity("Вороны Смерти", 12, 18, 50000, 600, 3))
            repository.saveGuild(GuildEntity("Первые Пробуждённые", 8, 14, 25000, 250, 1))
            repository.saveGuild(GuildEntity("Орден Творцов", 15, 34, 120000, 2000, 8))

            showToast("Вы присягнули ковенанту: ${element.god}!")
        }
    }

    // --- procedurual placement ---
    private suspend fun generateSurroundingPOIs(lat: Double, lon: Double) {
        val namesIce = listOf("Замёрзшая Расселина", "Алтарь Аурелии", "Холодные Врата Эфира")
        val namesBloom = listOf("Цветущая Роща Мизу", "Сады Спокойствия", "Оазис Пробужденных")
        val namesBlaze = listOf("Жертвенный Утес Герра", "Кузница Пепла", "Квартал Гнева Стихии")
        val namesMist = listOf("Туманная Преграда", "Мост Потерянных Душ", "Мавзолей Ветров")
        val namesAether = listOf("Узел Соединения Эфира", "Центральное Око Силы", "Осколочный Обелиск")

        val newPois = mutableListOf<PointOfInterest>()
        val types = PoiType.entries

        // Generate 6 procedural landmarks around center lat/lon
        for (i in 0 until 8) {
            val type = types[i % types.size]
            val element = Element.entries[i % Element.entries.size]
            
            // Randomly offset coordinates between 50m to 600m
            val latOffset = (Random.nextDouble() - 0.5) * 0.006 // ~300m max
            val lonOffset = (Random.nextDouble() - 0.5) * 0.010

            val targetLat = lat + latOffset
            val targetLon = lon + lonOffset

            val rName = when (element) {
                Element.ICE -> namesIce.random()
                Element.BLOOM -> namesBloom.random()
                Element.BLAZE -> namesBlaze.random()
                Element.MIST -> namesMist.random()
                Element.AETHER -> namesAether.random()
            } + " (Ур. ${(i * 3) + 2})"

            newPois.add(
                PointOfInterest(
                    id = "poi_proc_${i}_${System.currentTimeMillis() % 1000}",
                    name = rName,
                    type = type,
                    element = element,
                    latitude = targetLat,
                    longitude = targetLon,
                    minLevel = (i * 3) + 1,
                    maxLevel = (i * 3) + 5,
                    isCapturedByGuild = (i % 3 == 0),
                    capturedGuildName = if (i % 3 == 0) "Орден Творцов" else null
                )
            )
        }
        repository.savePOIs(newPois)
    }

    // --- PLAY DEVICE OR SIMULATED INTERACTIVE NAVIGATION ---
    fun toggleRealGpsTracker(enabled: Boolean) {
        _isRealGpsEnabled.value = enabled
        if (enabled) {
            setupLocationListener()
            showToast("Включена геолокация смартфона (GPS)")
        } else {
            removeLocationListener()
            showToast("Активирован ручной контроллер перемещения")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationListener() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 8000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(4000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                updateProfileLocation(loc.latitude, loc.longitude)
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Throwable) {
            Log.e("Shards", "GPS request failed", e)
            _isRealGpsEnabled.value = false
            showToast("Ошибка GPS датчика. Проверьте разрешения!")
        }
    }

    private fun removeLocationListener() {
        locationCallback?.let {
            try {
                fusedLocationClient?.removeLocationUpdates(it)
            } catch (e: Throwable) {
                Log.e("Shards", "Failed to remove GPS listener", e)
            }
        }
        locationCallback = null
    }

    // Move player in degrees (~100 meters per step)
    fun triggerVirtualMove(dir: String) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            var newLat = prof.currentLatitude
            var newLon = prof.currentLongitude

            val step = 0.0009 // approximate 100m
            when (dir) {
                "NORTH" -> newLat += step
                "SOUTH" -> newLat -= step
                "EAST" -> newLon += step * 1.2
                "WEST" -> newLon -= step * 1.2
            }

            updateProfileLocation(newLat, newLon)
            showToast("Прогулка: перемещение на ${translateDirection(dir)}")
        }
    }

    private fun translateDirection(dir: String) = when (dir) {
        "NORTH" -> "Север ⬆️"
        "SOUTH" -> "Юг ⬇️"
        "EAST" -> "Восток ➡️"
        else -> "Запад ⬅️"
    }

    private fun updateProfileLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val updated = prof.copy(currentLatitude = lat, currentLongitude = lon)
            repository.saveProfile(updated)

            // Let's decide if we clear and replenish spawned POIs when walked very far
            val dist = calculateDistance(prof.currentLatitude, prof.currentLongitude, lat, lon)
            if (dist > 1500f) {
                generateSurroundingPOIs(lat, lon)
                showToast("Вы обнаружили неизведанный сектор! Рождены новые разломы.")
            }
        }
    }

    // Distance in meters using Spherical law of cosines
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0 // Earth's radius in meters
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(radLat1) * cos(radLat2) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }


    // --- COMBAT SYSTEM: TURN BASED PROCEDURAL SYSTEM ---
    fun initiatePOICombat(poi: PointOfInterest) {
        viewModelScope.launch {
            val heroes = repository.getAllHeroesSync()
            if (heroes.isEmpty()) {
                showToast("У вас нет призванных героев! Активируйте призыв.")
                return@launch
            }

            // Map up to 3 heroes to combat squad
            val playerUnits = heroes.take(3).map { hero ->
                // Calculate combat values including any equipped items
                val gearValue = calculateGearBuffForHero(hero.id)
                BattleUnit(
                    uid = "player_${hero.id}",
                    id = hero.id,
                    name = hero.name,
                    isHero = true,
                    currentHp = hero.maxHp + gearValue.hp,
                    maxHp = hero.maxHp + gearValue.hp,
                    attack = hero.attack + gearValue.atk,
                    defense = hero.defense + gearValue.def,
                    speed = hero.speed + gearValue.spd,
                    element = hero.element
                )
            }

            // Create procedural enemy units based on POI level
            val elements = Element.entries
            val enemiesCount = if (poi.type == PoiType.ABYSSAL_GATE || poi.type == PoiType.NEXUS_POINT) 1 else Random.nextInt(2, 4)
            val enemyUnits = (1..enemiesCount).map { i ->
                val enemyElem = elements[(poi.element.ordinal + i) % elements.size]
                val enemyLvl = Random.nextInt(poi.minLevel, poi.maxLevel + 1)
                val health = 50 + enemyLvl * 15
                val baseAtk = 8 + enemyLvl * 3
                val baseDef = 4 + enemyLvl * 2
                val velocity = 8 + enemyLvl

                BattleUnit(
                    uid = "enemy_${poi.id}_$i",
                    id = "monster_$i",
                    name = if (enemiesCount == 1) "🔥 Вожак Бездны (${poi.element.title})" else "Приспешник Хаоса №$i",
                    isHero = false,
                    currentHp = health,
                    maxHp = health,
                    attack = baseAtk,
                    defense = baseDef,
                    speed = velocity,
                    element = enemyElem
                )
            }

            val fullTurnList = mutableListOf<String>()
            _activeBattle.value = ActiveBattleState(
                poiId = poi.id,
                poiName = poi.name,
                poiType = poi.type,
                element = poi.element,
                playerUnits = playerUnits,
                enemyUnits = enemyUnits,
                turnOrder = fullTurnList,
                activeUnitUid = null,
                combatLog = listOf("Разлом открыт! Инициализация тактических полей воителей."),
                qteState = CombatQteState(active = false)
            )

            _currentTab.value = "COMBAT_SCREEN"
        }
    }

    private class StatPack(val hp: Int = 0, val atk: Int = 0, val def: Int = 0, val spd: Int = 0)

    private suspend fun calculateGearBuffForHero(heroId: String): StatPack {
        val gears = repository.getAllGearSync().filter { it.equippedHeroId == heroId }
        var extraHp = 0
        var extraAtk = 0
        var extraDef = 0
        var extraSpd = 0

        for (g in gears) {
            val powerBonus = g.basePower
            when (g.slot) {
                GearSlot.WEAPON -> extraAtk += powerBonus
                GearSlot.HELMET -> extraHp += powerBonus * 3
                GearSlot.ARMOR -> extraDef += powerBonus
                GearSlot.GLOVES -> extraAtk += powerBonus / 2
                GearSlot.BOOTS -> extraSpd += powerBonus / 5
                GearSlot.AMULET -> extraHp += powerBonus * 2
                else -> extraAtk += powerBonus / 3
            }
            // Check affixes
            for (aff in g.affixes) {
                when (aff.attribute) {
                    "Сила" -> extraAtk += aff.value
                    "ОЗ" -> extraHp += aff.value
                    "Защита" -> extraDef += aff.value
                    "Скорость" -> extraSpd += aff.value
                }
            }
        }
        return StatPack(extraHp, extraAtk, extraDef, extraSpd)
    }

    // Tick AP meters and trigger next move
    private fun tickBattleTimer() {
        val current = _activeBattle.value ?: return
        if (current.isFinished || current.activeUnitUid != null) return

        val pUnits = current.playerUnits
        val eUnits = current.enemyUnits

        // Charge AP based on Speed and current atmospheric conditions
        for (unit in pUnits + eUnits) {
            if (unit.currentHp <= 0) continue
            // Standard AP increment
            var increment = unit.speed * 0.1f

            // Elemental condition check
            val weatherCheck = isWeatherBoostingElement(unit.element)
            if (weatherCheck) {
                increment *= 1.25f // 25% faster AP in favorable environment
            }

            unit.currentActionPoints += increment
        }

        // Find unit with AP over threshold
        val nextAct = (pUnits + eUnits).filter { it.currentHp > 0 && it.currentActionPoints >= 100f }
            .maxByOrNull { it.currentActionPoints }

        if (nextAct != null) {
            nextAct.currentActionPoints = 0f
            val updateLog = current.combatLog.toMutableList().apply {
                add("Ход принадлежит: ${nextAct.name} (${nextAct.element.title})")
            }

            _activeBattle.value = current.copy(
                activeUnitUid = nextAct.uid,
                combatLog = updateLog
            )

            // If it is enemy's turn, trigger quick automated monster action after a short lag
            if (!nextAct.isHero) {
                viewModelScope.launch {
                    delay(1200)
                    triggerMonsterTurn(nextAct)
                }
            }
        }
    }

    private fun isWeatherBoostingElement(elem: Element): Boolean {
        val currentW = currentWeather.value
        return when (currentW) {
            WeatherCondition.CLEAR -> elem == Element.AETHER
            WeatherCondition.SNOWY -> elem == Element.ICE
            WeatherCondition.RAIN -> elem == Element.ICE || elem == Element.BLOOM
            WeatherCondition.HOT_WAVE -> elem == Element.BLAZE
            WeatherCondition.FOGGY -> elem == Element.MIST
            WeatherCondition.WINDY -> elem == Element.MIST
        }
    }

    // Execute Monster attacks and trigger player block Swipe QTE
    private fun triggerMonsterTurn(monster: BattleUnit) {
        val current = _activeBattle.value ?: return
        if (current.isFinished) return

        // Target a living hero
        val livingHeroes = current.playerUnits.filter { it.currentHp > 0 }
        if (livingHeroes.isEmpty()) {
            resolveBattleFinish(victory = false)
            return
        }

        val target = livingHeroes.random()
        val directions = listOf("UP", "DOWN", "LEFT", "RIGHT")
        val randomDir = directions.random()

        // Init Block swipe QTE
        _activeBattle.value = current.copy(
            qteState = CombatQteState(
                active = true,
                type = QteType.BLOCK_SWIPE,
                promptText = "Защита! Проведите Свайп: $randomDir",
                targetDirection = randomDir,
                timeLeftMs = 1200
            )
        )

        // Give the player 1.2 seconds to hit block swipe
        viewModelScope.launch {
            delay(1200)
            val checkQte = _activeBattle.value?.qteState ?: return@launch
            executeMonsterAttackBlow(monster, target, blockSuccessful = checkQte.multiplier == 0f)
        }
    }

    private fun executeMonsterAttackBlow(monster: BattleUnit, target: BattleUnit, blockSuccessful: Boolean) {
        val current = _activeBattle.value ?: return
        var damage = max(5, monster.attack - target.defense)

        val logText = if (blockSuccessful) {
            damage = 0
            "🛡️ ${target.name} УСПЕШНО ОТРАЗИЛ атаку ${monster.name} Свайпом QTE!"
        } else {
            target.currentHp = max(0, target.currentHp - damage)
            "💥 ${monster.name} КУСАЕТ ${target.name} за $damage ед. урона."
        }

        val log = current.combatLog.toMutableList()
        log.add(logText)
        if (target.currentHp <= 0) {
            log.add("💀 Воин ${target.name} повержен!")
        }

        _activeBattle.value = current.copy(
            activeUnitUid = null,
            qteState = CombatQteState(active = false),
            combatLog = log
        )

        checkBattleFinishConditions()
    }

    // Player action inputs
    fun playerTriggerSkillAttack() {
        val current = _activeBattle.value ?: return
        val activeUid = current.activeUnitUid ?: return
        val actor = current.playerUnits.find { it.uid == activeUid } ?: return

        // Initiate Attack Shrinking Circle QTE
        _activeBattle.value = current.copy(
            qteState = CombatQteState(
                active = true,
                type = QteType.ATTACK_RING,
                promptText = "🎯 ТАП в идеальной границе сужения кольца!",
                timeLeftMs = 1500
            )
        )
    }

    fun playerFinishQTEHit(successRatio: Float) {
        val current = _activeBattle.value ?: return
        val activeUid = current.activeUnitUid ?: return
        val actor = current.playerUnits.find { it.uid == activeUid } ?: return

        val livingEnemies = current.enemyUnits.filter { it.currentHp > 0 }
        if (livingEnemies.isEmpty()) {
            checkBattleFinishConditions()
            return
        }

        // Mult targets Boss or first living enemy
        val target = livingEnemies.first()

        // Double damage if timed perfectly
        val qteMultiplier = when {
            successRatio > 0.85f -> 2.0f
            successRatio > 0.65f -> 1.5f
            else -> 0.7f
        }

        val elementFactor = calculateElementalAdvantage(actor.element, target.element)
        val baseDamage = max(10, actor.attack - target.defense)
        val totalDamage = (baseDamage * qteMultiplier * elementFactor).toInt()

        target.currentHp = max(0, target.currentHp - totalDamage)

        val ratingText = when {
            successRatio > 0.85f -> "ИДЕАЛЬНО QTE! ✨"
            successRatio > 0.65f -> "ОТЛИЧНО! 👍"
            else -> "Слабый Тайминг... 💤"
        }

        val comboCombo = if (actor.element == Element.ICE && totalDamage > 30) " [ЗАМОРОЗКА!]" else ""

        val log = current.combatLog.toMutableList().apply {
            add("⚔️ $ratingText ${actor.name} применяет Навык по ${target.name}.")
            add("$totalDamage урона стихией ${actor.element.title}$comboCombo. (Элемент-Множитель: ${elementFactor}x)")
            if (target.currentHp <= 0) {
                add("💥 Порождение хаоса ${target.name} испарилось в эфир!")
            }
        }

        _activeBattle.value = current.copy(
            activeUnitUid = null,
            qteState = CombatQteState(active = false),
            combatLog = log
        )

        checkBattleFinishConditions()
    }

    fun feedSwipeResult(correct: Boolean) {
        val current = _activeBattle.value ?: return
        if (correct) {
            _activeBattle.value = current.copy(
                qteState = current.qteState.copy(multiplier = 0f, promptText = "УСПЕШНЫЙ БЛОК! 🛡️")
            )
            showToast("Кувырок Блокирован QTE!")
        }
    }

    // Elemental damage scales
    private fun calculateElementalAdvantage(a: Element, b: Element): Float {
        if (a == Element.ICE && b == Element.MIST) return 1.5f
        if (a == Element.BLAZE && b == Element.ICE) return 1.5f
        if (a == Element.MIST && b == Element.BLAZE) return 1.5f
        if (b == Element.AETHER) return 1.0f
        return 1.0f
    }

    fun playerDefenseGuard() {
        val current = _activeBattle.value ?: return
        val activeUid = current.activeUnitUid ?: return
        val actor = current.playerUnits.find { it.uid == activeUid } ?: return

        val log = current.combatLog.toMutableList().apply {
            add("🛡️ ${actor.name} вошёл в глухую оборону (+50% Защиты до начала следующего хода).")
        }

        _activeBattle.value = current.copy(
            activeUnitUid = null,
            combatLog = log
        )
        tickBattleTimer()
    }

    fun runFromBattle() {
        showToast("Вы отступили обратно в безопасный сектор!")
        _activeBattle.value = null
        _currentTab.value = "MAP"
    }

    private fun checkBattleFinishConditions() {
        val current = _activeBattle.value ?: return
        val heroesLiving = current.playerUnits.filter { it.currentHp > 0 }
        val enemiesLiving = current.enemyUnits.filter { it.currentHp > 0 }

        if (enemiesLiving.isEmpty()) {
            resolveBattleFinish(victory = true)
        } else if (heroesLiving.isEmpty()) {
            resolveBattleFinish(victory = false)
        }
    }

    private fun resolveBattleFinish(victory: Boolean) {
        val current = _activeBattle.value ?: return
        viewModelScope.launch {
            if (victory) {
                // Award loot, shards, xp
                val baseGold = Random.nextInt(120, 300)
                val baseShards = Random.nextInt(5, 15)
                val xpEarned = 40

                val profile = repository.getProfileSync()
                if (profile != null) {
                    var isNewGearEarned = Random.nextFloat() > 0.4f // 60% gear drop
                    var gearDrop: GearItem? = null

                    if (isNewGearEarned) {
                        val gearRarity = when (Random.nextInt(100)) {
                            in 0..50 -> Rarity.COMMON
                            in 51..85 -> Rarity.RARE
                            in 86..96 -> Rarity.EPIC
                            else -> Rarity.LEGENDARY
                        }
                        val gearType = GearSlot.entries.random()
                        val gearLvl = profile.level
                        gearDrop = GearItem(
                            id = "gear_drop_${System.currentTimeMillis()}",
                            name = "${gearRarity.title} ${gearType.title}",
                            slot = gearType,
                            rarity = gearRarity,
                            levelReq = gearLvl,
                            basePower = 10 + gearLvl * 6,
                            affixes = generateAffixesForGear(gearRarity),
                            equippedHeroId = null
                        )
                        repository.saveGearItem(gearDrop)
                    }

                    // Level up check
                    var newXp = profile.xp + xpEarned
                    var newLvl = profile.level
                    val targetXp = profile.level * 100
                    if (newXp >= targetXp) {
                        newXp -= targetXp
                        newLvl += 1
                        showToast("🎉 ВЫ ПОВЫСИЛИ УРОВЕНЬ! Текущий уровень: $newLvl!")
                    }

                    val updatedProfile = profile.copy(
                        level = newLvl,
                        xp = newXp,
                        gold = profile.gold + baseGold,
                        abyssalShards = profile.abyssalShards + baseShards,
                        dailyDungeonsCleared = profile.dailyDungeonsCleared + 1
                    )
                    repository.saveProfile(updatedProfile)

                    // Add XP to player core heroes
                    val allTeam = repository.getAllHeroesSync()
                    for (hero in allTeam) {
                        val upHero = hero.copy(xp = hero.xp + xpEarned)
                        // Trigger level up on hero if needed
                        var heroLvl = upHero.currentLevel
                        var hXp = upHero.xp
                        if (hXp >= heroLvl * 80) {
                            hXp -= heroLvl * 80
                            heroLvl += 1
                        }
                        repository.saveHero(upHero.copy(currentLevel = heroLvl, xp = hXp))
                    }

                    _activeBattle.value = current.copy(
                        isFinished = true,
                        isVictory = true,
                        xpReward = xpEarned,
                        goldReward = baseGold,
                        shardReward = baseShards,
                        gearReward = gearDrop,
                        combatLog = current.combatLog.toMutableList().apply {
                            add("🏆 ПОБЕДА! Врата Бездны запечатаны.")
                            add("Награда: +$xpEarned XP, +$baseGold Золота, +$baseShards Осколков.")
                            if (gearDrop != null) {
                                add("🎁 Получен предмет: [${gearDrop.rarity.title}] ${gearDrop.name}")
                            }
                        }
                    )
                }
            } else {
                _activeBattle.value = current.copy(
                    isFinished = true,
                    isVictory = false,
                    combatLog = current.combatLog.toMutableList().apply {
                        add("💀 ПОРАЖЕНИЕ! Герои потеряли сознание и эвакуированы в Святилище.")
                    }
                )
            }
        }
    }

    fun claimBattleRewardsExit() {
        _activeBattle.value = null
        _currentTab.value = "MAP"
    }

    private fun generateAffixesForGear(rarity: Rarity): List<GearAffix> {
        val list = mutableListOf<GearAffix>()
        val statPools = listOf("Сила", "ОЗ", "Защита", "Скорость")
        val affixCount = when (rarity) {
            Rarity.COMMON -> 0
            Rarity.UNCOMMON -> 1
            Rarity.RARE -> 2
            Rarity.EPIC -> 3
            Rarity.LEGENDARY -> 4
            Rarity.MYTHIC -> 5
        }
        for (i in 0 until affixCount) {
            val stat = statPools.random()
            val valRoll = when (stat) {
                "ОЗ" -> Random.nextInt(20, 80)
                "Сила" -> Random.nextInt(4, 15)
                "Защита" -> Random.nextInt(3, 10)
                else -> Random.nextInt(2, 6)
            }
            list.add(GearAffix(stat, valRoll))
        }
        return list
    }


    // --- BLACKSMITH & ASTRAL FORGE MECHANICS ---
    fun dismantleItem(gear: GearItem) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val shardsEarned = when (gear.rarity) {
                Rarity.COMMON -> 3
                Rarity.UNCOMMON -> 5
                Rarity.RARE -> 10
                Rarity.EPIC -> 25
                Rarity.LEGENDARY -> 60
                Rarity.MYTHIC -> 150
            }

            repository.deleteGearItem(gear.id)
            val updated = prof.copy(abyssalShards = prof.abyssalShards + shardsEarned)
            repository.saveProfile(updated)

            showToast("Разобрано: ${gear.name}. Получено $shardsEarned осколков!")
        }
    }

    fun craftNewItem(slot: GearSlot) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val shardCost = 15
            val goldCost = 150

            if (prof.abyssalShards < shardCost || prof.gold < goldCost) {
                showToast("Недостаточно ресурсов! Требуется: 15 Осколков и 150 Золота.")
                return@launch
            }

            // RNG Rarity roll
            val dice = Random.nextInt(100)
            val craftedRarity = when {
                dice > 92 -> Rarity.LEGENDARY
                dice > 75 -> Rarity.EPIC
                dice > 40 -> Rarity.RARE
                else -> Rarity.COMMON
            }

            val newItem = GearItem(
                id = "gear_crafted_${System.currentTimeMillis()}",
                name = "🌌 Кованый ${slot.title}",
                slot = slot,
                rarity = craftedRarity,
                levelReq = prof.level,
                basePower = 12 + prof.level * 6,
                affixes = generateAffixesForGear(craftedRarity)
            )

            repository.saveGearItem(newItem)

            val updated = prof.copy(
                abyssalShards = prof.abyssalShards - shardCost,
                gold = prof.gold - goldCost
            )
            repository.saveProfile(updated)

            showToast("Выковано: [${craftedRarity.title}] ${newItem.name}!")
        }
    }

    fun reforgeEquipmentAffixes(gear: GearItem) {
        viewModelScope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val cost = 5
            if (prof.abyssalShards < cost) {
                showToast("Для перековки требуется 5 Осколков Бездны!")
                return@launch
            }

            val rerolled = gear.copy(
                affixes = generateAffixesForGear(gear.rarity)
            )
            repository.saveGearItem(rerolled)

            val updated = prof.copy(abyssalShards = prof.abyssalShards - cost)
            repository.saveProfile(updated)

            showToast("Аффиксы предмета перекованы в астральном огне!")
        }
    }

    fun equipItemToHero(gear: GearItem, heroId: String?) {
        viewModelScope.launch {
            // Un-equip other gear in same slot on that hero
            if (heroId != null) {
                val currentEquipped = repository.getAllGearSync()
                    .find { it.equippedHeroId == heroId && it.slot == gear.slot }
                if (currentEquipped != null) {
                    repository.saveGearItem(currentEquipped.copy(equippedHeroId = null))
                }
            }

            val updated = gear.copy(equippedHeroId = heroId)
            repository.saveGearItem(updated)
            showToast(if (heroId == null) "Предмет снят" else "Предмет экипирован!")
        }
    }


    // --- HERO ELEVATION & EXPEDIATIONS ---
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
            val costShards = 100 // Hard cap ascension requirement
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
            val summonCost = 15 // Shards required
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


    // --- SOCIAL INTEGRATIONS ---
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


    // --- DATA BACKUP AND SERVER HYBRID EXPORT ---
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

            // Heroes List
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

            // Gear Inventory
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

                // Decode Heroes
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

                // Decode Gear info
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

                generateSurroundingPOIs(updatedProf.currentLatitude, updatedProf.currentLongitude)
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
        fuzzerJob.cancel()
        battleTickerJob.cancel()
        removeLocationListener()
    }
}
