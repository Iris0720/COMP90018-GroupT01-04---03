# Android backend integration guide

This branch provides the remote half of the app's local-first design. Feature
code saves to Room first, then calls `BackendRepository` to synchronise the
same client-generated UUID to Supabase. A remote failure is retryable and must
not delete or hide the local Room record.

## Create the repository

Create one shared instance after the Supabase configuration has been added to
`local.properties`:

```kotlin
val backendRepository: BackendRepository = SupabaseBackendRepository()
```

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

`SIGNED_OUT` and `INVALID_INPUT` are not retryable. `NETWORK` and ordinary
remote failures are retryable. Guest mode should continue using Room without
calling the remote repository.

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

## Required device smoke test after merging authentication

1. Register or sign in as User A.
2. Update the profile and restart the app; confirm the value is restored.
3. Complete an activity; confirm one session and its ordered route points can
   be read back.
4. Save one health check-in and one meal; confirm both lists return them.
5. Sign in as User B; confirm User A's records are absent.
6. Disable networking; confirm local Room workflows still work and the remote
   error is shown as retryable.
