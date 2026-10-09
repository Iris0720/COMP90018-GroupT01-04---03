package com.example.comp90018.data.weather

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class WeatherRepositoryTest {
    private val now = 1_800_000_000L
    private fun snapshot(age: Long = 0) = WeatherSnapshot(-37.81, 144.96, 18.2, 12.0, 2, now - age, now - age)
    private class Cache(var value: WeatherSnapshot? = null) : WeatherCache {
        override suspend fun read() = value
        override suspend fun write(snapshot: WeatherSnapshot) { value = snapshot }
    }
    private val offline = WeatherSource { _, _, _ -> throw IOException("offline") }

    @Test fun freshCacheSkipsNetwork() = runBlocking {
        val result = WeatherRepository(offline, Cache(snapshot()), { now }).getWeather(-37.813, 144.963)
        assertEquals(WeatherResult.Available(snapshot(), false, true), result)
    }
    @Test fun staleCacheIsExplicitAfterFailure() = runBlocking {
        val old = snapshot(901)
        assertEquals(WeatherResult.Available(old, true, true),
            WeatherRepository(offline, Cache(old), { now }).getWeather(-37.81, 144.96))
    }
    @Test fun expiredOrWrongLocationCacheIsNotDisplayed() = runBlocking {
        for (cached in listOf(snapshot(86401), snapshot().copy(latitude = 0.0), snapshot(-1))) {
            assertTrue(WeatherRepository(offline, Cache(cached), { now }).getWeather(-37.81, 144.96) is WeatherResult.Unavailable)
        }
    }
    @Test fun invalidCoordinatesNeverCallProvider() = runBlocking {
        val repo = WeatherRepository(WeatherSource { _, _, _ -> error("must not call") }, Cache(), { now })
        for (lat in listOf(Double.NaN, Double.POSITIVE_INFINITY, 91.0)) {
            assertEquals(WeatherResult.Unavailable(WeatherResult.Reason.INVALID_LOCATION), repo.getWeather(lat, 0.0))
        }
    }
    @Test fun successfulRequestIsRoundedAndCached() = runBlocking {
        val cache = Cache()
        val source = WeatherSource { lat, lon, time ->
            assertEquals(-37.81, lat, 0.0); assertEquals(144.96, lon, 0.0); assertEquals(now, time)
            snapshot()
        }
        assertEquals(WeatherResult.Available(snapshot(), false, false),
            WeatherRepository(source, cache, { now }).getWeather(-37.813, 144.963))
        assertEquals(snapshot(), cache.value)
    }
    @Test fun forcedRefreshAndBrokenCacheStillReturnNetworkData() = runBlocking {
        val brokenCache = object : WeatherCache {
            override suspend fun read(): WeatherSnapshot? = throw IOException()
            override suspend fun write(snapshot: WeatherSnapshot) { throw IOException() }
        }
        assertTrue(WeatherRepository(WeatherSource { _, _, _ -> snapshot() }, brokenCache, { now })
            .getWeather(-37.81, 144.96, true) is WeatherResult.Available)
    }
    @Test fun forceRefreshBypassesFreshCache() = runBlocking {
        val result = WeatherRepository(offline, Cache(snapshot()), { now }).getWeather(-37.81, 144.96, true)
        assertEquals(WeatherResult.Available(snapshot(), true, true), result)
    }
    @Test fun cancellationIsNotConvertedToOffline() = runBlocking {
        try {
            WeatherRepository(WeatherSource { _, _, _ -> throw CancellationException() }, Cache(), { now })
                .getWeather(-37.81, 144.96)
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }
    @Test fun concurrentCallsShareFreshCache() = runBlocking {
        var calls = 0
        val repo = WeatherRepository(WeatherSource { _, _, _ -> calls++; snapshot() }, Cache(), { now })
        coroutineScope { List(10) { async { repo.getWeather(-37.81, 144.96) } }.awaitAll() }
        assertEquals(1, calls)
    }
    @Test fun malformedValuesAreRejected() = runBlocking {
        val bad = snapshot().copy(temperatureCelsius = Double.NaN)
        assertTrue(WeatherRepository(WeatherSource { _, _, _ -> bad }, Cache(), { now })
            .getWeather(-37.81, 144.96) is WeatherResult.Unavailable)
        assertEquals("Unknown conditions", snapshot().copy(weatherCode = 999).description)
    }
    @Test fun providerRequestUsesExplicitUnitsAndParsesResponse() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals("api.open-meteo.com", request.url.host)
            assertEquals("celsius", request.url.parameters["temperature_unit"])
            assertEquals("kmh", request.url.parameters["wind_speed_unit"])
            assertEquals("unixtime", request.url.parameters["timeformat"])
            respond("""{"current":{"time":1800000000,"temperature_2m":18.2,"weather_code":2,"wind_speed_10m":12.0},"extra":true}""")
        })
        OpenMeteoWeatherSource(client).use { assertEquals(snapshot(), it.fetch(-37.81, 144.96, now)) }
    }
    @Test fun providerErrorsAndMissingFieldsUseCache() = runBlocking {
        for ((status, body) in listOf(HttpStatusCode.TooManyRequests to "{}", HttpStatusCode.OK to "{\"current\":{}}")) {
            OpenMeteoWeatherSource(HttpClient(MockEngine { respond(body, status) })).use { source ->
                val result = WeatherRepository(source, Cache(snapshot(901)), { now }).getWeather(-37.81, 144.96)
                assertEquals(WeatherResult.Available(snapshot(901), true, true), result)
            }
        }
    }
}
