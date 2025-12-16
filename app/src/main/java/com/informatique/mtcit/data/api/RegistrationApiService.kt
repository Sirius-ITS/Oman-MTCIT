package com.informatique.mtcit.data.api

import com.informatique.mtcit.data.model.CreateNavigationResponse
import com.informatique.mtcit.data.model.CreateRegistrationRequest
import com.informatique.mtcit.data.model.CreateRegistrationResponse
import com.informatique.mtcit.data.model.DocumentValidationResponse
import com.informatique.mtcit.data.model.EngineFileUpload
import com.informatique.mtcit.data.model.EngineSubmissionRequest
import com.informatique.mtcit.data.model.EngineSubmissionResponse
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.data.model.OwnerSubmissionRequest
import com.informatique.mtcit.data.model.OwnerSubmissionResponse
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.UpdateWeightsRequest
import com.informatique.mtcit.data.model.cancelRegistration.DeletionFileUpload
import com.informatique.mtcit.data.model.cancelRegistration.DeletionReasonResponse
import com.informatique.mtcit.data.model.cancelRegistration.DeletionSubmitResponse
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.streams.asInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.boolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Service for Ship Registration Requests
 */
@Singleton
class RegistrationApiService @Inject constructor(
    private val repo: AppRepository,
    private val json: Json
) {

    /**
     * Create a new registration request
     * POST api/v1/registration-requests
     */
    suspend fun createRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        return try {
            println("🚀 RegistrationApiService: Creating registration request...")
            println("📤 Request Body: ${json.encodeToString(request)}")

            // Convert request to JSON string
            val requestBody = json.encodeToString(request)

            when (val response = repo.onPostAuth("api/v1/registration-requests", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            // Parse the full response
                            val registrationResponse: CreateRegistrationResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Registration request created successfully!")
                            println("   Request ID: ${registrationResponse.data.id}")
                            println("   Ship Info ID: ${registrationResponse.data.shipInfo?.id}")
                            println("   Ship ID: ${registrationResponse.data.shipInfo?.ship?.id}")
                            println("   Request Serial: ${registrationResponse.data.requestSerial}")
                            println("   Request Year: ${registrationResponse.data.requestYear}")
                            println("   Status: ${registrationResponse.data.status?.nameEn}")

                            Result.success(registrationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to create registration request"
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
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in createRegistrationRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to create registration request: ${e.message}"))
        }
    }

    /**
     * Update an existing registration request
     * PUT api/v1/registration-requests/update
     *
     * Used when user goes back and changes unit selection data
     */
    suspend fun updateRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        return try {
            println("🚀 RegistrationApiService: Updating registration request...")
            println("📤 Request Body: ${json.encodeToString(request)}")

            // Convert request to JSON string
            val requestBody = json.encodeToString(request)

            when (val response = repo.onPutAuth("api/v1/registration-requests/update", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            // Parse the full response
                            val registrationResponse: CreateRegistrationResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Registration request updated successfully!")
                            println("   Request ID: ${registrationResponse.data.id}")

                            Result.success(registrationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Unknown error"
                            println("❌ API Error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(Exception("Empty response"))
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception(response.error.toString()))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateRegistrationRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship dimensions
     * PUT api/v1/registration-requests/{requestId}/dimensions
     */
    suspend fun updateDimensions(requestId: String, dimensionsData: UpdateDimensionsRequest): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating dimensions for requestId=$requestId...")
            println("📤 Request Body: ${json.encodeToString(dimensionsData)}")

            val requestBody = json.encodeToString(dimensionsData)

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/dimensions", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Dimensions API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Dimensions updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update dimensions"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateDimensions: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship weights
     * PUT api/v1/registration-requests/{requestId}/weights
     */
    suspend fun updateWeights(requestId: String, weightsData: UpdateWeightsRequest): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating weights for requestId=$requestId...")
            println("📤 Request Body: ${json.encodeToString(weightsData)}")

            val requestBody = json.encodeToString(weightsData)

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/weights", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Weights API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Weights updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update weights"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateWeights: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship engines
     * PUT api/v1/registration-requests/{requestId}/engines
     */
    suspend fun updateEngines(requestId: String, enginesJson: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Submitting engines for requestId=$requestId...")
            println("📤 Request Body: $enginesJson")

            // ✅ FIXED: Changed from onPutAuth to onPostAuth because backend only supports POST
            when (val response = repo.onPostAuth("api/v1/registration-requests/$requestId/engines", enginesJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Engines API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Engines submitted successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to submit engines"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateEngines: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship owners
     * PUT api/v1/registration-requests/{requestId}/owners
     */
    suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating owners for requestId=$requestId...")
            println("📤 Request Body: $ownersJson")

            when (val response = repo.onPutAuth("api/v1/registration-requests/$requestId/owners", ownersJson)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Owners API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Owners updated successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update owners"
                            println("❌ API returned error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in updateOwners: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Submit engines with documents (multipart/form-data)
     * POST api/v1/registration-requests/{requestId}/engines
     *
     * This API consumes form-data with:
     * - dto: JSON array of engines
     * - files: Multipart files (mapped by docOwnerId)
     */
    suspend fun submitEngines(
        requestId: Int,
        engines: List<EngineSubmissionRequest>,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse> {
        // use context to avoid unused-parameter warning
        return try {
            println("🚀 RegistrationApiService: Submitting engines for requestId=$requestId...")
            println("📊 Engines count: ${engines.size}")
            println("📎 Files count: ${files.size}")

            // Build multipart form data
            val formData = mutableListOf<PartData>()

            // 1. Add DTO part (JSON array of engines)
            val enginesJson = json.encodeToString(engines)
            println("📤 Engines DTO: $enginesJson")

            formData.add(
                PartData.FormItem(
                    value = enginesJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // 2. Add file parts
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

            // 3. Send the multipart request
            val url = "api/v1/registration-requests/$requestId/engines"
            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Engines submission response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val engineResponse: EngineSubmissionResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Engines submitted successfully!")
                            println("   Engines created: ${engineResponse.data?.size ?: 0}")

                            Result.success(engineResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to submit engines"
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
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in submitEngines: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to submit engines: ${e.message}"))
        }
    }

    /**
     * Submit owners with documents (multipart/form-data)
     * POST api/v1/registration-requests/{requestId}/owners
     */
    suspend fun submitOwners(
        requestId: Int,
        owners: List<OwnerSubmissionRequest>,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse> {
        // use context to avoid unused-parameter warning
        try {
            println("🚀 RegistrationApiService: Submitting owners for requestId=$requestId...")
            println("📊 Owners count: ${owners.size}")
            println("📎 Files count: ${files.size}")

            val formData = mutableListOf<PartData>()
            val ownersJson = json.encodeToString(owners)
            println("📤 Owners DTO: $ownersJson")

            formData.add(
                PartData.FormItem(
                    value = ownersJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            files.forEach { fileUpload ->
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

            val url = "api/v1/registration-requests/$requestId/owners"
            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Owners submission response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        if (statusCode == 200 || statusCode == 201) {
                            val ownerResponse: OwnerSubmissionResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Owners submitted successfully!")
                            return Result.success(ownerResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content ?: "Failed to submit owners"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            return Result.failure(Exception(message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        return Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    return Result.failure(Exception(response.error?.toString() ?: "API error"))
                }
            }

            return Result.failure(Exception("Unknown error submitting owners"))
        } catch (e: Exception) {
            println("❌ Exception in submitOwners: ${e.message}")
            e.printStackTrace()
            return Result.failure(Exception("Failed to submit owners: ${e.message}"))
        }
    }

    /**
     * Validate build status documents
     */
    suspend fun validateBuildStatus(
        requestId: Int,
        shipbuildingCertificateFile: ByteArray?,
        shipbuildingCertificateName: String?,
        inspectionDocumentsFile: ByteArray?,
        inspectionDocumentsName: String?
    ): Result<DocumentValidationResponse> {
        // avoid unused-parameter warnings in implementations that require context in other flows
        return try {
            println("🚀 RegistrationApiService: Validating build status for requestId=$requestId...")
            // Build multipart and send, similar pattern as other multipart endpoints
            // For brevity reuse existing implementation if present elsewhere; here we call repo.onPostMultipart

            val formData = mutableListOf<PartData>()
            if (shipbuildingCertificateFile != null && shipbuildingCertificateName != null) {
                formData.add(
                    PartData.BinaryItem(
                        provider = { shipbuildingCertificateFile.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"files\"; filename=\"$shipbuildingCertificateName\"")
                            append(HttpHeaders.ContentType, "application/pdf")
                        }
                    )
                )
            }

            // Add inspection documents if provided (as 'files' array element)
            if (inspectionDocumentsFile != null && inspectionDocumentsName != null) {
                println("📎 Adding inspection documents: $inspectionDocumentsName (${inspectionDocumentsFile.size} bytes)")

                formData.add(
                    PartData.BinaryItem(
                        provider = { inspectionDocumentsFile.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"files\"; filename=\"$inspectionDocumentsName\""
                            )
                            append(HttpHeaders.ContentType, "application/octet-stream")
                        }
                    )
                )
            }

            val url = "api/v1/registration-requests/$requestId/validate-build-status"
            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        if (statusCode == 200 || statusCode == 201) {
                            val validationResponse: DocumentValidationResponse = json.decodeFromJsonElement(responseJson)
                            return Result.success(validationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content ?: "Failed to validate documents"
                            return Result.failure(Exception(message))
                        }
                    } else {
                        return Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> return Result.failure(Exception(response.error?.toString() ?: "API error"))
            }

            return Result.failure(Exception("Unknown error validating documents"))
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(Exception("Failed to validate build status: ${e.message}"))
        }
    }

    /**
     * Create navigation license request for a selected ship
     * POST api/v1/ship-navigation-license-request
     * Body: { "shipInfo": <shipInfoId> }
     */
    suspend fun createNavigationLicense(shipInfoId: Int): Result<CreateNavigationResponse> {
        return try {
            val requestBody = json.encodeToString(mapOf("shipInfo" to shipInfoId))
            println("🚀 RegistrationApiService: Creating navigation license - body: $requestBody")

            when (val response = repo.onPostAuth("api/v1/ship-navigation-license-request", requestBody)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        if (statusCode == 200 || statusCode == 201) {
                            val createResponse: CreateNavigationResponse = json.decodeFromJsonElement<CreateNavigationResponse>(responseJson)
                            println("✅ Navigation license created: id=${createResponse.data?.id}")
                            return Result.success(createResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to create navigation license"
                            println("⚠️ Navigation license creation returned status $statusCode: $message")

                            // ✅ Include status code in error message for 406 handling
                            val errorWithCode = "ERROR_CODE:$statusCode|$message"

                            // If API says ship info not found, try alternative payload shapes
                            if (message.contains("Ship info not found", ignoreCase = true)) {
                                println("🔁 Attempting alternate payloads for shipInfoId=$shipInfoId")

                                // 1) Try shipInfoId key
                                val alt1 = json.encodeToString(mapOf("shipInfoId" to shipInfoId))
                                println("📤 Trying payload: $alt1")
                                when (val r2 = repo.onPostAuth("api/v1/ship-navigation-license-request", alt1)) {
                                    is RepoServiceState.Success -> {
                                        val r2json = r2.response
                                        println("📥 Response for alt1: $r2json")
                                        if (!r2json.jsonObject.isEmpty()) {
                                            val sc2 = r2json.jsonObject.getValue("statusCode").jsonPrimitive.int
                                            if (sc2 == 200 || sc2 == 201) {
                                                val resp2: CreateNavigationResponse = json.decodeFromJsonElement(r2json)
                                                println("✅ Navigation license created with alt1: id=${resp2.data?.id}")
                                                return Result.success(resp2)
                                            }
                                        }
                                    }
                                    is RepoServiceState.Error -> println("❌ alt1 API error: ${r2.error}")
                                }

                                // 2) Try nested object {"shipInfo": {"id": <id>}}
                                val alt2map = mapOf("shipInfo" to mapOf("id" to shipInfoId))
                                val alt2 = json.encodeToString(alt2map)
                                println("📤 Trying payload: $alt2")
                                when (val r3 = repo.onPostAuth("api/v1/ship-navigation-license-request", alt2)) {
                                    is RepoServiceState.Success -> {
                                        val r3json = r3.response
                                        println("📥 Response for alt2: $r3json")
                                        if (!r3json.jsonObject.isEmpty()) {
                                            val sc3 = r3json.jsonObject.getValue("statusCode").jsonPrimitive.int
                                            if (sc3 == 200 || sc3 == 201) {
                                                val resp3: CreateNavigationResponse = json.decodeFromJsonElement(r3json)
                                                println("✅ Navigation license created with alt2: id=${resp3.data?.id}")
                                                return Result.success(resp3)
                                            }
                                        }
                                    }
                                    is RepoServiceState.Error -> println("❌ alt2 API error: ${r3.error}")
                                }
                            }

                            return Result.failure(Exception(errorWithCode))
                        }
                    } else {
                        return Result.failure(Exception("Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    // ✅ Parse the error response to extract the message field
                    val statusCode = response.code
                    val errorBody = response.error?.toString() ?: "API error"

                    println("⚠️ HTTP $statusCode - Response body: $errorBody")

                    // Try to parse JSON error response to extract "message" field
                    val errorMessage = try {
                        val errorJson = json.parseToJsonElement(errorBody).jsonObject
                        errorJson["message"]?.jsonPrimitive?.content ?: errorBody
                    } catch (e: Exception) {
                        errorBody
                    }

                    // Format with ERROR_CODE prefix for consistent handling
                    val formattedError = "ERROR_CODE:$statusCode|$errorMessage"
                    println("❌ Failed to create navigation license: $formattedError")

                    return Result.failure(Exception(formattedError))
                }
            }

            return Result.failure(Exception("Unknown error creating navigation license"))
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(Exception("Failed to create navigation license: ${e.message}"))
        }
    }

    /**
     * Send registration request and check if inspection is needed
     * POST api/v1/registration-requests/{request-id}/send-request
     */
    suspend fun sendRequest(requestId: Int): Result<com.informatique.mtcit.data.model.SendRequestResponse> {
        return try {
            println("🚀 RegistrationApiService: Sending request for requestId=$requestId...")

            when (val response = repo.onPostAuth("api/v1/registration-requests/$requestId/send-request", "")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Send Request API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            // Parse the full response
                            val sendRequestResponse: com.informatique.mtcit.data.model.SendRequestResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Send request successful!")
                            println("   Message: ${sendRequestResponse.data.message}")
                            println("   Need Inspection: ${sendRequestResponse.data.needInspection}")

                            Result.success(sendRequestResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to send request"
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
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in sendRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to send request: ${e.message}"))
        }
    }

    /**
     * Reserve ship/marine name
     * POST api/v1/registration-requests/{id}/{name}/shipNameReservtion
     */
    suspend fun shipNameReservation(requestId: Int, marineName: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Reserving marine name for requestId=$requestId...")
            println("📤 Marine Name: $marineName")

            val url = "api/v1/registration-requests/$requestId/$marineName/shipNameReservation"
            when (val response = repo.onPostAuth(url, "")) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Ship Name Reservation API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            println("✅ Marine name reserved successfully!")
                            Result.success(Unit)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to reserve marine name"
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
                    Result.failure(Exception("API Error: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in shipNameReservation: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to reserve marine name: ${e.message}"))
        }
    }


    /**
     * Get deletion reasons
     * GET api/v1/deletionmdreason
     */
    suspend fun getDeletionReasons(): Result<DeletionReasonResponse> {
        return try {
            val apiUrl = "api/v1/deletionmdreason" // Replace with your actual endpoint

            when (val response = repo.onGet(apiUrl)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean

                        if (statusCode == 200 && success) {
                            val deletionReasonResponse: DeletionReasonResponse =
                                json.decodeFromJsonElement(responseJson.jsonObject)
                            Result.success(deletionReasonResponse)
                        } else {
                            val message = responseJson.jsonObject.getValue("message").jsonPrimitive.content
                            Result.failure(Exception("Service failed: $message"))
                        }
                    } else {
                        Result.failure(Exception("Empty deletion reasons response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(Exception("Failed to get deletion reasons: ${response.error}"))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to get deletion reasons: ${e.message}"))
        }
    }

    /**
     * submit deletion request with files (multipart/form-data)
     * POST api/v1/deletion-requests
     *
     * This API consumes form-data with:
     * - dto: JSON object with deletionReasonId and shipInfoId
     * - files: Multipart files
     */
    suspend fun submitDeletionRequest(
        deletionReasonId: Int,
        shipInfoId: Int,
        files: List<DeletionFileUpload>
    ): Result<DeletionSubmitResponse> {
        return try {
            println("📤 submitDeletionRequest called")
            println("📤 reasonId=$deletionReasonId, shipId=$shipInfoId, files=${files.size}")

            val formData = mutableListOf<PartData>()

            // 1. Add DTO as FormItem (NOT BinaryItem) ✅
            val dtoJson = """{"deletionReason":{"id":$deletionReasonId}, "shipInfo":{"id":$shipInfoId}}"""
            println("📤 DTO JSON: $dtoJson")

            formData.add(
                PartData.BinaryItem(
                    provider = { dtoJson.toByteArray().inputStream().asInput() },
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // 2. Add files (same as submitEngines) ✅
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

            println("📤 Total parts: ${formData.size}")

            val apiUrl = "api/v1/deletion-requests"

            when (val response = repo.onPostMultipart(apiUrl, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Success: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int
                        val success = responseJson.jsonObject.getValue("success").jsonPrimitive.boolean

                        if (statusCode == 200 && success) {
                            val submitResponse: DeletionSubmitResponse =
                                json.decodeFromJsonElement(responseJson.jsonObject)
                            println("✅ Parsed response: ${submitResponse.data?.id}")
                            Result.success(submitResponse)
                        } else {
                            val message = responseJson.jsonObject.getValue("message").jsonPrimitive.content
                            println("❌ API Error: $message")
                            Result.failure(Exception(message))
                        }
                    } else {
                        Result.failure(Exception("Empty response"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ Error: ${response.error}")
                    Result.failure(Exception("Failed to submit: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to submit deletion request: ${e.message}"))
        }
    }

}
