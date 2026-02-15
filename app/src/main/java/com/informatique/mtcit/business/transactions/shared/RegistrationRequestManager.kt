package com.informatique.mtcit.business.transactions.shared

import android.content.Context
import com.informatique.mtcit.business.transactions.mapper.RegistrationRequestMapper
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.data.model.EngineFileUpload
import com.informatique.mtcit.data.model.EngineSubmissionRequest
import com.informatique.mtcit.data.model.OwnerFileUpload
import com.informatique.mtcit.data.model.OwnerSubmissionRequest
import com.informatique.mtcit.data.model.UpdateDimensionsRequest
import com.informatique.mtcit.data.model.UpdateWeightsRequest
import com.informatique.mtcit.ui.components.EngineData
import com.informatique.mtcit.data.repository.ShipRegistrationRepository
import com.informatique.mtcit.data.repository.LookupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Shared manager for handling registration request API calls
 * Used by all strategies to avoid duplicating API call logic
 *
 * ✅ NEW: Smart Draft Management
 * - Tracks which steps have been POSTed to avoid duplicate API calls
 * - Compares data to detect changes before calling PUT
 * - Supports draft resume from profile
 */
@Singleton
class RegistrationRequestManager @Inject constructor(
    private val repository: ShipRegistrationRepository,
    private val lookupRepository: LookupRepository,
    private val mapper: RegistrationRequestMapper
) {
    // ✅ Smart Draft Management: Track which steps have been POSTed
    private val postedSteps = mutableSetOf<StepType>()

    // ✅ Smart Draft Management: Store original data for each step to detect changes
    private val stepOriginalData = mutableMapOf<StepType, Map<String, String>>()

    // ✅ Smart Draft Management: Track if this is a draft resume
    private var isDraftResume = false

    /**
     * ✅ Mark a step as already posted (used when loading draft from API)
     * @param stepType The step type to mark
     * @param data The original data from API
     */
    fun markStepAsPosted(stepType: StepType, data: Map<String, String>) {
        postedSteps.add(stepType)
        stepOriginalData[stepType] = data.toMap()
        println("✅ Marked step $stepType as already posted")
    }

    /**
     * ✅ Check if step data has changed compared to original
     * @param stepType The step type to check
     * @param currentData The current form data
     * @return true if data changed, false if unchanged
     */
    private fun hasDataChanged(stepType: StepType, currentData: Map<String, String>): Boolean {
        val original = stepOriginalData[stepType]
        if (original == null) {
            println("🔍 No original data for $stepType, treating as changed")
            return true
        }

        // Debug: Show what we're comparing
        println("🔍 Comparing data for $stepType:")
        println("   📦 Original data: $original")
        println("   📦 Current data:  $currentData")
        println("   📊 Original size: ${original.size}, Current size: ${currentData.size}")

        // ✅ FIX: Normalize both maps for comparison
        // Remove null/empty values and trim strings
        val normalizedOriginal = original.filterValues { it != null && it.toString().isNotBlank() }
            .mapValues { it.value.toString().trim() }
        val normalizedCurrent = currentData
            .filterValues { value -> value != null && value.toString().isNotBlank() }
            .mapValues { (_, value) -> value.toString().trim() }

        // Deep equality check - compare all key-value pairs
        val changed = normalizedCurrent != normalizedOriginal

        if (changed) {
            println("🔄 Step $stepType data CHANGED")
            // Show what changed
            val allKeys = (normalizedOriginal.keys + normalizedCurrent.keys).toSet()
            allKeys.forEach { key ->
                val originalValue = normalizedOriginal[key]
                val currentValue = normalizedCurrent[key]
                if (originalValue != currentValue) {
                    println("   🔸 Key '$key': '$originalValue' → '$currentValue'")
                }
            }
        } else {
            println("⏭️ Step $stepType data UNCHANGED, will skip API call")
        }
        return changed
    }

    /**
     * ✅ Reset tracking for new transaction
     * Should be called when starting a new transaction (not resuming draft)
     */
    fun resetTracking() {
        postedSteps.clear()
        stepOriginalData.clear()
        isDraftResume = false
        println("🔄 RegistrationRequestManager: Tracking reset")
    }

    /**
     * ✅ Enable draft resume mode
     * Should be called when loading a draft request from profile
     */
    fun enableDraftResume() {
        isDraftResume = true
        println("📝 RegistrationRequestManager: Draft resume mode enabled")
    }

    /**
     * ✅ Initialize tracking from API response when loading a draft
     * This populates postedSteps and stepOriginalData so comparison logic works
     *
     * @param formData The restored form data from API response
     */
    fun initializeFromDraft(formData: Map<String, String>) {
        println("📝 RegistrationRequestManager: Initializing from draft data...")
        println("   Form data keys: ${formData.keys}")

        // Determine which steps have data (and thus were posted before)
        // MARINE_UNIT_DATA - Check for unit type (main indicator)
        if (formData.containsKey("unitType") && formData["unitType"]?.isNotEmpty() == true) {
            postedSteps.add(StepType.MARINE_UNIT_DATA)
            // ✅ FIX: Track the EXACT same fields we compare in processStepIfNeeded
            // ⚠️ IMPORTANT: Use the actual form field names (e.g., "mmsi" not "mmsiNumber")
            val relevantFields = listOf(
                "unitType", "unitClassification", "callSign", "imoNumber", "mmsi",
                "registrationPort", "manufacturerYear", "maritimeactivity",
                "proofType", "constructionEndDate", "firstRegistrationDate",
                "registrationCountry", "officialNumber", "buildingMaterial"
            )
            stepOriginalData[StepType.MARINE_UNIT_DATA] = formData.filterKeys {
                it in relevantFields
            }.mapValues { it.value.toString() }.toMutableMap()
            println("   ✅ MARINE_UNIT_DATA marked as posted (has unitType)")
            println("   📦 Tracked fields: ${stepOriginalData[StepType.MARINE_UNIT_DATA]?.keys}")
        }

        // SHIP_DIMENSIONS
        if (formData.containsKey("overallLength") && formData["overallLength"]?.isNotEmpty() == true) {
            postedSteps.add(StepType.SHIP_DIMENSIONS)
            stepOriginalData[StepType.SHIP_DIMENSIONS] = formData.filterKeys {
                it in listOf("overallLength", "overallWidth", "depth", "height", "decksCount")
            }.mapValues { it.value.toString() }.toMutableMap()
            println("   ✅ SHIP_DIMENSIONS marked as posted (has overallLength)")
        }

        // SHIP_WEIGHTS
        if (formData.containsKey("grossTonnage") && formData["grossTonnage"]?.isNotEmpty() == true) {
            postedSteps.add(StepType.SHIP_WEIGHTS)
            stepOriginalData[StepType.SHIP_WEIGHTS] = formData.filterKeys {
                it in listOf("grossTonnage", "netTonnage", "staticLoad", "maxPermittedLoad")
            }.mapValues { it.value.toString() }.toMutableMap()
            println("   ✅ SHIP_WEIGHTS marked as posted (has grossTonnage)")
        }

        // ENGINE_INFO
        if (formData.containsKey("engines") && formData["engines"]?.isNotEmpty() == true) {
            postedSteps.add(StepType.ENGINE_INFO)
            stepOriginalData[StepType.ENGINE_INFO] = formData.filterKeys {
                it == "engines"
            }.mapValues { it.value.toString() }.toMutableMap()
            println("   ✅ ENGINE_INFO marked as posted (has engines)")
        }

        // OWNER_INFO
        if (formData.containsKey("owners") && formData["owners"]?.isNotEmpty() == true) {
            postedSteps.add(StepType.OWNER_INFO)
            stepOriginalData[StepType.OWNER_INFO] = formData.filterKeys {
                it == "owners"
            }.mapValues { it.value.toString() }.toMutableMap()
            println("   ✅ OWNER_INFO marked as posted (has owners)")
        }

        // ✅ DOCUMENTS - Mark as posted if documents were uploaded in the draft
        if (formData.containsKey("hasDocuments") && formData["hasDocuments"] == "true") {
            postedSteps.add(StepType.DOCUMENTS)
            // No need to track document data for comparison
            stepOriginalData[StepType.DOCUMENTS] = mapOf("hasDocuments" to "true")
            println("   ✅ DOCUMENTS marked as posted (documents already uploaded)")
        }

        println("📝 Draft initialization complete:")
        println("   Posted steps: ${postedSteps.map { it.name }}")
        println("   Tracked step data: ${stepOriginalData.keys.map { it.name }}")
    }

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

            println("📤 Sending POST to /registration-requests...")

            // Call POST API
            val result = repository.createRegistrationRequest(request)

            return result.fold(
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

                    // ✅ FIX: Re-throw ApiException to preserve error code for banner display
                    if (exception is ApiException) {
                        throw exception
                    }

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

            println("📤 Sending PUT to /registration-requests/update with requestId=$existingRequestId...")

            // Call PUT API
            val result = repository.updateRegistrationRequest(request)

            return result.fold(
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

                    // ✅ FIX: Re-throw ApiException to preserve error code for banner display
                    if (exception is ApiException) {
                        throw exception
                    }

                    RegistrationRequestResult.Error(exception.message ?: "Unknown error")
                }
            )
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
     * Submit engine - wrapper method that parses engine data and calls submitEnginesWithFiles
     */
    suspend fun submitEngine(
        formData: Map<String, String>,
        requestId: Int,
        context: Context
    ): UpdateResult {
        return try {
            val enginesJson = formData["engines"]
            if (enginesJson.isNullOrEmpty() || enginesJson == "[]") {
                println("⚠️ No engines to submit")
                return UpdateResult.Success
            }

            val engines = parseEnginesFromJson(enginesJson, formData, context)
            val files = parseEngineFilesFromFormData(context, formData)

            submitEnginesWithFiles(context, requestId, engines, files)
        } catch (e: Exception) {
            println("❌ Exception in submitEngine: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Submit owner - wrapper method that parses owner data and calls submitOwnersWithFiles
     */
    suspend fun submitOwner(
        formData: Map<String, String>,
        requestId: Int,
        context: Context
    ): UpdateResult {
        return try {
            val ownersJson = formData["owners"]
            if (ownersJson.isNullOrEmpty() || ownersJson == "[]") {
                println("⚠️ No owners to submit")
                return UpdateResult.Success
            }

            val owners = parseOwnersFromJson(ownersJson, formData, context)
            val files = parseOwnerFilesFromFormData(context, formData)

            submitOwnersWithFiles(context, requestId, owners, files)
        } catch (e: Exception) {
            println("❌ Exception in submitOwner: ${e.message}")
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
     * Update engine immediately (for engines with dbId)
     * PUT /api/v1/registration-requests/{requestId}/engines/{engineId}
     *
     * @param context Android context for file operations
     * @param requestId The registration request ID
     * @param engine Engine data with dbId
     * @return Success or error result
     */
    suspend fun updateEngineImmediate(
        context: Context,
        requestId: Int,
        engine: EngineData
    ): UpdateResult {
        return try {
            val engineId = engine.dbId ?: return UpdateResult.Error("Engine ID not found for update")

            println("🔄 RegistrationRequestManager: Updating engine immediately...")
            println("   - requestId: $requestId")
            println("   - engineId: $engineId")
            println("   - number: ${engine.number}")

            // Parse engine to submission request
            val enginesJson = org.json.JSONArray().apply {
                val jsonObj = org.json.JSONObject()
                jsonObj.put("id", engine.id)
                jsonObj.put("dbId", engine.dbId)
                jsonObj.put("number", engine.number)
                jsonObj.put("type", engine.type)
                jsonObj.put("power", engine.power)
                jsonObj.put("cylinder", engine.cylinder)
                jsonObj.put("manufacturer", engine.manufacturer)
                jsonObj.put("model", engine.model)
                jsonObj.put("manufactureYear", engine.manufactureYear)
                jsonObj.put("productionCountry", engine.productionCountry)
                jsonObj.put("fuelType", engine.fuelType)
                jsonObj.put("condition", engine.condition)
                jsonObj.put("documentUri", engine.documentUri)
                jsonObj.put("documentName", engine.documentName)
                engine.documentRefNum?.let { jsonObj.put("documentRefNum", it) }
                engine.documentFileName?.let { jsonObj.put("documentFileName", it) }
                put(jsonObj)
            }.toString()

            // ✅ Create formData map for parsing
            val formData = mapOf("engines" to enginesJson)

            val engines = parseEnginesFromJson(enginesJson, formData, context)

            // ✅ Parse files from engine data - handles both new uploads and draft documents
            val files = parseEngineFilesFromFormData(context, formData)

            if (engines.isEmpty()) {
                return UpdateResult.Error("Failed to parse engine data")
            }

            val result = repository.updateEngine(context, requestId, engineId, engines[0], files)

            result.fold(
                onSuccess = { response ->
                    if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                        println("✅ Engine updated immediately!")
                        UpdateResult.Success
                    } else {
                        println("❌ API returned error: ${response.message}")
                        UpdateResult.Error(response.message)
                    }
                },
                onFailure = { exception ->
                    println("❌ Failed to update engine: ${exception.message}")
                    exception.printStackTrace()
                    UpdateResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in updateEngineImmediate: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Delete engine immediately (for engines with dbId)
     * DELETE /api/v1/registration-requests/{requestId}/engines/{engineId}
     *
     * @param requestId The registration request ID
     * @param engineId Engine database ID
     * @return Success or error result
     */
    suspend fun deleteEngineImmediate(
        requestId: Int,
        engineId: Int
    ): UpdateResult {
        return try {
            println("🗑️ RegistrationRequestManager: Deleting engine immediately...")
            println("   - requestId: $requestId")
            println("   - engineId: $engineId")

            val result = repository.deleteEngine(requestId, engineId)

            result.fold(
                onSuccess = {
                    println("✅ Engine deleted immediately!")
                    UpdateResult.Success
                },
                onFailure = { exception ->
                    println("❌ Failed to delete engine: ${exception.message}")
                    exception.printStackTrace()
                    UpdateResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in deleteEngineImmediate: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Update owner immediately (for owners with dbId)
     * PUT /api/v1/registration-requests/{requestId}/owners/{ownerId}
     *
     * @param context Android context for file operations
     * @param requestId The registration request ID
     * @param owner Owner data to update
     * @return Success or error result
     */
    suspend fun updateOwnerImmediate(
        context: Context,
        requestId: Int,
        owner: com.informatique.mtcit.ui.components.OwnerData
    ): UpdateResult {
        return try {
            val ownerId = owner.dbId ?: return UpdateResult.Error("Owner ID not found for update")

            println("🔄 RegistrationRequestManager: Updating owner immediately...")
            println("   - requestId: $requestId")
            println("   - ownerId: $ownerId")
            println("   - name: ${owner.ownerName}")

            // Parse owner to submission request
            val ownersJson = org.json.JSONArray().apply {
                val jsonObj = org.json.JSONObject()
                jsonObj.put("id", owner.id)
                jsonObj.put("dbId", owner.dbId)
                jsonObj.put("fullName", owner.fullName)
                jsonObj.put("ownerName", owner.ownerName)
                jsonObj.put("ownerNameEn", owner.ownerNameEn)
                jsonObj.put("nationality", owner.nationality)
                jsonObj.put("idNumber", owner.idNumber)
                jsonObj.put("ownerShipPercentage", owner.ownerShipPercentage)
                jsonObj.put("email", owner.email)
                jsonObj.put("mobile", owner.mobile)
                jsonObj.put("address", owner.address)
                jsonObj.put("city", owner.city)
                jsonObj.put("country", owner.country)
                jsonObj.put("postalCode", owner.postalCode)
                jsonObj.put("isCompany", owner.isCompany)
                jsonObj.put("companyRegistrationNumber", owner.companyRegistrationNumber)
                jsonObj.put("companyName", owner.companyName)
                jsonObj.put("companyType", owner.companyType)
                jsonObj.put("isRepresentative", owner.isRepresentative)

                // Add document info
                if (owner.ownershipProofDocument.isNotEmpty()) {
                    val documentsArray = org.json.JSONArray()
                    val docObj = org.json.JSONObject()
                    docObj.put("documentUri", owner.ownershipProofDocument)
                    owner.ownershipProofDocumentRefNum?.let { docObj.put("documentRefNum", it) }
                    owner.ownershipProofDocumentFileName?.let { docObj.put("documentFileName", it) }
                    docObj.put("docId", 1) // Default document type
                    documentsArray.put(docObj)
                    jsonObj.put("documents", documentsArray)
                }

                put(jsonObj)
            }.toString()

            // ✅ Create formData map for parsing
            val formData = mapOf("owners" to ownersJson)

            val owners = parseOwnersFromJson(ownersJson, formData, context)

            // ✅ Parse files from owner data - handles both new uploads and draft documents
            val files = parseOwnerFilesFromFormData(context, formData)

            if (owners.isEmpty()) {
                return UpdateResult.Error("Failed to parse owner data")
            }

            val result = repository.updateOwner(context, requestId, ownerId, owners[0], files)

            result.fold(
                onSuccess = { response ->
                    if (response.success && response.statusCode == 200) {
                        println("✅ Owner updated immediately!")
                        UpdateResult.Success
                    } else {
                        println("❌ API returned error: ${response.message}")
                        UpdateResult.Error(response.message)
                    }
                },
                onFailure = { exception ->
                    println("❌ Failed to update owner: ${exception.message}")
                    exception.printStackTrace()
                    UpdateResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in updateOwnerImmediate: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Delete owner immediately (for owners with dbId)
     * DELETE /api/v1/registration-requests/{requestId}/owners/{ownerId}
     *
     * @param requestId The registration request ID
     * @param ownerId Owner database ID
     * @return Success or error result
     */
    suspend fun deleteOwnerImmediate(
        requestId: Int,
        ownerId: Int
    ): UpdateResult {
        return try {
            println("🗑️ RegistrationRequestManager: Deleting owner immediately...")
            println("   - requestId: $requestId")
            println("   - ownerId: $ownerId")

            val result = repository.deleteOwner(requestId, ownerId)

            result.fold(
                onSuccess = {
                    println("✅ Owner deleted immediately!")
                    UpdateResult.Success
                },
                onFailure = { exception ->
                    println("❌ Failed to delete owner: ${exception.message}")
                    exception.printStackTrace()
                    UpdateResult.Error(exception.message ?: "Unknown error")
                }
            )
        } catch (e: Exception) {
            println("❌ Exception in deleteOwnerImmediate: ${e.message}")
            e.printStackTrace()
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Validate build status documents (NEW - multipart/form-data with files)
     * Called after documents step when user has uploaded shipbuilding certificate and/or inspection documents
     *
     * POST registration-requests/{requestId}/validate-build-status
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
     * POST registration-requests/{request-id}/send-request
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
     * POST registration-requests/{id}/{name}/shipNameReservtion
     *
     * @param requestId The registration request ID
     * @param marineName The marine/ship name entered by user
     * @return Success or error result
     */
    suspend fun reserveMarineName(
        requestId: String,
        marineName: String,
        marineNameEn: String
    ): UpdateResult {
        return try {
            println("🚀 RegistrationRequestManager: Reserving marine name for requestId=$requestId...")
            println("📤 Marine Name (Arabic): $marineName")
            println("📤 Marine Name (English): $marineNameEn")

            // Validate marine names are not empty
            if (marineName.isBlank()) {
                println("❌ Marine name (Arabic) is empty")
                return UpdateResult.Error("Marine name (Arabic) cannot be empty")
            }

            if (marineNameEn.isBlank()) {
                println("❌ Marine name (English) is empty")
                return UpdateResult.Error("Marine name (English) cannot be empty")
            }

            // Call API
            val result = repository.shipNameReservation(
                requestId = requestId.toInt(),
                marineName = marineName,
                marineNameEn = marineNameEn
            )

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
     * @param stepType The type of the current step
     * @param formData All accumulated form data
     * @param requestTypeId Type of registration (1=Temporary, 2=Permanent, etc.)
     * @param context Android context (needed for engine file uploads)
     * @return Result indicating success or error
     */
    suspend fun processStepIfNeeded(
        stepType: StepType,
        formData: MutableMap<String, String>,
        requestTypeId: String,
        context: Context? = null
    ): StepProcessResult {

        println("🔍 RegistrationRequestManager.processStepIfNeeded called")
        println("   - stepType: $stepType")
        println("   - requestTypeId: $requestTypeId")
        println("   - formData keys: ${formData.keys}")

        // Get requestId from formData (may be null initially)
        val requestId = formData["requestId"]

        return when (stepType) {
            // ✅ Marine Unit Selection Step - Extract requestId from existing ship
            StepType.MARINE_UNIT_SELECTION -> {
                println("🚢 Marine Unit Selection step detected")

                val selectedUnitsJson = formData["selectedMarineUnits"]
                val isAddingNewUnit = formData["isAddingNewUnit"]?.toBoolean() ?: false

                // Check if user selected an existing ship
                if (!selectedUnitsJson.isNullOrEmpty() && selectedUnitsJson != "[]" && !isAddingNewUnit) {
                    println("🔍 User selected EXISTING marine unit - extracting ship ID...")

                    try {
                        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        val unitsArray = json.parseToJsonElement(selectedUnitsJson).jsonArray

                        if (unitsArray.isNotEmpty()) {
                            val firstElement = unitsArray[0]
                            var shipId: String? = null

                            // Handle two formats: ["192"] or [{"id": "192", ...}]
                            if (firstElement is kotlinx.serialization.json.JsonPrimitive) {
                                shipId = firstElement.content
                                println("🔍 Format 1: Simple ID array -> $shipId")
                                // ✅ Use shipId as shipInfoId when no explicit shipInfoId
                                if (shipId != null) {
                                    formData["shipInfoId"] = shipId
                                    println("✅ Using shipId as shipInfoId: $shipId")
                                }
                            } else if (firstElement is kotlinx.serialization.json.JsonObject) {
                                val selectedUnit = firstElement.jsonObject
                                shipId = selectedUnit["id"]?.jsonPrimitive?.content
                                println("🔍 Format 2: Object array -> $shipId")

                                // Extract shipInfoId if available
                                val explicitShipInfoId = selectedUnit["shipInfoId"]?.jsonPrimitive?.content
                                if (explicitShipInfoId != null) {
                                    formData["shipInfoId"] = explicitShipInfoId
                                    println("✅ Found explicit shipInfoId: $explicitShipInfoId")
                                } else if (shipId != null) {
                                    // ✅ Fallback: Use shipId as shipInfoId
                                    formData["shipInfoId"] = shipId
                                    println("✅ Using shipId as shipInfoId: $shipId")
                                }
                            }

                            if (shipId != null) {
                                println("✅ Using existing ship ID as requestId: $shipId")
                                formData["requestId"] = shipId
                                formData["shipId"] = shipId
                                return StepProcessResult.Success("Using existing marine unit (ID: $shipId)")
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Error parsing selectedMarineUnits: ${e.message}")
                        e.printStackTrace()
                    }
                } else if (isAddingNewUnit) {
                    println("🔍 User is adding NEW marine unit - will create registration request later")
                    return StepProcessResult.NoAction
                }

                StepProcessResult.NoAction
            }

            // ✅ Unit Selection Step (Ship Info) - Create registration request for NEW ship
            StepType.MARINE_UNIT_DATA -> {
                // Check if this is unit selection step by looking for unitType field
                if (formData.containsKey("unitType")) {
                    println("🚢 Unit Selection step detected (NEW ship)")

                    // Check if user is adding new unit (not selecting existing)
                    val selectedUnitsJson = formData["selectedMarineUnits"]
                    val isAddingNewUnit = formData["isAddingNewUnit"]?.toBoolean() ?: false

                    when {
                        isAddingNewUnit || selectedUnitsJson == "[]" -> {
                            // ✅ SMART DRAFT: Check if step already posted
                            if (postedSteps.contains(StepType.MARINE_UNIT_DATA)) {
                                println("🔍 Step MARINE_UNIT_DATA already posted, checking for changes...")

                                // Extract only relevant fields for comparison
                                // ✅ FIX: Use the EXACT form field names (mmsi not mmsiNumber, maritimeactivity not maritimeActivity)
                                val relevantFields = listOf(
                                    "unitType", "unitClassification", "callSign", "imoNumber", "mmsi",
                                    "registrationPort", "manufacturerYear", "maritimeactivity",
                                    "proofType", "constructionEndDate", "firstRegistrationDate",
                                    "registrationCountry", "officialNumber", "buildingMaterial"
                                )

                                val currentStepData = formData.filterKeys { key ->
                                    key in relevantFields
                                }.mapValues { it.value.toString() } // Ensure string values

                                println("🔍 DEBUG: Current step data keys: ${currentStepData.keys}")
                                println("🔍 DEBUG: Current step data: $currentStepData")

                                if (!hasDataChanged(StepType.MARINE_UNIT_DATA, currentStepData)) {
                                    // Data unchanged → Skip API call
                                    println("⏭️ MARINE_UNIT_DATA unchanged, skipping API call")
                                    return StepProcessResult.NoAction
                                }

                                // Data changed → Call API (will use PUT because requestId exists)
                                println("🔄 Updating registration request (data changed)...")
                                val result = createOrUpdateRegistrationRequest(formData, requestTypeId.toInt())

                                when (result) {
                                    is RegistrationRequestResult.Success -> {
                                        println("✅ Registration request updated successfully")
                                        formData["requestId"] = result.requestId
                                        result.shipInfoId?.let { formData["shipInfoId"] = it }
                                        result.shipId?.let { formData["shipId"] = it }

                                        // Update original data
                                        stepOriginalData[StepType.MARINE_UNIT_DATA] = currentStepData.toMap()

                                        StepProcessResult.Success("Registration request updated: ${result.requestId}")
                                    }
                                    is RegistrationRequestResult.Error -> {
                                        println("❌ Failed to update registration request: ${result.message}")
                                        StepProcessResult.Error(result.message)
                                    }
                                }
                            } else {
                                // First time → POST
                                println("🔍 Creating registration request for NEW ship (POST)...")

                                val result = createOrUpdateRegistrationRequest(formData, requestTypeId.toInt())

                                when (result) {
                                    is RegistrationRequestResult.Success -> {
                                        println("✅ Registration request created successfully")
                                        formData["requestId"] = result.requestId
                                        result.shipInfoId?.let { formData["shipInfoId"] = it }
                                        result.shipId?.let { formData["shipId"] = it }

                                        // Mark as posted and save original data
                                        postedSteps.add(StepType.MARINE_UNIT_DATA)
                                        val relevantFields = listOf(
                                            "unitType", "unitClassification", "callSign", "imoNumber", "mmsi",
                                            "registrationPort", "manufacturerYear", "maritimeactivity",
                                            "proofType", "constructionEndDate", "firstRegistrationDate",
                                            "registrationCountry", "officialNumber", "buildingMaterial"
                                        )
                                        val currentStepData = formData.filterKeys { key ->
                                            key in relevantFields
                                        }.mapValues { it.value.toString() }
                                        stepOriginalData[StepType.MARINE_UNIT_DATA] = currentStepData.toMap()
                                        println("✅ Saved original data for MARINE_UNIT_DATA: ${stepOriginalData[StepType.MARINE_UNIT_DATA]?.keys}")

                                        StepProcessResult.Success("Registration request created: ${result.requestId}")
                                    }
                                    is RegistrationRequestResult.Error -> {
                                        println("❌ Failed to create registration request: ${result.message}")
                                        StepProcessResult.Error(result.message)
                                    }
                                }
                            }
                        }
                        else -> StepProcessResult.NoAction
                    }
                } else {
                    StepProcessResult.NoAction
                }
            }

            // ✅ Dimensions Step - Update dimensions
            StepType.SHIP_DIMENSIONS -> {
                println("📏 Ship Dimensions step detected")

                if (requestId == null) {
                    println("⚠️ No requestId - skipping dimensions update")
                    return StepProcessResult.NoAction
                }

                // ✅ SMART DRAFT: Check if step already posted
                if (postedSteps.contains(StepType.SHIP_DIMENSIONS)) {
                    println("🔍 Step SHIP_DIMENSIONS already posted, checking for changes...")

                    // Extract only dimension fields for comparison
                    val currentStepData = formData.filterKeys { key ->
                        key in listOf("overallLength", "overallWidth", "depth", "height", "decksCount")
                    }

                    if (!hasDataChanged(StepType.SHIP_DIMENSIONS, currentStepData)) {
                        // Data unchanged → Skip API call
                        println("⏭️ SHIP_DIMENSIONS unchanged, skipping API call")
                        return StepProcessResult.NoAction
                    }

                    // Data changed → Update via API
                    println("🔄 Updating dimensions (data changed)...")
                }

                val result = updateDimensions(requestId, formData)

                when (result) {
                    is UpdateResult.Success -> {
                        println("✅ Dimensions updated successfully")

                        // ✅ SMART DRAFT: Mark as posted and save original data
                        postedSteps.add(StepType.SHIP_DIMENSIONS)
                        val currentStepData = formData.filterKeys { key ->
                            key in listOf("overallLength", "overallWidth", "depth", "height", "decksCount")
                        }
                        stepOriginalData[StepType.SHIP_DIMENSIONS] = currentStepData.toMap()

                        StepProcessResult.Success("Dimensions updated")
                    }
                    is UpdateResult.Error -> {
                        println("❌ Failed to update dimensions: ${result.message}")
                        StepProcessResult.Error(result.message)
                    }
                }
            }

            // ✅ Weights Step - Update weights
            StepType.SHIP_WEIGHTS -> {
                println("⚖️ Ship Weights step detected")

                if (requestId == null) {
                    println("⚠️ No requestId - skipping weights update")
                    return StepProcessResult.NoAction
                }

                // ✅ SMART DRAFT: Check if step already posted
                if (postedSteps.contains(StepType.SHIP_WEIGHTS)) {
                    println("🔍 Step SHIP_WEIGHTS already posted, checking for changes...")

                    println("🔍 DEBUG: Raw formData for SHIP_WEIGHTS:")
                    formData.forEach { (k, v) ->
                        if (k in listOf("grossTonnage", "netTonnage", "staticLoad", "maxPermittedLoad")) {
                            println("   - $k = '$v' (type: ${v::class.simpleName})")
                        }
                    }

                    // Extract only weight fields for comparison
                    val currentStepData = formData.filterKeys { key ->
                        key in listOf("grossTonnage", "netTonnage", "staticLoad", "maxPermittedLoad")
                    }

                    println("🔍 DEBUG: Filtered currentStepData: $currentStepData")

                    if (!hasDataChanged(StepType.SHIP_WEIGHTS, currentStepData)) {
                        // Data unchanged → Skip API call
                        println("⏭️ SHIP_WEIGHTS unchanged, skipping API call")
                        return StepProcessResult.NoAction
                    }

                    // Data changed → Update via API
                    println("🔄 Updating weights (data changed)...")
                }

                val result = updateWeights(requestId, formData)

                when (result) {
                    is UpdateResult.Success -> {
                        println("✅ Weights updated successfully")

                        // ✅ SMART DRAFT: Mark as posted and save original data
                        postedSteps.add(StepType.SHIP_WEIGHTS)
                        val currentStepData = formData.filterKeys { key ->
                            key in listOf("grossTonnage", "netTonnage", "staticLoad", "maxPermittedLoad")
                        }
                        stepOriginalData[StepType.SHIP_WEIGHTS] = currentStepData.toMap()

                        StepProcessResult.Success("Weights updated")
                    }
                    is UpdateResult.Error -> {
                        println("❌ Failed to update weights: ${result.message}")
                        StepProcessResult.Error(result.message)
                    }
                }
            }

            // ✅ Engine Info Step - Smart handling: Add NEW / Update EXISTING / Delete
            // When step is already posted (from draft or previous visit):
            // - POST NEW engines (no dbId)
            // - PUT EXISTING engines (has dbId) if data changed
            // - DELETE removed engines (handled by comparing with API)
            StepType.ENGINE_INFO -> {
                println("🔧 Engine Info step detected")

                if (requestId == null) {
                    println("⚠️ No requestId - skipping engine submission")
                    return StepProcessResult.NoAction
                }

                if (context == null) {
                    println("⚠️ No context - cannot upload engine files")
                    return StepProcessResult.Error("Context required for engine file upload")
                }

                // ✅ Parse engines from form data
                val enginesJsonStr = formData["engines"] ?: "[]"
                val allEnginesList = parseEnginesFromFormData(enginesJsonStr)

                // ✅ Check if engines data has changed since last submission
                val originalEnginesJson = stepOriginalData[StepType.ENGINE_INFO]?.get("engines") ?: "[]"
                val hasChanged = enginesJsonStr != originalEnginesJson

                println("📊 Engine submission:")
                println("   - Total engines in form: ${allEnginesList.size}")
                println("   - Data changed since last submit: $hasChanged")

                // ✅ Skip API call if data hasn't changed
                if (!hasChanged && postedSteps.contains(StepType.ENGINE_INFO)) {
                    println("⏭️ Engine data unchanged - skipping API call")
                    return StepProcessResult.NoAction
                }

                println("   - All engines will be POSTED (new and edited)")

                // ✅ POST ALL engines (both new and edited)
                // Delete is handled via immediate API calls from UI buttons
                if (allEnginesList.isEmpty()) {
                    println("✅ No engines to submit, proceeding to next step")
                    return StepProcessResult.NoAction
                }

                println("➕ Posting ${allEnginesList.size} engine(s)...")
                val enginesJson = org.json.JSONArray().apply {
                    allEnginesList.forEach { engine ->
                        val jsonObj = org.json.JSONObject()
                        val dbId = engine["dbId"] // Get dbId if it exists

                        engine.forEach { (key, value) ->
                            if (key != "dbId") { // Exclude dbId from submission
                                jsonObj.put(key, value)
                            }
                        }

                        // ✅ For edited engines, add "id" field with dbId value
                        if (dbId != null) {
                            val idValue = when (dbId) {
                                is String -> dbId.toIntOrNull()
                                is Int -> dbId
                                is Number -> dbId.toInt()
                                else -> null
                            }
                            if (idValue != null) {
                                jsonObj.put("id", idValue)
                                println("   ✅ Engine with id=$idValue (edited)")
                            }
                        } else {
                            println("   ➕ New engine (no id)")
                        }

                        put(jsonObj)
                    }
                }.toString()

                val newFormData = formData.toMutableMap().apply {
                    put("engines", enginesJson)
                }

                val result = submitEngine(newFormData, requestId.toInt(), context)
                when (result) {
                    is UpdateResult.Success -> {
                        println("✅ New engine(s) submitted successfully")
                        // ✅ Save original data for change detection
                        stepOriginalData[StepType.ENGINE_INFO] = mapOf("engines" to enginesJson)
                        postedSteps.add(StepType.ENGINE_INFO)
                        println("✅ Saved original engine data for change detection")
                        StepProcessResult.Success("Engine(s) added")
                    }
                    is UpdateResult.Error -> {
                        println("❌ Failed to submit new engines: ${result.message}")
                        StepProcessResult.Error(result.message)
                    }
                }
            }

            // ✅ Owner Info Step - Simplified: Only POST new owners
            // Edit/Delete are handled via immediate API calls from UI buttons
            // Documents are loaded from draft via RequestDataConverter
            StepType.OWNER_INFO -> {
                println("👤 Owner Info step detected")

                if (requestId == null) {
                    println("⚠️ No requestId - skipping owner submission")
                    return StepProcessResult.NoAction
                }

                if (context == null) {
                    println("⚠️ No context - cannot upload owner files")
                    return StepProcessResult.Error("Context required for owner file upload")
                }

                // Parse owners from form data
                val ownersJsonStr = formData["owners"] ?: "[]"
                val allOwnersList = if (ownersJsonStr.isNotEmpty() && ownersJsonStr != "[]") {
                    parseOwnersFromFormData(ownersJsonStr)
                } else {
                    emptyList()
                }

                // ✅ Check if owners data has changed since last submission
                val originalOwnersJson = stepOriginalData[StepType.OWNER_INFO]?.get("owners") ?: "[]"
                val hasChanged = ownersJsonStr != originalOwnersJson

                println("📊 Owner submission:")
                println("   - Total owners in form: ${allOwnersList.size}")
                println("   - Data changed since last submit: $hasChanged")

                // ✅ Skip API call if data hasn't changed
                if (!hasChanged && postedSteps.contains(StepType.OWNER_INFO)) {
                    println("⏭️ Owner data unchanged - skipping API call")
                    return StepProcessResult.NoAction
                }

                println("   - All owners will be POSTED (new and edited)")

                // ✅ POST ALL owners (both new and edited)
                // Delete is handled via immediate API calls from UI buttons
                if (allOwnersList.isEmpty()) {
                    println("✅ No owners to submit, proceeding to next step")
                    return StepProcessResult.NoAction
                }

                println("➕ Posting ${allOwnersList.size} owner(s)...")
                val ownersJson = org.json.JSONArray().apply {
                    allOwnersList.forEach { owner ->
                        val jsonObj = org.json.JSONObject()
                        val dbId = owner["dbId"] // Get dbId if it exists

                        owner.forEach { (key, value) ->
                            if (key != "dbId") { // Exclude dbId from submission
                                jsonObj.put(key, value)
                            }
                        }

                        // ✅ For edited owners, add "id" field with dbId value
                        if (dbId != null) {
                            val idValue = when (dbId) {
                                is String -> dbId.toIntOrNull()
                                is Int -> dbId
                                is Number -> dbId.toInt()
                                else -> null
                            }
                            if (idValue != null) {
                                jsonObj.put("id", idValue)
                                println("   ✅ Owner with id=$idValue (edited)")
                            }
                        } else {
                            println("   ➕ New owner (no id)")
                        }

                        put(jsonObj)
                    }
                }.toString()

                val newFormData = formData.toMutableMap().apply {
                    put("owners", ownersJson)
                }

                val result = submitOwner(newFormData, requestId.toInt(), context)
                when (result) {
                    is UpdateResult.Success -> {
                        println("✅ New owner(s) submitted successfully")
                        // ✅ Save original data for change detection
                        stepOriginalData[StepType.OWNER_INFO] = mapOf("owners" to ownersJson)
                        postedSteps.add(StepType.OWNER_INFO)
                        println("✅ Saved original owner data for change detection")
                        StepProcessResult.Success("Owner(s) added")
                    }
                    is UpdateResult.Error -> {
                        println("❌ Failed to submit new owners: ${result.message}")
                        StepProcessResult.Error(result.message)
                    }
                }
            }

            // ✅ Maritime Identification Step - Add IMO, MMSI, Call Sign
            StepType.MARITIME_IDENTIFICATION -> {
                println("=" .repeat(80))
                println("🚢 MARITIME IDENTIFICATION STEP DETECTED")
                println("=" .repeat(80))
                println("📋 Current formData keys: ${formData.keys}")
                println("📋 All formData values:")
                formData.forEach { (key, value) ->
                    println("   $key = $value")
                }

                println("🔍 Ship ID candidates:")
                println("   shipId = ${formData["shipId"]}")
                println("   shipInfoId = ${formData["shipInfoId"]}")
                println("   selectedMarineUnits = ${formData["selectedMarineUnits"]}")

                // ✅ Clean the shipId by removing brackets and quotes
                val cleanShipId = formData["shipId"]?.trim()?.removeSurrounding("[", "]")?.trim()?.removeSurrounding("\"", "\"")?.toIntOrNull()
                    ?: formData["shipInfoId"]?.trim()?.removeSurrounding("[", "]")?.trim()?.removeSurrounding("\"", "\"")?.toIntOrNull()
                    ?: formData["selectedMarineUnits"]?.trim()?.removeSurrounding("[", "]")?.trim()?.removeSurrounding("\"", "\"")?.toIntOrNull()

                println("   Cleaned shipId = $cleanShipId")
                println("   Final shipId = $cleanShipId")

                if (cleanShipId == null) {
                    println("❌ No valid shipId found in formData after cleaning - skipping maritime identity update")
                    println("=" .repeat(80))
                    return StepProcessResult.NoAction
                }

                // Extract the values from form data
                // ✅ FIX: Use "mmsi" not "mmsiNumber" - matches the form field name
                val imoNumber = formData["imoNumber"]?.takeIf { it.isNotBlank() && it != "null" }
                val mmsiNumber = formData["mmsi"]?.takeIf { it.isNotBlank() && it != "null" }
                val callSign = formData["callSign"]?.takeIf { it.isNotBlank() && it != "null" }

                println("📝 Maritime identity fields:")
                println("   IMO Number: ${imoNumber ?: "(empty or unchanged)"}")
                println("   MMSI Number: ${mmsiNumber ?: "(empty or unchanged)"}")
                println("   Call Sign: ${callSign ?: "(empty or unchanged)"}")

                // Only call API if at least one field has a value
                if (imoNumber == null && mmsiNumber == null && callSign == null) {
                    println("⚠️ No maritime identity data to update - all fields are empty")
                    println("=" .repeat(80))
                    return StepProcessResult.NoAction
                }

                println("🚀 CALLING API: addMaritimeIdentity")
                println("   Ship ID: $cleanShipId")
                println("   IMO: ${imoNumber ?: "(not sending)"}")
                println("   MMSI: ${mmsiNumber ?: "(not sending)"}")
                println("   Call Sign: ${callSign ?: "(not sending)"}")
                println("=" .repeat(80))

                val result = repository.addMaritimeIdentity(
                    shipId = cleanShipId,
                    imoNumber = imoNumber,
                    mmsiNumber = mmsiNumber,
                    callSign = callSign
                )

                result.fold(
                    onSuccess = { response ->
                        println("=" .repeat(80))
                        if (response.success && response.statusCode in 200..201) {
                            println("✅ ✅ ✅ MARITIME IDENTITY UPDATED SUCCESSFULLY! ✅ ✅ ✅")
                            println("   Response: $response")
                            println("=" .repeat(80))
                            StepProcessResult.Success("Maritime identity updated")
                        } else {
                            println("❌ API RETURNED ERROR")
                            println("   Status Code: ${response.statusCode}")
                            println("   Message: ${response.message}")
                            println("=" .repeat(80))
                            StepProcessResult.Error(response.message)
                        }
                    },
                    onFailure = { exception ->
                        println("=" .repeat(80))
                        println("❌ ❌ ❌ EXCEPTION IN MARITIME IDENTITY UPDATE ❌ ❌ ❌")
                        println("   Exception: ${exception.message}")
                        exception.printStackTrace()
                        println("=" .repeat(80))
                        StepProcessResult.Error(exception.message ?: "Failed to update maritime identity")
                    }
                )
            }

            // ✅ Documents Step - Upload dynamic documents
            StepType.DOCUMENTS -> {
                println("📄 Documents step detected")

                // ✅ SMART DRAFT: Check if documents already uploaded
                if (postedSteps.contains(StepType.DOCUMENTS)) {
                    println("⏭️ DOCUMENTS already uploaded in draft, skipping API call")
                    return StepProcessResult.NoAction
                }

                if (requestId == null) {
                    println("⚠️ No requestId - skipping document upload")
                    return StepProcessResult.NoAction
                }

                if (context == null) {
                    println("⚠️ No context - cannot upload documents")
                    return StepProcessResult.Error("Context required for document upload")
                }

                // Parse dynamic documents from formData
                val documents = parseDocumentsFromFormData(context, formData)

                if (documents.isEmpty()) {
                    println("⚠️ No documents to upload")
                    return StepProcessResult.NoAction
                }

                println("📤 Uploading ${documents.size} documents...")
                println("🔍 Request Type ID: $requestTypeId")

                // ✅ Choose the correct API endpoint based on requestTypeId
                // requestTypeId "1" = Temporary Registration
                // requestTypeId "2" = Permanent Registration
                val result = if (requestTypeId == "2") {
                    println("📄 Using PERMANENT registration API endpoint")
                    repository.validatePermanentBuildStatusWithDocuments(requestId.toInt(), documents)
                } else {
                    println("📄 Using TEMPORARY registration API endpoint")
                    repository.validateBuildStatusWithDocuments(requestId.toInt(), documents)
                }

                result.fold(
                    onSuccess = { response ->
                        if (response.success && (response.statusCode == 200 || response.statusCode == 201)) {
                            println("✅ Documents uploaded successfully!")
                            StepProcessResult.Success("Documents uploaded successfully")
                        } else {
                            println("❌ Document upload failed: ${response.message}")
                            StepProcessResult.Error(response.message)
                        }
                    },
                    onFailure = { exception ->
                        println("❌ Failed to upload documents: ${exception.message}")
                        exception.printStackTrace()
                        StepProcessResult.Error(exception.message ?: "Failed to upload documents")
                    }
                )
            }

            // ✅ Review Step - NO LONGER HANDLED HERE
            // Each strategy should handle review in their own processStepData()
            // by calling marineUnitsApiService.sendTransactionRequest() with their specific endpoint
            StepType.REVIEW -> {
                println("📋 Review step detected - but NOT handled by RegistrationRequestManager")
                println("💡 Each strategy should handle review step in their processStepData()")
                StepProcessResult.NoAction
            }

            // ✅ Marine Unit Name Selection Step - Reserve ship name
            StepType.MARINE_UNIT_NAME_SELECTION -> {
                println("🏷️ Marine Unit Name Selection step detected")

                // ✅ Extract both Arabic and English names
                val marineName = formData["marineUnitName"]
                val marineNameEn = formData["marineUnitNameEn"]

                if (marineName.isNullOrBlank()) {
                    println("⚠️ Marine name (Arabic) is empty - skipping reservation")
                    return StepProcessResult.Error("Marine name (Arabic) is required")
                }

                if (marineNameEn.isNullOrBlank()) {
                    println("⚠️ Marine name (English) is empty - skipping reservation")
                    return StepProcessResult.Error("Marine name (English) is required")
                }

                if (requestId == null) {
                    println("❌ No requestId - cannot reserve name")
                    return StepProcessResult.Error("No request ID available")
                }

                println("✅ Marine names found:")
                println("   Arabic: '$marineName'")
                println("   English: '$marineNameEn'")
                println("   Proceeding with reservation...")

                try {
                    val result = reserveMarineName(
                        requestId = requestId,
                        marineName = marineName,
                        marineNameEn = marineNameEn
                    )

                    when (result) {
                        is UpdateResult.Success -> {
                            println("✅ Marine name reserved successfully")
                            StepProcessResult.Success("Marine name reserved: $marineName / $marineNameEn")
                        }
                        is UpdateResult.Error -> {
                            println("❌ Failed to reserve marine name: ${result.message}")
                            StepProcessResult.Error(result.message)
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Error reserving marine name: ${e.message}")
                    e.printStackTrace()
                    StepProcessResult.Error("Failed to reserve marine name: ${e.message}")
                }
            }

            else -> {
                println("ℹ️ Step type $stepType - no registration action needed")
                StepProcessResult.NoAction
            }
        }
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

                    // ✅ Extract id or dbId for PUT requests (will be null for POST)
                    // Check both "id" (new format) and "dbId" (internal format) fields
                    val dbId = engineObj["id"]?.jsonPrimitive?.intOrNull
                        ?: engineObj["dbId"]?.jsonPrimitive?.intOrNull

                    if (dbId != null) {
                        println("   ✅ Engine $index: Extracted id=$dbId from JSON")
                    }

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
                            val documentRefNum = docObj["documentRefNum"]?.jsonPrimitive?.content
                            val documentFileName = docObj["documentFileName"]?.jsonPrimitive?.content
                            val docId = docObj["docId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                            if (documentUri.isNotEmpty()) {
                                // ✅ Check if this is a draft document (format: "draft:refNum")
                                if (documentUri.startsWith("draft:") && documentRefNum != null) {
                                    // Draft document - include docRefNum AND file will be downloaded & re-uploaded
                                    println("📎 Engine draft document detected: $documentRefNum")
                                    val metadata = com.informatique.mtcit.data.model.EngineDocumentMetadata(
                                        fileName = documentFileName ?: "document.jpg",
                                        docOwnerId = docOwnerId,
                                        docId = docId,
                                        docRefNum = documentRefNum // ✅ Include actual refNum
                                    )
                                    println("📎 Created engine metadata with docRefNum: $documentRefNum (will also re-upload file)")
                                    documents.add(metadata)
                                } else {
                                    // New document - get fileName from URI
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
                        }
                    } else {
                        // ✅ BACKWARD COMPATIBILITY: Support old format with single documentUri
                        val documentUri = engineObj["documentUri"]?.jsonPrimitive?.content ?: ""
                        val documentRefNum = engineObj["documentRefNum"]?.jsonPrimitive?.content
                        val documentFileName = engineObj["documentFileName"]?.jsonPrimitive?.content

                        if (documentUri.isNotEmpty()) {
                            // ✅ Check if this is a draft document (format: "draft:refNum")
                            if (documentUri.startsWith("draft:") && documentRefNum != null) {
                                // Draft document - include docRefNum AND file will be downloaded & re-uploaded
                                println("📎 Draft document detected: $documentRefNum")
                                val metadata = com.informatique.mtcit.data.model.EngineDocumentMetadata(
                                    fileName = documentFileName ?: "document.jpg",
                                    docOwnerId = docOwnerId,
                                    docId = 1,
                                    docRefNum = documentRefNum // ✅ Include actual refNum
                                )
                                println("📎 Created metadata with docRefNum: $documentRefNum (will also re-upload file)")
                                documents.add(metadata)
                            } else {
                                // New document - upload file
                                val uri = documentUri.toUri()
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
                    }

                    // ✅ Look up full lookup objects by Arabic name
                    val engineType = engineTypes.find { it.nameAr == engineTypeArabic }
                    val country = countries.find { it.nameAr == engineCountryArabic }
                    val fuelType = fuelTypes.find { it.nameAr == engineFuelTypeArabic }
                    val status = engineStatuses.find { it.nameAr == engineConditionArabic }

                    EngineSubmissionRequest(
                        id = dbId, // ✅ Include engine ID for PUT requests (null for POST)
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
                            if (documentUri.startsWith("draft:")) {
                                // ✅ Draft document in UPDATE - need to download and re-upload it
                                // Extract refNum from "draft:refNum"
                                val refNum = documentUri.removePrefix("draft:")
                                println("📎 Draft document in UPDATE: $refNum - downloading to re-upload...")

                                try {
                                    // Get file URL from API
                                    val fileUrlResult = repository.getFilePreview(refNum)
                                    fileUrlResult.fold(
                                        onSuccess = { fileUrl ->
                                            println("✅ Got file URL: $fileUrl")

                                            // Download file from URL
                                            val url = java.net.URL(fileUrl)
                                            val connection = url.openConnection() as java.net.HttpURLConnection
                                            connection.connect()

                                            val fileBytes = connection.inputStream.use { it.readBytes() }
                                            println("✅ Downloaded ${fileBytes.size} bytes")

                                            // Get filename from engine data or URL
                                            val fileName = engineObj["documentFileName"]?.jsonPrimitive?.contentOrNull
                                                ?: android.net.Uri.parse(fileUrl).lastPathSegment?.substringBefore('?')
                                                ?: "document.jpg"

                                            // Detect MIME type from filename
                                            val mimeType = when {
                                                fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                                                fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                                                else -> "application/octet-stream"
                                            }

                                            files.add(
                                                EngineFileUpload(
                                                    fileName = fileName,
                                                    fileUri = fileUrl, // URL as reference
                                                    fileBytes = fileBytes, // Actual bytes
                                                    mimeType = mimeType,
                                                    docOwnerId = docOwnerId,
                                                    docId = 1
                                                )
                                            )
                                            println("📎 Re-uploaded draft document: $fileName (${fileBytes.size} bytes)")
                                        },
                                        onFailure = { error ->
                                            println("❌ Failed to download draft document: ${error.message}")
                                        }
                                    )
                                } catch (e: Exception) {
                                    println("❌ Exception downloading draft document: ${e.message}")
                                    e.printStackTrace()
                                }
                            } else {
                                // New document - upload file normally
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
        formData: Map<String, String>,
        context: Context
    ): List<OwnerSubmissionRequest> {
        return withContext(Dispatchers.IO) {
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val ownersArray = json.parseToJsonElement(ownersJson).jsonArray

                ownersArray.mapIndexed { index, ownerElement ->
                    val ownerObj = ownerElement.jsonObject
                    val docOwnerId = "owner${index + 1}"

                    // ✅ Extract id for PUT requests (will be null for POST)
                    // Note: We look for "id" not "dbId" because that's what's added to JSON in processStepIfNeeded
                    val dbId = ownerObj["id"]?.jsonPrimitive?.intOrNull

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
                            val documentRefNum = docObj["documentRefNum"]?.jsonPrimitive?.content
                            val documentFileName = docObj["documentFileName"]?.jsonPrimitive?.content
                            val docId = docObj["docId"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

                            if (documentUri.isNotEmpty()) {
                                // ✅ Check if this is a draft document
                                if (documentUri.startsWith("draft:") && documentRefNum != null) {
                                    // Draft document - include metadata with docRefNum
                                    println("📎 Owner draft document detected: $documentRefNum")
                                    documents.add(
                                        com.informatique.mtcit.data.model.OwnerDocumentMetadata(
                                            fileName = documentFileName ?: "document.jpg",
                                            docOwnerId = docOwnerId,
                                            docId = docId,
                                            docRefNum = documentRefNum // ✅ Include refNum
                                        )
                                    )
                                } else {
                                    // New document - will be uploaded
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
                        }
                    } else {
                        // Check for single ownershipProofDocument field (backward compatibility)
                        val ownershipProofDocument = ownerObj["ownershipProofDocument"]?.jsonPrimitive?.content
                        val ownershipProofDocumentRefNum = ownerObj["ownershipProofDocumentRefNum"]?.jsonPrimitive?.content
                        val ownershipProofDocumentFileName = ownerObj["ownershipProofDocumentFileName"]?.jsonPrimitive?.content

                        if (!ownershipProofDocument.isNullOrEmpty()) {
                            // Check if this is a draft document
                            if (ownershipProofDocument.startsWith("draft:") && !ownershipProofDocumentRefNum.isNullOrBlank()) {
                                // Draft document - include metadata with docRefNum
                                println("📎 Owner draft document (single field) detected: $ownershipProofDocumentRefNum")
                                documents.add(
                                    com.informatique.mtcit.data.model.OwnerDocumentMetadata(
                                        fileName = ownershipProofDocumentFileName ?: "document.jpg",
                                        docOwnerId = docOwnerId,
                                        docId = 1, // Default document type
                                        docRefNum = ownershipProofDocumentRefNum // ✅ Include refNum
                                    )
                                )
                            } else {
                                // New local document - will be uploaded
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
                    }

                    // ✅ Log final owner submission data
                    println("📋 Creating OwnerSubmissionRequest:")
                    println("   - dbId: $dbId")
                    println("   - ownerName: ${if (ownerName.isNotEmpty()) ownerName else fullName}")
                    println("   - ownershipPercentage: $ownerShipPercentage")
                    println("   - documents: ${documents.size}")
                    documents.forEach { doc ->
                        println("     📎 Doc: ${doc.fileName}, refNum: ${doc.docRefNum}")
                    }

                    OwnerSubmissionRequest(
                        id = dbId, // ✅ Include owner ID for PUT requests (null for POST)
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
                                if (documentUri.startsWith("draft:")) {
                                    // ✅ Draft document in UPDATE - download and re-upload
                                    val refNum = documentUri.removePrefix("draft:")
                                    println("📎 Owner draft document in UPDATE: $refNum - downloading to re-upload...")

                                    try {
                                        // Get file URL from API
                                        val fileUrlResult = repository.getFilePreview(refNum)
                                        fileUrlResult.fold(
                                            onSuccess = { fileUrl ->
                                                println("✅ Got owner file URL: $fileUrl")

                                                // Download file from URL
                                                val url = java.net.URL(fileUrl)
                                                val connection = url.openConnection() as java.net.HttpURLConnection
                                                connection.connect()

                                                val fileBytes = connection.inputStream.use { it.readBytes() }
                                                println("✅ Downloaded ${fileBytes.size} bytes")

                                                // Get filename
                                                val fileName = docObj["documentFileName"]?.jsonPrimitive?.contentOrNull
                                                    ?: android.net.Uri.parse(fileUrl).lastPathSegment?.substringBefore('?')
                                                    ?: "document.jpg"

                                                // Detect MIME type
                                                val mimeType = when {
                                                    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                                                    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                                    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                                                    else -> "application/octet-stream"
                                                }

                                                files.add(
                                                    OwnerFileUpload(
                                                        fileName = fileName,
                                                        fileUri = fileUrl,
                                                        fileBytes = fileBytes,
                                                        mimeType = mimeType,
                                                        docOwnerId = docOwnerId,
                                                        docId = docId
                                                    )
                                                )
                                                println("📎 Re-uploaded owner draft document: $fileName (${fileBytes.size} bytes)")
                                            },
                                            onFailure = { error ->
                                                println("❌ Failed to download owner draft document: ${error.message}")
                                            }
                                        )
                                    } catch (e: Exception) {
                                        println("❌ Exception downloading owner draft document: ${e.message}")
                                        e.printStackTrace()
                                    }
                                } else {
                                    // New document - upload file normally
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
                        }
                    } else {
                        // ✅ BACKWARD COMPATIBILITY: Support single ownershipProofDocument field
                        val ownershipProofDocument = ownerObj["ownershipProofDocument"]?.jsonPrimitive?.content
                        val ownershipProofDocumentRefNum = ownerObj["ownershipProofDocumentRefNum"]?.jsonPrimitive?.content

                        if (!ownershipProofDocument.isNullOrEmpty() && ownershipProofDocument != "") {
                            // Check if this is a draft document
                            if (ownershipProofDocument.startsWith("draft:") && !ownershipProofDocumentRefNum.isNullOrBlank()) {
                                // Draft document - download and re-upload
                                println("📎 Owner draft document (single field): $ownershipProofDocumentRefNum - downloading to re-upload...")

                                try {
                                    val fileUrlResult = repository.getFilePreview(ownershipProofDocumentRefNum)
                                    fileUrlResult.fold(
                                        onSuccess = { fileUrl ->
                                            println("✅ Got owner file URL: $fileUrl")

                                            // Download file from URL
                                            val url = java.net.URL(fileUrl)
                                            val connection = url.openConnection() as java.net.HttpURLConnection
                                            connection.connect()

                                            val fileBytes = connection.inputStream.use { it.readBytes() }
                                            println("✅ Downloaded ${fileBytes.size} bytes")

                                            // Get filename
                                            val fileName = ownerObj["ownershipProofDocumentFileName"]?.jsonPrimitive?.contentOrNull
                                                ?: android.net.Uri.parse(fileUrl).lastPathSegment?.substringBefore('?')
                                                ?: "document.jpg"

                                            // Detect MIME type
                                            val mimeType = when {
                                                fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                                                fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                                                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                                                else -> "application/octet-stream"
                                            }

                                            files.add(
                                                OwnerFileUpload(
                                                    fileName = fileName,
                                                    fileUri = fileUrl,
                                                    fileBytes = fileBytes,
                                                    mimeType = mimeType,
                                                    docOwnerId = docOwnerId,
                                                    docId = 1
                                                )
                                            )
                                            println("📎 Re-uploaded owner draft document: $fileName (${fileBytes.size} bytes)")
                                        },
                                        onFailure = { error ->
                                            println("❌ Failed to download owner draft document: ${error.message}")
                                        }
                                    )
                                } catch (e: Exception) {
                                    println("❌ Exception downloading owner draft document: ${e.message}")
                                    e.printStackTrace()
                                }
                            } else {
                                // New local document - upload normally
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
                }

                files
            } catch (e: Exception) {
                println("❌ Error parsing owner files: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Parse dynamic documents from formData and convert to DocumentFileUpload list
     * Documents are stored in formData with keys like "document_43" = "content://uri"
     * where 43 is the documentId from the required documents API
     */
    private suspend fun parseDocumentsFromFormData(
        context: Context,
        formData: Map<String, String>
    ): List<com.informatique.mtcit.data.model.DocumentFileUpload> {
        return withContext(Dispatchers.IO) {
            try {
                val documents = mutableListOf<com.informatique.mtcit.data.model.DocumentFileUpload>()

                // Iterate through formData to find document fields
                formData.forEach { (key, value) ->
                    // Check if this is a document field (e.g., "document_43")
                    if (key.startsWith("document_") && value.isNotEmpty() && value != "null") {
                        // Extract documentId from the key
                        val documentId = key.removePrefix("document_").toIntOrNull()

                        if (documentId != null) {
                            try {
                                // Convert URI to file upload
                                val uri = value.toUri()
                                val fileUpload = com.informatique.mtcit.data.helpers.FileUploadHelper.uriToFileUpload(context, uri)

                                if (fileUpload != null) {
                                    documents.add(
                                        com.informatique.mtcit.data.model.DocumentFileUpload(
                                            fileName = fileUpload.fileName,
                                            fileUri = fileUpload.fileUri,
                                            fileBytes = fileUpload.fileBytes,
                                            mimeType = fileUpload.mimeType,
                                            documentId = documentId
                                        )
                                    )
                                    println("📎 Added document: ${fileUpload.fileName} (documentId=$documentId)")
                                } else {
                                    println("⚠️ Could not convert URI to file upload for documentId=$documentId")
                                }
                            } catch (e: Exception) {
                                println("❌ Error processing document $documentId: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                }

                println("✅ Parsed ${documents.size} documents from formData")
                documents
            } catch (e: Exception) {
                println("❌ Error parsing documents from formData: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Parse engines from JSON string in form data
     * Returns list of maps with engine properties including dbId if present
     */
    private fun parseEnginesFromFormData(enginesJson: String): List<Map<String, Any?>> {
        return try {
            val jsonArray = org.json.JSONArray(enginesJson)
            val result = mutableListOf<Map<String, Any?>>()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                val map = mutableMapOf<String, Any?>()
                jsonObj.keys().forEach { key ->
                    map[key] = jsonObj.opt(key)
                }
                result.add(map)
            }
            result
        } catch (e: Exception) {
            println("❌ Error parsing engines from form data: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parse owners from JSON string in form data
     * Returns list of maps with owner properties including dbId if present
     */
    private fun parseOwnersFromFormData(ownersJson: String): List<Map<String, Any?>> {
        return try {
            val jsonArray = org.json.JSONArray(ownersJson)
            val result = mutableListOf<Map<String, Any?>>()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                val map = mutableMapOf<String, Any?>()
                jsonObj.keys().forEach { key ->
                    map[key] = jsonObj.opt(key)
                }
                result.add(map)
            }
            result
        } catch (e: Exception) {
            println("❌ Error parsing owners from form data: ${e.message}")
            emptyList()
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














