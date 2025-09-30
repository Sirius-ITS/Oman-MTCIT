package com.informatique.educationComposeVersion.data.repository

import com.informatique.educationComposeVersion.business.auth.AuthRepository
import com.informatique.educationComposeVersion.data.model.loginModels.LoginResponse
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val loginRepository: LoginRepository
) : AuthRepository {
    override suspend fun login(username: String, password: String): LoginResponse {
        return loginRepository.loginUser(username, password).first()
    }
}
