package com.example.viewmodel

import com.example.data.GameRepository
import com.example.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class ForgeViewModel(
    private val repository: GameRepository,
    private val scope: CoroutineScope,
    private val showToast: (String) -> Unit
) {

    fun dismantleItem(gear: GearItem) {
        scope.launch {
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
        scope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val shardCost = 15
            val goldCost = 150

            if (prof.abyssalShards < shardCost || prof.gold < goldCost) {
                showToast("Недостаточно ресурсов! Требуется: 15 Осколков и 150 Золота.")
                return@launch
            }

            val dice = Random.nextInt(100)
            val craftedRarity = when {
                dice > 92 -> Rarity.LEGENDARY
                dice > 75 -> Rarity.EPIC
                dice > 40 -> Rarity.RARE
                else -> Rarity.COMMON
            }

            val affs = generateAffixesForGear(craftedRarity)
            val newItem = GearItem(
                id = "gear_crafted_${System.currentTimeMillis()}",
                name = "🌌 Кованый ${slot.title}",
                slot = slot,
                rarity = craftedRarity,
                levelReq = prof.level,
                basePower = 12 + prof.level * 6,
                affixes = affs,
                equippedHeroId = null
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
        scope.launch {
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
        scope.launch {
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

    fun clear() {
        // No background timers or loops in forge
    }
}
