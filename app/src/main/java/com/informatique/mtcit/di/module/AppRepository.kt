package com.informatique.mtcit.di.module

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.http.content.PartData
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class AppRepository(client: HttpClient): AppHttpRequests(client = client) {

    suspend fun onGet(url: String): RepoServiceState {
        val data = onGetData(url = url)
        return when (data){
            is AppHttpRequest.AppHttpRequestModel -> {
                if (data.response.status.value == 200 || data.response.status.value == 201){
                    RepoServiceState.Success(
                        data.response.body(TypeInfo(JsonObject::class)))
                } else {
                    RepoServiceState.Error(
                        data.response.status.value,
                        data.response.status)
                }
            }
            is AppHttpRequest.AppHttpRequestErrorModel -> {
                RepoServiceState.Error(data.code, data.message)
            }
        }
    }

    suspend fun onPostAuth(url: String, body: Any): RepoServiceState {
        val data = onPostData(url = url, data = body)
        return when (data){
            is AppHttpRequest.AppHttpRequestModel -> {
                if (data.response.status.value == 200 || data.response.status.value == 201){
                    RepoServiceState.Success(
                        data.response.body(TypeInfo(JsonElement::class)))
                } else {
                    RepoServiceState.Error(
                        data.response.status.value,
                        data.response.status)
                }
            }
            is AppHttpRequest.AppHttpRequestErrorModel -> {
                RepoServiceState.Error(data.code, data.message)
            }
        }
    }

    suspend fun onPostMultipart(url: String, data: List<PartData>): RepoServiceState {
        return try {
            val data = onPostMultipartData(url = url, data = data)

            when(data){
                is AppHttpRequest.AppHttpRequestModel -> {
                    if (data.response.status.value == 200 || data.response.status.value == 201){
                        RepoServiceState.Success(
                            data.response.body(TypeInfo(JsonElement::class)))
                    } else {
                        RepoServiceState.Error(
                            data.response.status.value,
                            data.response.status)
                    }
                }

                is AppHttpRequest.AppHttpRequestErrorModel -> {
                    RepoServiceState.Error(data.code, data.message)
                }
            }
        } catch (ex: Exception){
            RepoServiceState.Error(0, ex.message)
        }
    }

    fun onClose(){
        client.close()
    }
}