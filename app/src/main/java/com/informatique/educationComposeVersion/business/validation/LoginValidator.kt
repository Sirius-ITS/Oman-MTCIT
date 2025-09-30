package com.informatique.educationComposeVersion.business.validation

import javax.inject.Inject

class LoginValidator @Inject constructor() {
    fun validateUsername(username: String): ValidationResult {
        if (username.isBlank()) {
            return ValidationResult.Error("Username cannot be empty")
        }
        if (username.length < 3) {
            return ValidationResult.Error("Username must be at least 3 characters long")
        }
        if (username.length > 50) {
            return ValidationResult.Error("Username cannot be longer than 50 characters")
        }
        if (!username.matches(Regex("^[a-zA-Z0-9._-]+$"))) {
            return ValidationResult.Error("Username can only contain letters, numbers, dots, underscores and hyphens")
        }
        return ValidationResult.Success
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult.Error("Password cannot be empty")
        }
        if (password.length < 6) {
            return ValidationResult.Error("Password must be at least 6 characters long")
        }
        if (password.length > 100) {
            return ValidationResult.Error("Password cannot be longer than 100 characters")
        }
        if (!password.contains(Regex("[A-Z]"))) {
            return ValidationResult.Error("Password must contain at least one uppercase letter")
        }
        if (!password.contains(Regex("[a-z]"))) {
            return ValidationResult.Error("Password must contain at least one lowercase letter")
        }
        if (!password.contains(Regex("[0-9]"))) {
            return ValidationResult.Error("Password must contain at least one number")
        }
        if (!password.contains(Regex("[^A-Za-z0-9]"))) {
            return ValidationResult.Error("Password must contain at least one special character")
        }
        return ValidationResult.Success
    }

    fun validateCredentials(username: String, password: String): LoginValidationResult {
        return LoginValidationResult(
            usernameValidation = validateUsername(username),
            passwordValidation = validatePassword(password)
        )
    }
}
