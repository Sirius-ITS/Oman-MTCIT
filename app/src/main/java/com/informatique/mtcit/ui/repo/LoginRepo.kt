package com.informatique.mtcit.ui.repo

import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.business.auth.LoginParams
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

@ViewModelScoped
class LoginRepo
@Inject constructor(
    val repo: AppRepository
) {

    fun onLogin(login: LoginParams): Flow<BusinessState<LoginResponse>> = flow {
        val serviceState = repo.onPostAuth("students/login", login)
        when (serviceState) {
            is RepoServiceState.Success -> {
                val response = serviceState.response
                if (!response.jsonObject.isEmpty()) {
                    val loginResponse: LoginResponse =
                        Json.decodeFromJsonElement(response.jsonObject)

                    emit(BusinessState.Success(loginResponse))
                }
            }

            is RepoServiceState.Error -> {
                emit(
                    BusinessState.Error(serviceState.error.toString()))
            }
        }
    }

}

sealed interface Response<out T> {
    data class Success(val data: LoginResponse): Response<LoginResponse>
    data class Error(val code: Int, val message: String): Response<Nothing>
}