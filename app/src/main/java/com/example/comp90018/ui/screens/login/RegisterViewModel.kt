// ui/screens/login/RegisterViewModel.kt
package com.example.comp90018.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comp90018.data.UserLoginRepository
import com.example.comp90018.model.RegisterUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.comp90018.data.SupabaseAuthRepository


class RegisterViewModel(
    private val repo: UserLoginRepository = SupabaseAuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = RegisterUiState(error = "Enter your email and password")
            return
        }
        viewModelScope.launch {
            _state.value = RegisterUiState(isRegistering = true)
            repo.register(email, password)
                .onSuccess { _state.value = RegisterUiState(
                    success = true,
                    isRegistering = false
                    )
                }
                .onFailure { _state.value = RegisterUiState(
                        isRegistering = false,
                        error = it.message ?: "Registration failed"
                    )
                }
        }
    }

}