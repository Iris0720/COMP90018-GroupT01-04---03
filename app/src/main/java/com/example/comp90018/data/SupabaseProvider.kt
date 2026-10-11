package com.example.comp90018.data

import com.example.comp90018.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
                BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    ) {
        install(Auth)
        install(Postgrest)
    }
}

class SupabaseConfigurationException : IllegalStateException(
    "Supabase is not configured. Add SUPABASE_URL and " +
            "SUPABASE_PUBLISHABLE_KEY to local.properties, then rebuild the app."
)
