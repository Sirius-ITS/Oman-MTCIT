package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.model.requests.RequestsApiResponse
import com.informatique.mtcit.data.model.requests.RequestDetailResponse
import com.informatique.mtcit.data.model.requests.StatusCountResponse
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
        page: Int = 0,
        sort: String = "lastChange,desc"
    ): Result<RequestsApiResponse> {
        return try {
            println("🔍 Fetching user requests for civilId: $civilId")
            println("📄 Page: $page, Size: $size")

            val endpoint = "registration-request-view/customer/$civilId?size=$size&page=$page&sort=$sort"
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
     * Get filtered user requests with pagination
     * Uses Base64 encoded filter parameter
     *
     * @param civilId User's civil ID (from token)
     * @param filter RequestFilterDto containing filter criteria
     * @return Result with RequestsApiResponse
     * @throws ApiException for HTTP error codes
     */
    suspend fun getFilteredUserRequests(
        civilId: String,
        filter: com.informatique.mtcit.data.model.requests.RequestFilterDto
    ): Result<RequestsApiResponse> {
        return try {
            val base64Filter = filter.toBase64()
            println("🔍 Fetching filtered user requests for civilId: $civilId")
            println("📋 Filter: statusId=${filter.statusId}, page=${filter.page}, size=${filter.size}")
            println("🔐 Base64 Filter: $base64Filter")

            val endpoint = "registration-request-view/customer/filtered/$civilId?filter=$base64Filter"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Filtered API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val apiResponse: RequestsApiResponse = json.decodeFromJsonElement(responseJson)

                            println("✅ Parsed ${apiResponse.data?.content?.size ?: 0} filtered requests")
                            println("📄 Total Elements: ${apiResponse.data?.totalElements}")
                            println("📄 Total Pages: ${apiResponse.data?.totalPages}")

                            Result.success(apiResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "حدث خطأ في الخادم"

                            println("❌ API Error: Status code $statusCode - $message")

                            when (statusCode) {
                                401 -> throw ApiException(401, message)
                                403 -> throw ApiException(403, "ليس لديك صلاحية للوصول")
                                404 -> throw ApiException(404, "الطلبات غير موجودة")
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
                        404 -> "الطلبات غير موجودة"
                        500 -> "خطأ في الخادم"
                        else -> errorMessage
                    }

                    println("❌ Throwing ApiException: $errorCode - $friendlyMessage")
                    throw ApiException(errorCode, friendlyMessage)
                }
            }
        } catch (e: ApiException) {
            println("❌ ApiException in getFilteredUserRequests: ${e.code} - ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in getFilteredUserRequests: ${e.message}")
            e.printStackTrace()
            throw ApiException(500, e.message ?: "حدث خطأ غير متوقع")
        }
    }

    /**
     * Get request detail by request ID and type
     * Dynamically constructs endpoint based on request type
     *
     * @param requestId The request ID
     * @param endpointPath The endpoint path (e.g., "registration-requests", "perm-registration-requests")
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
                        // ✅ Check if this is a standard wrapped response or direct data response
                        val hasStandardWrapper = responseJson.jsonObject.containsKey("statusCode") &&
                                                 responseJson.jsonObject.containsKey("success")

                        if (hasStandardWrapper) {
                            // ✅ Standard wrapped response (most transactions)
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
                            // ✅ Direct data response (e.g., change-ship-info endpoint)
                            println("📦 Direct data response detected - wrapping in standard structure")

                            // Wrap the direct data in the standard response structure
                            val wrappedResponse = RequestDetailResponse(
                                message = "Retrieved Successfully",
                                statusCode = 200,
                                success = true,
                                timestamp = "",
                                data = responseJson // The entire response is the data
                            )

                            println("✅ Direct response wrapped successfully")
                            Result.success(wrappedResponse)
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

            // ✅ Determine HTTP method based on endpoint
            // Mortgage endpoints (types 4 & 5) use PATCH, others use POST
            val usePatchMethod = issuanceEndpoint.contains("certificate/") &&
                                 (issuanceEndpoint.contains("mortgage-certificate") ||
                                  issuanceEndpoint.contains("mortgage-redemption-certificate"))

            val httpMethod = if (usePatchMethod) "PATCH" else "POST"
            println("📡 API Call ($httpMethod): $issuanceEndpoint")

            val response = if (usePatchMethod) {
                repo.onPatchAuth(issuanceEndpoint, "")
            } else {
                repo.onPostAuth(issuanceEndpoint, "")
            }

            when (response) {
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
     * ✅ Get certificate by certification number (for already issued certificates)
     * Endpoint: GET /certificate/{certificationNumber}
     *
     * @param certificationNumber The certificate number
     * @return Result with certificate data including QR code
     * @throws ApiException for HTTP error codes
     */
    suspend fun getCertificate(
        certificationNumber: String
    ): Result<RequestDetailResponse> {
        return try {
            println("🔍 Fetching certificate: certificationNumber=$certificationNumber")
            println("📡 API Call (GET): certificate/$certificationNumber")

            when (val response = repo.onGet("certificate/$certificationNumber")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Certificate response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            // Parse the response (common response format)
                            val certificateResponse: RequestDetailResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Certificate fetched successfully")

                            Result.success(certificateResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "حدث خطأ في جلب الشهادة"
                            println("❌ API Error: Status code $statusCode - $message")

                            when (statusCode) {
                                401 -> throw ApiException(401, message)
                                403 -> throw ApiException(403, "ليس لديك صلاحية للوصول")
                                404 -> throw ApiException(404, "الشهادة غير موجودة")
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
                    val errorMessage = response.error?.toString() ?: "حدث خطأ في جلب الشهادة"

                    val errorCode = when {
                        errorMessage.contains("401", ignoreCase = true) -> 401
                        errorMessage.contains("403", ignoreCase = true) -> 403
                        errorMessage.contains("404", ignoreCase = true) -> 404
                        errorMessage.contains("500", ignoreCase = true) -> 500
                        else -> extractStatusCode(errorMessage) ?: 500
                    }

                    val friendlyMessage = when (errorCode) {
                        401 -> "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة"
                        403 -> "ليس لديك صلاحية لعرض هذه الشهادة"
                        404 -> "الشهادة غير موجودة"
                        else -> errorMessage
                    }

                    throw ApiException(errorCode, friendlyMessage)
                }
            }
        } catch (e: ApiException) {
            println("❌ ApiException in getCertificate: ${e.code} - ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in issueCertificate: ${e.message}")
            e.printStackTrace()
            throw ApiException(500, e.message ?: "حدث خطأ غير متوقع")
        }
    }

    /**
     * Get request counts by status for a customer
     * Endpoint: GET /registration-request-view/customer/count-by-status/{customerId}
     *
     * @param customerId Customer's civil ID (from token)
     * @return Result with StatusCountResponse containing total count and status breakdown
     * @throws ApiException for HTTP error codes
     */
    suspend fun getStatusCounts(customerId: String): Result<StatusCountResponse> {
        return try {
            println("🔍 Fetching status counts for customerId: $customerId")

            val endpoint = "registration-request-view/customer/count-by-status/$customerId"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Status counts response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val statusCountResponse: StatusCountResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Status counts parsed successfully")
                            println("📊 Total Count: ${statusCountResponse.data?.totalCount}")
                            statusCountResponse.data?.statusCounts?.forEach { status ->
                                println("   StatusId ${status.statusId}: ${status.count}")
                            }

                            Result.success(statusCountResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "حدث خطأ في الخادم"
                            println("❌ API Error: Status code $statusCode - $message")

                            when (statusCode) {
                                401 -> throw ApiException(401, message)
                                403 -> throw ApiException(403, "ليس لديك صلاحية للوصول")
                                404 -> throw ApiException(404, "البيانات غير موجودة")
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
                        401 -> "انتهت صلاحية الجلسة"
                        403 -> "ليس لديك صلاحية للوصول"
                        404 -> "البيانات غير موجودة"
                        else -> errorMessage
                    }

                    throw ApiException(errorCode, friendlyMessage)
                }
            }
        } catch (e: ApiException) {
            println("❌ ApiException in getStatusCounts: ${e.code} - ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in getStatusCounts: ${e.message}")
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
