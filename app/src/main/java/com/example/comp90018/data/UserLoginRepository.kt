package com.example.comp90018.data

import kotlinx.coroutines.flow.Flow

interface UserLoginRepository {
    suspend fun signIn(email: String, password: String): Result<Unit> 
    suspend fun register(email: String, password: String): Result<Unit>

    fun observeAuth(): Flow<Boolean>
    fun observeUserId(): Flow<String?>
    suspend fun logout(): Result<Unit>

    suspend fun getUserId(): String
}
