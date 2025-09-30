package com.informatique.educationComposeVersion.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.informatique.educationComposeVersion.data.model.loginModels.CardProfile
import com.informatique.educationComposeVersion.data.model.loginModels.UserMainData
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

    fun setUserMainData(data: UserMainData?) {
        Log.d("SharedUserViewModel", "Setting UserMainData: $data")
        _userMainData.value = data
    }

    fun setCardProfile(profile: CardProfile?) {
        Log.d("SharedUserViewModel", "Setting CardProfile: $profile")
        _cardProfile.value = profile
    }
}