# Backend verification and team integration

Verified locally on 9 October 2026 (Australia/Melbourne), on
`feature/supabase-backend`. This records backend evidence, not completion of
the merged app or its submission to Canvas.

## Implemented work

- Supabase profile, activity/route, health check-in and meal repositories;
  authenticated ownership, input checks, server timestamp-safe writes,
  idempotent UUID upserts, route batching and paginated reads.
- Open-Meteo current conditions, persistent private cache, explicit stale/error
  states, timeouts, rounded coordinates and coroutine cancellation support.
- Optional stateless Compose `WeatherCard` with refresh, observation time and
  provider attribution. The frontend chooses its screen and connects its ViewModel.
- Android INTERNET permission and GitHub Actions unit test, APK and lint checks.

## Real cloud test evidence

Local unit tests: **23 passed**, zero failures. Debug APK: built successfully.
Android lint: **zero errors**, 12 dependency/update warnings; dependency upgrades
were not introduced merely to silence version notifications.

`WeatherDeviceTest`: **2 passed**, checking disk-cache restoration through a new
instance, stale fallback under a simulated network failure, and the actual
Open-Meteo endpoint through the Android HTTP engine. Tests use separate QA
filenames in the target app's private directory. The final weather verification
also completed unit tests, APK assembly and lint successfully.

`BackendLiveTest.sdkRoundTripAndServerRlsIsolation` passed on the
Pixel_10_Pro_XL Android emulator using the actual project and Kotlin Supabase SDK.
The final backend run at approximately 15:15 Melbourne time checked:

| Check | Result |
|---|---|
| Signed-out repository access | Explicit SIGNED_OUT result |
| Profile update/read | Correct owner and daily target |
| Activity save twice with same UUID | One session, updated distance |
| Route upload/read | 205 ordered points across multiple read pages |
| Health check-in and meal | Saved and read back |
| User B reads A's rows with direct Data API requests | No rows, across all five tables |
| User B updates/deletes A's activity | No effect on A's row |
| Forged owner on insert | Server denied |
| Route attached to another owner's session | Server denied |
| Anonymous table access | Server denied |
| Session deletion | Route cascade verified |

The test deliberately bypasses repository filters in negative cases, so it
checks server RLS rather than relying only on client-side filtering.
Two successful backend test runs created four synthetic QA app users/profiles.
Only generated business records were deleted; pre-existing team data was not
targeted. QA auth users/profiles remain identifiable as `backend-qa-...@example.com`
and `Backend QA A/B`. No service-role key was used or committed.

## Reproduce

```sh
# Ordinary checks, also run by GitHub Actions:
./gradlew testDebugUnitTest assembleDebug lintDebug

# Opt-in cloud and device checks (requires local config and connected device):
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.liveBackend=true \
  -Pandroid.testInstrumentationRunnerArguments.liveWeather=true
```

Reports are generated under `app/build/reports/tests/testDebugUnitTest/`,
`app/build/reports/androidTests/connected/debug/` and
`app/build/reports/lint-results-debug.html`. The debug APK is
`app/build/outputs/apk/debug/app-debug.apk`. They are ignored build outputs,
not source files to commit. Live tests are skipped without their opt-in flags.

## Remaining integration responsibilities

1. Merge/review this branch with authentication and feature branches. Inject
   Zarif's existing signed-in `SupabaseProvider.client` into the repository.
2. Room owner connects cloud sync using stable UUIDs and retry status. Session
   and route requests are not atomic; retain pending local records after failure.
3. Frontend connects `WeatherCard`/ViewModel, supplies location after permission
   handling and shows stale/error states without disabling activity tracking.
4. Complete the merged app's physical-device tracking, restart, account switch
   and offline demonstrations. Emulator repository tests do not replace these.
5. Use the test evidence, architecture and limitations in the A2 report; retain
   screenshots/demo evidence from the final app and explain the code you submit.

Teammate reference: [backend integration](../supabase/INTEGRATION_GUIDE.md),
[weather handoff](WEATHER_HANDOFF.md).

## Message for the team

> I have added and tested the Supabase repositories plus weather API/offline
> caching on `feature/supabase-backend`. Real SDK tests passed for profiles,
> activities/routes, health and meals, including two-user RLS isolation and
> idempotent saves. Please use the same signed-in Supabase client from Zarif's
> auth implementation when creating `SupabaseBackendRepository`. Keep Room as
> the local source of truth and preserve stable UUIDs for retries. The weather
> module needs no API key and includes an optional WeatherCard; the frontend
> can connect it to the existing location flow. Integration instructions are
> in `supabase/INTEGRATION_GUIDE.md` and `docs/WEATHER_HANDOFF.md`. We still need
> to merge the branches and test the complete app on a physical device.
