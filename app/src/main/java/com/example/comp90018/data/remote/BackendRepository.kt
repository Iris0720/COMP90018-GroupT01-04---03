package com.example.comp90018.data.remote

/**
 * Remote backend contract used by ViewModels or a future Room sync worker.
 *
 * All writes are idempotent UUID upserts. Room remains the offline source of
 * truth; a network failure from this interface must never erase local data.
 */
interface BackendRepository {
    suspend fun getProfile(): BackendResult<ProfileRow>

    suspend fun updateProfile(update: ProfileUpdate): BackendResult<ProfileRow>

    suspend fun upsertActivity(
        session: ActivitySessionInput,
        routePoints: List<RoutePointInput>
    ): BackendResult<Unit>

    suspend fun getActivities(limit: Int = 50): BackendResult<List<ActivitySessionRow>>

    suspend fun getRoutePoints(sessionId: String): BackendResult<List<RoutePointRow>>

    suspend fun deleteActivity(sessionId: String): BackendResult<Unit>

    suspend fun upsertHealthCheckIn(input: HealthCheckInInput): BackendResult<Unit>

    suspend fun getHealthCheckIns(limit: Int = 50): BackendResult<List<HealthCheckInRow>>

    suspend fun upsertMeal(input: MealEntryInput): BackendResult<Unit>

    suspend fun getMeals(limit: Int = 50): BackendResult<List<MealEntryRow>>
}
