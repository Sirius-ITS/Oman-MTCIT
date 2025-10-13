package com.informatique.mtcit.common

/**
 * Centralized error handling for the application
 */
sealed class AppError {
    data class Network(val message: String) : AppError()
    data class Validation(val fieldErrors: Map<String, String>) : AppError()
    data class FileUpload(val message: String) : AppError()
    data class CompanyLookup(val message: String) : AppError()
    data class Submission(val message: String) : AppError()
    data class Initialization(val message: String) : AppError() // For transaction initialization errors
    data class Unknown(val message: String) : AppError()
}
