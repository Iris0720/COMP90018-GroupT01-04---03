# Weather integration handoff

The backend provides `WeatherRepository`, `OpenMeteoWeatherSource`,
`AndroidWeatherCache` and an optional stateless Compose `WeatherCard`.
No API key or Supabase account is required for weather.

## Connect to a screen

Create the source/cache/repository once in the application dependency container:

```kotlin
val weatherSource = OpenMeteoWeatherSource()
val weatherRepository = WeatherRepository(
    source = weatherSource,
    cache = AndroidWeatherCache(applicationContext)
)
```

The feature ViewModel calls this from a coroutine using coordinates supplied by
the existing location/permission flow:

```kotlin
val result = weatherRepository.getWeather(latitude, longitude)
// User presses refresh:
val refreshed = weatherRepository.getWeather(latitude, longitude, forceRefresh = true)
```

Expose `WeatherResult?` and loading state from the screen's ViewModel, then:

```kotlin
WeatherCard(
    result = weatherResult,
    isLoading = weatherLoading,
    onRefresh = { viewModel.refreshWeather() }
)
```

These variables/functions are integration placeholders, not additional APIs
already present in this branch. The frontend owns the screen placement and
ViewModel. Keep activity tracking usable while weather loads/fails. Do not call
the suspend method directly from a Composable body or once per GPS update.
Call once on screen entry and on explicit refresh. Close `weatherSource` when
its application-level owner is disposed. Do not create/close it on every refresh.

## Behaviour agreed for the frontend

| Result | Display |
|---|---|
| `Available(stale=false)` | Temperature in °C, WMO condition, wind in km/h and observation time |
| `Available(stale=true)` | Last known values and an explicit stale warning; allow refresh |
| `Unavailable(INVALID_LOCATION)` | Explain that a valid location is needed |
| `Unavailable(NETWORK_OR_SERVICE)` | Weather unavailable; leave activity controls enabled |
| Loading | Loading indicator; existing values can stay visible |

The cache persists in app-private, no-backup storage. A 15-minute fresh cache
avoids unnecessary requests. A failed request can fall back to the same rounded
location for at most 24 hours; a different location, future timestamp, expired
cache or invalid numeric value is rejected. Location is rounded to 2 decimal
places before sending/storing it (approximately kilometre-scale precision,
depending on latitude). Only the latest location is cached. Requests time out
after 10 seconds. Concurrent ordinary requests share the newly fetched cache;
explicit forced refreshes should be disabled while a refresh is running.

Canonical units are °C and km/h; convert in presentation if the app uses imperial
settings. Weather is informational, not a medical recommendation or a safety
guarantee. Current conditions are model-derived, not a phone sensor reading.

## Attribution and provider limits

The supplied card links `Weather data by Open-Meteo (CC BY 4.0)` to the provider.
Keep this attribution if replacing the component. The free endpoint is for
non-commercial use and has usage limits (600/minute, 5,000/hour, 10,000/day).
For this coursework, cache and refresh sparingly; reassess the plan before any
commercial release. HTTP errors including rate limits use the stale fallback.

Sources: [Forecast API documentation](https://open-meteo.com/en/docs),
[API terms and attribution](https://open-meteo.com/en/terms).

## Tests and demonstration

`WeatherRepositoryTest` checks real HTTP request formatting using a mock engine,
JSON parsing, cache freshness/expiry, wrong-location fallback rejection, rounding,
force refresh, errors, corrupt cache, invalid values, cancellation and concurrent
requests. `WeatherDeviceTest` checks persistent Android cache and, when opted in,
the live API using the Android HTTP engine.

For the A2 demo, show weather loading online, disconnect the device, refresh and
show the stale warning, then continue tracking. These screen-level interactions
must be verified in the merged app on a physical device; repository tests alone
do not establish that the integrated UI works.
