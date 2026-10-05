package com.example.comp90018.data

interface UserLoginRepository {
    suspend fun signIn(email: String, password: String): Result<Unit> 
}