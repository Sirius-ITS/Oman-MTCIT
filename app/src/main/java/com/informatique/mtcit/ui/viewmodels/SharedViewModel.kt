package com.informatique.mtcit.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.informatique.mtcit.data.model.loginModels.CardProfile
import com.informatique.mtcit.data.model.loginModels.UserMainData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SharedUserViewModel @Inject constructor() : ViewModel() {

    init {
        Log.d("SharedUserViewModel", "ViewModel initialized")
    }

    private val _userMainData = MutableStateFlow<UserMainData?>(null)

    private val _cardProfile = MutableStateFlow<CardProfile?>(null)
    val cardProfile: StateFlow<CardProfile?> = _cardProfile

    // ✅ NEW: User role state
    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    // ✅ NEW: Computed property to check if user is engineer
    val isEngineer: Boolean
        get() = _userRole.value?.equals("engineer", ignoreCase = true) == true

    // ✅ NEW: Computed property to check if user is client
    val isClient: Boolean
        get() = _userRole.value?.equals("client", ignoreCase = true) == true

    fun setUserMainData(data: UserMainData?) {
        Log.d("SharedUserViewModel", "Setting UserMainData: $data")
        _userMainData.value = data
    }

    fun setCardProfile(profile: CardProfile?) {
        Log.d("SharedUserViewModel", "Setting CardProfile: $profile")
        _cardProfile.value = profile
    }

    // ✅ NEW: Set user role
    fun setUserRole(role: String?) {
        Log.d("SharedUserViewModel", "Setting UserRole: $role")
        _userRole.value = role
    }
}