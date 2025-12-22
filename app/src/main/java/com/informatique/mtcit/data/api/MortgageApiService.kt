package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.CreateMortgageRequest
import com.informatique.mtcit.data.model.CreateMortgageResponse
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.streams.asInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import com.informatique.mtcit.data.model.MortgageRequestData
import com.informatique.mtcit.data.model.MortgageShipRef
import com.informatique.mtcit.data.model.MortgageBankRef
import com.informatique.mtcit.data.model.MortgageReasonRef
import com.informatique.mtcit.data.model.MortgageStatusRef
import com.informatique.mtcit.data.model.CreateRedemptionRequest
import com.informatique.mtcit.data.model.CreateRedemptionResponse
import com.informatique.mtcit.data.model.RedemptionDocumentRef
import com.informatique.mtcit.data.model.RedemptionRequestData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * API Service for Mortgage Requests
 */
@Singleton
class MortgageApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json,
    private val marineUnitsApiService: MarineUnitsApiService
) {

    /**
     * Create a new mortgage request
     * POST api/v1/mortgage-request
     */
    suspend fun createMortgageRequest(request: CreateMortgageRequest): Result<CreateMortgageResponse> {
        return try {
            println("=".repeat(80))
            println("🚀 MortgageApiService: Creating mortgage request...")
            println("=".repeat(80))

            // Convert request to JSON string but also include alternate keys for mortgage mortgageValue
            // (some backends expect 'mortgageValue' or 'mortgage_value' instead of 'mortgageValue')
            val encodedElement = json.parseToJsonElement(json.encodeToString(request))
            val objMap = encodedElement.jsonObject.toMutableMap()
            // Ensure canonical 'value' key exists (backend expects 'value') and add alternate keys
            objMap["value"] = kotlinx.serialization.json.JsonPrimitive(request.mortgageValue)
            objMap["mortgageValue"] = kotlinx.serialization.json.JsonPrimitive(request.mortgageValue)
            objMap["mortgage_value"] = kotlinx.serialization.json.JsonPrimitive(request.mortgageValue)
            val requestBody = json.encodeToString(kotlinx.serialization.json.JsonObject(objMap))

            println("📤 Request Details:")
            println("   Ship ID: ${request.shipInfoId}")
            println("   Bank ID: ${request.bankId}")
            println("   Mortgage Reason ID: ${request.mortgageReasonId}")
            println("   Contract Number: ${request.financingContractNumber}")
            println("   Start Date: ${request.startDate}")
            println("   Mortgage Value: ${request.mortgageValue}")
            println("   Status ID: ${request.statusId}")
            println("\n📤 Full Request Body (JSON): $requestBody")
            println(requestBody)
            println("=".repeat(80))

            when (val response = repo.onPostAuthJson("api/v1/mortgage-request", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            // Parse the full response
                            val mortgageResponse: CreateMortgageResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Mortgage request created successfully!")
                            println("   Mortgage ID: ${mortgageResponse.data.id}")
                            println("   Ship ID: ${mortgageResponse.data.ship.id}")
                            println("   Bank ID: ${mortgageResponse.data.bank.id}")
                            println("   Contract Number: ${mortgageResponse.data.financingContractNumber}")
                            println("   Status ID: ${mortgageResponse.data.status.id}")

                            Result.success(mortgageResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to create mortgage request"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("   Error Code: ${response.code}")
                    println("   Error Type: ${response.error?.javaClass?.simpleName}")

                    // Try to parse the error body if it's JSON so we can show server validation messages
                    try {
                        val raw = response.error?.toString() ?: ""
                        val parsedError = if (raw.isNotBlank()) {
                            try {
                                json.parseToJsonElement(raw)
                            } catch (_: Exception) {
                                null
                            }
                        } else null

                        val detailedMessage = when {
                            parsedError != null && parsedError.jsonObject.containsKey("message") ->
                                parsedError.jsonObject["message"]?.jsonPrimitive?.content
                                    ?: parsedError.toString()

                            parsedError != null && parsedError.jsonObject.containsKey("errors") ->
                                // errors can be an object or array; print the node for inspection
                                parsedError.jsonObject["errors"].toString()

                            raw.isNotBlank() -> raw

                            else -> "Bad Request - Invalid data sent to server (check shipId, bankId, mortgageReasonId)"
                        }

                        println("❌ Detailed server error: $detailedMessage")
                        Result.failure(Exception(detailedMessage))
                    } catch (e: Exception) {
                        println("❌ Failed to parse error body: ${e.message}")
                        Result.failure(Exception("API Error: ${response.error}"))
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createMortgageRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to create mortgage request: ${e.message}"))
        }
    }

    /**
     * Submit mortgage-related documents (multipart/form-data)
     * POST api/v1/mortgage-request/{requestId}/documents
     *
     * Expects form-data with:
     * - dto: JSON (string)
     * - files: Multipart files (array)
     */
    suspend fun submitMortgageDocuments(
        requestId: Int,
        dtoJson: String,
        files: List<OwnerFileUpload>
    ): Result<CreateMortgageResponse> {
        try {
            println("🚀 MortgageApiService: Submitting mortgage documents for requestId=$requestId...")
            println("📊 Files count: ${files.size}")
            println("📤 DTO: $dtoJson")

            val formData = mutableListOf<PartData>()

            // dto part (JSON)
            formData.add(
                PartData.FormItem(
                    value = dtoJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // files parts
            files.forEach { fileUpload ->
                println("📎 Adding file: ${fileUpload.fileName} (${fileUpload.fileBytes.size} bytes)")

                formData.add(
                    PartData.BinaryItem(
                        provider = { fileUpload.fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"files\"; filename=\"${fileUpload.fileName}\""
                            )
                            append(HttpHeaders.ContentType, fileUpload.mimeType)
                        }
                    )
                )
            }

            val url = "api/v1/mortgage-request/$requestId/documents"

            // Debug: print form parts before sending
            println("📤 Debug FormData parts for submitMortgageDocuments:")
            formData.forEach { part ->
                when (part) {
                    is PartData.FormItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        println("   - FormItem header=$cd mortgageValue='${part.value}'")
                    }
                    is PartData.BinaryItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        println("   - BinaryItem header=$cd")
                    }
                    else -> println("   - Unknown part type: $part")
                }
            }

            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Mortgage documents response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        if (statusCode == 200 || statusCode == 201) {
                            val mortgageResponse: CreateMortgageResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Mortgage documents submitted successfully!")
                            return Result.success(mortgageResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to submit mortgage documents"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            return Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        return Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ MortgageApiService: Creating mortgage request WITH documents ${response.error}")
                    return Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in submitMortgageDocuments: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to submit mortgage documents: ${e.message}"))
        }
     }

    /**
     * Create a new mortgage request but also upload files in the same multipart request.
     * POST api/v1/mortgage-request (multipart/form-data with dto + files[])
     */
    suspend fun createMortgageRequestWithDocuments(
        request: CreateMortgageRequest,
        files: List<OwnerFileUpload>
    ): Result<CreateMortgageResponse> {
        return try {
            println("🚀 MortgageApiService: Creating mortgage request WITH documents...")

            // ✅ Build dto JSON with documents array
            val dtoElement = json.parseToJsonElement(json.encodeToString(CreateMortgageRequest.serializer(), request))
            val objMap = dtoElement.jsonObject.toMutableMap()

            // Add value under multiple keys for compatibility
            objMap["value"] = JsonPrimitive(request.mortgageValue)
            objMap["mortgageValue"] = JsonPrimitive(request.mortgageValue)
            objMap["mortgage_value"] = JsonPrimitive(request.mortgageValue)

            // ✅ Add documents array with fileName and documentId for each file
            if (files.isNotEmpty()) {
                val documentsArray = files.map { file ->
                    JsonObject(mapOf(
                        "fileName" to JsonPrimitive(file.fileName),
                        "documentId" to JsonPrimitive(file.docId)
                    ))
                }
                objMap["documents"] = kotlinx.serialization.json.JsonArray(documentsArray)
                println("📄 Added ${files.size} documents to dto: ${documentsArray.joinToString { "{fileName:${it.jsonObject["fileName"]}, documentId:${it.jsonObject["documentId"]}}" }}")
            }

            val dtoJson = json.encodeToString(JsonObject.serializer(), JsonObject(objMap))
            println("📤 DTO JSON: $dtoJson")

            val formData = mutableListOf<PartData>()

            // ✅ 1. Add "dto" field (REQUIRED by backend)
            formData.add(
                PartData.FormItem(
                    value = dtoJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // ✅ 2. Add files (binary uploads)
            files.forEach { fileUpload ->
                println("📎 Adding file: ${fileUpload.fileName} (${fileUpload.fileBytes.size} bytes) - documentId=${fileUpload.docId}")

                formData.add(
                    PartData.BinaryItem(
                        provider = { fileUpload.fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"${fileUpload.fileName}\"")
                            append(HttpHeaders.ContentType, fileUpload.mimeType)
                        }
                    )
                )
            }

            val url = "api/v1/mortgage-request"

            // Debug: print form parts before sending (createMortgageRequestWithDocuments)
            println("📤 Debug FormData parts for createMortgageRequestWithDocuments:")
            formData.forEach { part ->
                when (part) {
                    is PartData.FormItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        println("   - FormItem header=$cd mortgageValue='${part.value}'")
                    }
                    is PartData.BinaryItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        println("   - BinaryItem header=$cd")
                    }
                    else -> println("   - Unknown part type: $part")
                }
            }

            when (val response = repo.onPostMultipart(url, formData)) {
                 is RepoServiceState.Success -> {
                     val responseJson = response.response
                     println("✅ Mortgage create-with-docs response: $responseJson")

                     if (!responseJson.jsonObject.isEmpty()) {
                         try {
                             val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                             if (statusCode == 200 || statusCode == 201) {
                                 // Try full decode first
                                 try {
                                     val mortgageResponse: CreateMortgageResponse = json.decodeFromJsonElement(responseJson)
                                     println("✅ Mortgage created with documents successfully: id=${mortgageResponse.data}")
                                     return Result.success(mortgageResponse)
                                 } catch (decodeEx: Exception) {
                                     println("⚠️ Full decode failed: ${decodeEx.message}")
                                     // Fallback: extract minimal fields so callers can access data.id
                                     try {
                                         val obj = responseJson.jsonObject
                                         val message = obj["message"]?.jsonPrimitive?.content ?: ""
                                         val timestamp = obj["timestamp"]?.jsonPrimitive?.content ?: ""
                                         val success = try { obj["success"]?.jsonPrimitive?.content == "true" } catch (_: Exception) { false }
                                         val status = try { obj["statusCode"]?.jsonPrimitive?.int ?: 0 } catch (_: Exception) { 0 }

                                         // extract id from data node
                                         var id = 0
                                         var valueDoubleFromResponse = 0.0
                                         val dataElem = obj["data"]

                                         // helper: recursively search for numeric value in a JsonElement
                                         fun findNumericValue(element: kotlinx.serialization.json.JsonElement?, depth: Int = 0): Double {
                                             if (element == null) return 0.0
                                             // prevent too deep recursion
                                             if (depth > 5) return 0.0
                                             when (element) {
                                                 is JsonPrimitive -> {
                                                     // try to parse numeric content
                                                     val content = element.contentOrNull ?: element.content
                                                     return content.toDoubleOrNull() ?: 0.0
                                                 }
                                                 is JsonObject -> {
                                                     // common candidate keys
                                                     val candidateKeys = listOf("value", "mortgageValue", "mortgage_value", "amount", "mortgageAmount")
                                                     for (k in candidateKeys) {
                                                         element[k]?.let { v ->
                                                             val found = findNumericValue(v, depth + 1)
                                                             if (found != 0.0) return found
                                                         }
                                                     }
                                                     // search nested objects/fields
                                                     for ((_, v) in element) {
                                                         val found = findNumericValue(v, depth + 1)
                                                         if (found != 0.0) return found
                                                     }
                                                     return 0.0
                                                 }
                                                 else -> return 0.0
                                             }
                                         }

                                         if (dataElem != null) {
                                             when (dataElem) {
                                                 is JsonPrimitive -> {
                                                     val content = dataElem.content
                                                     id = content.toIntOrNull() ?: 0
                                                     // if primitive maybe it's the id only; value stays 0.0
                                                 }
                                                 is JsonObject -> {
                                                     // try direct id field
                                                     id = dataElem["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: id

                                                     // extract numeric value using helper (searches many keys and nested objects)
                                                     valueDoubleFromResponse = findNumericValue(dataElem)
                                                 }
                                                 else -> {
                                                     // fallback - leave defaults
                                                 }
                                             }
                                         }

                                         val fallbackData = MortgageRequestData(
                                             id = id,
                                             ship = MortgageShipRef(id = id),
                                             bank = MortgageBankRef(id = 0),
                                             mortgageValue = valueDoubleFromResponse,
                                             startDate = "",
                                             financingContractNumber = "",
                                             mortgageReason = MortgageReasonRef(id = 0),
                                             status = MortgageStatusRef(id = 0)
                                         )

                                         val fallback = CreateMortgageResponse(
                                             message = message,
                                             statusCode = status,
                                             success = success,
                                             timestamp = timestamp,
                                             data = fallbackData
                                         )

                                         println("✅ Returning fallback CreateMortgageResponse with id=$id")
                                         return Result.success(fallback)
                                     } catch (fallbackEx: Exception) {
                                         println("❌ Failed to build fallback response: ${fallbackEx.message}")
                                         return Result.failure(Exception("Failed to parse server response: ${fallbackEx.message}"))
                                     }
                                 }
                             } else {
                                 val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                     ?: "Failed to create mortgage request with documents"
                                 println("❌ API returned error: $message (Status: $statusCode)")
                                 return Result.failure(Exception(message))
                             }
                         } catch (e: Exception) {
                             println("❌ Error reading statusCode from response: ${e.message}")
                             return Result.failure(Exception("Invalid response from server: ${e.message}"))
                         }
                     } else {
                         println("❌ Empty response from API")
                         return Result.failure(Exception("Empty response from server"))
                     }
                 }
                 is RepoServiceState.Error -> {
                     println("❌ API Error: ${response.error}")
                     return Result.failure(Exception("API Error: ${response.error}"))
                 }
             }
        } catch (e: Exception) {
            println("❌ Exception in createMortgageRequestWithDocuments: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to create mortgage request with documents: ${e.message}"))
        }
    }


    /**
     * Update mortgage request status
     * PUT api/v1/mortgage-request/{requestId}/update-status
     *
     * @param requestId The mortgage request ID
     * @param statusId The new status ID
     * @return Result with success/failure
     */
    suspend fun updateMortgageStatus(requestId: Int, statusId: Int): Result<Boolean> {
        return marineUnitsApiService.updateTransactionStatus(
            endpoint = "api/v1/mortgage-request",
            requestId = requestId,
            statusId = statusId,
            transactionType = "Mortgage"
        )
    }

    /**
     * ✅ Send/Submit mortgage request (final submission after review)
     * PUT /api/v1/mortgage-request/{requestId}/send-request
     *
     * This is called when user clicks "Accept and Send" on review page
     *
     * @param requestId The mortgage request ID (obtained from createMortgageRequestWithDocuments)
     * @return Result with success/failure
     */
    suspend fun sendMortgageRequest(requestId: Int): Result<Boolean> {
        return try {
            println("=".repeat(80))
            println("📤 Sending Mortgage Request...")
            println("=".repeat(80))
            println("   Request ID: $requestId")
            println("   Endpoint: api/v1/mortgage-request/$requestId/send-request")

            when (val response = repo.onPutAuth("api/v1/mortgage-request/$requestId/send-request", "")) {
                is RepoServiceState.Success -> {
                    println("✅ Mortgage request sent successfully")
                    println("📥 Response: ${response.response}")
                    println("=".repeat(80))
                    Result.success(true)
                }
                is RepoServiceState.Error -> {
                    val errorMsg = "Failed to send mortgage request (code: ${response.code})"
                    println("❌ $errorMsg")
                    println("   Error: ${response.error}")
                    println("=".repeat(80))
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in sendMortgageRequest: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(e)
        }
    }

    /**
     * Create a mortgage redemption (release) request with file attachment
     * POST api/v1/mortgage-redemption-request
     * Content-Type: multipart/form-data
     *
     * @param request The redemption request data
     * @param files List of files to attach (mortgage certificate, etc.)
     * @return Result with CreateMortgageRedemptionResponse
     */
    suspend fun createMortgageRedemptionRequest(
        request: com.informatique.mtcit.data.model.CreateMortgageRedemptionRequest,
        files: List<OwnerFileUpload>
    ): Result<com.informatique.mtcit.data.model.CreateMortgageRedemptionResponse> {
        return try {
            println("=".repeat(80))
            println("🚀 MortgageApiService: Creating mortgage redemption request...")
            println("=".repeat(80))

            println("📤 Request Details:")
            println("   Ship ID: ${request.shipInfoId}")
            println("   Status ID: ${request.statusId}")
            println("   Files Count: ${files.size}")
            files.forEachIndexed { index, file ->
                println("   File $index: ${file.fileName} (${file.mimeType})")
            }
            println("=".repeat(80))

            val url = "api/v1/mortgage-redemption-request"

            // ✅ Build dto JSON (same as mortgage request)
            val dtoJson = json.encodeToString(request)
            println("📤 DTO JSON: $dtoJson")

            // Build multipart form data (EXACTLY like mortgage request)
            val formParts = mutableListOf<PartData>()

            // ✅ 1. Add "dto" field (NOT "request") - same as mortgage
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

            // ✅ 2. Add individual scalar fields (same as mortgage request does)
            try {
                formParts.add(
                    PartData.FormItem(
                        value = request.shipInfoId.toString(),
                        dispose = {},
                        partHeaders = Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"shipInfoId\"")
                        }
                    )
                )
                formParts.add(
                    PartData.FormItem(
                        value = request.statusId.toString(),
                        dispose = {},
                        partHeaders = Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"statusId\"")
                        }
                    )
                )
                println("✅ Added flat form fields: shipInfoId=${request.shipInfoId}, statusId=${request.statusId}")
            } catch (e: Exception) {
                println("⚠️ Failed to add scalar form fields: ${e.message}")
            }

            // Add files
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
                println("📎 Added file: ${file.fileName} (${file.fileBytes.size} bytes, type: ${file.mimeType})")
            }

            // ✅ Debug: Print all form parts before sending
            println("📤 Debug - All FormData parts for redemption request:")
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
                    else -> println("   [$index] Unknown part type: $part")
                }
            }

            println("📤 Sending ${formParts.size} form parts to $url")
            println("=".repeat(80))

            when (val response = repo.onPostMultipart(url, formParts)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val redemptionResponse: com.informatique.mtcit.data.model.CreateMortgageRedemptionResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Mortgage redemption request created successfully!")
                            println("   Redemption ID: ${redemptionResponse.data.id}")
                            println("   Ship ID: ${redemptionResponse.data.ship?.id ?: redemptionResponse.data.shipInfoId ?: "N/A"}")
                            println("   Status ID: ${redemptionResponse.data.status?.id ?: redemptionResponse.data.statusId ?: "N/A"}")
                            println("   Full Response Data: ${redemptionResponse.data}")
                            println("=".repeat(80))

                            Result.success(redemptionResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to create redemption request"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            println("   Full response: $responseJson")
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
                    println("❌ API Error Response:")
                    println("   Status Code: ${response.code}")
                    println("   Error Data Type: ${response.error?.javaClass?.simpleName}")
                    println("   Error Data: ${response.error}")
                    println("   Full Error: $response")

                    // Try to parse error details in multiple ways
                    val errorDetails = try {
                        when (val error = response.error) {
                            is String -> error
                            is JsonElement -> {
                                // Try to extract message from JSON
                                val errorJson = error.jsonObject
                                val message = errorJson["message"]?.jsonPrimitive?.content
                                val error_desc = errorJson["error"]?.jsonPrimitive?.content
                                val details = errorJson["details"]?.toString()

                                println("   📋 Parsed Error JSON:")
                                println("      message: $message")
                                println("      error: $error_desc")
                                println("      details: $details")

                                message ?: error_desc ?: details ?: error.toString()
                            }
                            else -> response.error?.toString() ?: "No error details"
                        }
                    } catch (e: Exception) {
                        println("   ⚠️ Failed to parse error: ${e.message}")
                        response.error?.toString() ?: "Could not parse error details"
                    }

                    println("   💡 Final Error Details: $errorDetails")
                    println("=".repeat(80))

                    val errorMsg = if (response.code == 400) {
                        "خطأ في البيانات المرسلة (400): $errorDetails"
                    } else {
                        "API Error: ${response.code} - $errorDetails"
                    }

                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createMortgageRedemptionRequest: ${e.message}")
            e.printStackTrace()
            println("=".repeat(80))
            Result.failure(e)
        }
    }

    /**
     * ✅ Create mortgage redemption request WITH documents (multipart/form-data)
     * POST /api/v1/mortgage-redemption-request
     *
     * Example request:
     * dto: {"shipInfoId":321,"documents":[{"fileName":"wallpaper.png","documentId":101}]}
     * files: wallpaper.png (binary)
     *
     * @param request The redemption request containing shipInfoId and document references
     * @param files List of files to upload (matching the documents array)
     * @return Result containing the created redemption response
     */
    suspend fun createRedemptionRequestWithDocuments(
        request: CreateRedemptionRequest,
        files: List<OwnerFileUpload>
    ): Result<CreateRedemptionResponse> {
        return try {
            println("🚀 MortgageApiService: Creating redemption request WITH documents...")

            // ✅ Build dto JSON with documents array
            val dtoElement = json.parseToJsonElement(json.encodeToString(CreateRedemptionRequest.serializer(), request))
            val objMap = dtoElement.jsonObject.toMutableMap()

            // ✅ Add documents array with fileName and documentId for each file
            if (files.isNotEmpty()) {
                val documentsArray = files.map { file ->
                    JsonObject(mapOf(
                        "fileName" to JsonPrimitive(file.fileName),
                        "documentId" to JsonPrimitive(file.docId)
                    ))
                }
//                objMap["documents"] = kotlinx.serialization.json.JsonArray(documentsArray)
//                println("📄 Added ${files.size} documents to dto: ${documentsArray.joinToString { "{fileName:${it.jsonObject["fileName"]}, documentId:${it.jsonObject["documentId"]}}" }}")
            }

            val dtoJson = json.encodeToString(JsonObject.serializer(), JsonObject(objMap))
            println("📤 DTO JSON: $dtoJson")

            val formData = mutableListOf<PartData>()

            // ✅ 1. Add "dto" field (REQUIRED by backend)
            formData.add(
                PartData.FormItem(
                    value = dtoJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // ✅ 2. Add files (binary uploads)
            files.forEach { fileUpload ->
//                println("📎 Adding file: ${fileUpload.fileName} (${fileUpload.fileBytes.size} bytes) - documentId=${fileUpload.docId}")

                formData.add(
                    PartData.BinaryItem(
                        provider = { fileUpload.fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"${fileUpload.fileName}\"")
                            append(HttpHeaders.ContentType, fileUpload.mimeType)
                        }
                    )
                )
            }

            val url = "api/v1/mortgage-redemption-request"

            // Debug: print form parts before sending
            println("📤 Debug FormData parts for createRedemptionRequestWithDocuments:")
            formData.forEach { part ->
                when (part) {
                    is PartData.FormItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
//                        println("   - FormItem header=$cd value='${part.value}'")
                    }
                    is PartData.BinaryItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
//                        println("   - BinaryItem header=$cd")
                    }
                    else ->
                        println("   - Unknown part type: $part")
                }
            }

            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Redemption create-with-docs response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        try {
                            val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                            if (statusCode == 200 || statusCode == 201) {
                                // Try full decode first
                                try {
                                    val redemptionResponse: CreateRedemptionResponse = json.decodeFromJsonElement(responseJson)
                                    println("✅ Redemption created with documents successfully: id=${redemptionResponse.data.id}")
                                    return Result.success(redemptionResponse)
                                } catch (decodeEx: Exception) {
                                    println("⚠️ Full decode failed: ${decodeEx.message}")
                                    // Fallback: extract minimal fields
                                    try {
                                        val obj = responseJson.jsonObject
                                        val message = obj["message"]?.jsonPrimitive?.content ?: ""
                                        val timestamp = obj["timestamp"]?.jsonPrimitive?.content ?: ""
                                        val success = try { obj["success"]?.jsonPrimitive?.content == "true" } catch (_: Exception) { false }
                                        val status = try { obj["statusCode"]?.jsonPrimitive?.int ?: 0 } catch (_: Exception) { 0 }

                                        // extract id from data node
                                        var id = 0
                                        val dataElem = obj["data"]

                                        if (dataElem != null) {
                                            when (dataElem) {
                                                is JsonPrimitive -> {
                                                    val content = dataElem.content
                                                    id = content.toIntOrNull() ?: 0
                                                }
                                                is JsonObject -> {
                                                    id = dataElem["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: id
                                                }
                                                else -> {
                                                    // fallback - leave defaults
                                                }
                                            }
                                        }

                                        val fallbackData = RedemptionRequestData(
                                            id = id
                                        )

                                        val fallback = CreateRedemptionResponse(
                                            message = message,
                                            statusCode = status,
                                            success = success,
                                            timestamp = timestamp,
                                            data = fallbackData
                                        )

                                        println("✅ Returning fallback CreateRedemptionResponse with id=$id")
                                        return Result.success(fallback)
                                    } catch (fallbackEx: Exception) {
                                        println("❌ Failed to build fallback response: ${fallbackEx.message}")
                                        return Result.failure(Exception("Failed to parse server response: ${fallbackEx.message}"))
                                    }
                                }
                            } else {
                                val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                    ?: "Failed to create redemption request with documents"
                                println("❌ API returned error: $message (Status: $statusCode)")
                                return Result.failure(Exception(message))
                            }
                        } catch (e: Exception) {
                            println("❌ Error reading statusCode from response: ${e.message}")
                            return Result.failure(Exception("Invalid response from server: ${e.message}"))
                        }
                    } else {
                        println("❌ Empty response from API")
                        return Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    return Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createRedemptionRequestWithDocuments: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to create redemption request with documents: ${e.message}"))
        }
    }
 }
