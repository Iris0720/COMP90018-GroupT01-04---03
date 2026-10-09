package com.example.comp90018.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ProfileRepository {

    suspend fun getProfileData(userId: String): ProfileData {
        return SupabaseProvider.client
            .from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<ProfileData>()
    }

    suspend fun setProfileData(
        userId: String,
        displayName: String
    ) {
        SupabaseProvider.client
            .from("profiles")
            .update(ProfileUpdate(displayName)) {
                filter {
                    eq("id", userId)
                }
            }
    }

    suspend fun updateProfile(userId: String, displayName: String, dailyStepTarget: Int, unitSystem: String) {
        SupabaseProvider.client.from("profiles").update({
            set("display_name", displayName)
            set("daily_step_target", dailyStepTarget)
            set("unit_system", unitSystem)
        }) {
            filter { eq("id", userId) }
        }
    }
    @Serializable
    private data class ProfileUpdate(
        @SerialName("display_name")
        val displayName: String
    )
}