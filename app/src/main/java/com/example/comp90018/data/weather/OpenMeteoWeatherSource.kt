package com.example.comp90018.data.weather

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable

/** Close when its application-level owner is disposed; no API key is required. */
class OpenMeteoWeatherSource(private val client: HttpClient = newClient()) : WeatherSource, Closeable {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetch(latitude: Double, longitude: Double, now: Long): WeatherSnapshot {
        val response = client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,weather_code,wind_speed_10m")
            parameter("temperature_unit", "celsius")
            parameter("wind_speed_unit", "kmh")
            parameter("timeformat", "unixtime")
            parameter("timezone", "UTC")
        }
        check(response.status.value in 200..299) { "Weather service unavailable" }
        val current = json.decodeFromString<Response>(response.bodyAsText()).current
        return WeatherSnapshot(latitude, longitude, current.temperature, current.windSpeed,
            current.code, current.time, now)
    }

    override fun close() = client.close()

    @Serializable private data class Response(val current: Current)
    @Serializable private data class Current(
        val time: Long,
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("weather_code") val code: Int,
        @SerialName("wind_speed_10m") val windSpeed: Double
    )

    companion object {
        private fun newClient() = HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 10_000
            }
        }
    }
}
