package com.example.comp90018.wear

import com.example.comp90018.watch.WatchActivityType
import com.example.comp90018.watch.WatchGoalType
import com.example.comp90018.watch.WatchStartOptions
import java.util.Locale

internal data class WatchGoalChoice(val label: String, val type: WatchGoalType, val value: Double? = null)

internal val watchGoalChoices = listOf(
    WatchGoalChoice("Open", WatchGoalType.OPEN),
    WatchGoalChoice("15 min", WatchGoalType.DURATION_SECONDS, 900.0),
    WatchGoalChoice("30 min", WatchGoalType.DURATION_SECONDS, 1800.0),
    WatchGoalChoice("60 min", WatchGoalType.DURATION_SECONDS, 3600.0),
    WatchGoalChoice("1 km", WatchGoalType.DISTANCE_METRES, 1000.0),
    WatchGoalChoice("5 km", WatchGoalType.DISTANCE_METRES, 5000.0)
)

internal fun watchStartOptions(activityIndex: Int, goalIndex: Int): WatchStartOptions {
    val activity = WatchActivityType.entries.getOrNull(activityIndex) ?: WatchActivityType.WALKING
    val goal = watchGoalChoices.getOrNull(goalIndex) ?: watchGoalChoices.first()
    return WatchStartOptions(activity, goal.type, goal.value)
}

internal fun watchElapsedTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d:%02d", safe / 3600, safe / 60 % 60, safe % 60)
}
