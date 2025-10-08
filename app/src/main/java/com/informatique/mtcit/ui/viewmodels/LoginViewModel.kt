package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.auth.LoginParams
import com.informatique.mtcit.business.validation.LoginValidator
import com.informatique.mtcit.common.NoInternetException
import com.informatique.mtcit.common.networkhelper.NetworkHelper
import com.informatique.mtcit.data.model.loginModels.CardProfile
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import com.informatique.mtcit.data.model.loginModels.UserMainData
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.repo.LoginRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LoginViewModel that handles login operations with Hilt dependency injection
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginValidator: LoginValidator,
    private val networkHelper: NetworkHelper,
    private val loginRepo: LoginRepo

//    private val loginUseCase: LoginUseCase,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _loginState = MutableStateFlow<UIState<LoginResponse>>(UIState.Empty)
    val loginState: StateFlow<UIState<LoginResponse>> = _loginState

    private val _userMainData = MutableStateFlow<UserMainData?>(null)

    private val _cardProfile = MutableStateFlow<CardProfile?>(null)

    fun login(username: String, password: String){
        if (!networkHelper.isNetworkConnected()) {
            _loginState.value = UIState.Failure(NoInternetException())
            return
        }

        val validationResult = loginValidator.validateCredentials(username, password)
        if (!validationResult.isValid()) {
            _loginState.value = UIState.Error((validationResult.getErrorMessage() ?: "Validation failed"))
            return
        }

        _loginState.value = UIState.Loading

        viewModelScope.launch {
            loginRepo.onLogin(LoginParams(username, password))
                .flowOn(Dispatchers.IO)
                .catch {
                    _loginState.value = UIState.Error((it.message ?: ""))
                }
                .collect {
                    when (it) {
                        is BusinessState.Success -> handleLoginSuccess(it.data)
                        is BusinessState.Error ->_loginState.value = UIState.Error((it.message))
                        BusinessState.Loading -> _loginState.value = UIState.Loading
                    }
                }
        }
    }

    /*fun login(username: String, password: String) = viewModelScope.launch(Dispatchers.IO) {
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
    }*/

    private fun handleLoginSuccess(response: LoginResponse) {
        _loginState.value = UIState.Success(response)
        _userMainData.value = response.userMainData
        _cardProfile.value = response.cardProfile
    }

    private fun handleLoginError(message: String) {
        _loginState.value = UIState.Failure(Exception(message))
    }
}