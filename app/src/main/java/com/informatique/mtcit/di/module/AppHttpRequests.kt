package com.informatique.mtcit.di.module

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.json.JsonElement

open class AppHttpRequests(val client: HttpClient) {

    protected suspend fun onPostAuthData(url: String, data: Any): AppHttpRequest {
        return try {
            val response = client.submitForm(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(data)
            }
            AppHttpRequest.AppHttpRequestModel(key = "", response = response)
        } catch (ex: Exception){
            AppHttpRequest.AppHttpRequestErrorModel(
                key = "", code = 0, message = ex.message.toString())
        }
    }

    protected suspend fun onPostData(url: String, data: Any): AppHttpRequest {
        return try {
            val response = client.post(url) { setBody(data) }
            AppHttpRequest.AppHttpRequestModel(key = "", response = response)
        } catch (ex: Exception){
            AppHttpRequest.AppHttpRequestErrorModel(
                key = "", code = 0, message = ex.message.toString())
        }
    }

    protected suspend fun onGetData(url: String): AppHttpRequest {
        return try {
            val response = client.get(url)
            AppHttpRequest.AppHttpRequestModel(key = "", response = response)
        } catch (ex: Exception){
            AppHttpRequest.AppHttpRequestErrorModel(
                key = "", code = 0, message = ex.message.toString())
        }
    }

    protected suspend fun onPutData(url: String, data: Any?): JsonElement {
        return client.put(url){
            setBody(data)
        }.body(TypeInfo(JsonElement::class))
    }

    protected suspend fun onPostMultipartData(url: String, data: List<PartData>): AppHttpRequest {
        return try {
            val response = client.submitFormWithBinaryData(url = url, data)
            AppHttpRequest.AppHttpRequestModel(key = "", response = response)
        } catch (ex: Exception){
            AppHttpRequest.AppHttpRequestErrorModel(
                key = "", code = 0, message = ex.message.toString())
        }
    }

    protected suspend fun onDeleteData(url: String): JsonElement {
        return client.delete(url){}
            .body(TypeInfo(JsonElement::class))
    }

}