package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.model.requests.RequestsApiResponse
import com.informatique.mtcit.data.model.requests.RequestDetailResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for User Requests
 * Endpoint: GET /request/{civilId}/user-requests?size={size}&page={page}
 *
 * ✅ Supports proper error handling with HTTP status codes (401, 500, etc.)
 */
@Singleton
class RequestsApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Get user requests with pagination
     *
     * @param civilId User's civil ID (from token)
     * @param size Number of items per page (default 10)
     * @param page Page number (0-based, default 0)
     * @return Result with RequestsApiResponse
     * @throws ApiException for HTTP error codes (401, 500, etc.)
     */
    suspend fun getUserRequests(
        civilId: String,
        size: Int = 10,
        page: Int = 0
    ): Result<RequestsApiResponse> {
        return try {
            println("🔍 Fetching user requests for civilId: $civilId")
            println("📄 Page: $page, Size: $size")

            val endpoint = "registration-request-view/customer/$civilId?size=$size&page=$page"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            // Parse the full response using kotlinx.serialization
                            val apiResponse: RequestsApiResponse = json.decodeFromJsonElement(responseJson)

                            println("✅ Parsed ${apiResponse.data?.content?.size ?: 0} requests")
                            println("📄 Total Elements: ${apiResponse.data?.totalElements}")
                            println("📄 Total Pages: ${apiResponse.data?.totalPages}")
                            println("📄 Current Page: ${apiResponse.data?.number}")
                            println("📄 Is Last Page: ${apiResponse.data?.last}")

                            // Log transaction type mappings
                            apiResponse.data?.content?.forEach { request ->
                                val transactionType = request.getTransactionType()
                                val displayName = request.getRequestTypeDisplayName()
                                println("📋 Request #${request.id}: typeId=${request.requestTypeId} → ${transactionType?.name ?: "UNKNOWN"} ($displayName)")
                            }

                            Result.success(apiResponse)
                        } else {
                            // ✅ Handle specific error codes
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "حدث خطأ في الخادم"

                            println("❌ API Error: Status code $statusCode - $message")

                            // ✅ Throw ApiException with status code
                            when (statusCode) {
                                401 -> throw ApiException(401, message)
                                403 -> throw ApiException(403, "ليس لديك صلاحية للوصول")
                                404 -> throw ApiException(404, "الطلبات غير موجودة")
                                406 -> throw ApiException(406, message)
                                500 -> throw ApiException(500, "خطأ في الخادم")
                                else -> throw ApiException(statusCode, message)
                            }
                        }
                    } else {
                        // ✅ Empty JSON response - this is likely a 401 error with empty body
                        println("❌ Empty JSON response - checking for HTTP error")
                        throw ApiException(500, "استجابة فارغة من الخادم")
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")

                    // ✅ Parse error string to extract status code
                    val errorMessage = response.error?.toString() ?: "حدث خطأ في الخادم"

                    // ✅ Check if error contains "401 Unauthorized"
                    val errorCode = when {
                        errorMessage.contains("401", ignoreCase = true) -> 401
                        errorMessage.contains("403", ignoreCase = true) -> 403
                        errorMessage.contains("404", ignoreCase = true) -> 404
                        errorMessage.contains("406", ignoreCase = true) -> 406
                        errorMessage.contains("500", ignoreCase = true) -> 500
                        else -> extractStatusCode(errorMessage) ?: 500
                    }

                    val friendlyMessage = when (errorCode) {
                        401 -> "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة"
                        403 -> "ليس لديك صلاحية للوصول"
                        404 -> "الطلبات غير موجودة"
                        else -> errorMessage
                    }

                    println("❌ Extracted error code: $errorCode")
                    throw ApiException(errorCode, friendlyMessage)
                }
            }
        } catch (e: ApiException) {
            // ✅ Re-throw ApiException to preserve error code
            println("❌ ApiException in getUserRequests: ${e.code} - ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in getUserRequests: ${e.message}")
            e.printStackTrace()

            // ✅ Wrap other exceptions as ApiException 500
            throw ApiException(500, e.message ?: "حدث خطأ غير متوقع")
        }
    }

    /**
     * Get request detail by request ID and type
     * Dynamically constructs endpoint based on request type
     *
     * @param requestId The request ID
     * @param endpointPath The endpoint path (e.g., "registration-requests", "perm_registration-requests")
     * @return Result with RequestDetailResponse containing dynamic data
     * @throws ApiException for HTTP error codes
     */
    suspend fun getRequestDetail(
        requestId: Int,
        endpointPath: String
    ): Result<RequestDetailResponse> {
        return try {
            println("🔍 Fetching request detail: requestId=$requestId, endpoint=$endpointPath")

            val endpoint = "$endpointPath/$requestId"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Request detail response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            // Parse the response
                            val detailResponse: RequestDetailResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Request detail parsed successfully")

                            Result.success(detailResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "حدث خطأ في الخادم"
                            println("❌ API Error: Status code $statusCode - $message")

                            when (statusCode) {
                                401 -> throw ApiException(401, message)
                                403 -> throw ApiException(403, "ليس لديك صلاحية للوصول")
                                404 -> throw ApiException(404, "الطلب غير موجود")
                                500 -> throw ApiException(500, "خطأ في الخادم")
                                else -> throw ApiException(statusCode, message)
                            }
                        }
                    } else {
                        println("❌ Empty JSON response")
                        throw ApiException(500, "استجابة فارغة من الخادم")
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = response.error?.toString() ?: "حدث خطأ في الخادم"

                    val errorCode = when {
                        errorMessage.contains("401", ignoreCase = true) -> 401
                        errorMessage.contains("403", ignoreCase = true) -> 403
                        errorMessage.contains("404", ignoreCase = true) -> 404
                        errorMessage.contains("500", ignoreCase = true) -> 500
                        else -> extractStatusCode(errorMessage) ?: 500
                    }

                    val friendlyMessage = when (errorCode) {
                        401 -> "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة"
                        403 -> "ليس لديك صلاحية للوصول"
                        404 -> "الطلب غير موجود"
                        else -> errorMessage
                    }

                    throw ApiException(errorCode, friendlyMessage)
                }
            }
        } catch (e: ApiException) {
            println("❌ ApiException in getRequestDetail: ${e.code} - ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in getRequestDetail: ${e.message}")
            e.printStackTrace()
            throw ApiException(500, e.message ?: "حدث خطأ غير متوقع")
        }
    }

    /**
     * Issue certificate for a request (POST)
     * Called when isPaid == 1 and status is APPROVED
     *
     * @param issuanceEndpoint The full issuance endpoint path
     * @return Result with common response containing certificate data
     * @throws ApiException for HTTP error codes
     */
    suspend fun issueCertificate(
        issuanceEndpoint: String
    ): Result<RequestDetailResponse> {
        return try {
            println("🔍 Issuing certificate: endpoint=$issuanceEndpoint")
            println("📡 API Call (POST): $issuanceEndpoint")

            when (val response = repo.onPostAuth(issuanceEndpoint, "")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Certificate issuance response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            // Parse the response (common response format)
                            val issuanceResponse: RequestDetailResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Certificate issued successfully")

                            Result.success(issuanceResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "حدث خطأ في إصدار الشهادة"
                            println("❌ API Error: Status code $statusCode - $message")

                            when (statusCode) {
                                401 -> throw ApiException(401, message)
                                403 -> throw ApiException(403, "ليس لديك صلاحية للوصول")
                                404 -> throw ApiException(404, "الطلب غير موجود")
                                500 -> throw ApiException(500, "خطأ في الخادم")
                                else -> throw ApiException(statusCode, message)
                            }
                        }
                    } else {
                        println("❌ Empty JSON response")
                        throw ApiException(500, "استجابة فارغة من الخادم")
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = response.error?.toString() ?: "حدث خطأ في إصدار الشهادة"

                    val errorCode = when {
                        errorMessage.contains("401", ignoreCase = true) -> 401
                        errorMessage.contains("403", ignoreCase = true) -> 403
                        errorMessage.contains("404", ignoreCase = true) -> 404
                        errorMessage.contains("500", ignoreCase = true) -> 500
                        else -> extractStatusCode(errorMessage) ?: 500
                    }

                    val friendlyMessage = when (errorCode) {
                        401 -> "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة"
                        403 -> "ليس لديك صلاحية لإصدار هذه الشهادة"
                        404 -> "الطلب غير موجود"
                        else -> errorMessage
                    }

                    throw ApiException(errorCode, friendlyMessage)
                }
            }
        } catch (e: ApiException) {
            println("❌ ApiException in issueCertificate: ${e.code} - ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in issueCertificate: ${e.message}")
            e.printStackTrace()
            throw ApiException(500, e.message ?: "حدث خطأ غير متوقع")
        }
    }

    /**
     * Extract status code from error message if present
     */
    private fun extractStatusCode(error: String): Int? {
        return try {
            // Try to parse status code from error message like "401: Unauthorized"
            val regex = Regex("^(\\d{3}):")
            regex.find(error)?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
