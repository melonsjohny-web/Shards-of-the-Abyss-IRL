package com.example.data

import com.example.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.io.Serializable

data class OverpassElement(
    val id: Long,
    val type: String,
    val lat: Double,
    val lon: Double,
    val tags: Map<String, String>
) : Serializable

class OverpassService(private val okHttpClient: OkHttpClient) {

    private val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

    suspend fun fetchPOIsNear(lat: Double, lon: Double, radiusM: Int = 1500): List<PointOfInterest> {
        val query = buildQuery(lat, lon, radiusM)

        val request = Request.Builder()
            .url(OVERPASS_URL)
            .post(FormBody.Builder().add("data", query).build())
            .build()

        return withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("Overpass error: ${resp.code}")
                    val bodyStr = resp.body?.string() ?: throw IOException("Empty body from Overpass")
                    parseResponse(bodyStr)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun buildQuery(lat: Double, lon: Double, r: Int): String = """
        [out:json][timeout:15];
        (
          node["historic"](around:$r,$lat,$lon);
          node["tourism"~"attraction|museum|monument|viewpoint|artwork"](around:$r,$lat,$lon);
          node["amenity"~"place_of_worship|theatre|library|cinema|bank|atm|cafe|restaurant|pub|bar|fast_food"](around:$r,$lat,$lon);
          node["natural"~"peak|spring|waterfall|cave_entrance"](around:$r,$lat,$lon);
          node["shop"](around:$r,$lat,$lon);
          way["leisure"~"park|garden|nature_reserve"](around:$r,$lat,$lon);
          way["building"~"cathedral|church|castle|mosque|synagogue"](around:$r,$lat,$lon);
          way["shop"](around:$r,$lat,$lon);
          way["amenity"~"cafe|restaurant|pub|bar|bank"](around:$r,$lat,$lon);
        );
        out center 60;
    """.trimIndent()

    private fun parseResponse(json: String): List<PointOfInterest> {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val rawList = mutableListOf<PointOfInterest>()

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val type = el.getString("type")
            val id = el.getLong("id")

            val lat = if (type == "way") {
                el.optJSONObject("center")?.optDouble("lat", Double.NaN) ?: Double.NaN
            } else {
                el.optDouble("lat", Double.NaN)
            }
            if (lat.isNaN()) continue

            val lon = if (type == "way") {
                el.optJSONObject("center")?.optDouble("lon", Double.NaN) ?: Double.NaN
            } else {
                el.optDouble("lon", Double.NaN)
            }
            if (lon.isNaN()) continue

            val tagsJson = el.optJSONObject("tags") ?: continue
            val tags = buildMap<String, String> {
                tagsJson.keys().forEach { key -> put(key, tagsJson.getString(key)) }
            }

            if (tags["name"] == null && tags["name:ru"] == null && tags["historic"] == null && tags["tourism"] == null) continue

            val osmEl = OverpassElement(id, type, lat, lon, tags)
            rawList.add(osmEl.toGamePOI())
        }

        // De-duplicate POIs that are extremely close to each other (e.g. <30 meters)
        // or have the same name within 120 meters (e.g., node+way representation of same shop/park)
        val filteredList = mutableListOf<PointOfInterest>()
        for (poi in rawList) {
            val isDuplicate = filteredList.any { existing ->
                val dist = calculateDistanceMeters(poi.latitude, poi.longitude, existing.latitude, existing.longitude)
                dist < 30.0 || (poi.name == existing.name && dist < 120.0)
            }
            if (!isDuplicate) {
                filteredList.add(poi)
            }
        }

        return filteredList
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val radLat1 = Math.toRadians(lat1)
        val radLat2 = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(radLat1) * Math.cos(radLat2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

fun OverpassElement.determineElement(): Element {
    val amenity = tags["amenity"] ?: ""
    return when {
        tags["natural"] == "water" || tags["natural"] == "spring" || tags["waterway"] != null || tags["amenity"] == "fountain" -> Element.ICE
        tags["leisure"] == "park" || tags["leisure"] == "garden" || tags["natural"] == "wood" || tags["natural"] == "forest" || tags["landuse"] == "forest" -> Element.BLOOM
        amenity in listOf("cafe", "restaurant", "fast_food", "food_court") -> Element.BLOOM
        amenity in listOf("pub", "bar") -> Element.BLAZE
        tags["historic"] != null || tags["amenity"] == "place_of_worship" || tags["religion"] != null -> Element.MIST
        tags["tourism"] == "viewpoint" || tags["natural"] == "peak" || tags["man_made"] == "tower" -> Element.BLAZE
        tags["tourism"] != null || tags["amenity"] == "theatre" || tags["amenity"] == "museum" || tags["amenity"] == "library" -> Element.AETHER
        else -> Element.entries[(id % Element.entries.size).coerceAtLeast(0).toInt()]
    }
}

fun OverpassElement.calculateLevel(): Int {
    val base = when {
        tags["historic"] == "castle" || tags["historic"] == "fortress" -> 20
        tags["historic"] == "ruins" -> 12
        tags["tourism"] == "museum" -> 15
        tags["amenity"] == "place_of_worship" -> when (tags["religion"]) {
            "christian" -> if (tags["building"] == "cathedral") 18 else 10
            "muslim" -> 14
            else -> 8
        }
        tags["leisure"] == "nature_reserve" -> 16
        tags["leisure"] == "park" -> 6
        tags["tourism"] == "viewpoint" -> 10
        tags["natural"] == "peak" -> 20
        tags["tourism"] == "attraction" -> 12
        else -> 5
    }
    return (base + (id % 5).toInt() - 2).coerceIn(1, 25)
}

fun OverpassElement.toGamePOI(): PointOfInterest {
    val stableId = "osm_${type}_${id}"
    val poiType = when {
        tags["shop"] != null -> PoiType.MERCHANT_CARAVAN
        tags["amenity"] in listOf("cafe", "restaurant", "pub", "bar", "fast_food", "food_court") -> PoiType.TAVERN
        tags["amenity"] in listOf("bank", "atm") -> PoiType.GUILD_VAULT
        tags["leisure"] in listOf("park", "garden", "nature_reserve") || tags["natural"] in listOf("wood", "forest") -> PoiType.SACRED_GROVE
        tags["historic"] != null -> PoiType.ABYSSAL_GATE
        tags["tourism"] == "museum" -> PoiType.NEXUS_POINT
        tags["amenity"] == "place_of_worship" -> PoiType.SANCTUM
        tags["natural"] != null || tags["tourism"] == "viewpoint" -> PoiType.CHAOS_SPIKE
        else -> if (id % 4L == 0L) PoiType.RANDOM_ENCOUNTER else PoiType.RIFT
    }

    val element = determineElement()
    val realNameStr = tags["name:ru"] ?: tags["name"] ?: generateNameFromTags(tags, poiType)
    val displayGameName = realNameStr

    val level = calculateLevel()

    return PointOfInterest(
        id = stableId,
        name = displayGameName,
        type = poiType,
        element = element,
        latitude = lat,
        longitude = lon,
        minLevel = level,
        maxLevel = (level + 4).coerceIn(1, 40),
        lastVisitedTimestamp = 0,
        isCapturedByGuild = false,
        capturedGuildName = null,
        osmTags = tags,
        realName = realNameStr,
        cooldownUntil = 0
    )
}

fun generateNameFromTags(tags: Map<String, String>, type: PoiType): String {
    val base = when {
        tags["shop"] != null -> "Обитель купцов Эфира"
        tags["amenity"] in listOf("cafe", "restaurant", "pub", "bar", "fast_food") -> "Эфирный Приют"
        tags["amenity"] in listOf("bank", "atm") -> "Хранилище Осколков"
        tags["leisure"] in listOf("park", "garden") -> "Таинственный Лес"
        tags["historic"] == "castle" -> "Цитадель"
        tags["historic"] == "ruins" -> "Развалины древних"
        tags["historic"] == "monument" -> "Стела Безмолвия"
        tags["amenity"] == "place_of_worship" -> "Оракул Эфира"
        else -> type.label
    }
    return base
}
