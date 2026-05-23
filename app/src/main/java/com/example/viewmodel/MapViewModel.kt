package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.data.GameRepository
import com.example.data.POICache
import com.example.data.OverpassService
import okhttp3.OkHttpClient
import com.example.domain.*
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

class MapViewModel(
    private val context: Context,
    private val repository: GameRepository,
    private val scope: CoroutineScope,
    private val showToast: (String) -> Unit
) {

    private val _isRealGpsEnabled = MutableStateFlow(false)
    val isRealGpsEnabled: StateFlow<Boolean> = _isRealGpsEnabled.asStateFlow()

    private val _currentWeather = MutableStateFlow(WeatherCondition.CLEAR)
    val currentWeather: StateFlow<WeatherCondition> = _currentWeather.asStateFlow()

    private val _isNight = MutableStateFlow(false)
    val isNight: StateFlow<Boolean> = _isNight.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var weatherRotatorJob: Job? = null

    private val okHttpClient = OkHttpClient()
    private val overpassService = OverpassService(okHttpClient)
    private val poiCache = POICache()

    init {
        // Night mode automatic check
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        _isNight.value = hour < 6 || hour >= 18

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Load actual OSM POIs on startup
        scope.launch {
            val prof = repository.getProfileSync()
            if (prof != null) {
                loadPOIsForPosition(prof.currentLatitude, prof.currentLongitude)
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
            Log.e("ShardsMap", "GPS failed", e)
            _isRealGpsEnabled.value = false
            showToast("Ошибка GPS датчика! Проверьте разрешения.")
        }
    }

    private fun removeLocationListener() {
        locationCallback?.let {
            try {
                fusedLocationClient?.removeLocationUpdates(it)
            } catch (e: Throwable) {
                Log.e("ShardsMap", "Failed to remove location listener", e)
            }
        }
        locationCallback = null
    }

    fun triggerVirtualMove(dir: String) {
        scope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            var newLat = prof.currentLatitude
            var newLon = prof.currentLongitude

            val step = 0.0009
            when (dir) {
                "NORTH" -> newLat += step
                "SOUTH" -> newLat -= step
                "EAST" -> newLon += step * 1.2
                "WEST" -> newLon -= step * 1.2
            }

            updateProfileLocation(newLat, newLon)
            val dirWord = when (dir) {
                "NORTH" -> "Север ⬆️"
                "SOUTH" -> "Юг ⬇️"
                "EAST" -> "Восток ➡️"
                else -> "Запад ⬅️"
            }
            showToast("Прогулка: перемещение на $dirWord")
        }
    }

    private fun updateProfileLocation(lat: Double, lon: Double) {
        scope.launch {
            val prof = repository.getProfileSync() ?: return@launch
            val updated = prof.copy(currentLatitude = lat, currentLongitude = lon)
            repository.saveProfile(updated)
            loadPOIsForPosition(lat, lon)
        }
    }

    suspend fun loadPOIsForPosition(lat: Double, lon: Double) {
        val cached = poiCache.get(lat, lon)
        if (cached != null) {
            return
        }
        val osmPois = overpassService.fetchPOIsNear(lat, lon)
        if (osmPois.isNotEmpty()) {
            poiCache.put(lat, lon, osmPois)
            repository.savePOIs(osmPois)
        } else {
            val dbPois = repository.getAllPOIsSync()
            if (dbPois.isEmpty()) {
                generateFallbackPOIs(lat, lon)
            } else {
                poiCache.put(lat, lon, dbPois)
            }
        }
    }

    private suspend fun generateFallbackPOIs(lat: Double, lon: Double) {
        val namesIce = listOf("Замёрзшая Расселина", "Алтарь Аурелии", "Холодные Врата Эфира")
        val namesBloom = listOf("Цветущая Роща Мизу", "Сады Спокойствия", "Оазис Пробужденных")
        val namesBlaze = listOf("Жертвенный Утес Герра", "Кузница Пепла", "Квартал Гнева Стихии")
        val namesMist = listOf("Туманная Преграда", "Мост Потерянных Душ", "Мавзолей Ветров")
        val namesAether = listOf("Узел Соединения Эфира", "Центральное Око Силы", "Осколочный Обелиск")

        val newPois = mutableListOf<PointOfInterest>()
        val types = PoiType.entries

        for (i in 0 until 6) {
            val type = types[i % types.size]
            val element = Element.entries[i % Element.entries.size]

            val targetLat = lat + (if (i % 2 == 0) 1 else -1) * (0.002 + i * 0.001)
            val targetLon = lon + (if (i % 3 == 0) 1 else -1) * (0.002 + i * 0.001)

            val rName = when (element) {
                Element.ICE -> namesIce[i % namesIce.size]
                Element.BLOOM -> namesBloom[i % namesBloom.size]
                Element.BLAZE -> namesBlaze[i % namesBlaze.size]
                Element.MIST -> namesMist[i % namesMist.size]
                Element.AETHER -> namesAether[i % namesAether.size]
            }

            newPois.add(
                PointOfInterest(
                    id = "proc_fall_$i",
                    name = rName,
                    type = type,
                    element = element,
                    latitude = targetLat,
                    longitude = targetLon,
                    minLevel = (i * 2) + 1,
                    maxLevel = (i * 2) + 5,
                    cooldownUntil = 0
                )
            )
        }
        repository.savePOIs(newPois)
        poiCache.put(lat, lon, newPois)
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0
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

    fun clear() {
        removeLocationListener()
        weatherRotatorJob?.cancel()
    }
}
