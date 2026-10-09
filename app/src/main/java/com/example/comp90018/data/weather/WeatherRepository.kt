package com.example.comp90018.data.weather

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class WeatherSnapshot(
    val latitude: Double,
    val longitude: Double,
    val temperatureCelsius: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val observedAtEpochSeconds: Long,
    val fetchedAtEpochSeconds: Long
) {
    val description: String get() = when (weatherCode) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy or overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Thunderstorm"
        else -> "Unknown conditions"
    }
}

sealed interface WeatherResult {
    data class Available(val weather: WeatherSnapshot, val stale: Boolean, val fromCache: Boolean) : WeatherResult
    data class Unavailable(val reason: Reason) : WeatherResult
    enum class Reason { INVALID_LOCATION, NETWORK_OR_SERVICE }
}

fun interface WeatherSource {
    suspend fun fetch(latitude: Double, longitude: Double, now: Long): WeatherSnapshot
}

interface WeatherCache {
    suspend fun read(): WeatherSnapshot?
    suspend fun write(snapshot: WeatherSnapshot)
}

/** One instance per application. Caller supplies coordinates from the existing permission flow. */
class WeatherRepository(
    private val source: WeatherSource,
    private val cache: WeatherCache,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) {
    private val mutex = Mutex()

    suspend fun getWeather(latitude: Double, longitude: Double, forceRefresh: Boolean = false): WeatherResult {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return WeatherResult.Unavailable(WeatherResult.Reason.INVALID_LOCATION)
        }
        // Do not send or store a precise route/GPS fix with the weather provider.
        val lat = round(latitude * 100) / 100
        val lon = round(longitude * 100) / 100
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val now = nowSeconds()
                val cached = safely { cache.read() }?.takeIf {
                    it.latitude == lat && it.longitude == lon && it.isValid(now) &&
                        now - it.fetchedAtEpochSeconds <= MAX_STALE_SECONDS
                }
                if (!forceRefresh && cached != null && now - cached.fetchedAtEpochSeconds < FRESH_SECONDS) {
                    return@withLock WeatherResult.Available(cached, stale = false, fromCache = true)
                }
                val fresh = safely { source.fetch(lat, lon, now) }?.takeIf {
                    it.latitude == lat && it.longitude == lon && it.isValid(now)
                }
                if (fresh != null) {
                    // A full disk must not hide a successful network response.
                    safely { cache.write(fresh) }
                    WeatherResult.Available(fresh, stale = false, fromCache = false)
                } else if (cached != null) {
                    WeatherResult.Available(cached, stale = true, fromCache = true)
                } else {
                    WeatherResult.Unavailable(WeatherResult.Reason.NETWORK_OR_SERVICE)
                }
            }
        }
    }

    private fun WeatherSnapshot.isValid(now: Long): Boolean =
        temperatureCelsius.isFinite() && windSpeedKmh.isFinite() && windSpeedKmh >= 0 &&
            fetchedAtEpochSeconds in 0..now && observedAtEpochSeconds in 0..(now + 900) &&
            now - observedAtEpochSeconds <= MAX_STALE_SECONDS

    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    companion object {
        const val FRESH_SECONDS = 15 * 60L
        const val MAX_STALE_SECONDS = 24 * 60 * 60L
        const val ATTRIBUTION = "Weather data by Open-Meteo (CC BY 4.0)"
        const val ATTRIBUTION_URL = "https://open-meteo.com/"
    }
}
