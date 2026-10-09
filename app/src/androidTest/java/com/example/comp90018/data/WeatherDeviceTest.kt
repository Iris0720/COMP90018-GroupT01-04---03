package com.example.comp90018.data

import androidx.test.platform.app.InstrumentationRegistry
import com.example.comp90018.data.weather.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.IOException

class WeatherDeviceTest {
    @Test fun diskCacheSurvivesNewInstanceAndSupportsOfflineFallback() = runBlocking {
        // Instrumentation runs as the target UID; use a separate filename in its private directory.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val now = System.currentTimeMillis() / 1000
        val value = WeatherSnapshot(-37.81, 144.96, 18.0, 5.0, 2, now - 901, now - 901)
        AndroidWeatherCache(context, "weather-qa-persistence.json").write(value)
        val restoredCache = AndroidWeatherCache(context, "weather-qa-persistence.json")
        assertEquals(value, restoredCache.read())
        val repo = WeatherRepository(WeatherSource { _, _, _ -> throw IOException("offline") }, restoredCache, { now })
        assertEquals(WeatherResult.Available(value, true, true), repo.getWeather(-37.81, 144.96))
    }

    @Test fun realWeatherApiThroughAndroidEngine() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveWeather") == "true")
        OpenMeteoWeatherSource().use { source ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val result = WeatherRepository(source, AndroidWeatherCache(context, "weather-qa-live.json"))
                .getWeather(-37.81, 144.96, true)
            assertTrue("Expected real API response, not cache fallback: $result",
                result is WeatherResult.Available && !result.fromCache && !result.stale)
        }
    }
}
