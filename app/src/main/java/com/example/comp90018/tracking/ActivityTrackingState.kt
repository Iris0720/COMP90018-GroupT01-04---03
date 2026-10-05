package com.example.comp90018.tracking

import com.example.comp90018.model.SessionState
import java.util.Locale


data class ActivityTrackingState(
    val phase: SessionState = SessionState.SETUP,
    val activity: String = "Walking",
    val target: String = "30 min",
    val elapsedMillis: Long = 0L,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMetres: Float? = null,
    val gpsStatus: String = "GPS not started",

    val routeSegments: List<List<TrackPoint>> = emptyList(),

    val distanceMetres: Float = 0f,
    val averagePaceSecondsPerKm: Double? = null,

    val sessionSteps: Long = 0L,
    val hasStepReading: Boolean = false,
    val stepStatus: String = "Step tracking not started",

    val sessionId: String? = null,
    val startedAtEpochMillis: Long? = null,
    val finishedSummary: ActivitySummary? = null
) {
    val formattedTime: String
        get() {
            val seconds = elapsedMillis / 1000
            return String.format(
                Locale.US,
                "%02d:%02d:%02d",
                seconds / 3600,
                seconds / 60 % 60,
                seconds % 60
            )
        }
    val goalProgress: Float
        get() {
            val goalAmount: Double
            val currentAmount: Double

            when {
                target.endsWith(" min") -> {
                    val minutes = target
                        .removeSuffix(" min")
                        .toDoubleOrNull()
                        ?: return 0f

                    goalAmount = minutes * 60_000.0
                    currentAmount = elapsedMillis.toDouble()
                }

                target.endsWith(" km") -> {
                    val kilometres = target
                        .removeSuffix(" km")
                        .toDoubleOrNull()
                        ?: return 0f

                    goalAmount = kilometres * 1_000.0
                    currentAmount = distanceMetres.toDouble()
                }

                else -> return 0f
            }

            if (!goalAmount.isFinite() ||
                goalAmount <= 0.0 ||
                !currentAmount.isFinite()
            ) {
                return 0f
            }

            return (currentAmount / goalAmount)
                .coerceIn(0.0, 1.0)
                .toFloat()
        }
    val hasFixedGoal: Boolean
        get() = target != "Open"

    val goalReached: Boolean
        get() = hasFixedGoal && goalProgress >= 1f

    val goalProgressText: String
        get() {
            if (!hasFixedGoal) return "Open goal"

            val percentage =
                (goalProgress * 100).toInt()

            return if (goalReached) {
                "Goal reached"
            } else {
                "$percentage% of goal"
            }
        }
}
