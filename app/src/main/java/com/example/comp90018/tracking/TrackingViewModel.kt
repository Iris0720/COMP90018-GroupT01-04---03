package com.example.comp90018.tracking

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.comp90018.model.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.location.Location
import java.util.UUID

class TrackingViewModel(
    application: Application
) : AndroidViewModel(application) {
    var uiState by mutableStateOf(ActivityTrackingState())
        private set

    private var timerJob: Job? = null

    // Time collected before the current running segment.
    private var accumulatedMillis = 0L

    // Start time of the current running segment.
    private var segmentStartedAt = 0L

    // Creates a LocationTracker to handle location updates for the app.
    private val locationTracker = LocationTracker(application)

    private var provisionalStartRecorded = false

    private val stepTracker = StepTracker(application)

    // Previous cumulative sensor reading for the current running segment.
    private var previousStepTotal: Long? = null

    // Last accepted GPS point used to calculate distance.
    private var previousLocation: Location? = null



    private companion object {
        const val MAX_ACCEPTED_ACCURACY_METRES = 30f
        const val MIN_MOVEMENT_METRES = 1.5f
        const val MAX_MOVEMENT_METRES = 100f
        const val MAX_SPEED_METRES_PER_SECOND = 15f
        const val MIN_DISTANCE_FOR_PACE_METRES = 10f
    }

    fun selectActivity(activity: String) {
        if (uiState.phase != SessionState.SETUP) return
        if (activity !in listOf("Walking", "Running", "Hiking")) return

        uiState = uiState.copy(activity = activity)
    }


    fun selectTarget(target: String) {
        if (uiState.phase != SessionState.SETUP) return

        val normalized = target.trim().replace(',', '.')

        val valid = when {
            normalized == "Open" -> true

            normalized in setOf(
                "15 min",
                "30 min",
                "45 min",
                "60 min"
            ) -> true

            normalized.endsWith(" km") -> {
                val numberText = normalized.removeSuffix(" km")
                val distance = numberText.toDoubleOrNull()

                numberText.matches(
                    Regex("""\d+(\.\d{1,2})?""")
                ) &&
                        distance != null &&
                        distance.isFinite() &&
                        distance in 0.1..1000.0
            }

            else -> false
        }

        if (!valid) return

        uiState = uiState.copy(target = normalized)
    }

    private fun currentElapsedMillis(): Long {
        return if (uiState.phase == SessionState.LIVE) {
            accumulatedMillis +
                    (SystemClock.elapsedRealtime() - segmentStartedAt)
                        .coerceAtLeast(0L)
        } else {
            accumulatedMillis
        }
    }

    // Calculates average pace in seconds per kilometre using active time,
    // excluding pauses. Returns null when the distance is below the minimum.
    private fun calculateAveragePace(
        elapsedMillis: Long,
        distanceMetres: Float
    ): Double? {
        if (distanceMetres < MIN_DISTANCE_FOR_PACE_METRES) {
            return null
        }

        val elapsedSeconds = elapsedMillis / 1_000.0
        val distanceKilometres = distanceMetres / 1_000.0

        return elapsedSeconds / distanceKilometres
    }


    private fun recordRoutePoint(
        location: Location,
        newSegment: Boolean
    ) {
        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude
        )

        val segments = uiState.routeSegments

        val updatedSegments =
            if (newSegment || segments.isEmpty()) {
                segments + listOf(listOf(point))
            } else {
                segments.dropLast(1) +
                        listOf(segments.last() + point)
            }

        uiState = uiState.copy(
            routeSegments = updatedSegments
        )
    }

    private fun replaceProvisionalStart(location: Location) {
        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude
        )

        uiState = uiState.copy(
            routeSegments = listOf(listOf(point))
        )
    }

    private fun handleLocation(location: Location) {
        // Ignore callbacks after pausing or finishing.
        if (uiState.phase != SessionState.LIVE) return

        val timestamp = location.elapsedRealtimeNanos
        val now = SystemClock.elapsedRealtimeNanos()


        // Ignore future timestamps and points older than 10 seconds.
        if (timestamp > now ||
            now - timestamp > 10_000_000_000L
        ) {
            return
        }

        if (!location.latitude.isFinite() ||
            !location.longitude.isFinite() ||
            location.latitude !in -90.0..90.0 ||
            location.longitude !in -180.0..180.0
        ) {
            return
        }

        val previous = previousLocation

        // Ignore duplicate or out-of-order readings.
        if (previous != null &&
            timestamp <= previous.elapsedRealtimeNanos
        ) {
            return
        }

        uiState = uiState.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMetres = if (location.hasAccuracy()) {
                location.accuracy
            } else {
                null
            }
        )

        if (!location.hasAccuracy() ||
            !location.accuracy.isFinite() ||
            location.accuracy < 0f ||
            location.accuracy > MAX_ACCEPTED_ACCURACY_METRES
        ) {
            previousLocation = null

            // Show an approximate starting point indoors,
            // but do not use it to calculate distance.
            if (uiState.routeSegments.isEmpty()) {
                recordRoutePoint(
                    location = location,
                    newSegment = true
                )
                provisionalStartRecorded = true
            }

            uiState = uiState.copy(
                gpsStatus = if (location.hasAccuracy()) {
                    "Approximate location · ${location.accuracy.toInt()} m"
                } else {
                    "Approximate location"
                }
            )
            return
        }

        if (previous == null) {
            previousLocation = Location(location)

            if (provisionalStartRecorded) {
                // Replace the indoor approximate point with the first accurate point.
                replaceProvisionalStart(location)
                provisionalStartRecorded = false
            } else {
                recordRoutePoint(location, newSegment = true)
            }

            uiState = uiState.copy(
                gpsStatus = "GPS connected"
            )
            return
        }

        val timeDifferenceSeconds =
            (timestamp - previous.elapsedRealtimeNanos) /
                    1_000_000_000.0

        // Do not draw a straight line across a long data gap.
        if (timeDifferenceSeconds > 120.0) {
            previousLocation = Location(location)
            recordRoutePoint(location, newSegment = true)

            uiState = uiState.copy(
                gpsStatus = "GPS connected"
            )
            return
        }

        val segmentDistance = previous.distanceTo(location)
        val segmentSpeed = segmentDistance / timeDifferenceSeconds

        if (!segmentDistance.isFinite() ||
            segmentDistance > MAX_MOVEMENT_METRES ||
            segmentSpeed > MAX_SPEED_METRES_PER_SECOND
        ) {
            // Keep the previous accepted point.
            uiState = uiState.copy(
                gpsStatus = "GPS jump ignored"
            )
            return
        }

        uiState = uiState.copy(
            gpsStatus = "GPS connected"
        )

        // Keep the previous point so small movements can accumulate.
        if (segmentDistance < MIN_MOVEMENT_METRES) return

        val newDistance = uiState.distanceMetres + segmentDistance
        val elapsed = currentElapsedMillis()

        previousLocation = Location(location)
        recordRoutePoint(location, newSegment = false)

        uiState = uiState.copy(
            distanceMetres = newDistance,
            elapsedMillis = elapsed,
            averagePaceSecondsPerKm = calculateAveragePace(
                elapsedMillis = elapsed,
                distanceMetres = newDistance
            )
        )
    }


    private fun startTimer() {
        timerJob?.cancel()

        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = currentElapsedMillis()

                uiState = uiState.copy(
                    elapsedMillis = elapsed,
                    averagePaceSecondsPerKm = calculateAveragePace(
                        elapsedMillis = elapsed,
                        distanceMetres = uiState.distanceMetres
                    )
                )

                delay(250L)
            }
        }
    }

    private fun startGps() {
        uiState = uiState.copy(
            gpsStatus = "Waiting for GPS..."
        )

        locationTracker.startLocationUpdates(
            onLocationReceived = { location ->
                handleLocation(location)
            },
            onError = { message ->
                previousLocation = null

                uiState = uiState.copy(
                    gpsStatus = message
                )
            }
        )
    }

    private fun startSteps() {
        // Establish a fresh baseline after starting or resuming.
        previousStepTotal = null

        stepTracker.start(
            onStepCount = { total ->
                if (uiState.phase == SessionState.LIVE) {
                    uiState = uiState.copy(hasStepReading = true)

                    val previous = previousStepTotal
                    previousStepTotal = total

                    if (previous != null) {
                        // A lower total means the sensor count has reset.
                        // Use it as the new baseline without subtracting steps.
                        val addedSteps = if (total >= previous) {
                            total - previous
                        } else {
                            0L
                        }

                        uiState = uiState.copy(
                            sessionSteps = uiState.sessionSteps + addedSteps
                        )
                    }
                }
            },
            onStatus = { status ->
                if (uiState.phase == SessionState.LIVE) {
                    uiState = uiState.copy(stepStatus = status)
                }
            }
        )
    }

    fun start() {
        if (uiState.phase != SessionState.SETUP) return

        accumulatedMillis = 0L
        segmentStartedAt = SystemClock.elapsedRealtime()

        previousLocation = null
        provisionalStartRecorded = false

        uiState = uiState.copy(
            phase = SessionState.LIVE,
            elapsedMillis = 0L,
            routeSegments = emptyList(),
            distanceMetres = 0f,
            averagePaceSecondsPerKm = null,
            latitude = null,
            longitude = null,
            accuracyMetres = null,
            gpsStatus = "Waiting for GPS...",
            sessionSteps = 0L,
            stepStatus = "Step tracking not started",
            hasStepReading = false,
            sessionId = UUID.randomUUID().toString(),
            startedAtEpochMillis = System.currentTimeMillis(),
            finishedSummary = null,
        )

        startTimer()
        startGps()
        startSteps()
    }

    fun pause() {
        if (uiState.phase != SessionState.LIVE) return

        accumulatedMillis = currentElapsedMillis()

        timerJob?.cancel()
        timerJob = null
        locationTracker.stopLocationUpdates()

        stepTracker.stop()
        previousStepTotal = null

        previousLocation = null

        uiState = uiState.copy(
            phase = SessionState.PAUSED,
            elapsedMillis = accumulatedMillis,
            stepStatus = "Step tracking paused"
        )
    }

    fun resume() {
        if (uiState.phase != SessionState.PAUSED) return

        segmentStartedAt = SystemClock.elapsedRealtime()
        uiState = uiState.copy(phase = SessionState.LIVE)

        startTimer()
        previousLocation = null
        startGps()
        startSteps()
    }

    fun finish() {
        if (
            uiState.phase != SessionState.LIVE &&
            uiState.phase != SessionState.PAUSED
        ) {
            return
        }

        val finalMillis = currentElapsedMillis()

        timerJob?.cancel()
        timerJob = null
        locationTracker.stopLocationUpdates()

        stepTracker.stop()
        previousStepTotal = null

        previousLocation = null
        accumulatedMillis = finalMillis

        val finishedAt = System.currentTimeMillis()

        val summary = ActivitySummary(
            sessionId = uiState.sessionId
                ?: UUID.randomUUID().toString(),
            activityType = uiState.activity,
            target = uiState.target,
            startedAtEpochMillis =
                uiState.startedAtEpochMillis ?: finishedAt,
            finishedAtEpochMillis = finishedAt,
            activeDurationMillis = finalMillis,
            distanceMetres = uiState.distanceMetres,
            averagePaceSecondsPerKm =
                calculateAveragePace(
                    elapsedMillis = finalMillis,
                    distanceMetres = uiState.distanceMetres
                ),
            recordedSteps = if (uiState.hasStepReading) {
                uiState.sessionSteps
            } else {
                null
            },
            routeSegments = uiState.routeSegments
        )

        uiState = uiState.copy(
            phase = SessionState.COMPLETE,
            elapsedMillis = finalMillis,
            averagePaceSecondsPerKm =
                summary.averagePaceSecondsPerKm,
            stepStatus = "Step tracking stopped",
            finishedSummary = summary
        )
    }

    fun reset() {
        if (uiState.phase != SessionState.COMPLETE) return

        timerJob?.cancel()
        timerJob = null
        locationTracker.stopLocationUpdates()

        stepTracker.stop()
        previousStepTotal = null

        previousLocation = null

        accumulatedMillis = 0L
        segmentStartedAt = 0L

        uiState = uiState.copy(
            phase = SessionState.SETUP,
            elapsedMillis = 0L,
            routeSegments = emptyList(),
            distanceMetres = 0f,
            averagePaceSecondsPerKm = null,
            latitude = null,
            longitude = null,
            accuracyMetres = null,
            gpsStatus = "GPS not started",
            sessionSteps = 0L,
            stepStatus = "Step tracking not started",
            hasStepReading = false,
            sessionId = null,
            startedAtEpochMillis = null,
            finishedSummary = null,
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
        locationTracker.stopLocationUpdates()
        stepTracker.stop()
        super.onCleared()
    }
}
