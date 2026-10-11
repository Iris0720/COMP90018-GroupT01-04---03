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
                .onFailure { _state.value = LoginUiState(error = it.message) }
        }
    }

    fun signOut() {
        viewModelScope.launch{ repo.logout() }
    }
}