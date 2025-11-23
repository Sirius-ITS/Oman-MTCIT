package com.informatique.mtcit.ui.viewmodels

import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionStrategyFactory
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.informatique.mtcit.business.transactions.MortgageCertificateStrategy
import com.informatique.mtcit.business.transactions.ReleaseMortgageStrategy
import com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy
import com.informatique.mtcit.business.transactions.ValidationResult
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction
import com.informatique.mtcit.data.repository.RequestRepository  // ✅ FIXED: Changed from data.model to data.repository
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import kotlinx.coroutines.delay

/**
 * Validation state sealed class for marine unit selection
 */
sealed class ValidationState {
    object Idle : ValidationState()
    object Validating : ValidationState()
    object Valid : ValidationState()
    object Invalid : ValidationState()
    object RequiresConfirmation : ValidationState()
    data class Error(val message: String) : ValidationState()
}

/**
 * Marine Unit Registration ViewModel
 *
 * Handles Marine Unit Registration Category (التسجيل):
 * - Temporary Registration Certificate
 * - Permanent Registration Certificate
 * - Suspend Permanent Registration
 * - Cancel Permanent Registration
 * - Mortgage Certificate (with validation)
 * - Release Mortgage (with validation)
 */
@HiltViewModel
class MarineRegistrationViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    navigationUseCase: StepNavigationUseCase,
    private val strategyFactory: TransactionStrategyFactory,
    private val requestRepository: RequestRepository  // ✅ NEW: Inject request repository
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    // NEW: Validation state for marine unit selection
    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState: StateFlow<ValidationState> = _validationState.asStateFlow()

    // NEW: Store validation result for later use on Next button click
    private var _storedValidationResult: ValidationResult? = null

    // NEW: Navigation to compliance detail screen (removed error dialog state)
    private val _navigationToComplianceDetail = MutableStateFlow<MarineUnitNavigationAction.ShowComplianceDetailScreen?>(null)
    val navigationToComplianceDetail: StateFlow<MarineUnitNavigationAction.ShowComplianceDetailScreen?> = _navigationToComplianceDetail.asStateFlow()

    // ✅ NEW: Request saved successfully (show message to user)
    private val _requestSaved = MutableStateFlow<String?>(null)
    val requestSaved: StateFlow<String?> = _requestSaved.asStateFlow()

    // ✅ NEW: Trigger navigation to transaction screen after resuming
    private val _navigateToTransactionScreen = MutableStateFlow(false)
    val navigateToTransactionScreen: StateFlow<Boolean> = _navigateToTransactionScreen.asStateFlow()

    // ✅ NEW: Store request ID to resume after navigation
    private var _pendingResumeRequestId: String? = null

    // ✅ NEW: Flag to prevent normal initialization during resume
    private val _isResuming = MutableStateFlow(false)
    val isResuming: StateFlow<Boolean> = _isResuming.asStateFlow()

    /**
     * Check if there's a pending resume request
     * Used by the screen to skip normal initialization
     */
    fun hasPendingResume(): Boolean {
        val hasPending = _pendingResumeRequestId != null || _isResuming.value
        println("🔍 hasPendingResume: $hasPending (_pendingResumeRequestId=$_pendingResumeRequestId, _isResuming=${_isResuming.value})")
        return hasPending
    }

    /**
     * ✅ NEW: Get pending request ID for navigation
     * Used by ProfileScreen to pass requestId as navigation argument
     */
    fun getPendingRequestId(): String? {
        return _pendingResumeRequestId
    }

    /**
     * Create strategy for Marine Unit Registration transactions
     */
    override suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy {
        // Validate that this is a marine registration transaction
        require(isMarineRegistrationTransaction(transactionType)) {
            "MarineRegistrationViewModel can only handle marine registration transactions, got: $transactionType"
        }

        // Delegate to factory to create the appropriate strategy
        return strategyFactory.create(transactionType)
    }

    /**
     * NEW: Called when user selects a marine unit in Marine Unit Selection step
     * Triggers validation for Mortgage Certificate and Release Mortgage transactions
     * For Temporary Registration: NO validation here - validation happens at submit
     */
    fun onMarineUnitSelected(unitId: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            val transactionType = currentState.transactionType ?: return@launch

            // Only validate for specific transactions (NOT Temporary Registration)
            if (!requiresMarineUnitValidation(transactionType)) {
                // For other transactions, just mark as valid (selection is already handled by onSelectionChange)
                _validationState.value = ValidationState.Valid
                return@launch
            }

            // For TEMPORARY_REGISTRATION: Skip validation here, will validate at submit
            if (transactionType == TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE) {
                println("⏭️ Temporary Registration: Skipping validation on selection, will validate at submit")
                _validationState.value = ValidationState.Valid
                return@launch
            }

            _validationState.value = ValidationState.Validating

            try {
                // Get current user ID (TODO: Replace with actual user ID from session)
                val userId = getCurrentUserId()

                // Validate based on transaction type (only Mortgage transactions now)
                val result = when (transactionType) {
                    TransactionType.MORTGAGE_CERTIFICATE -> {
                        val strategy = currentStrategy as? MortgageCertificateStrategy
                        strategy?.validateMarineUnitSelection(unitId, userId)
                    }

                    TransactionType.RELEASE_MORTGAGE -> {
                        val strategy = currentStrategy as? ReleaseMortgageStrategy
                        strategy?.validateMarineUnitSelection(unitId, userId)
                    }

                    else -> null
                }

                if (result != null) {
                    // Store result for Next button click
                    _storedValidationResult = result

                    // Update validation state based on result
                    when (result) {
                        is ValidationResult.Success -> {
                            when (result.navigationAction) {
                                is MarineUnitNavigationAction.ProceedToNextStep -> {
                                    _validationState.value = ValidationState.Valid
                                    // Save additional data (unit ID is already saved by onSelectionChange)
                                    val action = result.navigationAction
                                    action.additionalData.forEach { (key, value) ->
                                        onFieldValueChange(key, value.toString())
                                    }
                                }
                                is MarineUnitNavigationAction.RouteToConditionalStep -> {
                                    // Save condition data for conditional routing
                                    _validationState.value = ValidationState.Valid
                                    val action = result.navigationAction
                                    action.conditionData.forEach { (key, value) ->
                                        onFieldValueChange(key, value.toString())
                                    }
                                }
                                is MarineUnitNavigationAction.ShowComplianceDetailScreen -> {
                                    _validationState.value = ValidationState.Invalid

                                    // Auto-navigate for mortgage transactions only
                                    _navigationToComplianceDetail.value = result.navigationAction
                                }
                                else -> {
                                    _validationState.value = ValidationState.Invalid
                                }
                            }
                        }
                        is ValidationResult.Error -> {
                            _validationState.value = ValidationState.Error(result.message)
                        }
                    }
                } else {
                    _validationState.value = ValidationState.Error("Strategy not initialized")
                }

            } catch (e: Exception) {
                _validationState.value = ValidationState.Error(e.message ?: "Validation failed")
            }
        }
    }

    /**
     * NEW: Validate marine unit for Temporary Registration transaction
     * Checks inspection status and ownership
     */
    private suspend fun validateTemporaryRegistrationUnit(
        strategy: TemporaryRegistrationStrategy,
        unitId: String,
        userId: String
    ): ValidationResult? {
        return try {
            println("🔍 validateTemporaryRegistrationUnit - calling strategy.validateMarineUnitSelection()")

            // Use the strategy's validateMarineUnitSelection which uses TemporaryRegistrationRules
            strategy.validateMarineUnitSelection(unitId, userId)
        } catch (e: Exception) {
            println("❌ Validation error: ${e.message}")
            e.printStackTrace()
            ValidationResult.Error(e.message ?: "Validation failed")
        }
    }

    /**
     * Override nextStep to handle validation result before proceeding
     * Called when user clicks Next button
     */
    override fun nextStep() {
        println("🔘 nextStep called")
        val currentState = uiState.value
        val transactionType = currentState.transactionType ?: run {
            println("⚠️ No transaction type, calling super.nextStep()")
            super.nextStep()
            return
        }

        println("🔘 Transaction type: $transactionType")

        // Check if this transaction requires validation
        if (!requiresMarineUnitValidation(transactionType)) {
            println("✅ No validation required, calling super.nextStep()")
            super.nextStep()
            return
        }

        println("🔘 Validation required for this transaction")

        // Check if we're on the marine unit selection step
        val currentStepIndex = currentState.currentStep
        val isMarineUnitSelectionStep = currentState.steps.getOrNull(currentStepIndex)
            ?.fields?.any { it is com.informatique.mtcit.common.FormField.MarineUnitSelector } == true

        println("🔘 Is marine unit selection step: $isMarineUnitSelectionStep")

        if (!isMarineUnitSelectionStep) {
            println("✅ Not on marine unit selection step, calling super.nextStep()")
            super.nextStep()
            return
        }

        // Check validation state
        val state = _validationState.value
        println("🔘 Validation state: ${state::class.simpleName}")

        when (state) {
            is ValidationState.Valid -> {
                println("✅ Validation is Valid, proceeding...")
                // Check if we need conditional routing (e.g., based on inspection status)
                _storedValidationResult?.let { result ->
                    println("🔘 Stored validation result: ${result::class.simpleName}")
                    if (result is ValidationResult.Success) {
                        when (val action = result.navigationAction) {
                            is MarineUnitNavigationAction.RouteToConditionalStep -> {
                                println("🔀 Conditional routing to step: ${action.targetStepIndex}")
                                // Route to specific step based on condition (e.g., inspection status)
                                goToStep(action.targetStepIndex)
                                return
                            }
                            else -> {
                                println("➡️ Regular next step, calling super.nextStep()")
                                // Regular next step
                                super.nextStep()
                            }
                        }
                    } else {
                        println("➡️ Result not Success, calling super.nextStep()")
                        super.nextStep()
                    }
                } ?: run {
                    println("➡️ No stored result, calling super.nextStep()")
                    super.nextStep()
                }
            }
            is ValidationState.Invalid -> {
                println("❌ Validation is Invalid, showing RequestDetailScreen")
                // Unit is ineligible - navigate to RequestDetailScreen
                _storedValidationResult?.let { result ->
                    if (result is ValidationResult.Success) {
                        val action = result.navigationAction
                        if (action is MarineUnitNavigationAction.ShowComplianceDetailScreen) {
                            _navigationToComplianceDetail.value = action
                        }
                    }
                }
            }
            is ValidationState.Error -> {
                println("❌ Validation error: ${state.message}")
                // Show error message
                _error.value = com.informatique.mtcit.common.AppError.Unknown(state.message)
            }
            is ValidationState.Idle -> {
                println("⚠️ Validation is Idle, calling super.nextStep()")
                // No unit selected yet OR validation not triggered
                // Let base validation handle this (it will check if selectedMarineUnits field is filled)
                super.nextStep()
            }
            is ValidationState.Validating -> {
                println("⏳ Still validating...")
                // Still validating - wait
                _error.value = com.informatique.mtcit.common.AppError.Unknown("جاري التحقق من الوحدة البحرية...")
            }
            is ValidationState.RequiresConfirmation -> {
                println("⚠️ Requires confirmation, calling super.nextStep()")
                // Handle confirmation if needed
                super.nextStep()
            }
        }
    }

    /**
     * Clear compliance detail navigation after navigation is done
     */
    fun clearComplianceDetailNavigation() {
        _navigationToComplianceDetail.value = null
        _validationState.value = ValidationState.Idle
        _storedValidationResult = null
    }

    // ✅ NEW: Clear request saved message
    fun clearRequestSavedMessage() {
        _requestSaved.value = null
    }

    // ✅ NEW: Clear navigation flag after navigation is handled
    fun clearNavigationFlag() {
        _navigateToTransactionScreen.value = false
    }

    /**
     * ✅ NEW: Resume transaction from saved request
     * Called when user opens a request from their profile (الاستمارات)
     *
     * GENERIC APPROACH:
     * 1. Initialize transaction type
     * 2. Restore ALL form data to strategy's internal state
     * 3. Rebuild steps based on restored state
     * 4. Jump to the step specified by API (lastCompletedStep + 1)
     * 5. Lock all previous steps (user cannot go back)
     *
     * Works for ALL transaction types - no special logic needed
     *
     * Flow:
     * 1. Fetch latest request status from API
     * 2. If PENDING → Show RequestDetailScreen (still under review)
     * 3. If VERIFIED → Navigate to transaction screen, then resume
     * 4. If REJECTED → Show RequestDetailScreen with rejection reason
     */
    fun resumeTransaction(requestId: String) {
        viewModelScope.launch {
            try {
                println("🔄 Resuming transaction: $requestId")

                // Fetch latest request status
                val result = requestRepository.getRequestStatus(requestId)

                result.onSuccess { request ->
                    println("✅ Request found: ${request.id}, status: ${request.status}")

                    when (request.status) {
                        com.informatique.mtcit.data.model.RequestStatus.VERIFIED -> {
                            // Inspection verified - resume transaction
                            println("✅ Request VERIFIED - Will navigate to transaction screen")

                            // Store request ID and trigger navigation
                            _pendingResumeRequestId = requestId
                            _navigateToTransactionScreen.value = true
                        }

                        com.informatique.mtcit.data.model.RequestStatus.PENDING,
                        com.informatique.mtcit.data.model.RequestStatus.IN_PROGRESS -> {
                            // Still under review - show detail screen
                            println("⏳ Request still PENDING - Showing detail screen")

                            val action = MarineUnitNavigationAction.ShowComplianceDetailScreen(
                                marineUnit = request.marineUnit ?: createPlaceholderUnit(),
                                complianceIssues = listOf(
                                    com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                                        category = "حالة الفحص",
                                        title = "الطلب قيد المراجعة",
                                        description = "طلبك ��يد المراجعة من قبل الإدارة",
                                        severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.WARNING,
                                        details = mapOf(
                                            "رقم الطلب" to request.id,
                                            "تاري�� الإنشاء" to request.createdDate,
                                            "الموعد الم��وقع" to (request.estimatedCompletionDate ?: "غير محدد")
                                        )
                                    )
                                ),
                                rejectionReason = "طلبك قيد المراجعة. سيتم إشعارك عند اكتمال المراجعة.",
                                rejectionTitle = "طلب قيد المرا��عة"
                            )

                            _navigationToComplianceDetail.value = action
                        }

                        com.informatique.mtcit.data.model.RequestStatus.REJECTED -> {
                            // Request rejected - show detail screen with reason
                            println("❌ Request REJECTED - Showing detail screen")

                            val action = MarineUnitNavigationAction.ShowComplianceDetailScreen(
                                marineUnit = request.marineUnit ?: createPlaceholderUnit(),
                                complianceIssues = listOf(
                                    com.informatique.mtcit.business.transactions.marineunit.ComplianceIssue(
                                        category = "سبب الرفض",
                                        title = "تم رفض الطلب",
                                        description = request.rejectionReason ?: "لم يتم تحديد السبب",
                                        severity = com.informatique.mtcit.business.transactions.marineunit.IssueSeverity.BLOCKING,
                                        details = mapOf(
                                            "رقم الطلب" to request.id,
                                            "تاريخ الرفض" to request.lastUpdatedDate
                                        )
                                    )
                                ),
                                rejectionReason = request.rejectionReason ?: "تم رفض الطلب",
                                rejectionTitle = "تم رفض الطلب"
                            )

                            _navigationToComplianceDetail.value = action
                        }

                        com.informatique.mtcit.data.model.RequestStatus.COMPLETED -> {
                            // Transaction already completed
                            println("✅ Request COMPLETED - Nothing to do")
                            _error.value = com.informatique.mtcit.common.AppError.Unknown("هذا الطلب مكتمل بالفعل")
                        }
                    }
                }

                result.onFailure { error ->
                    println("❌ Failed to get request status: ${error.message}")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown(
                        error.message ?: "فشل في تحميل حالة الطلب"
                    )
                }

            } catch (e: Exception) {
                println("❌ Exception during resume: ${e.message}")
                e.printStackTrace()
                _error.value = com.informatique.mtcit.common.AppError.Unknown(
                    e.message ?: "حدث خطأ أثناء استعادة الطلب"
                )
            }
        }
    }

    /**
     * ✅ NEW: Complete the resume after navigation to transaction screen
     * Called by MarineRegistrationScreen when it detects a pending resume
     */
    fun completeResumeAfterNavigation() {
        val requestId = _pendingResumeRequestId ?: return

        println("🔄 Completing resume for request: $requestId")

        // ✅ Set resuming flag to prevent normal initialization
        _isResuming.value = true

        viewModelScope.launch {
            try {
                // Fetch request again
                val result = requestRepository.getRequestStatus(requestId)

                result.onSuccess { request ->
                    println("✅ Request VERIFIED - Resuming transaction")
                    println("📋 Transaction type: ${request.type}")
                    println("📋 Form data keys: ${request.formData.keys}")
                    println("📋 Last completed step: ${request.lastCompletedStep}")

                    // ✅ Step 1: Initialize transaction with saved type
                    initializeTransaction(request.type)

                    // Wait for initialization
                    delay(500)

                    // ✅ Step 2: Restore form data to strategy's internal state
                    val strategy = currentStrategy
                    if (strategy == null) {
                        println("❌ Strategy is null, cannot resume")
                        _error.value = com.informatique.mtcit.common.AppError.Unknown("فشل في استعادة المعاملة")
                        _isResuming.value = false
                        _pendingResumeRequestId = null
                        return@launch
                    }

                    println("🔧 Restoring form data to strategy...")

                    // Call processStepData to update strategy's accumulatedFormData
                    // This ensures getSteps() will return the correct steps
                    strategy.processStepData(0, request.formData)

                    println("✅ Strategy's internal state updated")

                    // ✅ Step 3: Rebuild steps based on restored state
                    val rebuiltSteps = strategy.getSteps()
                    println("📊 Steps after rebuild: ${rebuiltSteps.size}")

                    // Update UI state with rebuilt steps AND restored form data
                    updateUiState { state ->
                        state.copy(
                            steps = rebuiltSteps,
                            formData = request.formData
                        )
                    }

                    // Small delay for UI state update
                    delay(200)

                    println("📊 Final steps in UI state: ${uiState.value.steps.size}")
                    println("📊 Step titles:")
                    uiState.value.steps.forEachIndexed { index, step ->
                        println("   [$index] Step titleRes: ${step.titleRes}")
                    }

                    // ✅ Step 4: Calculate resume step
                    // API tells us the last completed step, so we resume from next step
                    val totalSteps = uiState.value.steps.size
                    val resumeStep = request.lastCompletedStep + 1

                    // Lock all previous steps (user cannot go back)
                    val lockedSteps = (0 until resumeStep).toSet()

                    println("🎯 Resume from step: $resumeStep (last completed was ${request.lastCompletedStep})")
                    println("🎯 Total steps: $totalSteps")
                    println("🔒 Locked steps: $lockedSteps")

                    // ✅ Step 5: Mark as resumed transaction and lock previous steps
                    updateUiState { currentState ->
                        currentState.copy(
                            isResumedTransaction = true,
                            lockedSteps = lockedSteps,
                            completedSteps = lockedSteps // Mark locked steps as completed
                        )
                    }

                    // ✅ Step 6: Navigate to resume step - DIRECTLY update currentStep
                    println("✅ Directly updating currentStep to $resumeStep")

                    when {
                        resumeStep < totalSteps -> {
                            // Resume step exists - update current step directly
                            updateUiState { currentState ->
                                currentState.copy(currentStep = resumeStep)
                            }
                            println("✅ Updated currentStep to $resumeStep")
                        }
                        resumeStep == totalSteps -> {
                            // Last step was completed, go to last step (review/submit)
                            updateUiState { currentState ->
                                currentState.copy(currentStep = totalSteps - 1)
                            }
                            println("✅ Updated currentStep to ${totalSteps - 1}")
                        }
                        else -> {
                            // Error: resume step beyond total steps
                            println("❌ Resume step $resumeStep exceeds total steps $totalSteps")
                            _error.value = com.informatique.mtcit.common.AppError.Unknown("خطأ في استعادة المعاملة")
                        }
                    }

                    // ✅ IMPORTANT: Wait for UI state to actually update
                    delay(300)
                    println("✅ Final currentStep: ${uiState.value.currentStep}")
                    println("🎬 Resume complete, clearing flags")

                    // Clear pending request ID and resuming flag
                    _pendingResumeRequestId = null
                    _isResuming.value = false
                }

                result.onFailure { error ->
                    println("❌ Failed to complete resume: ${error.message}")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown(
                        error.message ?: "فشل في استعادة المعاملة"
                    )
                    _pendingResumeRequestId = null
                    _isResuming.value = false
                }

            } catch (e: Exception) {
                println("❌ Exception completing resume: ${e.message}")
                e.printStackTrace()
                _error.value = com.informatique.mtcit.common.AppError.Unknown(
                    e.message ?: "حدث خطأ أثناء استعادة الطلب"
                )
                _pendingResumeRequestId = null
                _isResuming.value = false
            }
        }
    }

    /**
     * ✅ NEW: Set request ID and complete resume
     * Called by MarineRegistrationScreen when requestId is passed as navigation argument
     * This is the NEW approach that works across ViewModel recreation
     */
    fun setRequestIdAndCompleteResume(requestId: String) {
        println("🔄 setRequestIdAndCompleteResume called with requestId: $requestId")
        _pendingResumeRequestId = requestId
        completeResumeAfterNavigation()
    }

    /**
     * Clear all data and prepare for a new transaction
     * Called when user starts a new transaction from the dashboard
     */
    fun clearForNewTransaction() {
        // Clear marine unit selection and validation state
        _validationState.value = ValidationState.Idle
        _storedValidationResult = null
        _navigationToComplianceDetail.value = null

        // Clear request saved message
        _requestSaved.value = null

        // Clear pending resume request ID
        _pendingResumeRequestId = null
    }

    /**
     * ✅ NEW: Save request progress when inspection is PENDING
     * Called after validation shows PENDING status
     */
    private suspend fun saveRequestProgress(
        marineUnit: MarineUnit,
        currentStep: Int
    ) {
        try {
            val currentState = uiState.value
            val userId = getCurrentUserId()

            println("💾 Saving request progress for user $userId")

            val result = requestRepository.saveRequestProgress(
                userId = userId,
                transactionType = currentState.transactionType ?: TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
                marineUnit = marineUnit,
                formData = currentState.formData,
                lastCompletedStep = currentStep,
                status = com.informatique.mtcit.data.model.RequestStatus.PENDING
            )

            result.onSuccess { requestId ->
                println("✅ Request saved successfully: $requestId")
                _requestSaved.value = requestId
            }

            result.onFailure { error ->
                println("❌ Failed to save request: ${error.message}")
            }

        } catch (e: Exception) {
            println("❌ Exception saving request: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * NEW: Validate and submit form for Temporary Registration
     * This method should be called from UI instead of submitForm() for Temporary Registration
     * Validates inspection status before actual submission
     */
    fun validateAndSubmit() {
        val currentState = uiState.value
        val transactionType = currentState.transactionType

        // Only intercept for Temporary Registration Certificate
        if (transactionType != TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE) {
            println("📤 Not Temporary Registration, calling submitForm()")
            submitForm()
            return
        }

        println("🔍 Temporary Registration: Validating inspection before submit")

        viewModelScope.launch {
            try {
                // Get selected marine unit ID from form data
                val selectedUnitsJson = currentState.formData["selectedMarineUnits"]
                val isAddingNewUnit = currentState.formData["isAddingNewUnit"]?.toBoolean() ?: false

                println("🔍 Selected units JSON: $selectedUnitsJson")
                println("🔍 Is adding new unit: $isAddingNewUnit")
                println("🔍 All form data keys: ${currentState.formData.keys}")

                // Check if user is adding a NEW marine unit by looking for multiple possible field indicators
                val hasNewUnitData = currentState.formData.containsKey("marineUnitName") ||
                                    currentState.formData.containsKey("unitName") ||
                                    currentState.formData.containsKey("callSign") ||
                                    currentState.formData.containsKey("imoNumber") ||
                                    currentState.formData.containsKey("registrationPort") ||
                                    (selectedUnitsJson == "[]" && currentState.formData.size > 2) // Has form data but no selection

                println("🔍 hasNewUnitData: $hasNewUnitData")
                println("🔍 Form data size: ${currentState.formData.size}")

                if ((selectedUnitsJson.isNullOrEmpty() || selectedUnitsJson == "[]") && !hasNewUnitData) {
                    println("❌ No marine unit selected and no new unit data")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown("الرجاء اختيار وحدة بحرية أو إضافة وحدة جديدة")
                    return@launch
                }

                // Get the strategy
                val strategy = currentStrategy as? TemporaryRegistrationStrategy
                if (strategy == null) {
                    println("❌ Strategy not found")
                    submitForm()
                    return@launch
                }

                val userId = getCurrentUserId()
                val validationResult: ValidationResult?

                // Case 1: User is adding a NEW marine unit
                if (hasNewUnitData) {
                    println("✅ User is adding a NEW marine unit")

                    // Extract new unit data from form - try multiple possible field names
                    val unitName = currentState.formData["marineUnitName"]
                        ?: currentState.formData["unitName"]
                        ?: currentState.formData["callSign"]  // Fallback to callSign if name not found
                        ?: "وحدة بحرية جديدة"

                    val unitType = currentState.formData["unitType"]
                        ?: currentState.formData["unitClassification"]
                        ?: ""

                    val registrationPort = currentState.formData["registrationPort"] ?: ""
                    val imo = currentState.formData["imoNumber"] ?: currentState.formData["imo"] ?: ""
                    val callSign = currentState.formData["callSign"] ?: ""
                    val activity = currentState.formData["maritimeactivity"] ?: ""
                    val length = currentState.formData["length"] ?: currentState.formData["totalLength"] ?: ""
                    val width = currentState.formData["width"] ?: currentState.formData["totalWidth"] ?: ""
                    val height = currentState.formData["height"] ?: ""

                    println("📋 New unit data: name=$unitName, type=$unitType, port=$registrationPort, callSign=$callSign")

                    // Create a temporary MarineUnit object for validation
                    val newUnit = MarineUnit(
                        id = "new_${System.currentTimeMillis()}", // Temporary ID
                        name = unitName,
                        type = unitType,
                        imoNumber = imo,
                        callSign = callSign,
                        maritimeId = "", // Will be assigned after successful registration
                        registrationPort = registrationPort,
                        activity = activity,
                        isOwned = true, // User is adding their own unit
                        totalLength = length,
                        totalWidth = width,
                        height = height,
                        registrationStatus = "ACTIVE",
                        registrationType = "TEMPORARY"
                    )

                    // ✅ Use the NEW method for validating new units
                    validationResult = try {
                        strategy.validateNewMarineUnit(newUnit, userId)
                    } catch (e: Exception) {
                        println("❌ Validation error: ${e.message}")
                        e.printStackTrace()
                        ValidationResult.Error(e.message ?: "Validation failed")
                    }

                } else {
                    // Case 2: User selected an EXISTING marine unit
                    println("✅ User selected an EXISTING marine unit")

                    // Parse selected unit ID (maritimeId from JSON)
                    val selectedMaritimeIds = try {
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(selectedUnitsJson!!)
                    } catch (e: Exception) {
                        println("❌ Failed to parse selected units: ${e.message}")
                        _error.value = com.informatique.mtcit.common.AppError.Unknown("خطأ في قراءة الوحدة المختارة")
                        return@launch
                    }

                    if (selectedMaritimeIds.isEmpty()) {
                        println("❌ No units in selection")
                        _error.value = com.informatique.mtcit.common.AppError.Unknown("الرجاء اختيار وحدة بحرية")
                        return@launch
                    }

                    val selectedMaritimeId = selectedMaritimeIds.first()
                    println("🔍 Selected maritime ID: $selectedMaritimeId")

                    // Get marine units and find the selected one
                    val dynamicOptions = strategy.loadDynamicOptions()
                    val marineUnits = dynamicOptions["marineUnits"] as? List<*>

                    val selectedUnit = marineUnits?.firstOrNull {
                        (it as? MarineUnit)?.maritimeId == selectedMaritimeId
                    } as? MarineUnit

                    if (selectedUnit == null) {
                        println("❌ Selected unit not found")
                        _error.value = com.informatique.mtcit.common.AppError.Unknown("الوحدة البحرية المختارة غير موجودة")
                        return@launch
                    }

                    println("✅ Found selected unit: ${selectedUnit.name}, id: ${selectedUnit.id}")

                    // Validate the selected unit's inspection status
                    validationResult = validateTemporaryRegistrationUnit(strategy, selectedUnit.id, userId)
                }

                // Handle validation result (same for both cases)
                if (validationResult == null) {
                    println("❌ Validation returned null")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown("فشل التحقق من حالة الفحص")
                    return@launch
                }

                when (validationResult) {
                    is ValidationResult.Success -> {
                        when (val action = validationResult.navigationAction) {
                            is MarineUnitNavigationAction.ProceedToNextStep -> {
                                // Inspection is valid - proceed to next step (Marine Unit Name Selection)
                                println("✅ Inspection validated, proceeding to next step")
                                super.nextStep()
                            }
                            is MarineUnitNavigationAction.ShowComplianceDetailScreen -> {
                                // Inspection failed (pending/not verified) - show RequestDetailScreen
                                println("⏳ Inspection validation failed, showing RequestDetailScreen")

                                // ✅ NEW: Save request progress if status is PENDING
                                val isPending = action.rejectionTitle?.contains("قيد المعالجة") == true
                                if (isPending) {
                                    println("💾 Saving request progress (status: PENDING)")
                                    saveRequestProgress(
                                        marineUnit = action.marineUnit,
                                        currentStep = currentState.currentStep
                                    )
                                }

                                _navigationToComplianceDetail.value = action
                            }
                            else -> {
                                println("❌ Unexpected navigation action: ${action::class.simpleName}")
                                _error.value = com.informatique.mtcit.common.AppError.Unknown("خطأ في التحقق من حالة الفحص")
                            }
                        }
                    }
                    is ValidationResult.Error -> {
                        println("❌ Validation error: ${validationResult.message}")
                        _error.value = com.informatique.mtcit.common.AppError.Unknown(validationResult.message)
                    }
                }

            } catch (e: Exception) {
                println("❌ Exception during validation: ${e.message}")
                e.printStackTrace()
                _error.value = com.informatique.mtcit.common.AppError.Unknown(e.message ?: "حدث خطأ أثناء التحقق")
            }
        }
    }

    /**
     * NEW: Validate on review step for Temporary Registration
     * Called when user clicks "Accept & Send" on review step
     * Checks inspection status and navigates accordingly
     */
    fun validateOnReviewStep() {
        val currentState = uiState.value
        val transactionType = currentState.transactionType

        // Check if we're on review step
        val currentStepIndex = currentState.currentStep
        val isReviewStep = currentState.steps.getOrNull(currentStepIndex)?.fields?.isEmpty() == true

        if (!isReviewStep) {
            println("⚠️ Not on review step, calling regular nextStep()")
            nextStep()
            return
        }

        // For Temporary Registration, validate inspection on review step
        if (transactionType == TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE) {
            println("🔍 Review Step: Validating inspection for Temporary Registration")
            validateAndSubmit()
        } else {
            // For other transactions, just proceed to next step
            println("➡️ Review Step: Proceeding to next step for other transaction")
            nextStep()
        }
    }

    /**
     * Create placeholder marine unit for display purposes
     */
    private fun createPlaceholderUnit(): MarineUnit {
        return com.informatique.mtcit.business.transactions.shared.MarineUnit(
            id = "placeholder",
            name = "وحدة بحرية",
            type = "",
            imoNumber = "",
            callSign = "",
            maritimeId = "",
            registrationPort = "",
            activity = "",
            isOwned = true
        )
    }

    /**
     * Check if transaction requires marine unit validation
     */
    private fun requiresMarineUnitValidation(type: TransactionType): Boolean {
        return when (type) {
            TransactionType.MORTGAGE_CERTIFICATE,
            TransactionType.RELEASE_MORTGAGE,
            TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE -> true
            else -> false
        }
    }

    /**
     * Get current user ID from auth system
     */
    private fun getCurrentUserId(): String {
        // TODO: Replace with actual user ID from your auth/session manager
        return "currentUserId"
    }

    /**
     * Check if transaction type belongs to Marine Unit Registration category
     */
    private fun isMarineRegistrationTransaction(type: TransactionType): Boolean {
        return when (type) {
            TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE,
            TransactionType.PERMANENT_REGISTRATION_CERTIFICATE,
            TransactionType.SUSPEND_PERMANENT_REGISTRATION,
            TransactionType.CANCEL_PERMANENT_REGISTRATION,
            TransactionType.MORTGAGE_CERTIFICATE,
            TransactionType.RELEASE_MORTGAGE -> true
            else -> false
        }
    }
}
