package com.informatique.educationComposeVersion.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.educationComposeVersion.business.BusinessState
import com.informatique.educationComposeVersion.business.auth.LoginParams
import com.informatique.educationComposeVersion.business.auth.LoginUseCase
import com.informatique.educationComposeVersion.common.NoInternetException
import com.informatique.educationComposeVersion.common.networkhelper.NetworkHelper
import com.informatique.educationComposeVersion.data.model.loginModels.CardProfile
import com.informatique.educationComposeVersion.data.model.loginModels.LoginResponse
import com.informatique.educationComposeVersion.data.model.loginModels.UserMainData
import com.informatique.educationComposeVersion.di.IoDispatcher
import com.informatique.educationComposeVersion.ui.base.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LoginViewModel that handles login operations with Hilt dependency injection
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val networkHelper: NetworkHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _loginState = MutableStateFlow<UIState<LoginResponse>>(UIState.Empty)
    val loginState: StateFlow<UIState<LoginResponse>> = _loginState

    private val _userMainData = MutableStateFlow<UserMainData?>(null)

    private val _cardProfile = MutableStateFlow<CardProfile?>(null)

    fun login(username: String, password: String) = viewModelScope.launch(ioDispatcher) {
        try {
            if (!networkHelper.isNetworkConnected()) {
                _loginState.value = UIState.Failure(NoInternetException())
                return@launch
            }

            _loginState.value = UIState.Loading

            when (val result = loginUseCase(LoginParams(username, password))) {
                is BusinessState.Success -> handleLoginSuccess(result.data)
                is BusinessState.Error ->_loginState.value = UIState.Error((result.message))
                BusinessState.Loading -> _loginState.value = UIState.Loading
            }
        } catch (e: Exception) {
            _loginState.value = UIState.Error((e.message.toString()))
        }
    }

    private fun handleLoginSuccess(response: LoginResponse) {
        _loginState.value = UIState.Success(response)
        _userMainData.value = response.userMainData
        _cardProfile.value = response.cardProfile
    }

    private fun handleLoginError(message: String) {
        _loginState.value = UIState.Failure(Exception(message))
    }
}