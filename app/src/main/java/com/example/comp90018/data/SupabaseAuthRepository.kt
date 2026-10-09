package com.example.comp90018.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import io.github.jan.supabase.auth.status.SessionStatus
class SupabaseAuthRepository(
    private val client: SupabaseClient = SupabaseProvider.client
) : UserLoginRepository {

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        val user = client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        if (user != null && user.identities.isNullOrEmpty()) {
            throw Exception("An account with this email already exists")
        }
    }

    override fun observeAuth(): Flow<Boolean> = client.auth.sessionStatus.map {it is SessionStatus.Authenticated }.distinctUntilChanged()

    override suspend fun logout(): Result<Unit> = runCatching {
        client.auth.signOut();
    }

    override suspend fun getUserId(): String {
        return client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No authenticated user")
    }
    
}