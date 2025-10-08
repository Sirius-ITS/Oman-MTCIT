package com.informatique.mtcit.di.module

import kotlinx.serialization.json.JsonElement

sealed interface RepoServiceState {
    data class Success(val response: JsonElement): RepoServiceState
    data class Error(val code: Int, val error: Any?): RepoServiceState
}