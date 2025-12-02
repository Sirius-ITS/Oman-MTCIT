package com.informatique.mtcit.data.api

import android.content.Context
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
import com.informatique.mtcit.di.module.AppRepository
import com.informatique.mtcit.di.module.RepoServiceState
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.streams.asInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
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
        context: Context,
        requestId: Int,
        engines: List<EngineSubmissionRequest>,
        files: List<EngineFileUpload>
    ): Result<EngineSubmissionResponse> {
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
            Result.failure(Exception("Failed to submit engines: ${e.message}"))
        }
    }

    /**
     * Submit owners with documents (multipart/form-data)
     * POST api/v1/registration-requests/{requestId}/owners
     *
     * This API consumes form-data with:
     * - dto: JSON array of owners
     * - files: Multipart files (mapped by docOwnerId)
     */
    suspend fun submitOwners(
        context: Context,
        requestId: Int,
        owners: List<OwnerSubmissionRequest>,
        files: List<OwnerFileUpload>
    ): Result<OwnerSubmissionResponse> {
        return try {
            println("🚀 RegistrationApiService: Submitting owners for requestId=$requestId...")
            println("📊 Owners count: ${owners.size}")
            println("📎 Files count: ${files.size}")

            // Build multipart form data
            val formData = mutableListOf<PartData>()

            // 1. Add DTO part (JSON array of owners)
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
            val url = "api/v1/registration-requests/$requestId/owners"
            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Owners submission response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val ownerResponse: OwnerSubmissionResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Owners submitted successfully!")
                            println("   Owners created: ${ownerResponse.data?.size ?: 0}")

                            Result.success(ownerResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to submit owners"
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
            println("❌ Exception in submitOwners: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to submit owners: ${e.message}"))
        }
    }

    /**
     * Validate build status documents
     * POST api/v1/registration-requests/{requestId}/validate-build-status
     *
     * Multipart request with:
     * - shipbuildingCertificate: File (optional)
     * - inspectionDocuments: File (optional)
     */
    suspend fun validateBuildStatus(
        requestId: Int,
        shipbuildingCertificateFile: ByteArray?,
        shipbuildingCertificateName: String?,
        inspectionDocumentsFile: ByteArray?,
        inspectionDocumentsName: String?
    ): Result<DocumentValidationResponse> {
        return try {
            println("🚀 RegistrationApiService: Validating build status for requestId=$requestId...")
            println("📎 Shipbuilding Certificate: ${shipbuildingCertificateName ?: "Not provided"}")
            println("📎 Inspection Documents: ${inspectionDocumentsName ?: "Not provided"}")

            // Build multipart form data
            val formData = mutableListOf<PartData>()

            // Add shipbuilding certificate if provided (as 'files' array element)
            if (shipbuildingCertificateFile != null && shipbuildingCertificateName != null) {
                println("📎 Adding shipbuilding certificate: $shipbuildingCertificateName (${shipbuildingCertificateFile.size} bytes)")

                formData.add(
                    PartData.BinaryItem(
                        provider = { shipbuildingCertificateFile.inputStream().asInput() },
                        dispose = {},
                        partHeaders = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"files\"; filename=\"$shipbuildingCertificateName\""
                            )
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
                            append(HttpHeaders.ContentType, "application/pdf")
                        }
                    )
                )
            }

            // Send the multipart request
            val url = "api/v1/registration-requests/$requestId/validate-build-status"
            when (val response = repo.onPostMultipart(url, formData)) {
                is RepoServiceState.Success -> {
                    val responseJson = response.response
                    println("✅ Build status validation response: $responseJson")

                    if (!responseJson.jsonObject.isEmpty()) {
                        val statusCode = responseJson.jsonObject.getValue("statusCode").jsonPrimitive.int

                        if (statusCode == 200 || statusCode == 201) {
                            val validationResponse: DocumentValidationResponse =
                                json.decodeFromJsonElement(responseJson)

                            println("✅ Build status validated successfully!")

                            Result.success(validationResponse)
                        } else {
                            val message = responseJson.jsonObject["message"]?.jsonPrimitive?.content
                                ?: "Failed to validate build status"
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
            println("❌ Exception in validateBuildStatus: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Failed to validate build status: ${e.message}"))
        }
    }
}
