package com.informatique.mtcit.common

/**
 * Custom exception for API errors that preserves HTTP status code and message
 * Used to propagate structured error information from Repository -> Strategy -> ViewModel -> UI
 */
class ApiException(
    val code: Int,
    message: String
) : Exception(message)

