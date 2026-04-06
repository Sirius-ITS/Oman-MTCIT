package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ErrorMessageExtractor
import com.informatique.mtcit.data.model.CreateInspectionRequestDto
import com.informatique.mtcit.data.model.CreateInspectionRequestResponse
import com.informatique.mtcit.data.model.InspectionFileUpload
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.asInput
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import com.informatique.mtcit.common.util.AppLanguage

/**
 * API Service for Inspection Requests
 * Handles inspection request creation with document uploads
 */
@Singleton
class InspectionApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Get filtered inspection requests for engineer
     * GET api/v1/inspection-requests/filtered/engineer?filter={base64EncodedFilter}
     *
     * Filter structure: {"searchText":"","columnName":"requestNumber","page":0,"size":10}
     *
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @param searchText Optional search text
     * @param columnName Column to search in (default: "requestNumber")
     * @return Result with RequestsApiResponse
     */
    suspend fun getEngineerInspectionRequests(
        page: Int = 0,
        size: Int = 10,
        searchText: String = "",
        columnName: String = "requestNumber"
    ): Result<com.informatique.mtcit.data.model.requests.RequestsApiResponse> {
        return try {
            println("=".repeat(80))
            println("🔍 InspectionApiService: Getting engineer inspection requests...")
            println("=".repeat(80))

            // Create simplified filter
            val filter = com.informatique.mtcit.data.model.requests.EngineerRequestFilterDto(
                searchText = searchText,
                columnName = columnName,
                page = page,
                size = size
            )

            // Encode filter to Base64
            val filterJson = json.encodeToString(filter)
            println("📤 Filter JSON: $filterJson")

            val base64Filter = android.util.Base64.encodeToString(
                filterJson.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
            )
            println("🔐 Base64 Filter: $base64Filter")

            val endpoint = "inspection-requests/filtered/engineer?filter=$base64Filter"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Engineer Inspection API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val apiResponse: com.informatique.mtcit.data.model.requests.RequestsApiResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Parsed ${apiResponse.data?.content?.size ?: 0} engineer inspection requests")
                            println("📄 Total Elements: ${apiResponse.data?.totalElements}")
                            println("📄 Total Pages: ${apiResponse.data?.totalPages}")
                            println("=".repeat(80))

                            Result.success(apiResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: if (AppLanguage.isArabic) "فشل في جلب طلبات المعاينة" else "Failed to fetch inspection requests"
                            println("❌ API returned error: $message")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("   HTTP Code: ${response.code}")
                    println("=".repeat(80))
                    Result.failure(Exception("Failed to get engineer inspection requests: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getEngineerInspectionRequests: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to get engineer inspection requests: ${e.message}"))
        }
    }

    /**
     * Get engineer inspection request detail by ID
     * GET api/v1/scheduled-inspection-requests/{requestId}/details
     *
     * @param requestId The request ID
     * @return Result with RequestDetailResponse
     */
    suspend fun getEngineerRequestDetail(
        requestId: Int
    ): Result<com.informatique.mtcit.data.model.requests.RequestDetailResponse> {
        return try {
            println("=".repeat(80))
            println("🔍 InspectionApiService: Getting engineer request detail for ID: $requestId")
            println("=".repeat(80))

            val endpoint = "scheduled-inspection-requests/$requestId/details"
            println("📡 API Call: $endpoint")

            when (val response = repo.onGet(endpoint)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Engineer Request Detail API Response received")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        println("📊 Status Code: $statusCode")

                        if (statusCode == 200) {
                            val detailResponse: com.informatique.mtcit.data.model.requests.RequestDetailResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Parsed engineer request detail successfully")
                            println("=".repeat(80))

                            Result.success(detailResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: if (AppLanguage.isArabic) "فشل في جلب تفاصيل الطلب" else "Failed to fetch request details"
                            println("❌ API returned error: $message")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("   HTTP Code: ${response.code}")
                    println("=".repeat(80))
                    Result.failure(Exception("Failed to get engineer request detail: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in getEngineerRequestDetail: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to get engineer request detail: ${e.message}"))
        }
    }

    /**
     * Submit inspection request with documents
     * POST api/v1/inspection-requests
     * Content-Type: multipart/form-data
     *
     * Form data structure:
     * - dto: CreateInspectionRequestDto (JSON as text field)
     * - files: Array of files (binary fields)
     *
     * @param dto The inspection request DTO with metadata
     * @param files List of files to upload
     * @return Result with CreateInspectionRequestResponse
     */
    suspend fun createInspectionRequest(
        dto: CreateInspectionRequestDto,
        files: List<InspectionFileUpload>
    ): Result<CreateInspectionRequestResponse> {
        return try {
            println("=".repeat(80))
            println("🔍 InspectionApiService: Creating inspection request...")
            println("=".repeat(80))

            println("📤 Request Details:")
            println("   Ship Info ID: ${dto.shipInfoId}")
            println("   Purpose ID: ${dto.purposeId}")
            println("   Authority ID: ${dto.authorityId}")
            println("   Port ID: ${dto.placeId}")
            println("   Civil ID (crNumber): ${dto.crNumber}")
            println("   Documents Count: ${dto.documents.size}")
            println("   Files Count: ${files.size}")

            files.forEachIndexed { index, file ->
                println("   File $index: ${file.fileName} (docId=${file.documentId}, ${file.mimeType})")
            }
            println("=".repeat(80))

            val url = "inspection-requests"

            // ✅ Build DTO JSON
            val dtoJson = json.encodeToString(dto)
            println("📤 DTO JSON: $dtoJson")

            // ✅ Build multipart form data
            val formParts = mutableListOf<PartData>()

            // 1. Add "dto" field as JSON text
            formParts.add(
                PartData.FormItem(
                    value = dtoJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )
            println("✅ Added dto field: $dtoJson")

            // 2. Add files array
            files.forEach { file ->
                formParts.add(
                    PartData.BinaryItem(
                        provider = { file.fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"files\"; filename=\"${file.fileName}\""
                            )
                            append(HttpHeaders.ContentType, file.mimeType)
                        }
                    )
                )
                println("📎 Added file: ${file.fileName} (${file.fileBytes.size} bytes, docId=${file.documentId})")
            }

            // ✅ Debug: Print all form parts
            println("📤 Debug - All FormData parts:")
            formParts.forEachIndexed { index, part ->
                when (part) {
                    is PartData.FormItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        val ct = part.headers[HttpHeaders.ContentType] ?: ""
                        println("   [$index] FormItem: disposition='$cd', contentType='$ct', value='${part.value}'")
                    }
                    is PartData.BinaryItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        val ct = part.headers[HttpHeaders.ContentType] ?: ""
                        println("   [$index] BinaryItem: disposition='$cd', contentType='$ct'")
                    }
                    else -> println("   [$index] Unknown part type")
                }
            }

            println("📤 Sending ${formParts.size} form parts to $url")
            println("=".repeat(80))

            // 3. Send multipart request
            when (val response = repo.onPostMultipart(url, formParts)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject["success"]?.jsonPrimitive?.content == "true"

                        println("📊 Status Code: $statusCode, Success: $success")

                        if (statusCode == 200 || statusCode == 201) {
                            val inspectionResponse = json.decodeFromJsonElement<CreateInspectionRequestResponse>(responseJson)

                            println("✅ Inspection request created successfully!")
                            println("   Request ID: ${inspectionResponse.data}")
                            println("   Message: ${inspectionResponse.message}")
                            println("=".repeat(80))

                            Result.success(inspectionResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: if (AppLanguage.isArabic) "فشل في إنشاء طلب المعاينة" else "Failed to create inspection request"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            println("=".repeat(80))
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    val errorMsg = if (errorMessage.isNotBlank() && errorMessage != "Unknown error") {
                        errorMessage
                    } else {
                        if (AppLanguage.isArabic) "فشل في إنشاء طلب المعاينة (code: ${response.code})" else "Failed to create inspection request (code: ${response.code})"
                    }

                    println("❌ $errorMsg")
                    println("   HTTP Code: ${response.code}")
                    println("   Error Body: ${response.error}")
                    println("=".repeat(80))
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createInspectionRequest: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(Exception("Failed to create inspection request: ${e.message}"))
        }
    }
}

