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
            println("   ID: ${dto.id}")
            println("   Ship Info ID: ${dto.shipInfoId}")
            println("   Purpose ID: ${dto.purposeId}")
            println("   Authority ID: ${dto.authorityId}")
            println("   Port ID: ${dto.portId}")
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
                            println("   Request ID: ${inspectionResponse.data.id}")
                            println("   Request Serial: ${inspectionResponse.data.requestSerial}")
                            println("   Request Year: ${inspectionResponse.data.requestYear}")
                            println("   Message: ${inspectionResponse.message}")
                            println("=".repeat(80))

                            Result.success(inspectionResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "فشل في إنشاء طلب المعاينة"
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
                        "فشل في إنشاء طلب المعاينة (code: ${response.code})"
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

