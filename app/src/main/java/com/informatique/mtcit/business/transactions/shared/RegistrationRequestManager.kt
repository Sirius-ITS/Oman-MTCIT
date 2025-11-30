package com.informatique.mtcit.business.transactions.shared

import com.informatique.mtcit.data.model.CreateRegistrationRequest
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.UpdateWeightsRequest
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared manager for handling registration request API calls
 * Used by all strategies to avoid duplicating API call logic
 */
@Singleton
class RegistrationRequestManager @Inject constructor(
    private val repository: ShipRegistrationRepository
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
                val request = com.informatique.mtcit.business.transactions.mapper.RegistrationRequestMapper
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
                val request = com.informatique.mtcit.business.transactions.mapper.RegistrationRequestMapper
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
     * Detect which step was just completed and call appropriate API
     *
     * @param stepFields The field IDs in the current step
     * @param formData All accumulated form data
     * @return Result indicating success or error
     */
    suspend fun processStepIfNeeded(
        stepFields: List<String>,
        formData: MutableMap<String, String>,
        requestTypeId: Int
    ): StepProcessResult {

        // ✅ Check if this is the Unit Selection Step (ship info)
        val hasUnitSelectionFields = stepFields.any { it == "unitType" || it == "callSign" }
        if (hasUnitSelectionFields && formData.containsKey("unitType")) {
            // ✅ CHANGED: Always call API when completing unit selection step
            // First time (no requestId) → POST to create
            // Going back (has requestId) → PUT to update
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

            val result = updateDimensions(requestId, formData)
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

            val result = updateWeights(requestId, formData)
            return when (result) {
                is UpdateResult.Success -> StepProcessResult.Success("Weights updated")
                is UpdateResult.Error -> StepProcessResult.Error(result.message)
            }
        }

        // ✅ Check if this is the Engine Info Step
        // Always send PUT request (whether first time or user went back and changed)
        val hasEngineFields = stepFields.any { it == "engines" }
        if (hasEngineFields) {
            println("🔍 Detected Engine Info Step - Updating engines (always sends PUT)...")

            val result = updateEngines(requestId, formData)
            return when (result) {
                is UpdateResult.Success -> StepProcessResult.Success("Engines updated")
                is UpdateResult.Error -> StepProcessResult.Error(result.message)
            }
        }

        // ✅ Check if this is the Owner Info Step
        // Always send PUT request (whether first time or user went back and changed)
        val hasOwnerFields = stepFields.any { it == "owners" }
        if (hasOwnerFields) {
            println("🔍 Detected Owner Info Step - Updating owners (always sends PUT)...")

            val result = updateOwners(requestId, formData)
            return when (result) {
                is UpdateResult.Success -> StepProcessResult.Success("Owners updated")
                is UpdateResult.Error -> StepProcessResult.Error(result.message)
            }
        }

        // No API call needed for this step
        return StepProcessResult.NoAction
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
