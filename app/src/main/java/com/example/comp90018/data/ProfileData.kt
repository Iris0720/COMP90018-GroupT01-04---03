package com.example.comp90018.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileData(
    val id: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("daily_step_target")
    val dailyStepTarget: Int,

    @SerialName("unit_system")
    val unitSystem: String
)