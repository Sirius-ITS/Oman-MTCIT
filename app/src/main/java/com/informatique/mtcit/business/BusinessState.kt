package com.informatique.mtcit.business

sealed interface BusinessState<out T> {
    data class Success<T>(val data: T) : BusinessState<T>
    data class Error(val message: String) : BusinessState<Nothing>
    object Loading : BusinessState<Nothing>
}

