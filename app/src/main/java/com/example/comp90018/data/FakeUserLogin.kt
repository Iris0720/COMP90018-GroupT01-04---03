package com.example.comp90018.data

import kotlinx.coroutines.delay

class FakeUserLogin : UserLoginRepository {
    override suspend fun signIn(email: String, password: String): Result<Unit> {
        delay(800)
        return if (email == "test@example.com" && password == "password123") {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Incorrect email or password"))
        }
    }
}