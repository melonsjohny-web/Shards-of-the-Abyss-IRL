package com.example.viewmodel

import android.util.Log
import com.example.data.GameRepository
import com.example.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

class CombatViewModel(
    private val repository: GameRepository,
    private val scope: CoroutineScope,
    private val showToast: (String) -> Unit
) {

    private val _activeBattle = MutableStateFlow<ActiveBattleState?>(null)
    val activeBattle: StateFlow<ActiveBattleState?> = _activeBattle.asStateFlow()

    private var battleTickerJob: Job? = null

    fun startTicker(isWeatherBoostingElement: (Element) -> Boolean) {
        battleTickerJob?.cancel()
        battleTickerJob = scope.launch(Dispatchers.Default) {
            while (true) {
                delay(100)
                tickBattleTimer(isWeatherBoostingElement)
            }
        }
    }

    fun initiatePOICombat(poi: PointOfInterest) {
        scope.launch {
            val heroes = repository.getAllHeroesSync()
            if (heroes.isEmpty()) {
                showToast("У вас нет призванных героев! Активируйте призыв.")
                return@launch
            }

            // Map up to 3 heroes to combat squad with equipment calculations
            val playerUnits = heroes.take(3).map { hero ->
                val gears = repository.getAllGearSync().filter { it.equippedHeroId == hero.id }
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
                    for (aff in g.affixes) {
                        when (aff.attribute) {
                            "Сила" -> extraAtk += aff.value
                            "ОЗ" -> extraHp += aff.value
                            "Защита" -> extraDef += aff.value
                            "Скорость" -> extraSpd += aff.value
                        }
                    }
                }

                BattleUnit(
                    uid = "player_${hero.id}",
                    id = hero.id,
                    name = hero.name,
                    isHero = true,
                    currentHp = hero.maxHp + extraHp,
                    maxHp = hero.maxHp + extraHp,
                    attack = hero.attack + extraAtk,
                    defense = hero.defense + extraDef,
                    speed = hero.speed + extraSpd,
                    element = hero.element
                )
            }

            // Procedural enemies
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

            _activeBattle.value = ActiveBattleState(
                poiId = poi.id,
                poiName = poi.name,
                poiType = poi.type,
                element = poi.element,
                playerUnits = playerUnits,
                enemyUnits = enemyUnits,
                turnOrder = emptyList(),
                activeUnitUid = null,
                combatLog = listOf("Разлом открыт! Инициализация тактических полей воителей."),
                qteState = CombatQteState(active = false)
            )
        }
    }

    private fun tickBattleTimer(isWeatherBoostingElement: (Element) -> Boolean) {
        val current = _activeBattle.value ?: return
        if (current.isFinished || current.activeUnitUid != null) return

        val pUnits = current.playerUnits
        val eUnits = current.enemyUnits

        // Charge AP safely with copies
        val updatedPUnits = pUnits.map { unit ->
            if (unit.currentHp <= 0) unit
            else {
                var increment = unit.speed * 0.1f
                if (isWeatherBoostingElement(unit.element)) {
                    increment *= 1.25f
                }
                unit.copy(currentActionPoints = unit.currentActionPoints + increment)
            }
        }

        val updatedEUnits = eUnits.map { unit ->
            if (unit.currentHp <= 0) unit
            else {
                var increment = unit.speed * 0.1f
                if (isWeatherBoostingElement(unit.element)) {
                    increment *= 1.25f
                }
                unit.copy(currentActionPoints = unit.currentActionPoints + increment)
            }
        }

        val nextAct = (updatedPUnits + updatedEUnits)
            .filter { it.currentHp > 0 && it.currentActionPoints >= 100f }
            .maxByOrNull { it.currentActionPoints }

        if (nextAct != null) {
            val clearedNextAct = nextAct.copy(currentActionPoints = 0f)
            val finalPUnits = updatedPUnits.map { if (it.uid == nextAct.uid) clearedNextAct else it }
            val finalEUnits = updatedEUnits.map { if (it.uid == nextAct.uid) clearedNextAct else it }

            val updateLog = current.combatLog.toMutableList().apply {
                add("Ход принадлежит: ${nextAct.name} (${nextAct.element.title})")
            }

            _activeBattle.value = current.copy(
                playerUnits = finalPUnits,
                enemyUnits = finalEUnits,
                activeUnitUid = nextAct.uid,
                combatLog = updateLog
            )

            if (!nextAct.isHero) {
                scope.launch {
                    delay(1200)
                    triggerMonsterTurn(clearedNextAct)
                }
            }
        } else {
            _activeBattle.value = current.copy(
                playerUnits = updatedPUnits,
                enemyUnits = updatedEUnits
            )
        }
    }

    private fun triggerMonsterTurn(monster: BattleUnit) {
        val current = _activeBattle.value ?: return
        if (current.isFinished) return

        val livingHeroes = current.playerUnits.filter { it.currentHp > 0 }
        if (livingHeroes.isEmpty()) {
            resolveBattleFinish(victory = false)
            return
        }

        val target = livingHeroes.random()
        val directions = listOf("UP", "DOWN", "LEFT", "RIGHT")
        val randomDir = directions.random()

        _activeBattle.value = current.copy(
            qteState = CombatQteState(
                active = true,
                type = QteType.BLOCK_SWIPE,
                promptText = "Защита! Проведите Свайп: $randomDir",
                targetDirection = randomDir,
                timeLeftMs = 1200
            )
        )

        scope.launch {
            delay(1200)
            val checkQte = _activeBattle.value?.qteState ?: return@launch
            executeMonsterAttackBlow(monster, target, blockSuccessful = (checkQte.multiplier == 0f))
        }
    }

    private fun executeMonsterAttackBlow(monster: BattleUnit, target: BattleUnit, blockSuccessful: Boolean) {
        val current = _activeBattle.value ?: return
        var damage = max(5, monster.attack - target.defense)

        val updatedPUnits = current.playerUnits.map { unit ->
            if (unit.uid == target.uid) {
                if (blockSuccessful) unit
                else unit.copy(currentHp = max(0, unit.currentHp - damage))
            } else {
                unit
            }
        }

        val newTargetHp = if (blockSuccessful) target.currentHp else max(0, target.currentHp - damage)
        val log = current.combatLog.toMutableList()

        if (blockSuccessful) {
            log.add("🛡️ ${target.name} УСПЕШНО ОТРАЗИЛ атаку ${monster.name} Свайпом QTE!")
        } else {
            log.add("💥 ${monster.name} КУСАЕТ ${target.name} за $damage ед. урона.")
            if (newTargetHp <= 0) {
                log.add("💀 Воин ${target.name} повержен!")
            }
        }

        _activeBattle.value = current.copy(
            playerUnits = updatedPUnits,
            activeUnitUid = null,
            qteState = CombatQteState(active = false),
            combatLog = log
        )

        checkBattleFinishConditions()
    }

    fun playerTriggerSkillAttack() {
        val current = _activeBattle.value ?: return
        val activeUid = current.activeUnitUid ?: return
        current.playerUnits.find { it.uid == activeUid } ?: return

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

        val target = livingEnemies.first()

        val qteMultiplier = when {
            successRatio > 0.85f -> 2.0f
            successRatio > 0.65f -> 1.5f
            else -> 0.7f
        }

        val elementFactor = when {
            actor.element == Element.ICE && target.element == Element.MIST -> 1.5f
            actor.element == Element.BLAZE && target.element == Element.ICE -> 1.5f
            actor.element == Element.MIST && target.element == Element.BLAZE -> 1.5f
            target.element == Element.AETHER -> 1.0f
            else -> 1.0f
        }

        val baseDamage = max(10, actor.attack - target.defense)
        val totalDamage = (baseDamage * qteMultiplier * elementFactor).toInt()

        val updatedEUnits = current.enemyUnits.map { unit ->
            if (unit.uid == target.uid) {
                unit.copy(currentHp = max(0, unit.currentHp - totalDamage))
            } else {
                unit
            }
        }

        val ratingText = when {
            successRatio > 0.85f -> "ИДЕАЛЬНО QTE! ✨"
            successRatio > 0.65f -> "ОТЛИЧНО! 👍"
            else -> "Слабый Тайминг... 💤"
        }

        val comboCombo = if (actor.element == Element.ICE && totalDamage > 30) " [ЗАМОРОЗКА!]" else ""
        val log = current.combatLog.toMutableList().apply {
            add("⚔️ $ratingText ${actor.name} применяет Навык по ${target.name}.")
            add("$totalDamage урона стихией ${actor.element.title}$comboCombo. (Элемент-Множитель: ${elementFactor}x)")
            if (target.currentHp - totalDamage <= 0) {
                add("💥 Порождение хаоса ${target.name} испарилось в эфир!")
            }
        }

        _activeBattle.value = current.copy(
            enemyUnits = updatedEUnits,
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
    }

    fun runFromBattle(navigateBack: () -> Unit) {
        showToast("Вы отступили обратно в безопасный сектор!")
        _activeBattle.value = null
        navigateBack()
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
        scope.launch {
            if (victory) {
                val baseGold = Random.nextInt(120, 300)
                val baseShards = Random.nextInt(5, 15)
                val xpEarned = 40

                val profile = repository.getProfileSync()
                if (profile != null) {
                    val isNewGearEarned = Random.nextFloat() > 0.4f
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
                        
                        val affs = mutableListOf<GearAffix>()
                        val statPools = listOf("Сила", "ОЗ", "Защита", "Скорость")
                        val affixCount = when (gearRarity) {
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
                            affs.add(GearAffix(stat, valRoll))
                        }

                        gearDrop = GearItem(
                            id = "gear_drop_${System.currentTimeMillis()}",
                            name = "${gearRarity.title} ${gearType.title}",
                            slot = gearType,
                            rarity = gearRarity,
                            levelReq = gearLvl,
                            basePower = 10 + gearLvl * 6,
                            affixes = affs,
                            equippedHeroId = null
                        )
                        repository.saveGearItem(gearDrop)
                    }

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

                    val allTeam = repository.getAllHeroesSync()
                    for (hero in allTeam) {
                        val upHero = hero.copy(xp = hero.xp + xpEarned)
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

    fun claimBattleRewardsExit(navigateBack: () -> Unit) {
        _activeBattle.value = null
        navigateBack()
    }

    fun clear() {
        battleTickerJob?.cancel()
    }
}
