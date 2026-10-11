// ui/screens/login/LoginViewModel.kt
package com.example.comp90018.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comp90018.data.UserLoginRepository
import com.example.comp90018.model.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.comp90018.data.SupabaseAuthRepository
import com.example.comp90018.data.SupabaseConfigurationException
import kotlinx.coroutines.flow.collect   // optional on recent coroutines, harmless if present
import kotlinx.coroutines.flow.update


class LoginViewModel(
    private val repo: UserLoginRepository = SupabaseAuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAuth().collect { loggedIn ->
                _state.update { it.copy(loggedIn = loggedIn) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = LoginUiState(error = "Enter your email and password")
            return
        }
        viewModelScope.launch {
            _state.value = LoginUiState(isLoading = true)
            repo.signIn(email, password)
                .onSuccess { _state.value = LoginUiState(loggedIn = true) }
                .onFailure { failure ->
                    val errorDetails = generateSequence(failure) { it.cause }
                        .joinToString(" ") { "${it::class.simpleName} ${it.message.orEmpty()}" }
                        .lowercase()
                    val message = when {
                        failure is SupabaseConfigurationException -> failure.message.orEmpty()
                        "invalid_credentials" in errorDetails ||
                                "invalid credentials" in errorDetails -> "invalid_credentials"
                        else -> "Sign in failed. Check your connection and try again."
                    }
                    _state.value = LoginUiState(error = message)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch{ repo.logout() }
    }
}
