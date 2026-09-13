package com.example.comp90018.model

enum class AppTab(val label: String, val mark: String) {
    TODAY("Today", "●"), ACTIVITY("Activity", "▲"), HEALTH("Health", "+")
}

enum class SessionState { SETUP, LIVE, PAUSED, COMPLETE }

enum class WellbeingStatus { GOOD, CAUTION, UNKNOWN }
