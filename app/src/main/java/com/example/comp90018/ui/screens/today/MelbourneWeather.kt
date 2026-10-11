package com.example.comp90018.ui.screens.today

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MelbourneWeather(
    val temperatureCelsius: Int,
    val summary: String,
    val highCelsius: Int,
    val lowCelsius: Int,
    val updatedAt: String
)

suspend fun fetchMelbourneWeather(): MelbourneWeather = withContext(Dispatchers.IO) {
    val address = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=-37.8136&longitude=144.9631" +
        "&current=temperature_2m,weather_code" +
        "&daily=temperature_2m_max,temperature_2m_min" +
        "&timezone=Australia%2FMelbourne&forecast_days=1"
    val connection = (URL(address).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
    }

    try {
        val status = connection.responseCode
        val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        check(status in 200..299) { "Weather service returned HTTP $status" }

        val response = JSONObject(body)
        val current = response.getJSONObject("current")
        val daily = response.getJSONObject("daily")
        val localTime = LocalDateTime.parse(current.getString("time"))
        MelbourneWeather(
            temperatureCelsius = current.getDouble("temperature_2m").toInt(),
            summary = weatherDescription(current.getInt("weather_code")),
            highCelsius = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt(),
            lowCelsius = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt(),
            updatedAt = localTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        )
    } finally {
        connection.disconnect()
    }
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorms"
    else -> "Weather unavailable"
}
