# Android backend integration guide

This branch provides the remote half of the app's local-first design. Feature
code saves to Room first, then calls `BackendRepository` to synchronise the
same client-generated UUID to Supabase. A remote failure must not delete or
hide the local Room record. Retry only when `error.retryable` is true.

## Create the repository

Create one shared instance after the Supabase configuration has been added to
`local.properties`:

```kotlin
val backendRepository: BackendRepository = SupabaseBackendRepository()
```

After merging Zarif's authentication branch, inject its **same signed-in client**:

```kotlin
val backendRepository: BackendRepository =
    SupabaseBackendRepository(com.example.comp90018.data.SupabaseProvider.client)
```

Creating another default client can give repositories a different auth session.
The injected constructor is the recommended team integration path.

The repository waits for Supabase Auth to restore its session. Every write
takes `owner_id` from that authenticated session; UI code never supplies or
chooses another user's id.

## Save a finished activity

The tracking team already produces an `ActivitySummary`. Map its values to the
backend input after Room has saved them:

```kotlin
val result = backendRepository.upsertActivity(
    session = ActivitySessionInput(
        id = summary.sessionId,
        startedAt = Instant.ofEpochMilli(summary.startedAtEpochMillis).toString(),
        finishedAt = Instant.ofEpochMilli(summary.finishedAtEpochMillis).toString(),
        activityType = summary.activityType.lowercase(),
        sessionState = "complete",
        durationSeconds = summary.activeDurationMillis / 1_000,
        distanceMetres = summary.distanceMetres.toLong(),
        steps = summary.recordedSteps
    ),
    routePoints = persistedRoutePoints.map { point ->
        RoutePointInput(
            id = point.id,
            sessionId = summary.sessionId,
            segmentNumber = point.segmentNumber,
            sequenceNumber = point.sequenceNumber,
            recordedAt = Instant.ofEpochMilli(point.recordedAtEpochMillis).toString(),
            latitude = point.latitude,
            longitude = point.longitude,
            accuracyMetres = point.accuracyMetres
        )
    }
)
```

`persistedRoutePoints` represents the final Room route entity agreed during
integration; it is not part of this backend branch. The current tracking
branch's `TrackPoint` contains only latitude and longitude, so the integration
owner must add stable id, segment, sequence, timestamp and optional accuracy
before route history can be faithfully restored. The same stable UUID makes a
retry an upsert rather than a duplicate insert.

Session and route batches are separate HTTP requests, not one database
transaction. If a later batch fails, earlier rows may already exist. Keep the
Room record pending and retry the entire payload with identical UUIDs and
segment/sequence pairs. Route uploads use batches of 500; reads use pages of
200 to avoid truncating long routes at the Data API's default response limit.
Only sync completed, immutable routes; do not append/change the route while
reading paginated history. This branch does not implement a Room sync worker.

## Save health and meal records

```kotlin
backendRepository.upsertHealthCheckIn(
    HealthCheckInInput(
        id = localRecord.id,
        recordedAt = Instant.ofEpochMilli(localRecord.recordedAt).toString(),
        feeling = localRecord.feeling.name.lowercase(),
        breathingDifficulty = localRecord.breathing.name.lowercase(),
        fatigue = localRecord.fatigue.name.lowercase(),
        condition = localRecord.condition.name.lowercase()
    )
)

backendRepository.upsertMeal(
    MealEntryInput(
        id = meal.id,
        recordedAt = Instant.now().toString(),
        name = meal.name,
        serving = meal.serving,
        calories = meal.calories,
        proteinGrams = meal.proteinGrams,
        carbohydrateGrams = meal.carbohydrateGrams,
        fatGrams = meal.fatGrams,
        sourceLabel = "LogMeal estimate, user confirmed"
    )
)
```

## Handle results

```kotlin
when (val result = backendRepository.getActivities()) {
    is BackendResult.Success -> showActivities(result.value)
    is BackendResult.Failure -> {
        if (result.error.retryable) scheduleSyncRetry()
        showNonBlockingCloudStatus(result.error.message)
    }
}
```

`SIGNED_OUT`, `INVALID_INPUT`, `CONFIGURATION` and unclassified remote failures
are not automatically retryable. `NETWORK`, HTTP 429 and HTTP 5xx are retryable
with a delayed/backoff retry. Inspect the session or
input after a remote rejection. Coroutine cancellation propagates normally.
Guest mode should continue using Room without calling the remote repository.

## Implemented operations

| Data | Operations |
|---|---|
| Profile | read and update |
| Activity session | UUID upsert, list and delete |
| Route points | batch UUID upsert and ordered list |
| Health check-in | UUID upsert and list |
| Meal entry | UUID upsert and list |

Deleting an activity session also deletes its route points through the
database foreign-key cascade. RLS applies to every operation.

## Repeatable live backend test

Set the project URL and publishable key in ignored `local.properties` (see
`local.properties.example`). Never put a service-role key in an Android app.
With an emulator/device connected, run:

```sh
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.liveBackend=true \
  -Pandroid.testInstrumentationRunnerArguments.liveWeather=true
```

`BackendLiveTest` creates two synthetic `backend-qa-...@example.com` app users,
requires signup to return a session, tests SDK round trips and **direct** server
RLS requests, and deletes only this run's randomly identified business rows.
The QA auth users and their profiles remain for audit. It checks retries do not
duplicate sessions, route cascade, cross-user reads/update/delete, forged owner,
cross-owner route attachment and anonymous access. Without opt-in, live tests
are skipped; ordinary CI does not need credentials or create cloud records.

The following app-level smoke test is still required after merging feature UI:

1. Register or sign in as User A.
2. Update the profile and restart the app; confirm the value is restored.
3. Complete an activity; confirm one session and its ordered route points can
   be read back.
4. Save one health check-in and one meal; confirm both lists return them.
5. Sign in as User B; confirm User A's records are absent.
6. Disable networking; confirm local Room workflows still work and the remote
   error is shown as retryable.
