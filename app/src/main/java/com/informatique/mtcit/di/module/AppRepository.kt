package com.informatique.mtcit.di.module

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.http.content.PartData
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class AppRepository(client: HttpClient): AppHttpRequests(client = client) {

    suspend fun onGet(url: String): RepoServiceState {
        return when (val data = onGetData(url = url)){
            is AppHttpRequest.AppHttpRequestModel -> {
                if (data.response.status.value == 200 || data.response.status.value == 201){
                    RepoServiceState.Success(
                        data.response.body(TypeInfo(JsonElement::class)))  // ✅ غيرها هنا
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
                val statusCode = data.response.status.value

                // Always try to parse the response body
                val responseBody = try {
                    data.response.body<JsonElement>(TypeInfo(JsonElement::class))
                } catch (e: Exception) {
                    println("❌ Failed to parse response body: ${e.message}")
                    null
                }

                if (statusCode == 200 || statusCode == 201) {
                    RepoServiceState.Success(responseBody ?: JsonElement.serializer().descriptor.let {
                        kotlinx.serialization.json.JsonObject(emptyMap())
                    })
                } else {
                    // For error responses, include the body for better error messages
                    println("⚠️ HTTP $statusCode - Response body: $responseBody")
                    RepoServiceState.Error(statusCode, responseBody?.toString() ?: data.response.status.toString())
                }
            }
            is AppHttpRequest.AppHttpRequestErrorModel -> {
                RepoServiceState.Error(data.code, data.message)
            }
        }
    }

    // New: JSON-specific POST that sets Content-Type: application/json
    suspend fun onPostAuthJson(url: String, jsonBody: String): RepoServiceState {
        val data = onPostJsonData(url = url, jsonBody = jsonBody)
        return when (data){
            is AppHttpRequest.AppHttpRequestModel -> {
                val statusCode = data.response.status.value

                // Always try to parse the response body
                val responseBody = try {
                    data.response.body<JsonElement>(TypeInfo(JsonElement::class))
                } catch (e: Exception) {
                    println("❌ Failed to parse response body: ${e.message}")
                    null
                }

                if (statusCode == 200 || statusCode == 201) {
                    RepoServiceState.Success(responseBody ?: JsonElement.serializer().descriptor.let {
                        kotlinx.serialization.json.JsonObject(emptyMap())
                    })
                } else {
                    // For error responses, include the body for better error messages
                    println("⚠️ HTTP $statusCode - Response body: $responseBody")
                    RepoServiceState.Error(statusCode, responseBody?.toString() ?: data.response.status.toString())
                }
            }
            is AppHttpRequest.AppHttpRequestErrorModel -> {
                RepoServiceState.Error(data.code, data.message)
            }
        }
    }

    suspend fun onPutAuth(url: String, body: Any): RepoServiceState {
        val data = onPutData(url = url, data = body)
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

    suspend fun onPatchAuth(url: String, body: Any): RepoServiceState {
        val data = onPatchData(url = url, data = body)
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

    // New helper: fetch raw response body as text (for HTML responses like payment redirect form)
    suspend fun fetchRawString(url: String): Result<String> {
        return try {
            when (val data = onGetData(url = url)) {
                is AppHttpRequest.AppHttpRequestModel -> {
                    val statusCode = data.response.status.value
                    if (statusCode == 200 || statusCode == 201) {
                        val text = try {
                            data.response.body(TypeInfo(String::class))
                        } catch (e: Exception) {
                            println("❌ Failed to read response as text: ${e.message}")
                            ""
                        }
                        Result.success(text)
                    } else {
                        Result.failure(Exception("HTTP $statusCode"))
                    }
                }
                is AppHttpRequest.AppHttpRequestErrorModel -> {
                    Result.failure(Exception(data.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}