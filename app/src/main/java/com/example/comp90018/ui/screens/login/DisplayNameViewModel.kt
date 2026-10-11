package com.example.comp90018.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comp90018.data.ProfileData
import com.example.comp90018.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.comp90018.data.UserLoginRepository
import com.example.comp90018.data.SupabaseAuthRepository


class DisplayNameViewModel(
    private val repo: ProfileRepository = ProfileRepository(),
    private val authRepo: UserLoginRepository = SupabaseAuthRepository()
) : ViewModel() {

    private val _profile = MutableStateFlow<ProfileData?>(null)
    val profile: StateFlow<ProfileData?> = _profile.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            val userId = authRepo.getUserId()
            _profile.value = repo.getProfileData(userId)
        }
    }

    fun setProfileDataDisplayName(displayName: String) {
        viewModelScope.launch {
            val userId = authRepo.getUserId()
            repo.setProfileData(userId, displayName)
            loadProfile()
        }
    }

    fun saveProfile(displayName: String, dailyStepTarget: Int, unitSystem: String) {
        viewModelScope.launch {
            val id = authRepo.getUserId()
            repo.updateProfile(id, displayName, dailyStepTarget, unitSystem)
            loadProfile() 
        }
    }
}