package com.informatique.mtcit.business.validation

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

data class LoginValidationResult(
    val usernameValidation: ValidationResult,
    val passwordValidation: ValidationResult
) {
    fun isValid(): Boolean {
        return usernameValidation is ValidationResult.Success &&
               passwordValidation is ValidationResult.Success
    }

    fun getErrorMessage(): String? {
        return when {
            usernameValidation is ValidationResult.Error -> usernameValidation.message
            passwordValidation is ValidationResult.Error -> passwordValidation.message
            else -> null
        }
    }
}
