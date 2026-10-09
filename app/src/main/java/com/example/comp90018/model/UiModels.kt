package com.example.comp90018.model

enum class AppTab(val label: String, val mark: String) {
    TODAY("Today", "●"), ACTIVITY("Activity", "▲"), HEALTH("Health", "+")
}

enum class SessionState { SETUP, LIVE, PAUSED, COMPLETE }

enum class WellbeingStatus { GOOD, CAUTION, UNKNOWN }

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
)

data class RegisterUiState(
    val isRegistering: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)