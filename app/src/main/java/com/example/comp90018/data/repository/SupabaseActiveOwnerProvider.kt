package com.example.comp90018.data.repository

import com.example.comp90018.data.SupabaseAuthRepository
import com.example.comp90018.data.UserLoginRepository
import com.example.comp90018.domain.repository.ActiveOwnerProvider
import kotlinx.coroutines.flow.Flow

// Connects locally stored records to the user exposed by the login repository.
class SupabaseActiveOwnerProvider(
    private val authRepository: UserLoginRepository = SupabaseAuthRepository()
) : ActiveOwnerProvider {
    override fun observeActiveOwnerId(): Flow<String?> {
        return authRepository.observeUserId()
    }

    override suspend fun currentOwnerId(): String? {
        return runCatching {
            authRepository.getUserId()
        }.getOrNull()
    }
}
