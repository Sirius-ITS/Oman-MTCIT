package com.informatique.mtcit.business.transactions.shared

import android.content.Context
import com.informatique.mtcit.data.model.RequiredDocumentItem
import com.informatique.mtcit.data.repository.LookupRepository
import com.informatique.mtcit.ui.components.DropdownSection
import com.informatique.mtcit.ui.viewmodels.StepData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ✅ InspectionFlowManager
 *
 * Manages the inspection flow for transactions that may require inspection:
 * - Temporary Registration
 * - Permanent Registration
 * - Issue Navigation Permit
 * - Renew Navigation Permit
 * - Request Inspection (standalone)
 *
 * Responsibilities:
 * 1. Check if inspection is required after Review Step
 * 2. Show "Inspection Required" dialog
 * 3. Inject inspection purpose step dynamically
 * 4. Submit inspection request via InspectionRequestManager
 *
 * @constructor Injected by Hilt
 */
@Singleton
class InspectionFlowManager @Inject constructor(
    private val lookupRepository: LookupRepository,
    private val inspectionRequestManager: InspectionRequestManager
) {

    /**
     * Check if inspection is required for the current transaction
     * This is determined by the send-request API response
     *
     * @param needInspection Flag from ReviewResult
     * @return true if inspection is needed
     */
    fun isInspectionRequired(needInspection: Boolean): Boolean {
        println("🔍 InspectionFlowManager: Checking if inspection required - $needInspection")
        return needInspection
    }

    /**
     * Prepare inspection required dialog data
     *
     * @param message Message from API (e.g., "في انتظار نتيجة الفحص الفني")
     * @param formData Accumulated form data to store dialog state
     * @param allowContinue If true, show "Continue" button to inject inspection step
     * @param parentRequestId Parent request ID (if inspection triggered from another transaction)
     * @param parentRequestType Parent request type (1=temp, 2=perm, 3=issue nav, 5=renew nav)
     */
    fun prepareInspectionDialog(
        message: String,
        formData: MutableMap<String, String>,
        allowContinue: Boolean = true,
        parentRequestId: Int? = null,
        parentRequestType: Int? = null
    ) {
        println("🔍 InspectionFlowManager: Preparing inspection dialog")
        println("   Message: $message")
        println("   Allow Continue: $allowContinue")

        if (parentRequestId != null && parentRequestType != null) {
            println("   Parent Request ID: $parentRequestId")
            println("   Parent Request Type: $parentRequestType (1=temp, 2=perm, 3=issue nav, 5=renew nav)")
        } else {
            println("   No parent transaction (standalone inspection)")
        }

        formData["showInspectionDialog"] = "true"
        formData["inspectionMessage"] = message
        formData["canContinueToInspection"] = allowContinue.toString()

        // ✅ Store parent transaction info (will be passed to inspection API)
        if (parentRequestId != null) {
            formData["needInspectionRequestId"] = parentRequestId.toString()
        }
        if (parentRequestType != null) {
            formData["needInspectionRequestTypeId"] = parentRequestType.toString()
        }

        println("✅ Dialog state stored in formData")
    }

    /**
     * Prepare inspection success dialog data
     * This is shown after inspection request is submitted successfully
     *
     * @param message Success message from API
     * @param requestId Inspection request ID
     * @param formData Accumulated form data to store dialog state
     */
    fun prepareInspectionSuccessDialog(
        message: String,
        requestId: Int,
        formData: MutableMap<String, String>
    ) {
        println("🔍 InspectionFlowManager: Preparing inspection success dialog")
        println("   Message: $message")
        println("   Request ID: $requestId")

        formData["showInspectionSuccessDialog"] = "true"
        formData["inspectionSuccessMessage"] = message
        formData["inspectionRequestId"] = requestId.toString()

        println("✅ Inspection success dialog state stored in formData")
    }

    /**
     * Create inspection purpose step dynamically
     * This step is added AFTER review when inspection is required
     *
     * @param inspectionPurposes List of inspection purposes from API
     * @param inspectionPlaces List of inspection places from API
     * @param authoritySections Sectioned dropdown for authorities
     * @param requiredDocuments List of required documents for inspection
     * @return StepData for inspection purpose step
     */
    suspend fun createInspectionPurposeStep(
        inspectionPurposes: List<String>,
        inspectionPlaces: List<String>,
        authoritySections: List<DropdownSection>,
        requiredDocuments: List<RequiredDocumentItem>
    ): StepData {
        println("🔍 InspectionFlowManager: Creating inspection purpose step")
        println("   Purposes: ${inspectionPurposes.size}")
        println("   Places: ${inspectionPlaces.size}")
        println("   Authority Sections: ${authoritySections.size}")
        println("   Documents: ${requiredDocuments.size}")

        // Reuse SharedSteps.inspectionPurposeAndAuthorityStep
        return SharedSteps.inspectionPurposeAndAuthorityStep(
            inspectionPurposes = inspectionPurposes,
            inspectionPlaces = inspectionPlaces,
            authoritySections = authoritySections,
            documents = requiredDocuments
        )
    }

    /**
     * Load inspection-specific lookups (purposes, places, authorities, documents)
     *
     * @param shipInfoId The ship info ID to fetch authorities for
     * @return InspectionLookups containing all necessary data
     */
    suspend fun loadInspectionLookups(shipInfoId: Int): InspectionLookups {
        println("🔍 InspectionFlowManager: Loading inspection lookups for shipInfoId=$shipInfoId...")

        try {
            // Load purposes
            val purposesResult = lookupRepository.getInspectionPurposes()
            val purposes = purposesResult.getOrNull() ?: emptyList()

            // Load places
            val placesResult = lookupRepository.getInspectionPlaces()
            val places = placesResult.getOrNull() ?: emptyList()

            // Load authorities (grouped by type)
            val authoritiesResult = lookupRepository.getInspectionAuthorities(shipInfoId)
            val authoritiesMap = authoritiesResult.getOrNull() ?: emptyMap()

            // Convert Map<String, List<String>> to List<DropdownSection>
            val authoritySections = authoritiesMap.map { (groupName, authorities) ->
                DropdownSection(
                    title = groupName,
                    items = authorities.map { authorityName ->
                        // Try to inject ID if available
                        val id = lookupRepository.getInspectionAuthorityId(authorityName)
                        if (id != null) "$id|$authorityName" else authorityName
                    }
                )
            }

            // ✅ Load inspection-specific required documents (requestTypeId = 8)
            val inspectionRequestTypeId = "8" // Inspection transaction type ID
            val documentsResult = lookupRepository.getRequiredDocumentsByRequestType(inspectionRequestTypeId)
            val documents = documentsResult.getOrNull() ?: emptyList()

            println("✅ Loaded inspection lookups:")
            println("   Purposes: ${purposes.size}")
            println("   Places: ${places.size}")
            println("   Authority groups: ${authoritySections.size}")
            println("   Documents: ${documents.size}") // ✅ Log documents count

            return InspectionLookups(
                purposes = purposes,
                places = places,
                authoritySections = authoritySections,
                documents = documents // ✅ Include inspection documents
            )
        } catch (e: Exception) {
            println("❌ Failed to load inspection lookups: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Process inspection purpose step submission
     * Called when user completes the inspection purpose form
     *
     * @param formData Accumulated form data with inspection details
     * @param context Android context for file access
     * @return Result indicating success or failure
     */
    suspend fun submitInspectionRequest(
        formData: Map<String, String>,
        context: Context
    ): InspectionSubmitResult {
        println("🔍 InspectionFlowManager: Submitting inspection request...")

        // Delegate to InspectionRequestManager
        return inspectionRequestManager.submitInspectionRequest(
            formData = formData,
            context = context
        )
    }

    /**
     * Check if current step is inspection purpose step
     *
     * @param stepType Current step type
     * @return true if it's inspection purpose step
     */
    fun isInspectionPurposeStep(stepType: StepType): Boolean {
        return stepType == StepType.INSPECTION_PURPOSES_AND_AUTHORITIES
    }

    /**
     * Check if inspection step injection is requested
     *
     * @param formData Accumulated form data
     * @return true if user clicked "Continue" on inspection dialog
     */
    fun shouldInjectInspectionStep(formData: Map<String, String>): Boolean {
        val shouldInject = formData["injectInspectionStep"]?.toBoolean() ?: false
        println("🔍 InspectionFlowManager: shouldInjectInspectionStep = $shouldInject")
        return shouldInject
    }

    /**
     * Handle inspection purpose step completion
     * This processes the form data and submits the inspection request
     *
     * @param formData Accumulated form data
     * @param context Android context for file access
     * @return StepProcessResult indicating success, error, or no action
     */
    suspend fun handleInspectionPurposeStepCompletion(
        formData: MutableMap<String, String>,
        context: Context
    ): StepProcessResult {
        println("🔍 InspectionFlowManager: Handling inspection purpose step completion")

        try {
            val result = submitInspectionRequest(formData, context)

            when (result) {
                is InspectionSubmitResult.Success -> {
                    println("✅ Inspection request submitted successfully!")
                    println("   Message: ${result.message}")
                    println("   Request ID: ${result.requestId}")

                    // ✅ Use dedicated method to prepare success dialog
                    prepareInspectionSuccessDialog(
                        message = result.message,
                        requestId = result.requestId,
                        formData = formData
                    )

                    return StepProcessResult.Success(
                        message = result.message
                    )
                }
                is InspectionSubmitResult.Error -> {
                    println("❌ Inspection request submission failed: ${result.message}")
                    formData["apiError"] = result.message
                    return StepProcessResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            println("❌ Exception handling inspection purpose step: ${e.message}")
            e.printStackTrace()
            val errorMsg = "حدث خطأ أثناء إرسال طلب المعاينة: ${e.message}"
            formData["apiError"] = errorMsg
            return StepProcessResult.Error(errorMsg)
        }
    }
}

/**
 * Data class holding inspection-related lookups
 */
data class InspectionLookups(
    val purposes: List<String>,
    val places: List<String>,
    val authoritySections: List<DropdownSection>,
    val documents: List<RequiredDocumentItem> = emptyList() // ✅ Inspection-specific documents
)
