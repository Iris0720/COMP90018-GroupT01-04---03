package com.example.comp90018.data.repository

import com.example.comp90018.domain.repository.ActiveOwnerProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Temporary owner provider for local integration testing.
class LocalActiveOwnerProvider : ActiveOwnerProvider {
    override fun observeActiveOwnerId(): Flow<String?> {
        return flowOf("local-test-user")
    }

    override suspend fun currentOwnerId(): String {
        return "local-test-user"
    }
}