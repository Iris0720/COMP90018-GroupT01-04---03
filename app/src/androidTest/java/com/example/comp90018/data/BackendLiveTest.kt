package com.example.comp90018.data

import androidx.test.platform.app.InstrumentationRegistry
import com.example.comp90018.BuildConfig
import com.example.comp90018.data.remote.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

/** Opt in with -Pandroid.testInstrumentationRunnerArguments.liveBackend=true.
 * Creates two synthetic app users (no real email/person) and removes only this run's business records.
 * The QA auth accounts/profiles remain for audit; no admin/service-role key is used.
 */
class BackendLiveTest {
    @Test(timeout = 120_000) fun sdkRoundTripAndServerRlsIsolation() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveBackend") == "true")
        check(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank())
        fun newClient() = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            install(Auth) {
                autoLoadFromStorage = false
                autoSaveToStorage = false
                alwaysAutoRefresh = false
            }
            install(Postgrest)
        }
        val a = newClient()
        val b = newClient()
        val anon = newClient()
        val runId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        val healthId = UUID.randomUUID().toString()
        val mealId = UUID.randomUUID().toString()
        val pointId = UUID.randomUUID().toString()
        var ownerA: String? = null
        try {
            val signedOut = SupabaseBackendRepository(anon).getProfile() as BackendResult.Failure
            assertEquals(BackendErrorKind.SIGNED_OUT, signedOut.error.kind)
            for ((client, label) in listOf(a to "a", b to "b")) {
                client.auth.signUpWith(Email) {
                    email = "backend-qa-$label-$runId@example.com"
                    password = "Qa!${UUID.randomUUID()}9x"
                    data = buildJsonObject { put("display_name", "Backend QA $label") }
                }
                check(client.auth.currentUserOrNull() != null) {
                    "Live QA signup did not create a session; check email-confirmation setting."
                }
            }
            ownerA = a.auth.currentUserOrNull()!!.id
            val ownerB = b.auth.currentUserOrNull()!!.id
            assertNotEquals(ownerA, ownerB)
            val ra = SupabaseBackendRepository(a)
            val rb = SupabaseBackendRepository(b)
            success(ra.updateProfile(ProfileUpdate("Backend QA A", 8500, "metric")))
            assertEquals(8500, success(ra.getProfile()).dailyStepTarget)
            assertEquals(ownerB, success(rb.getProfile()).id)
            val time = Instant.now().toString()
            val session = ActivitySessionInput(sessionId, time, time, "walking",
                durationSeconds = 10, distanceMetres = 12, steps = 15)
            val point = RoutePointInput(pointId, sessionId, 0, 0, time, -37.81, 144.96, 5f)
            val points = List(205) { sequence ->
                point.copy(id = if (sequence == 0) pointId else UUID.randomUUID().toString(), sequenceNumber = sequence)
            }
            success(ra.upsertActivity(session, points))
            success(ra.upsertActivity(session.copy(distanceMetres = 14), points))
            assertEquals(1, success(ra.getActivities()).count { it.id == sessionId })
            assertEquals(14L, success(ra.getActivities()).single { it.id == sessionId }.distanceMetres)
            val restoredPoints = success(ra.getRoutePoints(sessionId))
            assertEquals(205, restoredPoints.size)
            assertEquals(pointId, restoredPoints.first().id)
            assertEquals((0..204).toList(), restoredPoints.map { it.sequenceNumber })
            success(ra.upsertHealthCheckIn(HealthCheckInInput(healthId, time, "okay", "none", "mild", "unknown")))
            success(ra.upsertMeal(MealEntryInput(mealId, time, "QA meal", "1 portion", 500, 20, 60, 12)))
            assertTrue(success(ra.getHealthCheckIns()).any { it.id == healthId })
            assertTrue(success(ra.getMeals()).any { it.id == mealId })
            assertFalse(success(rb.getActivities()).any { it.id == sessionId })
            assertTrue(success(rb.getRoutePoints(sessionId)).isEmpty())
            assertFalse(success(rb.getHealthCheckIns()).any { it.id == healthId })
            assertFalse(success(rb.getMeals()).any { it.id == mealId })

            // Direct requests deliberately omit repository owner filters: verify SERVER RLS.
            for ((table, id) in listOf("profiles" to ownerA, "activity_sessions" to sessionId,
                "route_points" to pointId, "health_check_ins" to healthId, "meal_entries" to mealId)) {
                val visible = b.from(table).select { filter { eq("id", id) } }.decodeList<JsonObject>()
                assertTrue("B can see A's $table", visible.isEmpty())
            }
            b.from("activity_sessions").update(buildJsonObject { put("distance_metres", 999) }) {
                filter { eq("id", sessionId) }
            }
            b.from("activity_sessions").delete { filter { eq("id", sessionId) } }
            assertEquals(14L, success(ra.getActivities()).single { it.id == sessionId }.distanceMetres)
            expectDenied(setOf(403)) {
                b.from("activity_sessions").insert(buildJsonObject {
                    put("id", UUID.randomUUID().toString()); put("owner_id", ownerA)
                    put("started_at", time); put("activity_type", "walking")
                })
            }
            expectDenied(setOf(409)) {
                b.from("route_points").insert(buildJsonObject {
                    put("id", UUID.randomUUID().toString()); put("owner_id", ownerB)
                    put("session_id", sessionId); put("segment_number", 0); put("sequence_number", 1)
                    put("recorded_at", time); put("latitude", -37.81); put("longitude", 144.96)
                })
            }
            expectDenied(setOf(401, 403)) { anon.from("activity_sessions").select().decodeList<JsonObject>() }
            success(ra.deleteActivity(sessionId))
            assertTrue(success(ra.getRoutePoints(sessionId)).isEmpty())
            println("LIVE_BACKEND_PASS: profile, activity/route, health, meal, idempotency, cascade, B isolation, forged ownership, anonymous denial")
        } finally {
            // Filters contain only fresh random ids from THIS test, never pre-existing team data.
            if (ownerA != null) {
                for ((table, id) in listOf("activity_sessions" to sessionId, "health_check_ins" to healthId, "meal_entries" to mealId)) {
                    a.from(table).delete { filter { eq("id", id); eq("owner_id", ownerA) } }
                }
            }
            a.close(); b.close(); anon.close()
        }
    }

    private fun <T> success(result: BackendResult<T>): T = when (result) {
        is BackendResult.Success -> result.value
        is BackendResult.Failure -> error("Repository failed: ${result.error.kind}: ${result.error.message}")
    }

    private suspend fun expectDenied(statuses: Set<Int>, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: io.github.jan.supabase.exceptions.RestException) {
            // A network timeout cannot masquerade as a successful security check.
            assertTrue("Expected access/constraint denial (${error.statusCode})", error.statusCode in statuses)
            return
        }
        fail("Expected server denial")
    }
}
