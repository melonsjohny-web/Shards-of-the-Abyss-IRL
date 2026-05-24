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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val combatMutex = Mutex()
    private var lastTickTime: Long = 0

    fun startTicker(isWeatherBoostingElement: (Element) -> Boolean) {
        battleTickerJob?.cancel()
        battleTickerJob = scope.launch(Dispatchers.Default) {
            while (true) {
                val current = _activeBattle.value
                if (current == null || current.isFinished) {
                    delay(500) // Keep idle overhead close to zero
                    lastTickTime = 0L
                    continue
                }
                if (current.activeUnitUid != null || current.qteState.active) {
                    delay(200) // Sleep during player actions or active QTE
                    lastTickTime = 0L
                    continue
                }
                delay(100)
                tickBattleTimer(isWeatherBoostingElement)
            }
        }
    }

    fun initiatePOICombat(poi: PointOfInterest, companionIds: Set<String>) {
        scope.launch {
            val heroes = repository.getAllHeroesSync()
            if (heroes.isEmpty()) {
                showToast("У вас нет призванных героев! Активируйте призыв.")
                return@launch
            }

            // Lead hero is the Avatar
            val leadHero = heroes.find { it.id.startsWith("hero_lead_") } ?: heroes.first()

            // Companions block (take selected ones that exist, max 2 helpers)
            val selectedHelpers = heroes.filter { it.id in companionIds && it.id != leadHero.id }.take(2).toMutableList()
            if (selectedHelpers.size < 2) {
                // Pad with any other non-lead heroes to ensure a complete squad of 3 players
                val extra = heroes.filter { it.id != leadHero.id && it.id !in companionIds }
                selectedHelpers.addAll(extra.take(2 - selectedHelpers.size))
            }

            val squad = listOf(leadHero) + selectedHelpers.take(2)

            // Map combat squad with equipment calculations
            val playerUnits = squad.map { hero ->
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

            // Procedural enemies with soft player level scaling
            val elements = Element.entries
            val enemiesCount = if (poi.type == PoiType.ABYSSAL_GATE || poi.type == PoiType.NEXUS_POINT) 1 else Random.nextInt(2, 4)
            val playerLevel = repository.getProfileSync()?.level ?: 1
            val effectiveLevel = ((poi.minLevel + poi.maxLevel) / 2 + playerLevel) / 2
            val isBoss = enemiesCount == 1

            val enemyUnits = (1..enemiesCount).map { i ->
                val enemyElem = elements[(poi.element.ordinal + i) % elements.size]
                val level = (effectiveLevel + Random.nextInt(-2, 3)).coerceAtLeast(1)
                val health = if (isBoss) 150 + level * 25 else 50 + level * 12
                val baseAtk = if (isBoss) 15 + level * 4 else 8 + level * 2
                val baseDef = if (isBoss) 10 + level * 3 else 4 + level * 2
                val velocity = 8 + level + (if (isBoss) 5 else 0)

                BattleUnit(
                    uid = "enemy_${poi.id}_$i",
                    id = if (isBoss) "boss_${poi.type}" else "minion_$i",
                    name = if (isBoss) generateBossName(poi) else generateMinionName(enemyElem, i),
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
            lastTickTime = System.currentTimeMillis()
        }
    }

    private fun tickBattleTimer(isWeatherBoostingElement: (Element) -> Boolean) {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                if (current.isFinished || current.activeUnitUid != null || current.qteState.active) {
                    lastTickTime = 0L
                    return@withLock
                }

                val now = System.currentTimeMillis()
                if (lastTickTime == 0L) {
                    lastTickTime = now
                    return@withLock
                }
                val deltaSeconds = (now - lastTickTime) / 1000f
                lastTickTime = now

                val pUnits = current.playerUnits
                val eUnits = current.enemyUnits

                // Charge AP safely with copies and delta time
                val updatedPUnits = pUnits.map { unit ->
                    if (unit.currentHp <= 0) unit
                    else {
                        var increment = unit.speed * deltaSeconds
                        if (isWeatherBoostingElement(unit.element)) {
                            increment *= 1.25f
                        }
                        unit.copy(currentActionPoints = unit.currentActionPoints + increment)
                    }
                }

                val updatedEUnits = eUnits.map { unit ->
                    if (unit.currentHp <= 0) unit
                    else {
                        var increment = unit.speed * deltaSeconds
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
                    val clearedNextAct = nextAct.copy(
                        currentActionPoints = 0f,
                        statusEffect = if (nextAct.statusEffect == "GUARD") null else nextAct.statusEffect
                    )
                    
                    val finalPUnits = updatedPUnits.map { 
                        if (it.uid == nextAct.uid) clearedNextAct 
                        else it 
                    }
                    val finalEUnits = updatedEUnits.map { 
                        if (it.uid == nextAct.uid) clearedNextAct 
                        else it 
                    }

                    val updateLog = current.combatLog.toMutableList().apply {
                        add("Ход принадлежит: ${nextAct.name} (${nextAct.element.title})")
                    }

                    _activeBattle.value = current.copy(
                        playerUnits = finalPUnits,
                        enemyUnits = finalEUnits,
                        activeUnitUid = nextAct.uid,
                        combatLog = updateLog
                    )

                    lastTickTime = 0L // Pause AP charging during player choice phase

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
        }
    }

    private fun triggerMonsterTurn(monster: BattleUnit) {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                if (current.isFinished) return@withLock

                val livingHeroes = current.playerUnits.filter { it.currentHp > 0 }
                if (livingHeroes.isEmpty()) {
                    resolveBattleFinish(victory = false)
                    return@withLock
                }

                val roll = Random.nextInt(3)
                // Tactical Monster Targeting selection
                val target = when (roll) {
                    0 -> livingHeroes.minByOrNull { it.currentHp } ?: livingHeroes.random() // Focus weakest hp
                    1 -> livingHeroes.maxByOrNull { it.maxHp } ?: livingHeroes.random() // Attack tank
                    else -> {
                        // Elemental Counter Target Match
                        val counters = livingHeroes.filter { hero ->
                            (monster.element == Element.BLAZE && hero.element == Element.ICE) ||
                            (monster.element == Element.ICE && hero.element == Element.BLOOM) ||
                            (monster.element == Element.BLOOM && hero.element == Element.MIST) ||
                            (monster.element == Element.MIST && hero.element == Element.BLAZE)
                        }
                        counters.randomOrNull() ?: livingHeroes.random()
                    }
                }

                val directions = listOf("UP", "DOWN", "LEFT", "RIGHT")
                val randomDir = directions.random()

                val attackName = when (roll) {
                    0 -> "обычную атаку"
                    1 -> "💥 ТЯЖЕЛЫЙ КРИТИЧЕСКИЙ УДАР 💥"
                    else -> "🔥 СТИХИЙНОЕ ДЫХАНИЕ 🔥"
                }
                val durationMs = when (roll) {
                    0 -> 1500L
                    1 -> 950L
                    else -> 1200L
                }

                val prompt = when (roll) {
                    0 -> "Защита! Размашистый удар монстра: Свайп $randomDir за 1.5 сек!"
                    1 -> "БЕРЕГИТЕСЬ! Сверхбыстрый сокрушительный удар: Свайп $randomDir за 0.9 сек!"
                    else -> "ВНИМАНИЕ! Магический выброс Бездны: Свайп $randomDir за 1.2 сек!"
                }

                val updateLog = current.combatLog.toMutableList().apply {
                    add("👹 ${monster.name} готовит $attackName по воину ${target.name}...")
                }

                _activeBattle.value = current.copy(
                    combatLog = updateLog,
                    qteState = CombatQteState(
                        active = true,
                        type = QteType.BLOCK_SWIPE,
                        promptText = prompt,
                        targetDirection = randomDir,
                        timeLeftMs = durationMs,
                        multiplier = 1.0f + roll * 0.5f
                    )
                )

                scope.launch {
                    val currentPoiId = current.poiId
                    delay(durationMs)
                    combatMutex.withLock {
                        val state = _activeBattle.value ?: return@withLock
                        if (state.poiId != currentPoiId || state.isFinished || !state.qteState.active) return@withLock
                        
                        val blockSuccessful = state.qteState.multiplier == 0f
                        executeMonsterAttackBlow(monster, target, blockSuccessful, attackType = roll)
                    }
                }
            }
        }
    }

    private fun executeMonsterAttackBlow(monster: BattleUnit, target: BattleUnit, blockSuccessful: Boolean, attackType: Int) {
        val current = _activeBattle.value ?: return
        if (current.isFinished || current.playerUnits.none { it.uid == target.uid } || !current.qteState.active) return

        // Fetch target again to ensure up-to-date HP and Guard values
        val actualTarget = current.playerUnits.find { it.uid == target.uid } ?: return
        val isGuarding = actualTarget.statusEffect == "GUARD"
        val effectiveDefense = if (isGuarding) (actualTarget.defense * 1.5f).toInt() else actualTarget.defense

        val damage = when (attackType) {
            0 -> max(6, monster.attack - effectiveDefense) // Regular
            1 -> max(12, (monster.attack * 1.8f).toInt() - effectiveDefense) // Heavy Crit
            else -> max(10, monster.attack - (effectiveDefense * 0.4f).toInt()) // Ignored defense
        }

        // Apply element counters against player
        val elementFactor = when {
            monster.element == Element.BLAZE && actualTarget.element == Element.ICE -> 1.4f
            monster.element == Element.ICE && actualTarget.element == Element.BLOOM -> 1.4f
            monster.element == Element.BLOOM && actualTarget.element == Element.MIST -> 1.4f
            monster.element == Element.MIST && actualTarget.element == Element.BLAZE -> 1.4f
            monster.element == Element.AETHER && actualTarget.element != Element.AETHER -> 1.4f
            actualTarget.element == Element.AETHER && monster.element != Element.AETHER -> 1.4f
            else -> 1.0f
        }
        val finalDamage = (damage * elementFactor).toInt()

        val updatedPUnits = current.playerUnits.map { unit ->
            if (unit.uid == actualTarget.uid) {
                if (blockSuccessful) unit
                else unit.copy(currentHp = max(0, unit.currentHp - finalDamage))
            } else {
                unit
            }
        }

        val newTargetHp = if (blockSuccessful) actualTarget.currentHp else max(0, actualTarget.currentHp - finalDamage)
        val log = current.combatLog.toMutableList()

        if (blockSuccessful) {
            log.add("🛡️ ${actualTarget.name} УСПЕШНО ИЗБЕЖАЛ урон от атаки ${monster.name}!")
        } else {
            val attackTypeLabel = when (attackType) {
                0 -> "простым ударом когтей"
                1 -> "💥 КРИТИЧЕСКИМ сокрушением"
                else -> "🔥 СТИХИЙНЫМ магическим выдохом"
            }
            log.add("💥 ${monster.name} наносит ${actualTarget.name} $finalDamage урона $attackTypeLabel! (Элемент: ${elementFactor}x)")
            if (newTargetHp <= 0) {
                log.add("💀 Хранитель ${actualTarget.name} повержен!")
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

    fun setCombatTarget(targetUid: String) {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                val target = current.enemyUnits.find { it.uid == targetUid && it.currentHp > 0 } ?: return@withLock
                _activeBattle.value = current.copy(selectedTargetUid = target.uid)
                showToast("Цель зафиксирована: ${target.name}")
            }
        }
    }

    fun playerTriggerStrikeAttack() {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                val activeUid = current.activeUnitUid ?: return@withLock
                current.playerUnits.find { it.uid == activeUid } ?: return@withLock

                _activeBattle.value = current.copy(
                    qteState = CombatQteState(
                        active = true,
                        type = QteType.ATTACK_RING,
                        promptText = "🎯 ТАП в идеальной границе сужения кольца!",
                        timeLeftMs = 1500,
                        multiplier = 1.0f
                    )
                )
            }
        }
    }

    fun playerTriggerUltimateAttack() {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                val activeUid = current.activeUnitUid ?: return@withLock
                current.playerUnits.find { it.uid == activeUid } ?: return@withLock

                _activeBattle.value = current.copy(
                    qteState = CombatQteState(
                        active = true,
                        type = QteType.ATTACK_RING,
                        promptText = "🌟 СУПЕРПРИЕМ СТИХИЙ! ТАП в идеальной границе сужения!",
                        timeLeftMs = 1100,
                        multiplier = 1.8f
                    )
                )
            }
        }
    }

    fun playerTriggerHeal() {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                val activeUid = current.activeUnitUid ?: return@withLock
                val actor = current.playerUnits.find { it.uid == activeUid } ?: return@withLock

                val healAmount = (actor.maxHp * 0.35f).toInt()
                val updatedPUnits = current.playerUnits.map { unit ->
                    if (unit.uid == actor.uid) {
                        unit.copy(currentHp = (unit.currentHp + healAmount).coerceAtMost(unit.maxHp))
                    } else {
                        unit
                    }
                }

                val log = current.combatLog.toMutableList().apply {
                    add("✨ Оракул Помощи: ${actor.name} исцеляет свои раны на $healAmount ОЗ древней формулой Жизни!")
                }

                _activeBattle.value = current.copy(
                    playerUnits = updatedPUnits,
                    activeUnitUid = null,
                    combatLog = log
                )

                checkBattleFinishConditions()
            }
        }
    }

    fun playerFinishQTEHit(successRatio: Float) {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                val activeUid = current.activeUnitUid ?: return@withLock
                val actor = current.playerUnits.find { it.uid == activeUid } ?: return@withLock

                val livingEnemies = current.enemyUnits.filter { it.currentHp > 0 }
                if (livingEnemies.isEmpty()) {
                    checkBattleFinishConditions()
                    return@withLock
                }

                // Lock on selected target if valid and alive, otherwise target first living
                val target = current.enemyUnits.find { it.uid == current.selectedTargetUid && it.currentHp > 0 }
                    ?: livingEnemies.first()

                val qteMultiplier = when {
                    successRatio > 0.85f -> 2.2f
                    successRatio > 0.65f -> 1.5f
                    else -> 0.6f
                }

                // Expanded strategic 5-element counter system
                val elementFactor = when {
                    actor.element == Element.BLAZE && target.element == Element.ICE -> 1.5f
                    actor.element == Element.ICE && target.element == Element.BLOOM -> 1.5f
                    actor.element == Element.BLOOM && target.element == Element.MIST -> 1.5f
                    actor.element == Element.MIST && target.element == Element.BLAZE -> 1.5f
                    actor.element == Element.AETHER && target.element != Element.AETHER -> 1.5f
                    target.element == Element.AETHER && actor.element != Element.AETHER -> 1.5f
                    else -> 1.0f
                }

                val baseDamage = max(10, actor.attack - target.defense)
                val actionFactor = current.qteState.multiplier
                val totalDamage = (baseDamage * qteMultiplier * elementFactor * actionFactor).toInt()

                val updatedEUnits = current.enemyUnits.map { unit ->
                    if (unit.uid == target.uid) {
                        unit.copy(currentHp = max(0, unit.currentHp - totalDamage))
                    } else {
                        unit
                    }
                }

                val isUltimate = actionFactor > 1.2f
                val ratingText = when {
                    successRatio > 0.85f -> "СВЕРХ-УСПЕШНО! ✨"
                    successRatio > 0.65f -> "ОТЛИЧНО! 👍"
                    else -> "Слабый Тайминг... 💤"
                }

                val actionName = if (isUltimate) "💥 СУПЕРПРИЕМ СТИХИЙ" else "⚔️ Навык"
                val comboCombo = if (actor.element == Element.ICE && totalDamage > 30) " [ЗАМОРОЗКА!]" else ""

                val log = current.combatLog.toMutableList().apply {
                    add("⚔️ $ratingText ${actor.name} применяет $actionName по ${target.name}.")
                    add("$totalDamage урона стихией ${actor.element.title}$comboCombo. (Множитель: ${elementFactor}x)")
                    if (target.currentHp - totalDamage <= 0) {
                        add("💥 Порождение хаоса ${target.name} испарилось в изначальный эфир!")
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
        }
    }

    fun feedSwipeResult(correct: Boolean) {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                if (correct) {
                    _activeBattle.value = current.copy(
                        qteState = current.qteState.copy(multiplier = 0f, promptText = "УСПЕШНЫЙ БЛОК! 🛡️")
                    )
                    showToast("Кувырок Блокирован QTE!")
                }
            }
        }
    }

    fun playerDefenseGuard() {
        scope.launch(Dispatchers.Default) {
            combatMutex.withLock {
                val current = _activeBattle.value ?: return@withLock
                val activeUid = current.activeUnitUid ?: return@withLock
                val actor = current.playerUnits.find { it.uid == activeUid } ?: return@withLock

                val log = current.combatLog.toMutableList().apply {
                    add("🛡️ ${actor.name} вошёл в глухую оборону (+50% Защиты до начала следующего хода).")
                }

                val updatedPUnits = current.playerUnits.map { unit ->
                    if (unit.uid == actor.uid) {
                        unit.copy(statusEffect = "GUARD", statusDuration = 1)
                    } else {
                        unit
                    }
                }

                _activeBattle.value = current.copy(
                    playerUnits = updatedPUnits,
                    activeUnitUid = null,
                    combatLog = log
                )
            }
        }
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
        scope.launch(Dispatchers.IO) {
            combatMutex.withLock {
                val doubleCheck = _activeBattle.value ?: return@withLock
                if (doubleCheck.isFinished) return@withLock

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

                        // Put completed POI on cooldown inside Room database
                        val poisList = repository.getAllPOIsSync()
                        val matchPoi = poisList.find { it.id == doubleCheck.poiId }
                        if (matchPoi != null) {
                            val cooldownPeriodMs = matchPoi.type.cooldownMinutes * 60 * 1000L
                            val cooledPoi = matchPoi.copy(cooldownUntil = System.currentTimeMillis() + cooldownPeriodMs)
                            repository.savePOIs(listOf(cooledPoi))
                        }

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

                        _activeBattle.value = doubleCheck.copy(
                            isFinished = true,
                            isVictory = true,
                            xpReward = xpEarned,
                            goldReward = baseGold,
                            shardReward = baseShards,
                            gearReward = gearDrop,
                            combatLog = doubleCheck.combatLog.toMutableList().apply {
                                add("🏆 ПОБЕДА! Врата Бездны запечатаны.")
                                add("Награда: +$xpEarned XP, +$baseGold Золота, +$baseShards Осколков.")
                                if (gearDrop != null) {
                                    add("🎁 Получен предмет: [${gearDrop.rarity.title}] ${gearDrop.name}")
                                }
                            }
                        )
                    }
                } else {
                    _activeBattle.value = doubleCheck.copy(
                        isFinished = true,
                        isVictory = false,
                        combatLog = doubleCheck.combatLog.toMutableList().apply {
                            add("💀 ПОРАЖЕНИЕ! Герои потеряли сознание и эвакуированы в Святилище.")
                        }
                    )
                }
            }
        }
    }

    fun claimBattleRewardsExit(navigateBack: () -> Unit) {
        _activeBattle.value = null
        navigateBack()
    }

    private fun generateBossName(poi: PointOfInterest): String {
        val prefix = when (poi.element) {
            Element.ICE -> "Ледяной"
            Element.BLOOM -> "Живой"
            Element.BLAZE -> "Пылающий"
            Element.MIST -> "Туманный"
            Element.AETHER -> "Эфирный"
        }
        val suffix = when (poi.type) {
            PoiType.ABYSSAL_GATE -> "Страж Врат"
            PoiType.NEXUS_POINT -> "Хранитель Узла"
            else -> "Вожак Бездны"
        }
        return "👹 $prefix $suffix"
    }

    private fun generateMinionName(element: Element, index: Int): String {
        val kind = when (element) {
            Element.ICE -> "Ледяной Осколок"
            Element.BLOOM -> "Цветочный Терновник"
            Element.BLAZE -> "Пылающий Огонёк"
            Element.MIST -> "Призрачный Дух"
            Element.AETHER -> "Кадет Пустоты"
        }
        return "👾 $kind №$index"
    }

    fun clear() {
        battleTickerJob?.cancel()
    }
}
