// ui/screens/login/LoginViewModel.kt
package com.example.comp90018.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comp90018.data.FakeUserLogin
import com.example.comp90018.data.UserLoginRepository
import com.example.comp90018.model.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class LoginViewModel(
    private val repo: UserLoginRepository = FakeUserLogin()
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

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

    fun continueAsGuest() {
        _state.value = LoginUiState(loggedIn = true)
    }

    fun signOut() {
        _state.value = LoginUiState()
    }
}