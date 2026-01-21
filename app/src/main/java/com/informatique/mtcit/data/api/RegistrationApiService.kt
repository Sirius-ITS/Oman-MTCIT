package com.informatique.mtcit.data.api

import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.ErrorMessageExtractor
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
import com.informatique.mtcit.data.model.cancelRegistration.DeletionRequestData
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

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
     * POST registration-requests
     */
    suspend fun createRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        return try {
            println("🚀 RegistrationApiService: Creating registration request...")
            println("📤 Request Body: ${json.encodeToString(request)}")

            // Convert request to JSON string
            val requestBody = json.encodeToString(request)

            when (val response = repo.onPostAuth("registration-requests", requestBody)) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    println("📝 Extracted error message: $errorMessage")
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e // Re-throw ApiException
        } catch (e: Exception) {
            println("❌ Exception in createRegistrationRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to create registration request: ${e.message}"))
        }
    }

    /**
     * Update an existing registration request
     * PUT registration-requests/update
     *
     * Used when user goes back and changes unit selection data
     */
    suspend fun updateRegistrationRequest(request: CreateRegistrationRequest): Result<CreateRegistrationResponse> {
        return try {
            println("🚀 RegistrationApiService: Updating registration request...")
            println("📤 Request Body: ${json.encodeToString(request)}")

            // Convert request to JSON string
            val requestBody = json.encodeToString(request)

            when (val response = repo.onPutAuth("registration-requests/update", requestBody)) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response"))
                    }
                }

                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    println("📝 Extracted error message: $errorMessage")
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateRegistrationRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship dimensions
     * PUT registration-requests/{requestId}/dimensions
     */
    suspend fun updateDimensions(requestId: String, dimensionsData: UpdateDimensionsRequest): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating dimensions for requestId=$requestId...")
            println("📤 Request Body: ${json.encodeToString(dimensionsData)}")

            val requestBody = json.encodeToString(dimensionsData)

            when (val response = repo.onPutAuth("registration-requests/$requestId/dimensions", requestBody)) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateDimensions: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship weights
     * PUT registration-requests/{requestId}/weights
     */
    suspend fun updateWeights(requestId: String, weightsData: UpdateWeightsRequest): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating weights for requestId=$requestId...")
            println("📤 Request Body: ${json.encodeToString(weightsData)}")

            val requestBody = json.encodeToString(weightsData)

            when (val response = repo.onPutAuth("registration-requests/$requestId/weights", requestBody)) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateWeights: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship engines
     * PUT registration-requests/{requestId}/engines
     */
    suspend fun updateEngines(requestId: String, enginesJson: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Submitting engines for requestId=$requestId...")
            println("📤 Request Body: $enginesJson")

            // ✅ FIXED: Changed from onPutAuth to onPostAuth because backend only supports POST
            when (val response = repo.onPostAuth("registration-requests/$requestId/engines", enginesJson)) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateEngines: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Update ship owners
     * PUT registration-requests/{requestId}/owners
     */
    suspend fun updateOwners(requestId: String, ownersJson: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Updating owners for requestId=$requestId...")
            println("📤 Request Body: $ownersJson")

            when (val response = repo.onPutAuth("registration-requests/$requestId/owners", ownersJson)) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateOwners: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Submit engines with documents (multipart/form-data)
     * POST registration-requests/{requestId}/engines
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

            // ✅ Log each engine's id before serialization
            engines.forEachIndexed { index, engine ->
                println("📋 Engine $index: id=${engine.id}, serialNumber=${engine.engineSerialNumber}")
            }

            // Build multipart form data
            val formData = mutableListOf<PartData>()

            // 1. Add DTO part (JSON array of engines)
            // Use same encoder as owners to ensure consistency
            val jsonEncoder = Json {
                explicitNulls = false // ✅ Omit null fields from JSON
                encodeDefaults = false // ✅ Don't encode default values
                ignoreUnknownKeys = true
            }
            val enginesJson = jsonEncoder.encodeToString(engines)
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
            val url = "registration-requests/$requestId/engines"
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in submitEngines: ${e.message}")
            e.printStackTrace()
            return Result.failure(ApiException(500, "Failed to submit engines: ${e.message}"))
        }
    }

    /**
     * Submit owners with documents (multipart/form-data)
     * POST registration-requests/{requestId}/owners
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

            // ✅ Log each owner's id before serialization
            owners.forEachIndexed { index, owner ->
                println("📋 Owner $index: id=${owner.id}, name=${owner.ownerName}, percentage=${owner.ownershipPercentage}")
            }

            val formData = mutableListOf<PartData>()

            // Create JSON encoder that excludes null values but includes all non-null fields
            val jsonEncoder = Json {
                explicitNulls = false // ✅ Omit null fields from JSON
                encodeDefaults = false // ✅ Don't encode default values (id=null won't be encoded)
                ignoreUnknownKeys = true
            }

            val ownersJson = jsonEncoder.encodeToString(owners)
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

            val url = "registration-requests/$requestId/owners"
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
                            return Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        return Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    return Result.failure(ApiException(response.code, errorMessage))
                }
            }

            return Result.failure(ApiException(500, "Unknown error submitting owners"))
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in submitOwners: ${e.message}")
            e.printStackTrace()
            return Result.failure(ApiException(500, "Failed to submit owners: ${e.message}"))
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

            val url = "registration-requests/$requestId/validate-build-status"
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
                            return Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        return Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> return Result.failure(ApiException(response.code, response.error?.toString() ?: "API error"))
            }

            return Result.failure(ApiException(500, "Unknown error validating documents"))
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(ApiException(500, "Failed to validate build status: ${e.message}"))
        }
    }

    /**
     * Create navigation license request for a selected ship
     * POST ship-navigation-license-request
     * Body: { "shipInfo": <shipInfoId> }
     */
    suspend fun createNavigationLicense(shipInfoId: Int): Result<CreateNavigationResponse> {
        return try {
            val requestBody = json.encodeToString(mapOf("shipInfo" to shipInfoId))
            println("🚀 RegistrationApiService: Creating navigation license - body: $requestBody")

            when (val response = repo.onPostAuth("ship-navigation-license-request", requestBody)) {
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
                                when (val r2 = repo.onPostAuth("ship-navigation-license-request", alt1)) {
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
                                when (val r3 = repo.onPostAuth("ship-navigation-license-request", alt2)) {
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
     * POST registration-requests/{request-id}/send-request
     */
    suspend fun sendRequest(requestId: Int): Result<com.informatique.mtcit.data.model.SendRequestResponse> {
        return try {
            println("🚀 RegistrationApiService: Sending request for requestId=$requestId...")

            when (val response = repo.onPostAuth("registration-requests/$requestId/send-request", "")) {
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(ApiException(response.code, response.error.toString()))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in sendRequest: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to send request: ${e.message}"))
        }
    }

    /**
     * Reserve ship/marine name
     * POST registration-requests/{id}/{name}/shipNameReservtion
     */
    suspend fun shipNameReservation(requestId: Int, marineName: String): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Reserving marine name for requestId=$requestId...")
            println("📤 Marine Name: $marineName")

            val url = "registration-requests/$requestId/$marineName/shipNameReservation"
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
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    Result.failure(ApiException(response.code, response.error.toString()))
                }
            }
        } catch (e: Exception) {
            println("❌ Exception in shipNameReservation: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to reserve marine name: ${e.message}"))
        }
    }


    /**
     * Get deletion reasons
     * GET deletionmdreason
     */
    suspend fun getDeletionReasons(): Result<DeletionReasonResponse> {
        return try {
            val apiUrl = "deletionmdreason" // Replace with your actual endpoint

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
                            Result.failure(ApiException(statusCode, "Service failed: $message"))
                        }
                    } else {
                        Result.failure(ApiException(500, "Empty deletion reasons response"))
                    }
                }

                is RepoServiceState.Error -> {
                    Result.failure(ApiException(response.code, "Failed to get deletion reasons: ${response.error}"))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to get deletion reasons: ${e.message}"))
        }
    }

    /**
     * submit deletion request with files (multipart/form-data)
     * POST deletion-requests
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
            println("📤 ========== submitDeletionRequest ==========")
            println("📤 reasonId=$deletionReasonId, shipId=$shipInfoId, files=${files.size}")

            // ✅ Build DTO JSON with documents array (same pattern as Mortgage)
            val documentsArray = if (files.isNotEmpty()) {
                files.joinToString(prefix = "[", postfix = "]", separator = ",") { file ->
                    """{"fileName":"${file.fileName}","documentId":${file.docId}}"""
                }
            } else {
                "[]"
            }

            val dtoJson = """{
            "deletionReason": {"id": $deletionReasonId},
            "shipInfo": {"id": $shipInfoId},
            "documents": $documentsArray
        }""".trimIndent()

            println("📤 DTO JSON: $dtoJson")
            println("📄 Documents array contains ${files.size} items:")
            files.forEach { file ->
                println("   - fileName: ${file.fileName}, documentId: ${file.docId}")
            }

            val formData = mutableListOf<PartData>()

            // ✅ 1. Add "dto" field as BinaryItem with JSON content
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

            // ✅ 2. Add files (binary uploads)
            files.forEach { fileUpload ->
                println("📎 Adding file: ${fileUpload.fileName} (${fileUpload.fileBytes.size} bytes) - documentId=${fileUpload.docId}")

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

            println("📤 Total parts: ${formData.size} (1 dto + ${files.size} files)")

            // Debug: print form parts before sending
            println("📤 Debug FormData parts:")
            formData.forEachIndexed { index, part ->
                when (part) {
                    is PartData.FormItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        println("   Part #$index - FormItem: $cd")
                    }
                    is PartData.BinaryItem -> {
                        val cd = part.headers[HttpHeaders.ContentDisposition] ?: ""
                        val ct = part.headers[HttpHeaders.ContentType] ?: ""
                        println("   Part #$index - BinaryItem: $cd, ContentType: $ct")
                    }
                    else -> println("   Part #$index - Unknown type")
                }
            }

            val apiUrl = "deletion-requests"

            when (val response = repo.onPostMultipart(apiUrl, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Success response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        try {
                            val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                            if (statusCode == 200 || statusCode == 201) {
                                // ✅ Try full decode first
                                try {
                                    val submitResponse: DeletionSubmitResponse =
                                        json.decodeFromJsonElement(responseJson)
                                    println("✅ Deletion request created successfully: id=${submitResponse.data?.id}")
                                    return Result.success(submitResponse)
                                } catch (decodeEx: Exception) {
                                    println("⚠️ Full decode failed: ${decodeEx.message}")

                                    // ✅ Fallback: extract minimal fields (same pattern as Mortgage)
                                    try {
                                        val obj = responseJson.jsonObject
                                        val message = obj["message"]?.jsonPrimitive?.content ?: ""
                                        val timestamp = obj["timestamp"]?.jsonPrimitive?.content ?: ""
                                        val success = obj["success"]?.jsonPrimitive?.booleanOrNull ?: false
                                        val status = obj["statusCode"]?.jsonPrimitive?.intOrNull ?: 0

                                        // Extract ID from data node
                                        var id = 0
                                        val dataElem = obj["data"]

                                        if (dataElem != null) {
                                            when (dataElem) {
                                                is JsonPrimitive -> {
                                                    id = dataElem.content.toIntOrNull() ?: 0
                                                }
                                                is JsonObject -> {
                                                    id = dataElem["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                                                }
                                                else -> {
                                                    println("⚠️ Unexpected data type in response")
                                                }
                                            }
                                        }

                                        println("✅ Extracted deletion request ID: $id")

                                        // Build fallback response
                                        val fallbackData = DeletionRequestData(
                                            id = id,
                                            deletionReasonId = deletionReasonId,
                                            shipInfoId = shipInfoId,
                                            statusId = 1,
                                            createdAt = timestamp,
                                            updatedAt = null
                                        )

                                        val fallbackResponse = DeletionSubmitResponse(
                                            message = message,
                                            statusCode = status,
                                            success = success,
                                            timestamp = timestamp,
                                            data = fallbackData
                                        )

                                        println("✅ Returning fallback DeletionSubmitResponse with id=$id")
                                        return Result.success(fallbackResponse)

                                    } catch (fallbackEx: Exception) {
                                        println("❌ Failed to build fallback response: ${fallbackEx.message}")
                                        return Result.failure(ApiException(500, "Failed to parse server response: ${fallbackEx.message}"))
                                    }
                                }
                            } else {
                                val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                    ?: "Failed to create deletion request"
                                println("❌ API returned error: $message (Status: $statusCode)")
                                return Result.failure(ApiException(statusCode, message))
                            }
                        } catch (e: Exception) {
                            println("❌ Error reading statusCode from response: ${e.message}")
                            return Result.failure(ApiException(500, "Invalid response from server: ${e.message}"))
                        }
                    } else {
                        println("❌ Empty response from API")
                        return Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    return Result.failure(ApiException(response.code, "API Error: ${response.error}"))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in submitDeletionRequest: ${e.message}")
            e.printStackTrace()
            return Result.failure(ApiException(500, "Failed to submit deletion request: ${e.message}"))
        }
    }

    /**
     * Validate build status with dynamic documents
     * POST registration-requests/{requestId}/validate-build-status
     *
     * Similar to Release Mortgage redemption request, but ONLY files (no other data)
     *
     * @param requestId The registration request ID
     * @param documents List of document file uploads with documentId mapping
     * @return Result with DocumentValidationResponse
     */
    suspend fun validateBuildStatusWithDocuments(
        requestId: Int,
        documents: List<com.informatique.mtcit.data.model.DocumentFileUpload>
    ): Result<DocumentValidationResponse> {
        return try {
            println("=".repeat(80))
            println("🚀 RegistrationApiService: Validating build status with dynamic documents...")
            println("=".repeat(80))

            println("📤 Request Details:")
            println("   Request ID: $requestId")
            println("   Documents Count: ${documents.size}")
            documents.forEachIndexed { index, doc ->
                println("   Document $index: ${doc.fileName} (documentId=${doc.documentId}, ${doc.mimeType})")
            }
            println("=".repeat(80))

            val url = "registration-requests/$requestId/validate-build-status"

            // Build multipart form data
            val formParts = mutableListOf<PartData>()

            // ✅ 1. Build documents DTO array
            val documentsDto = documents.map { doc ->
                com.informatique.mtcit.data.model.DocumentMetadata(
                    fileName = doc.fileName,
                    documentId = doc.documentId
                )
            }

            val dtoWrapper = com.informatique.mtcit.data.model.DocumentValidationRequestDto(
                documents = documentsDto
            )

            val dtoJson = json.encodeToString(dtoWrapper)
            println("📤 DTO JSON: $dtoJson")

            // Add "dto" field
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

            // ✅ 2. Add files (matching the DTO structure)
            documents.forEach { doc ->
                formParts.add(
                    PartData.BinaryItem(
                        provider = { doc.fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"files\"; filename=\"${doc.fileName}\""
                            )
                            append(HttpHeaders.ContentType, doc.mimeType)
                        }
                    )
                )
                println("📎 Added file: ${doc.fileName} (${doc.fileBytes.size} bytes, documentId=${doc.documentId})")
            }

            // Debug: Print all form parts
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
                            val validationResponse: DocumentValidationResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Documents validated successfully!")
                            println("   Uploaded files: ${validationResponse.data?.uploadedFiles?.size ?: 0}")
                            println("=".repeat(80))

                            Result.success(validationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to validate documents"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            println("   Full response: $responseJson")
                            println("=".repeat(80))
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error Response:")
                    val errorMsg = if (response.code == 400) {
                        "خطأ في البيانات المرسلة (400)"
                    } else {
                        "API Error: ${response.code}"
                    }
                    Result.failure(ApiException(response.code, errorMsg))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in validateBuildStatusWithDocuments: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, e.message ?: "Failed to validate documents"))
        }
    }

    /**
     * Validate Insurance Document for Permanent Registration
     * POST /api/v1/perm-registration-requests/validate-insurance-document
     *
     * Sends insurance document with DTO + file (multipart)
     * - If countryId = "OM": insuranceCompanyId is required (selected from dropdown)
     * - If countryId != "OM": insuranceCompanyName is required (text field input)
     */
    suspend fun validateInsuranceDocument(
        insuranceDto: com.informatique.mtcit.data.model.InsuranceDocumentRequestDto,
        file: com.informatique.mtcit.data.model.DocumentFileUpload
    ): Result<com.informatique.mtcit.data.model.InsuranceDocumentResponse> {
        return try {
            println("=".repeat(80))
            println("🚀 RegistrationApiService: Validating insurance document...")
            println("=".repeat(80))

            println("📤 Insurance Document Request Details:")
            println("   Ship Info ID: ${insuranceDto.shipInfoId}")
            println("   Insurance Number: ${insuranceDto.insuranceNumber}")
            println("   Country ID: ${insuranceDto.countryId}")
            println("   Insurance Company ID: ${insuranceDto.insuranceCompanyId}")
            println("   Insurance Company Name: ${insuranceDto.insuranceCompanyName}")
            println("   Insurance Expiry Date: ${insuranceDto.insuranceExpiryDate}")
            println("   CR Number: ${insuranceDto.crNumber}")
            println("   File: ${file.fileName} (${file.fileBytes.size} bytes)")
            println("=".repeat(80))

            val url = "perm-registration-requests/validate-insurance-document"

            // Build multipart form data
            val formParts = mutableListOf<PartData>()

            // ✅ 1. Add DTO as JSON
            val dtoJson = json.encodeToString(insuranceDto)
            println("📤 DTO JSON: $dtoJson")

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

            // ✅ 2. Add insurance file
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
            println("📎 Added file: ${file.fileName} (${file.fileBytes.size} bytes)")

            println("📤 Sending ${formParts.size} form parts to $url")
            println("=".repeat(80))

            when (val response = repo.onPostMultipart(url, formParts)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ API Response received: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            try {
                                val insuranceResponse: com.informatique.mtcit.data.model.InsuranceDocumentResponse =
                                    json.decodeFromJsonElement(responseJson)

                                println("✅ Insurance document validated successfully!")
                                println("   Message: ${insuranceResponse.message}")
                                println("   Request ID: ${insuranceResponse.data?.id}")
                                println("   Status ID: ${insuranceResponse.data?.status?.id}")
                                println("   Insurance Doc: ${insuranceResponse.data?.insuranceDoc?.fileName}")
                                println("=".repeat(80))

                                Result.success(insuranceResponse)
                            } catch (e: Exception) {
                                println("❌ Failed to parse insurance response: ${e.message}")
                                println("   Response JSON: $responseJson")
                                println("=".repeat(80))
                                e.printStackTrace()
                                Result.failure(ApiException(500, "Failed to parse insurance response: ${e.message}"))
                            }
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to validate insurance document"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            println("   Full response: $responseJson")
                            println("=".repeat(80))
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        println("=".repeat(80))
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error Response:")
                    val errorMsg = ErrorMessageExtractor.extract(response.code.toString())
                    println("   Error: $errorMsg")
                    println("=".repeat(80))
                    Result.failure(ApiException(response.code, errorMsg))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in validateInsuranceDocument: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, e.message ?: "Failed to validate insurance document"))
        }
    }

    /**
     * Validate build status with dynamic documents for PERMANENT registration
     * POST perm-registration-requests/{requestId}/validate-build-status
     *
     * @param requestId The permanent registration request ID
     * @param documents List of document file uploads with documentId mapping
     * @return Result with DocumentValidationResponse
     */
    suspend fun validatePermanentBuildStatusWithDocuments(
        requestId: Int,
        documents: List<com.informatique.mtcit.data.model.DocumentFileUpload>
    ): Result<DocumentValidationResponse> {
        return try {
            println("=".repeat(80))
            println("🚀 RegistrationApiService: Validating PERMANENT build status with dynamic documents...")
            println("=".repeat(80))

            println("📤 Request Details:")
            println("   Request ID: $requestId")
            println("   Documents Count: ${documents.size}")
            documents.forEachIndexed { index, doc ->
                println("   Document $index: ${doc.fileName} (documentId=${doc.documentId}, ${doc.mimeType})")
            }
            println("=".repeat(80))

            val url = "perm-registration-requests/$requestId/validate-build-status"

            // Build multipart form data
            val formParts = mutableListOf<PartData>()

            // ✅ 1. Build documents DTO array
            val documentsDto = documents.map { doc ->
                com.informatique.mtcit.data.model.DocumentMetadata(
                    fileName = doc.fileName,
                    documentId = doc.documentId
                )
            }

            val dtoWrapper = com.informatique.mtcit.data.model.DocumentValidationRequestDto(
                documents = documentsDto
            )

            val dtoJson = json.encodeToString(dtoWrapper)
            println("📤 DTO JSON: $dtoJson")

            // Add "dto" field
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

            // ✅ 2. Add files (matching the DTO structure)
            documents.forEach { doc ->
                formParts.add(
                    PartData.BinaryItem(
                        provider = { doc.fileBytes.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"files\"; filename=\"${doc.fileName}\""
                            )
                            append(HttpHeaders.ContentType, doc.mimeType)
                        }
                    )
                )
                println("📎 Added file: ${doc.fileName} (${doc.fileBytes.size} bytes, documentId=${doc.documentId})")
            }

            // Debug: Print all form parts
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
                            val validationResponse: DocumentValidationResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Documents validated successfully!")
                            println("=".repeat(80))
                            return Result.success(validationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content ?: "Failed to validate documents"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            println("=".repeat(80))
                            return Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from server")
                        println("=".repeat(80))
                        return Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    println("=".repeat(80))
                    return Result.failure(ApiException(response.code, response.error?.toString() ?: "API error"))
                }
            }

            return Result.failure(ApiException(500, "Unknown error validating documents"))
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Exception in validatePermanentBuildStatusWithDocuments: ${e.message}")
            println("=".repeat(80))
            return Result.failure(ApiException(500, "Failed to validate build status: ${e.message}"))
        }
    }

    /**
     * Update an existing engine with multipart/form-data
     * PUT /api/v1/registration-requests/{requestId}/engines/{engineId}
     */
    suspend fun updateEngine(
        requestId: Int,
        engineId: Int,
        engine: EngineSubmissionRequest,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse> {
        return try {
            println("🚀 RegistrationApiService: Updating engine $engineId for requestId=$requestId...")
            println("📊 Engine data: ${json.encodeToString(engine)}")
            println("📎 Files count: ${files.size}")

            val formData = mutableListOf<PartData>()

            // Add engine DTO as JSON
            val engineJson = json.encodeToString(engine)
            formData.add(
                PartData.FormItem(
                    value = engineJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // Add file uploads
            files.forEach { fileUpload ->
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

            val url = "registration-requests/$requestId/engines/$engineId"
            when (val response = repo.onPutMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Engine update response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val engineResponse: EngineSubmissionResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Engine updated successfully!")
                            Result.success(engineResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update engine"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateEngine: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to update engine: ${e.message}"))
        }
    }

    /**
     * Update an existing owner with multipart/form-data
     * PUT /api/v1/registration-requests/{requestId}/owners/{ownerId}
     */
    suspend fun updateOwner(
        requestId: Int,
        ownerId: Int,
        owner: OwnerSubmissionRequest,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse> {
        return try {
            println("🚀 RegistrationApiService: Updating owner $ownerId for requestId=$requestId...")
            println("📊 Owner data: ${json.encodeToString(owner)}")
            println("📎 Files count: ${files.size}")

            val formData = mutableListOf<PartData>()

            // Add owner DTO as JSON
            val ownerJson = json.encodeToString(owner)
            formData.add(
                PartData.FormItem(
                    value = ownerJson,
                    dispose = {},
                    partHeaders = Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"dto\"")
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )
            )

            // Add file uploads
            files.forEach { fileUpload ->
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

            val url = "registration-requests/$requestId/owners/$ownerId"
            when (val response = repo.onPutMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Owner update response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val ownerResponse: OwnerSubmissionResponse = json.decodeFromJsonElement(responseJson)
                            println("✅ Owner updated successfully!")
                            Result.success(ownerResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to update owner"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    println("❌ API Error: ${response.error}")
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in updateOwner: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to update owner: ${e.message}"))
        }
    }

    /**
     * Delete an engine
     * DELETE /api/v1/registration-requests/{requestId}/engines/{engineId}
     */
    suspend fun deleteEngine(requestId: Int, engineId: Int): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Deleting engine $engineId for requestId=$requestId...")

            when (val response = repo.onDeleteAuth("registration-requests/$requestId/engines/$engineId")) {
                is RepoServiceState.Success -> {
                    println("✅ Engine deleted successfully!")
                    Result.success(Unit)
                }
                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    println("❌ API Error: $errorMessage")
                    Result.failure(ApiException(response.code, errorMessage))
                }
                else -> {
                    println("❌ Unknown response state")
                    Result.failure(ApiException(500, "Unknown response state"))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in deleteEngine: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to delete engine: ${e.message}"))
        }
    }

    /**
     * Get file preview by reference number
     * GET /api/v1/registration-request-view/file-preview?refNo={refNo}
     * Returns the actual file URL from MinIO storage
     *
     * Response format:
     * {
     *   "message": "Data fetched successfully",
     *   "statusCode": 200,
     *   "success": true,
     *   "timestamp": "2026-01-15 00:34:25",
     *   "data": "http://omanminio.isfpdomain.com/media/..."
     * }
     */
    suspend fun getFilePreview(refNo: String): Result<String> {
        return try {
            println("🚀 RegistrationApiService: Getting file preview for refNo=$refNo...")

            // Build the URL with query parameter
            val url = "registration-request-view/file-preview?refNo=$refNo"

            when (val response = repo.onGet(url)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ File preview API Response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200) {
                            // ✅ Extract the file URL from the "data" field
                            val fileUrl = responseJson.jsonObject["data"]?.jsonPrimitive?.content

                            if (!fileUrl.isNullOrEmpty()) {
                                println("✅ File URL extracted: $fileUrl")
                                Result.success(fileUrl)
                            } else {
                                println("❌ No file URL in response data")
                                Result.failure(ApiException(500, "No file URL in response"))
                            }
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to get file preview"
                            println("❌ API returned error: $message (Status: $statusCode)")
                            Result.failure(ApiException(statusCode, message))
                        }
                    } else {
                        println("❌ Empty response from API")
                        Result.failure(ApiException(500, "Empty response from server"))
                    }
                }
                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    println("❌ Failed to get file preview: $errorMessage")
                    Result.failure(ApiException(response.code, errorMessage))
                }
            }
        } catch (e: ApiException) {
            println("❌ API Exception in getFilePreview: ${e.message}")
            throw e
        } catch (e: Exception) {
            println("❌ Exception in getFilePreview: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to get file preview: ${e.message}"))
        }
    }

    /**
     * Delete an owner
     * DELETE /api/v1/registration-requests/{requestId}/owners/{ownerId}
     */
    suspend fun deleteOwner(requestId: Int, ownerId: Int): Result<Unit> {
        return try {
            println("🚀 RegistrationApiService: Deleting owner $ownerId for requestId=$requestId...")

            when (val response = repo.onDeleteAuth("registration-requests/$requestId/owners/$ownerId")) {
                is RepoServiceState.Success -> {
                    println("✅ Owner deleted successfully!")
                    Result.success(Unit)
                }
                is RepoServiceState.Error -> {
                    val errorMessage = ErrorMessageExtractor.extract(response.error)
                    println("❌ API Error: $errorMessage")
                    Result.failure(ApiException(response.code, errorMessage))
                }
                else -> {
                    println("❌ Unknown response state")
                    Result.failure(ApiException(500, "Unknown response state"))
                }
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            println("❌ Exception in deleteOwner: ${e.message}")
            e.printStackTrace()
            Result.failure(ApiException(500, "Failed to delete owner: ${e.message}"))
        }
    }
}


