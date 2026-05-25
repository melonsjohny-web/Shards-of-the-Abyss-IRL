package com.example.domain

import java.io.Serializable

enum class Element(val title: String, val god: String, val colorHex: Long, val description: String) {
    ICE("Ледяной", "Аурелия", 0xFF29B6F6, "Контроль и ледяные щиты. +10% урона Снежной ночью."),
    BLOOM("Цветущий", "Мизу", 0xFF66BB6A, "Лечебные сады и регенерация. +10% урона Утром в парках."),
    BLAZE("Пылающий", "Герра", 0xFFFF7043, "Разрушительный урон и горение. +10% урона в Полдень."),
    MIST("Туманный", "Винд", 0xFFAB47BC, "Уклонения и ослабления. +10% урона Туманным вечером."),
    AETHER("Срединный", "Эфир", 0xFFFFCA28, "Универсальный тип, использующий энергию баланса.");

    companion object {
        fun fromString(name: String?): Element =
            entries.find { it.name == name } ?: AETHER
    }
}

enum class Rarity(val title: String, val colorHex: Long) {
    COMMON("Обычный", 0xFFB0BEC5),
    UNCOMMON("Необычный", 0xFF81C784),
    RARE("Редкий", 0xFF4FC3F7),
    EPIC("Эпический", 0xFFBA68C8),
    LEGENDARY("Легендарный", 0xFFFFB74D),
    MYTHIC("Мифический", 0xFFF06292);

    companion object {
        fun fromString(name: String?): Rarity =
            entries.find { it.name == name } ?: COMMON
    }
}

enum class GearSlot(val title: String) {
    WEAPON("Оружие"),
    HELMET("Шлем"),
    ARMOR("Броня"),
    GLOVES("Перчатки"),
    BOOTS("Сапоги"),
    AMULET("Амулет"),
    RING_1("Кольцо 1"),
    RING_2("Кольцо 2")
}

data class GearAffix(
    val attribute: String,
    val value: Int,
    val isPercent: Boolean = false
) : Serializable {
    fun format(): String = "$attribute +$value${if (isPercent) "%" else ""}"
}

data class GearItem(
    val id: String,
    val name: String,
    val slot: GearSlot,
    val rarity: Rarity,
    val levelReq: Int,
    val basePower: Int,
    val affixes: List<GearAffix>,
    val socketedShardId: String? = null,
    val socketedShardName: String? = null,
    val equippedHeroId: String? = null // null means in inventory
) : Serializable

data class AwakenedHero(
    val id: String,
    val name: String,
    val element: Element,
    val currentLevel: Int = 1,
    val starRating: Int = 1,
    val xp: Int = 0,
    val maxHp: Int = 120,
    val attack: Int = 22,
    val defense: Int = 12,
    val speed: Int = 15,
    val isAscended: Boolean = false,
    val activeSkillId: String = "primary_strike",
    val unlockedSkillTreeNodes: List<String> = emptyList()
) : Serializable

data class GameQuest(
    val id: String,
    val name: String,
    val description: String,
    val requiredProgress: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val goldReward: Int = 150,
    val shardsReward: Int = 10
) : Serializable

enum class PoiType(val label: String, val markerColorHex: Long, val cooldownMinutes: Int) {
    RIFT("Разлом Бездны", 0xFFFF5722, 15),
    ABYSSAL_GATE("Врата Бездны", 0xFF9C27B0, 30),
    SANCTUM("Святилище Бога", 0xFF4CAF50, 0),
    CHAOS_SPIKE("Шип Хаоса", 0xFFE91E63, 180),
    NEXUS_POINT("Узел Силы", 0xFFFFC107, 60),
    MERCHANT_CARAVAN("Торговый Караван Эфира", 0xFFFF9800, 30),
    TAVERN("Древняя Таверна Эфира", 0xFF795548, 45),
    GUILD_VAULT("Казначейство Ордена", 0xFF00BCD4, 60),
    SACRED_GROVE("Священная Роща Мизу", 0xFF4CAF50, 60),
    RANDOM_ENCOUNTER("Искажение Судьбы", 0xFF9E9E9E, 15)
}

data class PointOfInterest(
    val id: String,
    val name: String,
    val type: PoiType,
    val element: Element,
    val latitude: Double,
    val longitude: Double,
    val minLevel: Int,
    val maxLevel: Int,
    val lastVisitedTimestamp: Long = 0,
    val isCapturedByGuild: Boolean = false,
    val capturedGuildName: String? = null,
    val osmTags: Map<String, String> = emptyMap(),
    val realName: String? = null,
    val cooldownUntil: Long = 0
) : Serializable

enum class WeatherCondition(val title: String, val associatedElement: Element) {
    CLEAR("Ясно", Element.AETHER),
    RAIN("Дождь", Element.ICE),
    HOT_WAVE("Жара", Element.BLAZE),
    FOGGY("Густой Туман", Element.MIST),
    WINDY("Ветрено", Element.MIST),
    SNOWY("Снегопад", Element.ICE);

    companion object {
        fun fromString(title: String?): WeatherCondition =
            entries.find { it.title == title } ?: CLEAR
    }
}

// Combat Engines
data class BattleUnit(
    val uid: String,
    val id: String, // Hero or Mob id
    val name: String,
    val isHero: Boolean,
    val currentHp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val element: Element,
    val currentActionPoints: Float = 0f,
    val statusEffect: String? = null,
    val statusDuration: Int = 0
) : Serializable

enum class QteType {
    ATTACK_RING, // Tap when circle shrinks to golden ring
    BLOCK_SWIPE,  // Swipe/Tap direction in 0.8s
    RHYTHM_ULTIMATE // Press matching symbols beats
}

data class CombatQteState(
    val active: Boolean = false,
    val type: QteType = QteType.ATTACK_RING,
    val multiplier: Float = 1.0f,
    val promptText: String = "",
    val timeLeftMs: Long = 1000,
    val targetDirection: String = "UP", // UP, DOWN, LEFT, RIGHT for Swipe QTE
    val rhythmBeatsCount: Int = 0,
    val rhythmBeatsSuccessful: Int = 0,
    val attackNonce: String = ""
) : Serializable
