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
          node["amenity"~"place_of_worship|theatre|library|cinema"](around:$r,$lat,$lon);
          node["natural"~"peak|spring|waterfall|cave_entrance"](around:$r,$lat,$lon);
          way["leisure"~"park|garden|nature_reserve"](around:$r,$lat,$lon);
          way["building"~"cathedral|church|castle|mosque|synagogue"](around:$r,$lat,$lon);
        );
        out center 40;
    """.trimIndent()

    private fun parseResponse(json: String): List<PointOfInterest> {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val result = mutableListOf<PointOfInterest>()

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val type = el.getString("type")
            val id = el.getLong("id")

            val lat = if (type == "way") {
                el.optJSONObject("center")?.optDouble("lat") ?: continue
            } else {
                el.optDouble("lat")
            }
            if (lat == null || lat.isNaN()) continue

            val lon = if (type == "way") {
                el.optJSONObject("center")?.optDouble("lon") ?: continue
            } else {
                el.optDouble("lon")
            }
            if (lon == null || lon.isNaN()) continue

            val tagsJson = el.optJSONObject("tags") ?: continue
            val tags = buildMap<String, String> {
                tagsJson.keys().forEach { key -> put(key, tagsJson.getString(key)) }
            }

            if (tags["name"] == null && tags["name:ru"] == null && tags["historic"] == null && tags["tourism"] == null) continue

            val osmEl = OverpassElement(id, type, lat, lon, tags)
            result.add(osmEl.toGamePOI())
        }

        return result
    }
}

fun OverpassElement.determineElement(): Element {
    return when {
        tags["natural"] == "water" || tags["natural"] == "spring" || tags["waterway"] != null || tags["amenity"] == "fountain" -> Element.ICE
        tags["leisure"] == "park" || tags["leisure"] == "garden" || tags["natural"] == "wood" || tags["natural"] == "forest" || tags["landuse"] == "forest" -> Element.BLOOM
        tags["historic"] != null || tags["amenity"] == "place_of_worship" || tags["religion"] != null -> Element.MIST
        tags["tourism"] == "viewpoint" || tags["natural"] == "peak" || tags["man_made"] == "tower" -> Element.BLAZE
        tags["tourism"] != null || tags["amenity"] == "theatre" || tags["amenity"] == "museum" || tags["amenity"] == "library" -> Element.AETHER
        else -> Element.entries[(id % Element.entries.size).coerceAtLeast(0).toInt()]
    }
}

fun OverpassElement.toGamePOI(): PointOfInterest {
    val stableId = "osm_${type}_${id}"
    val poiType = when {
        tags["historic"] != null -> PoiType.ABYSSAL_GATE
        tags["tourism"] == "museum" -> PoiType.NEXUS_POINT
        tags["leisure"] == "park" || tags["leisure"] == "garden" -> PoiType.SANCTUM
        tags["amenity"] == "place_of_worship" -> PoiType.RIFT
        tags["natural"] != null -> PoiType.CHAOS_SPIKE
        tags["tourism"] == "viewpoint" -> PoiType.CHAOS_SPIKE
        else -> PoiType.RIFT
    }

    val element = determineElement()
    val realNameStr = tags["name:ru"] ?: tags["name"] ?: generateNameFromTags(tags, poiType)
    val displayGameName = realNameStr

    val importance = tags["importance"]?.toFloatOrNull() ?: 0.5f
    val level = (1 + (importance * 20).toInt()).coerceIn(1, 25)

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
        tags["historic"] == "castle" -> "Замок"
        tags["historic"] == "ruins" -> "Руины"
        tags["historic"] == "monument" -> "Памятник"
        tags["amenity"] == "place_of_worship" -> when (tags["religion"]) {
            "christian" -> "Церковь"
            "muslim" -> "Мечеть"
            "jewish" -> "Синагога"
            else -> "Святилище"
        }
        tags["leisure"] == "park" -> "Парк"
        tags["tourism"] == "museum" -> "Музей"
        tags["natural"] == "peak" -> "Вершина"
        tags["tourism"] == "viewpoint" -> "Смотровая"
        else -> type.label
    }
    return "$base Бездны"
}
