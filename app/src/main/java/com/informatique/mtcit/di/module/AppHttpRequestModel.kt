package com.informatique.mtcit.di.module

import io.ktor.client.statement.HttpResponse

sealed class AppHttpRequest {

    data class AppHttpRequestModel(
        val key: String,
        val response: HttpResponse): AppHttpRequest()

    data class AppHttpRequestErrorModel(
        val key: String,
        val code: Int,
        val message: String): AppHttpRequest()

}
