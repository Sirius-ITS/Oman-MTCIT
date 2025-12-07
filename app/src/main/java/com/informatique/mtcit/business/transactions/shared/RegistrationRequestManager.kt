package com.informatique.mtcit.business.transactions.shared

import android.content.Context
import com.informatique.mtcit.business.transactions.mapper.RegistrationRequestMapper
import com.informatique.mtcit.data.model.EngineFileUpload
import com.informatique.mtcit.data.model.EngineSubmissionRequest
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.data.model.OwnerSubmissionRequest
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.UpdateWeightsRequest
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/**
 * Shared manager for handling registration request API calls
 * Used by all strategies to avoid duplicating API call logic
 */
@Singleton
class RegistrationRequestManager @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val lookupRepository: LookupRepository,
    private val mapper: RegistrationRequestMapper
) {

    /**
     * Create OR update registration request based on whether requestId exists
     *
     * First time (no requestId): POST to create
     * Going back and changing (has requestId): PUT to update
     *
     * @param formData Accumulated form data
     * @param requestTypeId Type of registration (1=Temporary, 2=Permanent, etc.)
     * @return Result with extracted IDs or error
     */
    suspend fun createOrUpdateRegistrationRequest(
        formData: Map<String, String>,
        requestTypeId: Int
    ): RegistrationRequestResult {
        return try {
            val existingRequestId = formData["requestId"]?.toIntOrNull()

            if (existingRequestId == null) {
                // ✅ First time - POST to create new request
                println("🚀 RegistrationRequestManager: Creating NEW registration request (type=$requestTypeId)...")

                // Map form data to API request (without id)
                val request = mapper
                    .mapToCreateRegistrationRequest(
                        formData = formData,
                        requestTypeId = requestTypeId
                    )

                println("📤 Sending POST to /api/v1/registration-requests...")

                // Call POST API
                val result = repository.createRegistrationRequest(request)

                result.fold(
                    onSuccess = { response ->
                        if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                            // Extract all important IDs
                            val requestId = response.data.id.toString()
                            val shipInfoId = response.data.shipInfo?.id?.toString()
                            val shipId = response.data.shipInfo?.ship?.id?.toString()
                            val requestSerial = response.data.requestSerial
                            val requestYear = response.data.requestYear

                            println("✅ Registration request created successfully!")
                            println("   Request ID: $requestId")
                            println("   Ship Info ID: $shipInfoId")
                            println("   Ship ID: $shipId")
                            println("   Request Serial: $requestSerial/$requestYear")

                            RegistrationRequestResult.Success(
                                requestId = requestId,
                                shipInfoId = shipInfoId,
                                shipId = shipId,
                                requestNumber = if (requestSerial != null && requestYear != null)
                                    "$requestSerial/$requestYear" else null
                            )
                        } else {
                            println("❌ API returned error: ${response.message}")
                            RegistrationRequestResult.Error(response.message)
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Failed to create registration request: ${exception.message}")
                        exception.printStackTrace()
                        RegistrationRequestResult.Error(exception.message ?: "Unknown error")
                    }
                )
            } else {
                // ✅ User went back - PUT to update existing request
                println("🔄 RegistrationRequestManager: UPDATING existing registration request (id=$existingRequestId, type=$requestTypeId)...")

                // Map form data to API request (WITH id this time)
                val request = mapper
                    .mapToCreateRegistrationRequest(
                        formData = formData,
                        requestTypeId = requestTypeId,
                        requestId = existingRequestId // ✅ Include the existing request ID
                    )

                println("📤 Sending PUT to /api/v1/registration-requests/update with requestId=$existingRequestId...")

                // Call PUT API
                val result = repository.updateRegistrationRequest(request)

                result.fold(
                    onSuccess = { response ->
                        if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                            val requestId = response.data.id.toString()
                            val shipInfoId = response.data.shipInfo?.id?.toString()
                            val shipId = response.data.shipInfo?.ship?.id?.toString()
                            val requestSerial = response.data.requestSerial
                            val requestYear = response.data.requestYear

                            println("✅ Registration request updated successfully!")
                            println("   Request ID: $requestId (unchanged)")

                            RegistrationRequestResult.Success(
                                requestId = requestId,
                                shipInfoId = shipInfoId,
                                shipId = shipId,
                                requestNumber = if (requestSerial != null && requestYear != null)
                                    "$requestSerial/$requestYear" else null
                            )
                        } else {
                            println("❌ API returned error: ${response.message}")
                            RegistrationRequestResult.Error(response.message)
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Failed to update registration request: ${exception.message}")
                        exception.printStackTrace()
                        RegistrationRequestResult.Error(exception.message ?: "Unknown error")
                    }
                )
            }
        } catch (e: Exception) {
            println("❌ Exception in createOrUpdateRegistrationRequest: ${e.message}")
            e.printStackTrace()
            RegistrationRequestResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Update ship dimensions (PUT)
     * Called after dimensions step
     *
     * @param requestId The registration request ID
     * @param formData Form data containing dimension values
     * @return Success or error result
     */
    suspend fun updateDimensions(
        requestId: String,
        formData: Map<String, String>
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Updating dimensions for requestId=$requestId...")

            // Extract dimension values and create request object
            val dimensionsRequest = UpdateDimensionsRequest(
                vesselLengthOverall = formData["overallLength"]?.toDoubleOrNull() ?: 0.0,
                vesselBeam = formData["overallWidth"]?.toDoubleOrNull() ?: 0.0,
                vesselDraft = formData["depth"]?.toDoubleOrNull() ?: 0.0,
                vesselHeight = formData["height"]?.toDoubleOrNull() ?: 0.0,
                decksNumber = formData["decksCount"]?.toIntOrNull() ?: 0
            )

            println("📤 Dimensions request: $dimensionsRequest")

            // Call API
            val result = repository.updateDimensions(requestId, dimensionsRequest)

            if (result.isSuccess) {
                println("✅ Dimensions updated successfully!")
                UpdateResult.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Failed to update dimensions: $errorMessage")
                UpdateResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("❌ Exception in updateDimensions: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Update ship weights (PUT)
     * Called after weights step
     *
     * @param requestId The registration request ID
     * @param formData Form data containing weight values
     * @return Success or error result
     */
    suspend fun updateWeights(
        requestId: String,
        formData: Map<String, String>
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Updating weights for requestId=$requestId...")

            // Extract weight values and create request object
            val weightsRequest = UpdateWeightsRequest(
                grossTonnage = formData["grossTonnage"]?.toDoubleOrNull() ?: 0.0,
                netTonnage = formData["netTonnage"]?.toDoubleOrNull() ?: 0.0,
                deadweightTonnage = formData["staticLoad"]?.toDoubleOrNull() ?: 0.0,
                maxLoadCapacity = formData["maxPermittedLoad"]?.toDoubleOrNull() ?: 0.0
            )

            println("📤 Weights request: $weightsRequest")

            // Call API
            val result = repository.updateWeights(requestId, weightsRequest)

            if (result.isSuccess) {
                println("✅ Weights updated successfully!")
                UpdateResult.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Failed to update weights: $errorMessage")
                UpdateResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("❌ Exception in updateWeights: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Update ship engines (PUT)
     * Called after engine info step
     *
     * @param requestId The registration request ID
     * @param formData Form data containing engines JSON
     * @return Success or error result
     */
    suspend fun updateEngines(
        requestId: String,
        formData: Map<String, String>
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Updating engines for requestId=$requestId...")

            val enginesJson = formData["engines"]

            if (enginesJson.isNullOrEmpty() || enginesJson == "[]") {
                println("⚠️ No engines to update")
                return UpdateResult.Success
            }

            println("📤 Engines JSON: $enginesJson")

            // Call API
            val result = repository.updateEngines(requestId, enginesJson)

            if (result.isSuccess) {
                println("✅ Engines updated successfully!")
                UpdateResult.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Failed to update engines: $errorMessage")
                UpdateResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("❌ Exception in updateEngines: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Update ship owners (PUT)
     * Called after owner info step
     *
     * @param requestId The registration request ID
     * @param formData Form data containing owners JSON
     * @return Success or error result
     */
    suspend fun updateOwners(
        requestId: String,
        formData: Map<String, String>
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Updating owners for requestId=$requestId...")

            val ownersJson = formData["owners"]

            if (ownersJson.isNullOrEmpty() || ownersJson == "[]") {
                println("⚠️ No owners to update")
                return UpdateResult.Success
            }

            println("📤 Owners JSON: $ownersJson")

            // Call API
            val result = repository.updateOwners(requestId, ownersJson)

            if (result.isSuccess) {
                println("✅ Owners updated successfully!")
                UpdateResult.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Failed to update owners: $errorMessage")
                UpdateResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("❌ Exception in updateOwners: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Submit engines with documents (NEW - multipart/form-data with files)
     * Called after engine info step when user has uploaded engine documents
     *
     * @param context Android context for file operations
     * @param requestId The registration request ID
     * @param engines List of engine submission requests
     * @param files List of file uploads
     * @return Success or error result
     */
    suspend fun submitEnginesWithFiles(
        context: Context,
        requestId: Int,
        engines: List<EngineSubmissionRequest>,
        files: List<EngineFileUpload>
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Submitting engines with files for requestId=$requestId...")
            println("📊 Engines count: ${engines.size}")
            println("📎 Files count: ${files.size}")

            if (engines.isEmpty()) {
                println("⚠️ No engines to submit")
                return UpdateResult.Success
            }

            // Call the new multipart API
            val result = repository.submitEngines(context, requestId, engines, files)

            result.fold(
                onSuccess = { response ->
                    if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                        println("✅ Engines and files submitted successfully!")
                        println("   Engines created: ${response.data?.size ?: 0}")
                        UpdateResult.Success
                    } else {
                        println("❌ API returned error: ${response.message}")
                        UpdateResult.Error(response.message)
                    }
                },
                onFailure = { exception ->
                    println("❌ Failed to submit engines: ${exception.message}")
                    exception.printStackTrace()
                    UpdateResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in submitEnginesWithFiles: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Submit owners with documents (NEW - multipart/form-data with files)
     * Called after owner info step when user has uploaded owner documents
     *
     * @param context Android context for file operations
     * @param requestId The registration request ID
     * @param owners List of owner submission requests
     * @param files List of file uploads
     * @return Success or error result
     */
    suspend fun submitOwnersWithFiles(
        context: Context,
        requestId: Int,
        owners: List<OwnerSubmissionRequest>,
        files: List<OwnerFileUpload>
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Submitting owners with files for requestId=$requestId...")
            println("📊 Owners count: ${owners.size}")
            println("📎 Files count: ${files.size}")

            if (owners.isEmpty()) {
                println("⚠️ No owners to submit")
                return UpdateResult.Success
            }

            // Call the new multipart API
            val result = repository.submitOwners(context, requestId, owners, files)

            result.fold(
                onSuccess = { response ->
                    if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                        println("✅ Owners and files submitted successfully!")
                        println("   Owners created: ${response.data?.size ?: 0}")
                        UpdateResult.Success
                    } else {
                        println("❌ API returned error: ${response.message}")
                        UpdateResult.Error(response.message)
                    }
                },
                onFailure = { exception ->
                    println("❌ Failed to submit owners: ${exception.message}")
                    exception.printStackTrace()
                    UpdateResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in submitOwnersWithFiles: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Validate build status documents (NEW - multipart/form-data with files)
     * Called after documents step when user has uploaded shipbuilding certificate and/or inspection documents
     *
     * POST api/v1/registration-requests/{requestId}/validate-build-status
     *
     * @param context Android context for file operations
     * @param requestId The registration request ID
     * @param formData Form data containing document URIs
     * @return ValidationResult with success/error and field-specific errors
     */
    suspend fun validateBuildStatusDocuments(
        context: Context,
        requestId: Int,
        formData: Map<String, String>
    ): DocumentValidationResult {
        return try {
            println("🚀 RegistrationRequestManager: Validating build status documents for requestId=$requestId...")

            // Extract document URIs from form data
            val shipbuildingCertUri = formData["shipbuildingCertificate"]
            val inspectionDocsUri = formData["inspectionDocuments"]

            println("📎 Shipbuilding Certificate URI: $shipbuildingCertUri")
            println("📎 Inspection Documents URI: $inspectionDocsUri")

            // Convert URIs to file uploads
            var shipbuildingCertFile: ByteArray? = null
            var shipbuildingCertName: String? = null
            var inspectionDocsFile: ByteArray? = null
            var inspectionDocsName: String? = null

            if (!shipbuildingCertUri.isNullOrEmpty() && shipbuildingCertUri != "null") {
                val uri = shipbuildingCertUri.toUri()
                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)
                if (fileUpload != null) {
                    shipbuildingCertFile = fileUpload.fileBytes
                    shipbuildingCertName = fileUpload.fileName
                    println("✅ Loaded shipbuilding certificate: $shipbuildingCertName (${shipbuildingCertFile.size} bytes)")
                }
            }

            if (!inspectionDocsUri.isNullOrEmpty() && inspectionDocsUri != "null") {
                val uri = inspectionDocsUri.toUri()
                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)
                if (fileUpload != null) {
                    inspectionDocsFile = fileUpload.fileBytes
                    inspectionDocsName = fileUpload.fileName
                    println("✅ Loaded inspection documents: $inspectionDocsName (${inspectionDocsFile.size} bytes)")
                }
            }

            // Call API to validate documents
            val result = repository.validateBuildStatus(
                requestId = requestId,
                shipbuildingCertificateFile = shipbuildingCertFile,
                shipbuildingCertificateName = shipbuildingCertName,
                inspectionDocumentsFile = inspectionDocsFile,
                inspectionDocumentsName = inspectionDocsName
            )

            result.fold(
                onSuccess = { response ->
                    if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                        println("✅ Build status documents validated successfully!")
                        DocumentValidationResult.Success
                    } else {
                        // Check for field-specific errors
                        val fieldErrors = response.errors ?: emptyMap()
                        if (fieldErrors.isNotEmpty()) {
                            println("❌ Validation failed with field errors: $fieldErrors")
                            DocumentValidationResult.ValidationErrors(fieldErrors)
                        } else {
                            println("❌ API returned error: ${response.message}")
                            DocumentValidationResult.Error(response.message)
                        }
                    }
                },
                onFailure = { exception ->
                    println("❌ Failed to validate build status: ${exception.message}")
                    exception.printStackTrace()
                    DocumentValidationResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in validateBuildStatusDocuments: ${e.message}")
            e.printStackTrace()
            DocumentValidationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Send registration request and check if inspection is needed
     * Called when user reaches Review Step
     *
     * POST api/v1/registration-requests/{request-id}/send-request
     *
     * @param requestId The registration request ID
     * @return SendRequestResult with inspection status
     */
    suspend fun sendRequest(requestId: Int): SendRequestResult {
        return try {
            println("🚀 RegistrationRequestManager: Sending request for requestId=$requestId...")

            val result = repository.sendRequest(requestId)

            result.fold(
                onSuccess = { response ->
                    if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                        println("✅ Request sent successfully!")
                        println("   Message: ${response.data.message}")
                        println("   Need Inspection: ${response.data.needInspection}")

                        SendRequestResult.Success(
                            message = response.data.message,
                            needInspection = response.data.needInspection
                        )
                    } else {
                        println("❌ API returned error: ${response.message}")
                        SendRequestResult.Error(response.message)
                    }
                },
                onFailure = { exception ->
                    println("❌ Failed to send request: ${exception.message}")
                    exception.printStackTrace()
                    SendRequestResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in sendRequest: ${e.message}")
            e.printStackTrace()
            SendRequestResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Reserve marine/ship name
     * Called when user enters marine name and clicks "Proceed to Payment"
     *
     * POST api/v1/registration-requests/{id}/{name}/shipNameReservtion
     *
     * @param requestId The registration request ID
     * @param marineName The marine/ship name entered by user
     * @return Success or error result
     */
    suspend fun reserveMarineName(
        requestId: String,
        marineName: String
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Reserving marine name for requestId=$requestId...")
            println("📤 Marine Name: $marineName")

            // Validate marine name is not empty
            if (marineName.isBlank()) {
                println("❌ Marine name is empty")
                return UpdateResult.Error("Marine name cannot be empty")
            }

            // Call API
            val result = repository.shipNameReservation(requestId.toInt(), marineName)

            if (result.isSuccess) {
                println("✅ Marine name reserved successfully!")
                UpdateResult.Success
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                println("❌ Failed to reserve marine name: $errorMessage")
                UpdateResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            println("❌ Exception in reserveMarineName: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Detect which step was just completed and call appropriate API
     *
     * @param stepFields The field IDs in the current step
     * @param formData All accumulated form data
     * @param context Android context (needed for engine file uploads)
     * @return Result indicating success or error
     */
    suspend fun processStepIfNeeded(
        stepFields: List<String>,
        formData: MutableMap<String, String>,
        requestTypeId: Int,
        context: Context? = null // ✅ NEW: Optional context for file operations
    ): StepProcessResult {

        // ✅ Check if this is the Unit Selection Step (ship info)
        val hasUnitSelectionFields = stepFields.any { it == "unitType" || it == "callSign" }
        if (hasUnitSelectionFields && formData.containsKey("unitType")) {
            // ✅ SIMULATION MODE: Use hardcoded test data instead of calling API
            // This prevents creating new ships during engine testing
            /*println("🔍 Detected Unit Selection Step - SIMULATION MODE ENABLED")
            println("⚠️ Skipping API call - Using test data instead")

            // Store simulated IDs in form data
            formData["requestId"] = "446"
            formData["shipInfoId"] = "486"
            formData["shipId"] = "566"
            formData["requestNumber"] = "324/2025"

            println("✅ SIMULATED Request ID: 381")
            println("✅ SIMULATED Ship Info ID: 421")
            println("✅ SIMULATED Ship ID: 503")
            println("✅ SIMULATED Request Serial: 266/2025")

            return StepProcessResult.Success("Registration request simulated (test mode)")*/

            // ✅ TODO: Uncomment below to use REAL API instead of simulation

            println("🔍 Detected Unit Selection Step - Creating or updating registration request...")

            val result = createOrUpdateRegistrationRequest(formData, requestTypeId)
            return when (result) {
                is RegistrationRequestResult.Success -> {
                    // Store IDs in form data
                    formData["requestId"] = result.requestId
                    result.shipInfoId?.let { formData["shipInfoId"] = it }
                    result.shipId?.let { formData["shipId"] = it }
                    result.requestNumber?.let { formData["requestNumber"] = it }

                    StepProcessResult.Success("Registration request created/updated")
                }
                is RegistrationRequestResult.Error -> {
                    StepProcessResult.Error(result.message)
                }
            }
        }

        // Get requestId for subsequent calls
        val requestId = formData["requestId"]

        if (requestId == null) {
            // No requestId yet, this step doesn't need API call
            return StepProcessResult.NoAction
        }

        // ✅ Check if this is the Dimensions Step
        // Always send PUT request (whether first time or user went back and changed)
        val hasDimensionsFields = stepFields.containsAll(listOf("overallLength", "overallWidth", "depth"))
        if (hasDimensionsFields) {
            println("🔍 Detected Dimensions Step - Updating dimensions (always sends PUT)...")

            val result = updateDimensions(requestId!!, formData) // Use !! since we checked for null above
            return when (result) {
                is UpdateResult.Success -> StepProcessResult.Success("Dimensions updated")
                is UpdateResult.Error -> StepProcessResult.Error(result.message)
            }
        }

        // ✅ Check if this is the Weights Step
        // Always send PUT request (whether first time or user went back and changed)
        val hasWeightsFields = stepFields.containsAll(listOf("grossTonnage", "netTonnage"))
        if (hasWeightsFields) {
            println("🔍 Detected Weights Step - Updating weights (always sends PUT)...")

            val result = updateWeights(requestId!!, formData) // Use !! since we checked for null above
            return when (result) {
                is UpdateResult.Success -> StepProcessResult.Success("Weights updated")
                is UpdateResult.Error -> StepProcessResult.Error(result.message)
            }
        }

        // ✅ Check if this is the Owner Info Step
        val hasOwnerFields = stepFields.contains("owners")
        if (hasOwnerFields) {
            println("🔍 Detected Owner Info Step - Submitting owners with multipart API...")

            val ownersJson = formData["owners"]

            if (ownersJson.isNullOrEmpty() || ownersJson == "[]") {
                println("⚠️ No owners to submit")
                return StepProcessResult.Success("No owners to update")
            }

            // ✅ Context is REQUIRED for multipart owner submission
            if (context == null) {
                println("❌ Context not available for owner file uploads - Cannot proceed")
                return StepProcessResult.Error("Context required for owner submission")
            }

            // Parse owners JSON and convert to OwnerSubmissionRequest list
            return try {
                val owners = parseOwnersFromJson(ownersJson, context)
                val files = parseOwnerFilesFromFormData(context, formData)

                println("📊 Parsed ${owners.size} owners and ${files.size} files")

                // ✅ ALWAYS call the multipart API (no JSON-only fallback)
                val result = submitOwnersWithFiles(
                    context = context,
                    requestId = requestId.toInt(),
                    owners = owners,
                    files = files
                )

                when (result) {
                    is UpdateResult.Success -> StepProcessResult.Success("Owners submitted successfully")
                    is UpdateResult.Error -> StepProcessResult.Error(result.message)
                }
            } catch (e: Exception) {
                println("❌ Error parsing owner data: ${e.message}")
                e.printStackTrace()
                StepProcessResult.Error("Failed to parse owner data: ${e.message}")
            }
        }

        // ✅ Check if this is the Engine Info Step
        val hasEngineFields = stepFields.contains("engines")
        if (hasEngineFields) {
            println("🔍 Detected Engine Info Step - Submitting engines with multipart API...")

            val enginesJson = formData["engines"]

            if (enginesJson.isNullOrEmpty() || enginesJson == "[]") {
                println("⚠️ No engines to submit")
                return StepProcessResult.Success("No engines to update")
            }

            // ✅ FIXED: Context is REQUIRED for multipart engine submission
            if (context == null) {
                println("❌ Context not available for engine file uploads - Cannot proceed")
                return StepProcessResult.Error("Context required for engine submission")
            }

            // Parse engines JSON and convert to EngineSubmissionRequest list
            return try {
                val engines = parseEnginesFromJson(enginesJson, formData, context)
                val files = parseEngineFilesFromFormData(context, formData)

                println("📊 Parsed ${engines.size} engines and ${files.size} files")

                // ✅ ALWAYS call the multipart API (no JSON-only fallback)
                val result = submitEnginesWithFiles(
                    context = context,
                    requestId = requestId.toInt(),
                    engines = engines,
                    files = files
                )

                when (result) {
                    is UpdateResult.Success -> StepProcessResult.Success("Engines submitted successfully")
                    is UpdateResult.Error -> StepProcessResult.Error(result.message)
                }
            } catch (e: Exception) {
                println("❌ Error parsing engine data: ${e.message}")
                e.printStackTrace()
                StepProcessResult.Error("Failed to parse engine data: ${e.message}")
            }
        }

        // ✅ Check if this is the Documents Step
        val hasDocumentFields = stepFields.any { it == "shipbuildingCertificate" || it == "inspectionDocuments" }
        if (hasDocumentFields) {
            println("🔍 Detected Documents Step - Validating build status documents...")

            // ✅ Context is REQUIRED for document validation
            if (context == null) {
                println("❌ Context not available for document validation - Cannot proceed")
                return StepProcessResult.Error("Context required for document validation")
            }

            // Check if at least one document is uploaded
            val shipbuildingCertUri = formData["shipbuildingCertificate"]
            val inspectionDocsUri = formData["inspectionDocuments"]

            val hasShipbuildingCert = !shipbuildingCertUri.isNullOrEmpty() && shipbuildingCertUri != "null"
            val hasInspectionDocs = !inspectionDocsUri.isNullOrEmpty() && inspectionDocsUri != "null"

            if (!hasShipbuildingCert && !hasInspectionDocs) {
                println("⚠️ No documents uploaded - skipping validation")
                return StepProcessResult.Success("No documents to validate")
            }

            println("📎 Validating documents...")
            println("   - Shipbuilding Certificate: ${if (hasShipbuildingCert) "✓" else "✗"}")
            println("   - Inspection Documents: ${if (hasInspectionDocs) "✓" else "✗"}")

            // Call validation API
            return try {
                val result = validateBuildStatusDocuments(
                    context = context,
                    requestId = requestId.toInt(),
                    formData = formData
                )

                when (result) {
                    is DocumentValidationResult.Success -> {
                        println("✅ Documents validated successfully!")
                        StepProcessResult.Success("Documents validated successfully")
                    }
                    is DocumentValidationResult.ValidationErrors -> {
                        // Store field errors in formData for display in UI
                        result.fieldErrors.forEach { (fieldId, error) ->
                            formData["${fieldId}_error"] = error
                        }
                        println("❌ Document validation failed with errors: ${result.fieldErrors}")
                        StepProcessResult.Error("Document validation failed")
                    }
                    is DocumentValidationResult.Error -> {
                        println("❌ Document validation error: ${result.message}")
                        StepProcessResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                println("❌ Error validating documents: ${e.message}")
                e.printStackTrace()
                StepProcessResult.Error("Failed to validate documents: ${e.message}")
            }
        }

        // ✅ NEW: Check if this is the Review Step (no fields = review step)
        if (stepFields.isEmpty()) {
            println("🔍 Detected Review Step - Sending request...")

            return try {
                val result = sendRequest(requestId.toInt())

                when (result) {
                    is SendRequestResult.Success -> {
                        println("✅ Request sent successfully!")
                        // Store the inspection flag in formData for the strategy to check
                        formData["needInspection"] = result.needInspection.toString()
                        formData["sendRequestMessage"] = result.message
                        StepProcessResult.Success(result.message)
                    }
                    is SendRequestResult.Error -> {
                        println("❌ Send request error: ${result.message}")
                        StepProcessResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                println("❌ Error sending request: ${e.message}")
                e.printStackTrace()
                StepProcessResult.Error("Failed to send request: ${e.message}")
            }
        }

        // ✅ NEW: Check if this is the Marine Unit Name Step (final step before payment)
        val hasMarineNameField = stepFields.contains("marineUnitName")
        if (hasMarineNameField) {
            println("🔍 Detected Marine Unit Name Step - Reserving ship name...")

            val marineName = formData["marineUnitName"]

            if (marineName.isNullOrBlank()) {
                println("⚠️ Marine name is empty - skipping reservation")
                return StepProcessResult.Error("Marine name is required")
            }

            return try {
                val result = reserveMarineName(
                    requestId = requestId,
                    marineName = marineName.trim()
                )

                when (result) {
                    is UpdateResult.Success -> {
                        println("✅ Marine name reserved successfully!")
                        // Show success dialog using InspectionRequiredDialog
                        formData["showInspectionDialog"] = "true"
                        formData["inspectionMessage"] = "تم حجز اسم الوحدة البحرية بنجاح!\n\nاسم الوحدة: ${marineName.trim()}\n\nجاهز للانتقال إلى الدفع"
                        StepProcessResult.Success("Marine name reserved successfully")
                    }
                    is UpdateResult.Error -> {
                        println("❌ Marine name reservation error: ${result.message}")
                        // Show error dialog using InspectionRequiredDialog
                        formData["showInspectionDialog"] = "true"
                        formData["inspectionMessage"] = "فشل حجز اسم الوحدة البحرية\n\n${result.message}"
                        StepProcessResult.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                println("❌ Error reserving marine name: ${e.message}")
                e.printStackTrace()
                // Show error dialog for exception
                formData["showInspectionDialog"] = "true"
                formData["inspectionMessage"] = "فشل حجز اسم الوحدة البحرية\n\n${e.message ?: "خطأ غير معروف"}"
                StepProcessResult.Error("Failed to reserve marine name: ${e.message}")
            }
        }

        // No API call needed for this step
        return StepProcessResult.NoAction
    }

    /**
     * Parse engines JSON from formData and convert to EngineSubmissionRequest list
     */
    private suspend fun parseEnginesFromJson(
        enginesJson: String,
        formData: Map<String, String>,
        context: Context
    ): List<EngineSubmissionRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val enginesArray = json.parseToJsonElement(enginesJson).jsonArray

                // ✅ Load lookup data to get proper IDs and English names
                val engineTypes = lookupRepository.getEngineTypesRaw()
                val countries = lookupRepository.getCountriesRaw()
                val fuelTypes = lookupRepository.getFuelTypesRaw()
                val engineStatuses = lookupRepository.getEngineStatusesRaw()

                enginesArray.mapIndexed { index, engineElement ->
                    val engineObj = engineElement.jsonObject
                    val docOwnerId = "engine-${index + 1}"

                    // Extract values from JSON
                    val engineSerialNumber = engineObj["number"]?.jsonPrimitive?.content ?: ""
                    val engineTypeArabic = engineObj["type"]?.jsonPrimitive?.content ?: ""
                    val enginePower = engineObj["power"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val cylindersCount = engineObj["cylinder"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val engineModel = engineObj["model"]?.jsonPrimitive?.content ?: ""
                    val engineManufacturer = engineObj["manufacturer"]?.jsonPrimitive?.content ?: ""
                    val engineCountryArabic = engineObj["productionCountry"]?.jsonPrimitive?.content ?: ""
                    val engineBuildYear = engineObj["manufactureYear"]?.jsonPrimitive?.content ?: ""
                    val engineFuelTypeArabic = engineObj["fuelType"]?.jsonPrimitive?.content ?: ""
                    val engineConditionArabic = engineObj["condition"]?.jsonPrimitive?.content ?: ""

                    // ✅ Parse documents array (supports multiple documents per engine)
                    val documentsArray = engineObj["documents"]?.jsonArray
                    val documents = mutableListOf<com.informatique.mtcit.data.model.EngineDocumentMetadata>()

                    if (documentsArray != null) {
                        documentsArray.forEach { docElement ->
                            val docObj = docElement.jsonObject
                            val documentUri = docObj["documentUri"]?.jsonPrimitive?.content ?: ""
                            val docId = docObj["docId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                            if (documentUri.isNotEmpty()) {
                                val uri = android.net.Uri.parse(documentUri)
                                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)
                                val fileName = fileUpload?.fileName ?: ""

                                if (fileName.isNotEmpty()) {
                                    documents.add(
                                        com.informatique.mtcit.data.model.EngineDocumentMetadata(
                                            fileName = fileName,
                                            docOwnerId = docOwnerId,
                                            docId = docId
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // ✅ BACKWARD COMPATIBILITY: Support old format with single documentUri
                        val documentUri = engineObj["documentUri"]?.jsonPrimitive?.content ?: ""
                        if (documentUri.isNotEmpty()) {
                            val uri = android.net.Uri.parse(documentUri)
                            val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)
                            val fileName = fileUpload?.fileName ?: ""

                            if (fileName.isNotEmpty()) {
                                documents.add(
                                    com.informatique.mtcit.data.model.EngineDocumentMetadata(
                                        fileName = fileName,
                                        docOwnerId = docOwnerId,
                                        docId = 1 // Default docId for backward compatibility
                                    )
                                )
                            }
                        }
                    }

                    // ✅ Look up full lookup objects by Arabic name
                    val engineType = engineTypes.find { it.nameAr == engineTypeArabic }
                    val country = countries.find { it.nameAr == engineCountryArabic }
                    val fuelType = fuelTypes.find { it.nameAr == engineFuelTypeArabic }
                    val status = engineStatuses.find { it.nameAr == engineConditionArabic }

                    EngineSubmissionRequest(
                        engineSerialNumber = engineSerialNumber,
                        engineType = com.informatique.mtcit.data.model.EngineTypeRef(
                            id = engineType?.id?.toString() ?: engineTypeArabic,
                            nameEn = engineType?.nameEn ?: engineTypeArabic,
                            nameAr = engineType?.nameAr ?: engineTypeArabic
                        ),
                        enginePower = enginePower,
                        cylindersCount = cylindersCount,
                        engineModel = engineModel,
                        engineManufacturer = engineManufacturer,
                        engineCountry = if (engineCountryArabic.isNotEmpty()) {
                            com.informatique.mtcit.data.model.EngineCountryRef(
                                id = country?.id?.toString() ?: engineCountryArabic,
                                nameEn = country?.nameEn ?: engineCountryArabic,
                                nameAr = country?.nameAr ?: engineCountryArabic
                            )
                        } else null,
                        engineBuildYear = engineBuildYear,
                        engineFuelType = if (engineFuelTypeArabic.isNotEmpty()) {
                            com.informatique.mtcit.data.model.EngineFuelTypeRef(
                                id = fuelType?.id?.toString() ?: engineFuelTypeArabic,
                                nameEn = fuelType?.nameEn ?: engineFuelTypeArabic,
                                nameAr = fuelType?.nameAr ?: engineFuelTypeArabic
                            )
                        } else null,
                        engineStatus = com.informatique.mtcit.data.model.EngineStatusRef(
                            id = status?.id?.toString() ?: engineConditionArabic,
                            nameEn = status?.nameEn ?: engineConditionArabic,
                            nameAr = status?.nameAr ?: engineConditionArabic
                        ),
                        docOwnerId = docOwnerId,
                        documents = documents
                    )
                }
            } catch (e: Exception) {
                println("❌ Error parsing engines JSON: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Parse engine files from formData and convert to EngineFileUpload list
     */
    private suspend fun parseEngineFilesFromFormData(
        context: Context,
        formData: Map<String, String>
    ): List<EngineFileUpload> {
        return withContext(Dispatchers.IO) {
            try {
                // Parse the engines JSON to extract file URIs
                val enginesJson = formData["engines"] ?: return@withContext emptyList()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val enginesArray = json.parseToJsonElement(enginesJson).jsonArray

                val files = mutableListOf<EngineFileUpload>()

                enginesArray.forEachIndexed { engineIndex, engineElement ->
                    val engineObj = engineElement.jsonObject
                    val docOwnerId = "engine-${engineIndex + 1}"

                    // ✅ Support multiple documents per engine
                    val documentsArray = engineObj["documents"]?.jsonArray

                    if (documentsArray != null) {
                        documentsArray.forEach { docElement ->
                            val docObj = docElement.jsonObject
                            val documentUri = docObj["documentUri"]?.jsonPrimitive?.content
                            val docId = docObj["docId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                            if (!documentUri.isNullOrEmpty() && documentUri != "") {
                                // Convert URI to EngineFileUpload
                                val uri = android.net.Uri.parse(documentUri)
                                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)

                                if (fileUpload != null) {
                                    files.add(
                                        EngineFileUpload(
                                            fileName = fileUpload.fileName,
                                            fileUri = fileUpload.fileUri,
                                            fileBytes = fileUpload.fileBytes,
                                            mimeType = fileUpload.mimeType,
                                            docOwnerId = docOwnerId,
                                            docId = docId
                                        )
                                    )
                                    println("📎 Added engine file: ${fileUpload.fileName} (docOwnerId=$docOwnerId, docId=$docId)")
                                }
                            }
                        }
                    } else {
                        // ✅ BACKWARD COMPATIBILITY: Support old format with single documentUri
                        val documentUri = engineObj["documentUri"]?.jsonPrimitive?.content

                        if (!documentUri.isNullOrEmpty() && documentUri != "") {
                            val uri = android.net.Uri.parse(documentUri)
                            val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)

                            if (fileUpload != null) {
                                files.add(
                                    EngineFileUpload(
                                        fileName = fileUpload.fileName,
                                        fileUri = fileUpload.fileUri,
                                        fileBytes = fileUpload.fileBytes,
                                        mimeType = fileUpload.mimeType,
                                        docOwnerId = docOwnerId,
                                        docId = 1 // Default docId for backward compatibility
                                    )
                                )
                                println("📎 Added engine file: ${fileUpload.fileName} (docOwnerId=$docOwnerId, docId=1)")
                            }
                        }
                    }
                }

                files
            } catch (e: Exception) {
                println("❌ Error parsing engine files: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Parse owners JSON from formData and convert to OwnerSubmissionRequest list
     */
    private suspend fun parseOwnersFromJson(
        ownersJson: String,
        context: Context
    ): List<OwnerSubmissionRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val ownersArray = json.parseToJsonElement(ownersJson).jsonArray

                ownersArray.mapIndexed { index, ownerElement ->
                    val ownerObj = ownerElement.jsonObject
                    val docOwnerId = "owner${index + 1}"

                    // Extract values from JSON - map form field names to API field names
                    val ownerName = ownerObj["ownerName"]?.jsonPrimitive?.content ?: ""
                    val ownerNameEn = ownerObj["ownerNameEn"]?.jsonPrimitive?.content
                    val fullName = ownerObj["fullName"]?.jsonPrimitive?.content ?: "" // Backward compatibility
                    val idNumber = ownerObj["idNumber"]?.jsonPrimitive?.content
                    val ownerShipPercentage = ownerObj["ownerShipPercentage"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                    val email = ownerObj["email"]?.jsonPrimitive?.content
                    val mobile = ownerObj["mobile"]?.jsonPrimitive?.content
                    val address = ownerObj["address"]?.jsonPrimitive?.content

                    // Check if it's a company or individual
                    val isCompany = ownerObj["isCompany"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val commercialRegNumber = ownerObj["companyRegistrationNumber"]?.jsonPrimitive?.content

                    // Get isRepresentative value (boolean from form -> 0 or 1 for API)
                    val isRepresentativeBool = ownerObj["isRepresentative"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    val isRepresentative = if (isRepresentativeBool) 1 else 0

                    // Parse documents array - look for ownershipProofDocument field
                    val documents = mutableListOf<com.informatique.mtcit.data.model.OwnerDocumentMetadata>()

                    // Check if there's a documents array
                    val documentsArray = ownerObj["documents"]?.jsonArray
                    if (documentsArray != null) {
                        documentsArray.forEach { docElement ->
                            val docObj = docElement.jsonObject
                            val documentUri = docObj["documentUri"]?.jsonPrimitive?.content ?: ""
                            val docId = docObj["docId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                            // Extract actual file name from document URI
                            if (documentUri.isNotEmpty()) {
                                val uri = documentUri.toUri()
                                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)
                                val fileName = fileUpload?.fileName ?: ""

                                if (fileName.isNotEmpty()) {
                                    documents.add(
                                        com.informatique.mtcit.data.model.OwnerDocumentMetadata(
                                            fileName = fileName,
                                            docOwnerId = docOwnerId,
                                            docId = docId
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Check for single ownershipProofDocument field (backward compatibility)
                        val ownershipProofDocument = ownerObj["ownershipProofDocument"]?.jsonPrimitive?.content
                        if (!ownershipProofDocument.isNullOrEmpty()) {
                            val uri = ownershipProofDocument.toUri()
                            val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)
                            val fileName = fileUpload?.fileName ?: ""

                            if (fileName.isNotEmpty()) {
                                documents.add(
                                    com.informatique.mtcit.data.model.OwnerDocumentMetadata(
                                        fileName = fileName,
                                        docOwnerId = docOwnerId,
                                        docId = 1 // Default document type
                                    )
                                )
                            }
                        }
                    }

                    OwnerSubmissionRequest(
                        isCompany = isCompany,
                        ownerName = if (ownerName.isNotEmpty()) ownerName else fullName, // Use new field or fallback to fullName
                        ownerNameEn = ownerNameEn,
                        ownerCivilId = idNumber,
                        commercialRegNumber = commercialRegNumber,
                        ownershipPercentage = ownerShipPercentage,
                        isRepresentative = isRepresentative,
                        ownerAddress = address,
                        ownerPhone = mobile,
                        ownerEmail = email,
                        docOwnerId = docOwnerId,
                        documents = documents
                    )
                }
            } catch (e: Exception) {
                println("❌ Error parsing owners JSON: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Parse owner files from formData and convert to OwnerFileUpload list
     */
    private suspend fun parseOwnerFilesFromFormData(
        context: Context,
        formData: Map<String, String>
    ): List<OwnerFileUpload> {
        return withContext(Dispatchers.IO) {
            try {
                // Parse the owners JSON to extract file URIs
                val ownersJson = formData["owners"] ?: return@withContext emptyList()
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val ownersArray = json.parseToJsonElement(ownersJson).jsonArray

                val files = mutableListOf<OwnerFileUpload>()

                ownersArray.forEachIndexed { ownerIndex, ownerElement ->
                    val ownerObj = ownerElement.jsonObject
                    val docOwnerId = "owner${ownerIndex + 1}"

                    // Check if there's a documents array
                    val documentsArray = ownerObj["documents"]?.jsonArray

                    if (documentsArray != null) {
                        documentsArray.forEach { docElement ->
                            val docObj = docElement.jsonObject
                            val documentUri = docObj["documentUri"]?.jsonPrimitive?.content
                            val docId = docObj["docId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                            if (!documentUri.isNullOrEmpty() && documentUri != "") {
                                // Convert URI to OwnerFileUpload
                                val uri = android.net.Uri.parse(documentUri)
                                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)

                                if (fileUpload != null) {
                                    files.add(
                                        OwnerFileUpload(
                                            fileName = fileUpload.fileName,
                                            fileUri = fileUpload.fileUri,
                                            fileBytes = fileUpload.fileBytes,
                                            mimeType = fileUpload.mimeType,
                                            docOwnerId = docOwnerId,
                                            docId = docId
                                        )
                                    )
                                    println("📎 Added owner file: ${fileUpload.fileName} (docOwnerId=$docOwnerId, docId=$docId)")
                                }
                            }
                        }
                    } else {
                        // ✅ BACKWARD COMPATIBILITY: Support single ownershipProofDocument field
                        val ownershipProofDocument = ownerObj["ownershipProofDocument"]?.jsonPrimitive?.content

                        if (!ownershipProofDocument.isNullOrEmpty() && ownershipProofDocument != "") {
                            val uri = android.net.Uri.parse(ownershipProofDocument)
                            val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)

                            if (fileUpload != null) {
                                files.add(
                                    OwnerFileUpload(
                                        fileName = fileUpload.fileName,
                                        fileUri = fileUpload.fileUri,
                                        fileBytes = fileUpload.fileBytes,
                                        mimeType = fileUpload.mimeType,
                                        docOwnerId = docOwnerId,
                                        docId = 1 // Default document type
                                    )
                                )
                                println("📎 Added owner file: ${fileUpload.fileName} (docOwnerId=$docOwnerId, docId=1)")
                            }
                        }
                    }
                }

                files
            } catch (e: Exception) {
                println("❌ Error parsing owner files: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }
}

/**
 * Result types for registration request operations
 */
sealed class RegistrationRequestResult {
    data class Success(
        val requestId: String,
        val shipInfoId: String?,
        val shipId: String?,
        val requestNumber: String?
    ) : RegistrationRequestResult()

    data class Error(val message: String) : RegistrationRequestResult()
}

sealed class UpdateResult {
    object Success : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

sealed class StepProcessResult {
    data class Success(val message: String) : StepProcessResult()
    data class Error(val message: String) : StepProcessResult()
    object NoAction : StepProcessResult()
}

sealed class DocumentValidationResult {
    object Success : DocumentValidationResult()
    data class Error(val message: String) : DocumentValidationResult()
    data class ValidationErrors(val fieldErrors: Map<String, String>) : DocumentValidationResult()
}

sealed class SendRequestResult {
    data class Success(
        val message: String,
        val needInspection: Boolean
    ) : SendRequestResult()

    data class Error(val message: String) : SendRequestResult()
}
