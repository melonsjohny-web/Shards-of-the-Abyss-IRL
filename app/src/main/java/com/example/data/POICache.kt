package com.example.data

import com.example.domain.PointOfInterest

class POICache {
    private data class CacheEntry(
        val pois: List<PointOfInterest>,
        val centerLat: Double,
        val centerLon: Double,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var cache: CacheEntry? = null
    private val CACHE_RADIUS = 1000.0       // 1000 meters cache radius limits API updates
    private val CACHE_TTL = 10 * 60 * 1000L  // 10 minutes cache lifespan

    fun get(lat: Double, lon: Double): List<PointOfInterest>? = synchronized(this) {
        val entry = cache ?: return null
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL) return null

        val dist = haversine(lat, lon, entry.centerLat, entry.centerLon)
        if (dist > CACHE_RADIUS) return null // stepped outside cached radius

        return entry.pois
    }

    fun put(lat: Double, lon: Double, pois: List<PointOfInterest>) = synchronized(this) {
        cache = CacheEntry(pois, lat, lon)
    }

    fun invalidate() = synchronized(this) {
        cache = null
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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
