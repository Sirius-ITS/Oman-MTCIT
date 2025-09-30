package com.informatique.educationComposeVersion.business.auth

import com.informatique.educationComposeVersion.business.BusinessState
import com.informatique.educationComposeVersion.business.base.BaseUseCase
import com.informatique.educationComposeVersion.business.validation.LoginValidator
import com.informatique.educationComposeVersion.data.model.loginModels.LoginResponse
import javax.inject.Inject

data class LoginParams(
    val username: String,
    val password: String
)

interface AuthRepository {
    suspend fun login(username: String, password: String): LoginResponse
}

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val loginValidator: LoginValidator
) : BaseUseCase<LoginParams, LoginResponse>() {
    override suspend fun invoke(parameters: LoginParams): BusinessState<LoginResponse> {
        return try {
            // Perform validation first
            val validationResult = loginValidator.validateCredentials(
                parameters.username,
                parameters.password
            )

            if (!validationResult.isValid()) {
                return BusinessState.Error(validationResult.getErrorMessage() ?: "Validation failed")
            }

            // If validation passes, proceed with login
            val result = repository.login(parameters.username, parameters.password)
            BusinessState.Success(result)
        } catch (e: Exception) {
            BusinessState.Error(e.message ?: "Unknown error")
        }
    }
}
