package com.example.comp90018.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.io.IOException

class SupabaseBackendRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client
) : BackendRepository {

    override suspend fun getProfile(): BackendResult<ProfileRow> = withOwner { ownerId ->
        val profile = client.from(PROFILES).select {
            filter { eq("id", ownerId) }
            limit(1)
        }.decodeSingleOrNull<ProfileRow>()
            ?: return@withOwner BackendResult.Failure(notFound("Profile not found."))
        BackendResult.Success(profile)
    }

    override suspend fun updateProfile(update: ProfileUpdate): BackendResult<ProfileRow> {
        BackendInputValidator.profile(update)?.let { return invalid(it) }
        return withOwner { ownerId ->
            client.from(PROFILES).update(
                ProfileUpdateRow(
                    displayName = update.displayName.trim(),
                    dailyStepTarget = update.dailyStepTarget,
                    unitSystem = update.unitSystem
                )
            ) {
                filter { eq("id", ownerId) }
            }
            getProfile()
        }
    }

    override suspend fun upsertActivity(
        session: ActivitySessionInput,
        routePoints: List<RoutePointInput>
    ): BackendResult<Unit> {
        BackendInputValidator.activity(session)?.let { return invalid(it) }
        routePoints.forEach { point ->
            BackendInputValidator.routePoint(point, session.id)?.let { return invalid(it) }
        }

        return withOwner { ownerId ->
            client.from(ACTIVITY_SESSIONS).upsert(session.toWriteRow(ownerId)) {
                onConflict = "id"
            }
            if (routePoints.isNotEmpty()) {
                client.from(ROUTE_POINTS).upsert(routePoints.map { it.toWriteRow(ownerId) }) {
                    onConflict = "id"
                }
            }
            BackendResult.Success(Unit)
        }
    }

    override suspend fun getActivities(limit: Int): BackendResult<List<ActivitySessionRow>> {
        if (limit !in 1..200) return invalid("Activity limit must be between 1 and 200.")
        return withOwner { ownerId ->
            val rows = client.from(ACTIVITY_SESSIONS).select {
                filter { eq("owner_id", ownerId) }
                order("started_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<ActivitySessionRow>()
            BackendResult.Success(rows)
        }
    }

    override suspend fun getRoutePoints(sessionId: String): BackendResult<List<RoutePointRow>> {
        if (sessionId.isBlank()) return invalid("Session id is required.")
        return withOwner { ownerId ->
            val rows = client.from(ROUTE_POINTS).select {
                filter {
                    eq("owner_id", ownerId)
                    eq("session_id", sessionId)
                }
                order("segment_number", Order.ASCENDING)
                order("sequence_number", Order.ASCENDING)
            }.decodeList<RoutePointRow>()
            BackendResult.Success(rows)
        }
    }

    override suspend fun deleteActivity(sessionId: String): BackendResult<Unit> {
        if (sessionId.isBlank()) return invalid("Session id is required.")
        return withOwner { ownerId ->
            client.from(ACTIVITY_SESSIONS).delete {
                filter {
                    eq("id", sessionId)
                    eq("owner_id", ownerId)
                }
            }
            BackendResult.Success(Unit)
        }
    }

    override suspend fun upsertHealthCheckIn(input: HealthCheckInInput): BackendResult<Unit> {
        BackendInputValidator.health(input)?.let { return invalid(it) }
        return withOwner { ownerId ->
            client.from(HEALTH_CHECK_INS).upsert(input.toWriteRow(ownerId)) {
                onConflict = "id"
            }
            BackendResult.Success(Unit)
        }
    }

    override suspend fun getHealthCheckIns(limit: Int): BackendResult<List<HealthCheckInRow>> {
        if (limit !in 1..200) return invalid("Health check-in limit must be between 1 and 200.")
        return withOwner { ownerId ->
            val rows = client.from(HEALTH_CHECK_INS).select {
                filter { eq("owner_id", ownerId) }
                order("recorded_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<HealthCheckInRow>()
            BackendResult.Success(rows)
        }
    }

    override suspend fun upsertMeal(input: MealEntryInput): BackendResult<Unit> {
        BackendInputValidator.meal(input)?.let { return invalid(it) }
        return withOwner { ownerId ->
            client.from(MEAL_ENTRIES).upsert(input.toWriteRow(ownerId)) {
                onConflict = "id"
            }
            BackendResult.Success(Unit)
        }
    }

    override suspend fun getMeals(limit: Int): BackendResult<List<MealEntryRow>> {
        if (limit !in 1..200) return invalid("Meal limit must be between 1 and 200.")
        return withOwner { ownerId ->
            val rows = client.from(MEAL_ENTRIES).select {
                filter { eq("owner_id", ownerId) }
                order("recorded_at", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<MealEntryRow>()
            BackendResult.Success(rows)
        }
    }

    private suspend fun <T> withOwner(
        block: suspend (String) -> BackendResult<T>
    ): BackendResult<T> {
        val ownerId = try {
            // Auth restores a persisted session asynchronously after process start.
            // Waiting here avoids reporting a false signed-out state during startup.
            client.auth.awaitInitialization()
            client.auth.currentUserOrNull()?.id
        } catch (error: Throwable) {
            return BackendResult.Failure(error.toBackendError())
        } ?: return BackendResult.Failure(
                BackendError(
                    kind = BackendErrorKind.SIGNED_OUT,
                    message = "Sign in before accessing cloud records.",
                    retryable = false
                )
            )
        return try {
            block(ownerId)
        } catch (error: Throwable) {
            BackendResult.Failure(error.toBackendError())
        }
    }

    private fun ActivitySessionInput.toWriteRow(ownerId: String) = ActivitySessionWriteRow(
        id = id,
        ownerId = ownerId,
        startedAt = startedAt,
        finishedAt = finishedAt,
        activityType = activityType,
        sessionState = sessionState,
        goalType = goalType,
        goalValue = goalValue,
        durationSeconds = durationSeconds,
        distanceMetres = distanceMetres,
        steps = steps
    )

    private fun RoutePointInput.toWriteRow(ownerId: String) = RoutePointWriteRow(
        id = id,
        ownerId = ownerId,
        sessionId = sessionId,
        segmentNumber = segmentNumber,
        sequenceNumber = sequenceNumber,
        recordedAt = recordedAt,
        latitude = latitude,
        longitude = longitude,
        accuracyMetres = accuracyMetres,
        altitudeMetres = altitudeMetres,
        speedMetresPerSecond = speedMetresPerSecond
    )

    private fun HealthCheckInInput.toWriteRow(ownerId: String) = HealthCheckInWriteRow(
        id = id,
        ownerId = ownerId,
        recordedAt = recordedAt,
        feeling = feeling,
        breathingDifficulty = breathingDifficulty,
        fatigue = fatigue,
        condition = condition
    )

    private fun MealEntryInput.toWriteRow(ownerId: String) = MealEntryWriteRow(
        id = id,
        ownerId = ownerId,
        recordedAt = recordedAt,
        name = name.trim(),
        serving = serving.trim(),
        calories = calories,
        proteinGrams = proteinGrams,
        carbohydrateGrams = carbohydrateGrams,
        fatGrams = fatGrams,
        sourceLabel = sourceLabel.trim(),
        photoStoragePath = photoStoragePath
    )

    private fun Throwable.toBackendError(): BackendError {
        val readableMessage = message ?: "Remote backend request failed."
        return when {
            this is IOException -> BackendError(BackendErrorKind.NETWORK, readableMessage, true)
            this is IllegalStateException && readableMessage.contains("SUPABASE_") ->
                BackendError(BackendErrorKind.CONFIGURATION, readableMessage, false)
            else -> BackendError(BackendErrorKind.REMOTE, readableMessage, true)
        }
    }

    private fun notFound(message: String) = BackendError(
        kind = BackendErrorKind.NOT_FOUND,
        message = message,
        retryable = false
    )

    private fun <T> invalid(message: String): BackendResult<T> = BackendResult.Failure(
        BackendError(
            kind = BackendErrorKind.INVALID_INPUT,
            message = message,
            retryable = false
        )
    )

    private companion object {
        const val PROFILES = "profiles"
        const val ACTIVITY_SESSIONS = "activity_sessions"
        const val ROUTE_POINTS = "route_points"
        const val HEALTH_CHECK_INS = "health_check_ins"
        const val MEAL_ENTRIES = "meal_entries"
    }
}
