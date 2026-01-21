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
import com.informatique.mtcit.business.transactions.MarineUnitValidatable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.informatique.mtcit.business.transactions.MortgageCertificateStrategy
import com.informatique.mtcit.business.transactions.ReleaseMortgageStrategy
import com.informatique.mtcit.business.transactions.RequestInspectionStrategy
import com.informatique.mtcit.business.transactions.ValidationResult
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction
import com.informatique.mtcit.business.transactions.shared.MarineActivity
import com.informatique.mtcit.data.repository.RequestRepository
import com.informatique.mtcit.business.transactions.shared.MarineUnit
import com.informatique.mtcit.business.transactions.shared.PortOfRegistry
import com.informatique.mtcit.business.transactions.shared.ShipType
import com.informatique.mtcit.business.transactions.shared.StepType
import com.informatique.mtcit.data.model.requests.StatusCountResponse
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
    private val requestRepository: RequestRepository,
    private val marineUnitsApiService: com.informatique.mtcit.data.api.MarineUnitsApiService,  // ✅ Use generic API service
    private val authRepository: com.informatique.mtcit.data.repository.AuthRepository,  // ✅ NEW: Inject AuthRepository for token refresh
    private val requestsApiService: com.informatique.mtcit.data.api.RequestsApiService,  // ✅ NEW: For status counts
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context  // ✅ NEW: For UserHelper
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    init {
        // ✅ Observe formData changes to detect request submission success
        viewModelScope.launch {
            uiState.collect { state ->
                // Check if strategy has set success flags in formData
                val formData = state.formData
                val requestSubmitted = formData["requestSubmitted"]?.toBoolean() ?: false

                if (requestSubmitted) {
                    println("🔔 Detected requestSubmitted flag in formData")
                    checkForRequestSubmissionSuccess()
                }
            }
        }
    }

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

    // ✅ NEW: Request submission success (for showing success dialog after review step)
    private val _requestSubmissionSuccess = MutableStateFlow<RequestSubmissionResult?>(null)
    val requestSubmissionSuccess: StateFlow<RequestSubmissionResult?> = _requestSubmissionSuccess.asStateFlow()

    // ✅ NEW: Trigger navigation to transaction screen after resuming
    private val _navigateToTransactionScreen = MutableStateFlow(false)
    val navigateToTransactionScreen: StateFlow<Boolean> = _navigateToTransactionScreen.asStateFlow()

    // ✅ NEW: Store request ID to resume after navigation
    private var _pendingResumeRequestId: String? = null

    // ✅ NEW: Flag to prevent normal initialization during resume
    private val _isResuming = MutableStateFlow(false)
    val isResuming: StateFlow<Boolean> = _isResuming.asStateFlow()

    // ✅ NEW: Status counts for profile screen
    private val _statusCounts = MutableStateFlow<StatusCountResponse?>(null)
    val statusCounts: StateFlow<StatusCountResponse?> = _statusCounts.asStateFlow()

    private val _statusCountsLoading = MutableStateFlow(false)
    val statusCountsLoading: StateFlow<Boolean> = _statusCountsLoading.asStateFlow()

    private val _statusCountsError = MutableStateFlow<String?>(null)
    val statusCountsError: StateFlow<String?> = _statusCountsError.asStateFlow()

    /**
     * Data class for request submission result
     */
    data class RequestSubmissionResult(
        val requestNumber: String,
        val message: String
    )

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
        strategy: MarineUnitValidatable,
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

//        if (!isMarineUnitSelectionStep) {
            println("✅ Not on marine unit selection step, calling super.nextStep()")
            super.nextStep()
            return
//        }
    }

    /**
     * Check if request submission was successful and show success dialog
     * Called after super.nextStep() to check if review step was completed
     */
    private fun checkForRequestSubmissionSuccess() {
        println("🔍🔍🔍 checkForRequestSubmissionSuccess() called")
        val currentState = uiState.value
        val formData = currentState.formData

        println("🔍 FormData keys: ${formData.keys}")
        println("🔍 FormData size: ${formData.size}")

        // Check if review step set success flag
        val requestSubmitted = formData["requestSubmitted"]?.toBoolean() ?: false
        val requestNumber = formData["requestNumber"]
        val successMessage = formData["successMessage"]

        println("🔍 requestSubmitted flag: $requestSubmitted")
        println("🔍 requestNumber value: $requestNumber")
        println("🔍 successMessage value: $successMessage")
        println("🔍 Condition check: requestSubmitted=$requestSubmitted, requestNumber.isNullOrEmpty()=${requestNumber.isNullOrEmpty()}")

        if (requestSubmitted && !requestNumber.isNullOrEmpty()) {
            println("✅✅✅ Request submitted successfully!")
            println("   Request Number: $requestNumber")
            println("   Message: $successMessage")

            // Set success dialog data
            _requestSubmissionSuccess.value = RequestSubmissionResult(
                requestNumber = requestNumber,
                message = successMessage?.toString() ?: "تم إرسال الطلب بنجاح"
            )

            println("✅ _requestSubmissionSuccess.value set to: ${_requestSubmissionSuccess.value}")

            // Clear flags from formData
            val cleanedFormData = formData.toMutableMap().apply {
                remove("requestSubmitted")
                remove("requestNumber")
                remove("successMessage")
            }

            updateUiState { currentState.copy(formData = cleanedFormData) }
        } else {
            println("❌ Condition NOT met - dialog will NOT show")
            if (!requestSubmitted) {
                println("   ❌ requestSubmitted is false")
            }
            if (requestNumber.isNullOrEmpty()) {
                println("   ❌ requestNumber is null or empty")
            }
        }
    }

//        when (state) {
//            is ValidationState.Valid -> {
//                println("✅ Validation is Valid, proceeding...")
//                // Check if we need conditional routing (e.g., based on inspection status)
//                _storedValidationResult?.let { result ->
//                    println("🔘 Stored validation result: ${result::class.simpleName}")
//                    if (result is ValidationResult.Success) {
//                        when (val action = result.navigationAction) {
//                            is MarineUnitNavigationAction.RouteToConditionalStep -> {
//                                println("🔀 Conditional routing to step: ${action.targetStepIndex}")
//                                // Route to specific step based on condition (e.g., inspection status)
//                                goToStep(action.targetStepIndex)
//                                return
//                            }
//                            else -> {
//                                println("➡️ Regular next step, calling super.nextStep()")
//                                // Regular next step
//                                super.nextStep()
//                            }
//                        }
//                    } else {
//                        println("➡️ Result not Success, calling super.nextStep()")
//                        super.nextStep()
//                    }
//                } ?: run {
//                    println("➡️ No stored result, calling super.nextStep()")
//                    super.nextStep()
//                }
//            }
//            is ValidationState.Invalid -> {
//                println("❌ Validation is Invalid, showing RequestDetailScreen")
//                // Unit is ineligible - navigate to RequestDetailScreen
//                _storedValidationResult?.let { result ->
//                    if (result is ValidationResult.Success) {
//                        val action = result.navigationAction
//                        if (action is MarineUnitNavigationAction.ShowComplianceDetailScreen) {
//                            _navigationToComplianceDetail.mortgageValue = action
//                        }
//                    }
//                }
//            }
//            is ValidationState.Error -> {
//                println("❌ Validation error: ${state.message}")
//                // Show error message
//                _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown(state.message)
//            }
//            is ValidationState.Idle -> {
//                println("⚠️ Validation is Idle, calling super.nextStep()")
//                // No unit selected yet OR validation not triggered
//                // Let base validation handle this (it will check if selectedMarineUnits field is filled)
//                super.nextStep()
//            }
//            is ValidationState.Validating -> {
//                println("⏳ Still validating...")
//                // Still validating - wait
//                _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("جاري التحقق من الوحدة البحرية...")
//            }
//            is ValidationState.RequiresConfirmation -> {
//                println("⚠️ Requires confirmation, calling super.nextStep()")
//                // Handle confirmation if needed
//                super.nextStep()
//            }
//        }

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

    // ✅ NEW: Clear request submission success dialog
    fun clearRequestSubmissionSuccess() {
        _requestSubmissionSuccess.value = null
    }

    // ✅ NEW: Clear navigation flag after navigation is handled
    fun clearNavigationFlag() {
        _navigateToTransactionScreen.value = false
    }

    /**
     * ✅ NEW: Get status counts for profile screen
     * Fetches total count and breakdown by status (Accepted, Send, Rejected)
     */
    fun getStatusCounts() {
        viewModelScope.launch {
            try {
                _statusCountsLoading.value = true
                _statusCountsError.value = null

                // Get customerId from token using UserHelper
                val customerId = com.informatique.mtcit.util.UserHelper.getOwnerCivilId(appContext)
                if (customerId == null) {
                    println("❌ No customerId found in token")
                    _statusCountsError.value = "لم يتم العثور على معرف المستخدم"
                    _statusCountsLoading.value = false
                    return@launch
                }

                println("🔍 Fetching status counts for customerId: $customerId")

                val result = requestsApiService.getStatusCounts(customerId)

                result.onSuccess { response ->
                    println("✅ Status counts fetched successfully")
                    _statusCounts.value = response
                    _statusCountsLoading.value = false
                }.onFailure { exception ->
                    println("❌ Failed to fetch status counts: ${exception.message}")
                    _statusCountsError.value = exception.message ?: "فشل في جلب الإحصائيات"
                    _statusCountsLoading.value = false
                }
            } catch (e: Exception) {
                println("❌ Exception in getStatusCounts: ${e.message}")
                e.printStackTrace()
                _statusCountsError.value = "حدث خطأ غير متوقع"
                _statusCountsLoading.value = false
            }
        }
    }

    /**
     * ✅ NEW: Clear status counts error
     */
    fun clearStatusCountsError() {
        _statusCountsError.value = null
    }

    /**
     * ✅ NEW: Complete the resume after navigation to transaction screen
     * Called by MarineRegistrationScreen when it detects a pending resume
     */
    fun completeResumeAfterNavigation(transactionType: TransactionType) {
        val requestId = _pendingResumeRequestId ?: return

        println("🔄 Completing resume for request: $requestId")

        // ✅ Set resuming flag to prevent normal initialization
        _isResuming.value = true

        viewModelScope.launch {
            try {
                // Fetch request again
                val result = requestRepository.getRequestStatus(requestId, transactionType)

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

                    val finalStep = when {
                        resumeStep < totalSteps -> {
                            // Resume step exists - update current step directly
                            updateUiState { currentState ->
                                currentState.copy(currentStep = resumeStep)
                            }
                            println("✅ Updated currentStep to $resumeStep")
                            resumeStep
                        }
                        resumeStep == totalSteps -> {
                            // Last step was completed, go to last step (review/submit)
                            updateUiState { currentState ->
                                currentState.copy(currentStep = totalSteps - 1)
                            }
                            println("✅ Updated currentStep to ${totalSteps - 1}")
                            totalSteps - 1
                        }
                        else -> {
                            // Error: resume step beyond total steps
                            println("❌ Resume step $resumeStep exceeds total steps $totalSteps")
                            _error.value = com.informatique.mtcit.common.AppError.Unknown("خطأ في استعادة المعاملة")
                            -1
                        }
                    }

                    // ✅ IMPORTANT: Wait for UI state to actually update
                    delay(300)
                    println("✅ Final currentStep: ${uiState.value.currentStep}")

                    // ✅ CRITICAL FIX: Trigger onStepOpened for the resume step to load payment details
                    if (finalStep >= 0) {
                        println("🔄 Triggering onStepOpened for step $finalStep to load step-specific data (e.g., payment details)")
                        strategy.onStepOpened(finalStep)
                        delay(200) // Give time for async loading
                    }

                    println("✅ Direct resume complete, clearing flags")

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
    fun setRequestIdAndCompleteResume(requestId: String, transactionType: TransactionType) {
        println("🔄 setRequestIdAndCompleteResume called with requestId: $requestId")
        _pendingResumeRequestId = requestId
        completeResumeAfterNavigation(transactionType)
    }

    /**
     * ✅ NEW: Resume directly with step number (no API call)
     * Called when lastCompletedStep is passed through navigation from ApiRequestDetailScreen
     * This avoids duplicate API calls since ApiRequestDetailScreen already fetched the data
     */
    fun resumeDirectlyWithStep(requestId: String, lastCompletedStep: Int, transactionType: TransactionType) {
        println("🔄 resumeDirectlyWithStep called with requestId: $requestId, lastCompletedStep: $lastCompletedStep, transactionType: $transactionType")

        // ✅ Set resuming flag to prevent normal initialization
        _isResuming.value = true
        _pendingResumeRequestId = requestId

        viewModelScope.launch {
            try {
                // ✅ Fetch request data from API to get form data and transaction type
                val result = requestRepository.getRequestStatus(requestId, transactionType)

                result.onSuccess { request ->
                    println("✅ Request data fetched - Resuming transaction WITHOUT recalculating step")
                    println("📋 Transaction type: ${request.type}")
                    println("📋 Status ID: ${request.statusId}")
                    println("📋 Is Draft: ${request.statusId == 1}")
                    // ✅ USE the lastCompletedStep from RequestRepository (which is smart-calculated based on status)
                    // NOT the one from navigation arguments!
                    println("📋 Using lastCompletedStep from RequestRepository: ${request.lastCompletedStep} (navigation had: $lastCompletedStep)")

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

                    // ✅ NEW: If this is a draft request, enable draft resume mode
                    if (request.statusId == 1) {
                        println("📝 DRAFT REQUEST DETECTED - Enabling draft resume mode")

                        // Enable draft resume in RegistrationRequestManager
                        if (strategy is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ||
                            strategy is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy) {

                            println("📝 Initializing draft tracking from API response...")

                            // Access the RegistrationRequestManager through the strategy
                            val registrationManager = when (strategy) {
                                is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ->
                                    strategy.getRegistrationRequestManager()
                                is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ->
                                    strategy.getRegistrationRequestManager()
                                else -> null
                            }

                            if (registrationManager != null) {
                                registrationManager.enableDraftResume()
                                registrationManager.initializeFromDraft(request.formData)
                                println("✅ Draft tracking initialized successfully")
                            } else {
                                println("⚠️ Could not access RegistrationRequestManager")
                            }
                        }
                    }

                    // Call processStepData to update strategy's accumulatedFormData
                    // This MUST happen before loadDynamicOptions() so the strategy knows what options to load
                    strategy.processStepData(0, request.formData)

                    println("✅ Strategy's internal state updated")

                    // ✅ CRITICAL FIX: Reload dynamic options AFTER form data is restored
                    // This ensures dropdowns get populated with the correct options based on the draft data
                    println("🔄 Reloading dynamic options with restored form data...")
                    strategy.loadDynamicOptions()
                    println("✅ Dynamic options reloaded")

                    // ✅ Step 3: Rebuild steps based on restored state
                    val rebuiltSteps = strategy.getSteps()
                    println("📊 Steps after rebuild: ${rebuiltSteps.size}")
                    println("📊 Step types: ${rebuiltSteps.mapIndexed { i, s -> "$i:${s.stepType}" }}")

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

                    // ✅ Step 4: Find the correct resume step using StepType-based logic
                    // USE request.lastCompletedStep from RequestRepository (not navigation parameter)
                    val totalSteps = uiState.value.steps.size

                    // ✅ NEW: Handle DRAFT vs APPROVED requests differently
                    val (resumeStep, lockedSteps, isResumedTransaction) = if (request.statusId == 1) {
                        // DRAFT: Calculate which step to resume at based on available data
                        // The backend doesn't track step completion for drafts, so we must infer it
                        val calculatedLastCompletedStep = calculateDraftLastCompletedStep(request.formData, rebuiltSteps)

                        val draftResumeStep = if (calculatedLastCompletedStep >= 0 && calculatedLastCompletedStep < totalSteps - 1) {
                            calculatedLastCompletedStep + 1  // Resume at next step after last completed
                        } else {
                            0  // Start from beginning if no steps completed
                        }
                        println("📝 DRAFT: Calculated last completed step: $calculatedLastCompletedStep")
                        println("📝 DRAFT: Resuming at step $draftResumeStep")
                        println("📝 DRAFT: All previous steps are UNLOCKED (user can edit)")

                        Triple(
                            draftResumeStep,
                            emptySet<Int>(),  // No locked steps for drafts
                            false  // Not a "resumed transaction" (user can navigate freely)
                        )
                    } else {
                        // APPROVED: Resume at payment step, lock all previous steps
                        val approvedResumeStep = findResumeStepByType(rebuiltSteps, request.lastCompletedStep)
                        val approvedLockedSteps = (0 until approvedResumeStep).toSet()

                        println("✅ APPROVED: Resuming at step $approvedResumeStep (last completed: ${request.lastCompletedStep})")
                        println("🔒 APPROVED: Locked steps: $approvedLockedSteps")

                        Triple(
                            approvedResumeStep,
                            approvedLockedSteps,
                            true  // This is a "resumed transaction" (locked previous steps)
                        )
                    }

                    println("🎯 Mapped lastCompletedStep=${request.lastCompletedStep} to resumeStep=$resumeStep")
                    println("🎯 Resume from step: $resumeStep (last completed was ${request.lastCompletedStep})")
                    println("🎯 Total steps: $totalSteps")
                    println("🔒 Locked steps: $lockedSteps")

                    // ✅ Step 5: Mark as resumed transaction and lock previous steps (or not, for drafts)
                    val completedStepsSet = if (request.statusId == 1) {
                        // For drafts, mark steps up to lastCompletedStep as completed
                        (0..request.lastCompletedStep.coerceAtMost(totalSteps - 1)).toSet()
                    } else {
                        // For approved requests, mark locked steps as completed
                        lockedSteps
                    }

                    updateUiState { currentState ->
                        currentState.copy(
                            isResumedTransaction = isResumedTransaction,
                            lockedSteps = lockedSteps,
                            completedSteps = completedStepsSet
                        )
                    }

                    // ✅ Step 6: Navigate to resume step - DIRECTLY update currentStep
                    println("✅ Directly updating currentStep to $resumeStep")

                    val finalStep = when {
                        resumeStep < totalSteps -> {
                            // Resume step exists - update current step directly
                            updateUiState { currentState ->
                                currentState.copy(currentStep = resumeStep)
                            }
                            println("✅ Updated currentStep to $resumeStep")
                            resumeStep
                        }
                        resumeStep == totalSteps -> {
                            // Last step was completed, go to last step (review/submit)
                            updateUiState { currentState ->
                                currentState.copy(currentStep = totalSteps - 1)
                            }
                            println("✅ Updated currentStep to ${totalSteps - 1}")
                            totalSteps - 1
                        }
                        else -> {
                            // Error: resume step beyond total steps
                            println("❌ Resume step $resumeStep exceeds total steps $totalSteps")
                            _error.value = com.informatique.mtcit.common.AppError.Unknown("خطأ في استعادة المعاملة")
                            -1
                        }
                    }

                    println("✅ Final currentStep: $finalStep")

                    // ✅ CRITICAL FIX: Trigger onStepOpened for the resume step to load payment details
                    if (finalStep >= 0) {
                        println("🔄 Triggering onStepOpened for step $finalStep to load step-specific data (dropdown options, payment details, etc.)")

                        // ✅ CRITICAL: For drafts, we need to load dropdown options for ALL steps with data
                        // so when user navigates to those steps, the dropdowns show the correct selected values
                        if (request.statusId == 1 && finalStep > 0) {
                            println("📝 DRAFT: Loading dropdown options for all steps with data...")

                            // Load lookups for all steps that have required lookups (async, no delays needed)
                            for (stepIdx in 0..finalStep) {
                                val stepData = rebuiltSteps.getOrNull(stepIdx)
                                if (stepData != null && stepData.requiredLookups.isNotEmpty()) {
                                    println("   📥 Loading lookups for step $stepIdx (${stepData.titleRes}): ${stepData.requiredLookups}")
                                    strategy.onStepOpened(stepIdx)
                                }
                            }
                            println("✅ DRAFT: All step lookups loading initiated (async)")
                        } else {
                            // For non-draft requests, just load the current step's data
                            strategy.onStepOpened(finalStep)
                        }


                        // ✅ CRITICAL FIX #2: Recalculate canProceedToNext after loading payment details
                        // This ensures the "Pay" button is enabled when payment data is loaded
                        println("🔄 Recalculating canProceedToNext after loading step data...")
                        val updatedCanProceed = navigationUseCase.canProceedToNext(
                            finalStep,
                            uiState.value.steps,
                            uiState.value.formData
                        )
                        println("✅ Updated canProceedToNext: $updatedCanProceed")

                        updateUiState { currentState ->
                            currentState.copy(canProceedToNext = updatedCanProceed)
                        }
                    }

                    println("✅ Direct resume complete, clearing flags")

                    // Clear pending request ID and resuming flag
                    _pendingResumeRequestId = null
                    _isResuming.value = false
                }

                result.onFailure { error ->
                    println("❌ Failed to fetch request data: ${error.message}")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown(
                        error.message ?: "فشل في استعادة المعاملة"
                    )
                    _pendingResumeRequestId = null
                    _isResuming.value = false
                }

            } catch (e: Exception) {
                println("❌ Exception in direct resume: ${e.message}")
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
     * NEW: Validate and submit form for Temporary Registration
     * This method should be called from UI instead of submitForm() for Temporary Registration
     * Validates inspection status before actual submission
     */
    fun validateAndSubmit() {
        val currentState = uiState.value
        submitForm()

//        viewModelScope.launch {
//            try {
//                // Get selected marine unit ID from form data
//                val selectedUnitsJson = currentState.formData["selectedMarineUnits"]
//                val isAddingNewUnit = currentState.formData["isAddingNewUnit"]?.toBoolean() ?: false
//
//                println("🔍 Selected units JSON: $selectedUnitsJson")
//                println("🔍 Is adding new unit: $isAddingNewUnit")
//                println("🔍 All form data keys: ${currentState.formData.keys}")
//
//                // Check if user is adding a NEW marine unit by looking for multiple possible field indicators
//                val hasNewUnitData = currentState.formData.containsKey("marineUnitName") ||
//                                    currentState.formData.containsKey("unitName") ||
//                                    currentState.formData.containsKey("callSign") ||
//                                    currentState.formData.containsKey("imoNumber") ||
//                                    currentState.formData.containsKey("registrationPort") ||
//                                    (selectedUnitsJson == "[]" && currentState.formData.size > 2) // Has form data but no selection
//
//                println("🔍 hasNewUnitData: $hasNewUnitData")
//                println("🔍 Form data size: ${currentState.formData.size}")
//
//                if ((selectedUnitsJson.isNullOrEmpty() || selectedUnitsJson == "[]") && !hasNewUnitData) {
//                    println("❌ No marine unit selected and no new unit data")
//                    _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("الرجاء اختيار وحدة بحرية أو إضافة وحدة جديدة")
//                    return@launch
//                }
//
//                // ✅ DYNAMIC: Check if the current strategy supports marine unit validation
//                val validatableStrategy = currentStrategy as? MarineUnitValidatable
//                if (validatableStrategy == null) {
//                    println("⚠️ Current strategy (${currentStrategy!!::class.simpleName}) does not support marine unit validation - proceeding with normal flow")
//                    submitForm()
//                    return@launch
//                }
//
//                println("✅ Strategy ${validatableStrategy::class.simpleName} supports marine unit validation")
//
//                val userId = getCurrentUserId()
//                val validationResult: ValidationResult?
//
//                // Case 1: User is adding a NEW marine unit
//                if (hasNewUnitData) {
//                    println("✅ User is adding a NEW marine unit")
//
//                    // Extract new unit data from form - try multiple possible field names
//                    val unitName = currentState.formData["marineUnitName"]
//                        ?: currentState.formData["unitName"]
//                        ?: currentState.formData["callSign"]  // Fallback to callSign if name not found
//                        ?: "وحدة بحرية جديدة"
//
//                    val unitType = currentState.formData["unitType"]
//                        ?: currentState.formData["unitClassification"]
//                        ?: ""
//
//                    val registrationPort = currentState.formData["registrationPort"] ?: ""
//                    val imo = currentState.formData["imoNumber"] ?: currentState.formData["imo"] ?: ""
//                    val callSign = currentState.formData["callSign"] ?: ""
//                    val activity = currentState.formData["maritimeactivity"] ?: ""
//                    val length = currentState.formData["length"] ?: currentState.formData["totalLength"] ?: ""
//                    val width = currentState.formData["width"] ?: currentState.formData["totalWidth"] ?: ""
//                    val height = currentState.formData["height"] ?: ""
//
//                    println("📋 New unit data: name=$unitName, type=$unitType, port=$registrationPort, callSign=$callSign")
//
//                    // Create a temporary MarineUnit object for validation
//                    val newUnit = MarineUnit(
//                        id = "new_${System.currentTimeMillis()}", // Temporary ID
//                        shipName = unitName,
//                        imoNumber = imo,
//                        callSign = callSign,
//                        mmsiNumber = "", // Will be assigned after successful registration
//                        portOfRegistry = PortOfRegistry(registrationPort),
//                        marineActivity = MarineActivity(0), // Default or parse from activity
//                        shipType = ShipType(0), // Default or parse from unitType
//                        isTemp = "1", // Temporary registration
//                        totalLength = length,
//                        totalWidth = width,
//                        height = height
//                    )
//
//                    // ✅ DYNAMIC: Use the interface method for validating new units
//                    validationResult = try {
//                        validatableStrategy.validateNewMarineUnit(newUnit, userId)
//                    } catch (e: Exception) {
//                        println("❌ Validation error: ${e.message}")
//                        e.printStackTrace()
//                        ValidationResult.Error(e.message ?: "Validation failed")
//                    }
//
//                } else {
//                    // Case 2: User selected an EXISTING marine unit
//                    println("✅ User selected an EXISTING marine unit")
//
//                    // Parse selected unit ID (maritimeId from JSON)
//                    val selectedMaritimeIds = try {
//                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(selectedUnitsJson!!)
//                    } catch (e: Exception) {
//                        println("❌ Failed to parse selected units: ${e.message}")
//                        _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("خطأ في قراءة الوحدة المختارة")
//                        return@launch
//                    }
//
//                    if (selectedMaritimeIds.isEmpty()) {
//                        println("❌ No units in selection")
//                        _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("الرجاء اختيار وحدة بحرية")
//                        return@launch
//                    }
//
//                    val selectedMaritimeId = selectedMaritimeIds.first()
//                    println("🔍 Selected maritime ID: $selectedMaritimeId")
//
//                    // Get marine units from the strategy (cast to TransactionStrategy to access loadDynamicOptions)
//                    val strategyAsTransaction = validatableStrategy as? TransactionStrategy
//                    if (strategyAsTransaction == null) {
//                        println("❌ Strategy doesn't implement TransactionStrategy")
//                        _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("خطأ في النظام")
//                        return@launch
//                    }
//
//                    val dynamicOptions = strategyAsTransaction.loadDynamicOptions()
//                    val marineUnitsAny = dynamicOptions["marineUnits"]
//
//                    // Marine units are returned as List<MarineUnit> from the strategy
//                    val marineUnits = when (marineUnitsAny) {
//                        is List<*> -> {
//                            // Filter and safely cast to MarineUnit
//                            marineUnitsAny.mapNotNull { it as? MarineUnit }
//                        }
//                        else -> emptyList()
//                    }
//
//                    if (marineUnits.isEmpty()) {
//                        println("⚠️ No marine units found in dynamic options")
//                    }
//
//                    val selectedUnit = marineUnits.firstOrNull { unit ->
//                        unit.maritimeId == selectedMaritimeId
//                    }
//
//                    if (selectedUnit == null) {
//                        println("❌ Selected unit not found")
//                        _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("الوحدة البحرية المختارة غير موجودة")
//                        return@launch
//                    }
//
//                    println("✅ Found selected unit: ${selectedUnit.name}, id: ${selectedUnit.id}")
//
//                    // Validate the selected unit's inspection status
//                    validationResult = validateTemporaryRegistrationUnit(validatableStrategy, selectedUnit.id, userId)
//                }
//
//                // Handle validation result (same for both cases)
//                if (validationResult == null) {
//                    println("❌ Validation returned null")
//                    _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("فشل التحقق من حالة الفحص")
//                    return@launch
//                }
//
//                when (validationResult) {
//                    is ValidationResult.Success -> {
//                        when (val action = validationResult.navigationAction) {
//                            is MarineUnitNavigationAction.ProceedToNextStep -> {
//                                // Inspection is valid - proceed with actual submission
//                                println("✅ Inspection validated, proceeding with submission")
//                                submitForm()
//                            }
//                            is MarineUnitNavigationAction.ShowComplianceDetailScreen -> {
//                                // Inspection failed (pending/not verified) - show RequestDetailScreen
//                                println("⏳ Inspection validation failed, showing RequestDetailScreen")
//
//                                // ✅ NEW: Save request progress if status is PENDING
//                                val isPending = action.rejectionTitle.contains("قيد المعالجة")
//                                if (isPending) {
//                                    println("💾 Saving request progress (status: PENDING)")
//                                    saveRequestProgress(
//                                        marineUnit = action.marineUnit,
//                                        currentStep = currentState.currentStep
//                                    )
//                                }
//
//                                _navigationToComplianceDetail.mortgageValue = action
//                            }
//                            else -> {
//                                println("❌ Unexpected navigation action: ${action::class.simpleName}")
//                                _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown("خطأ في التحقق من حالة الفحص")
//                            }
//                        }
//                    }
//                    is ValidationResult.Error -> {
//                        println("❌ Validation error: ${validationResult.message}")
//                        _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown(validationResult.message)
//                    }
//                }
//
//            } catch (e: Exception) {
//                println("❌ Exception during validation: ${e.message}")
//                e.printStackTrace()
//                _error.mortgageValue = com.informatique.mtcit.common.AppError.Unknown(e.message ?: "حدث خطأ أثناء التحقق")
//            }
//        }
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

        // ✅ DYNAMIC: Check if the current strategy supports marine unit validation
        val validatableStrategy = currentStrategy as? MarineUnitValidatable
        // For Temporary Registration, validate inspection on review step
        if (validatableStrategy != null) {
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
        return MarineUnit(
            id = "placeholder",
            shipName = "وحدة بحرية",
            callSign = "",
            mmsiNumber = "",
            portOfRegistry = com.informatique.mtcit.business.transactions.shared.PortOfRegistry("")
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
            TransactionType.REQUEST_FOR_INSPECTION,
            TransactionType.SUSPEND_PERMANENT_REGISTRATION,
            TransactionType.CANCEL_PERMANENT_REGISTRATION,
            TransactionType.MORTGAGE_CERTIFICATE,
            TransactionType.RELEASE_MORTGAGE,
            TransactionType.ISSUE_NAVIGATION_PERMIT,
            TransactionType.RENEW_NAVIGATION_PERMIT -> true
            else -> false
        }
    }

    // ✅ NEW: Success state for transaction submission
    private val _transactionSubmitSuccess = MutableStateFlow(false)
    val transactionSubmitSuccess: StateFlow<Boolean> = _transactionSubmitSuccess.asStateFlow()

    /**
     * Clear success flag after it's been handled by UI
     */
    fun clearTransactionSuccessFlag() {
        _transactionSubmitSuccess.value = false
    }

    /**
     * Clear navigation flags after navigation is complete
     */
    fun clearNavigationFlags() {
        _transactionSubmitSuccess.value = false
    }

    /**
     * ✅ NEW: Refresh expired access token
     * Called by UI when user clicks "Refresh Token" button in 401 error banner
     */
    fun refreshToken() {
        viewModelScope.launch {
            val success = handleTokenRefresh(authRepository)

            if (success) {
                println("✅ Token refreshed - user can continue their transaction")
                // Token refreshed successfully, error banner will be cleared automatically
                // User can now retry their last action (e.g., click Next again)
            } else {
                println("❌ Token refresh failed - user needs to login again")
                // Error will be shown via _error state flow
            }
        }
    }

    /**
     * ✅ Dismiss error banner (called when user clicks X on error banner)
     */
    fun dismissError() {
        clearError() // Call base class method
    }

    /**
     * ✅ NEW: Refresh token and clear error (called when user clicks refresh button on 401 error banner)
     */
    fun refreshTokenAndRetry() {
        viewModelScope.launch {
            val success = handleTokenRefresh(authRepository)

            if (success) {
                println("✅ Token refreshed successfully - clearing error banner")
                clearError() // Call base class method to clear the error
                // User can now retry their action (click Next button again)
            } else {
                println("❌ Token refresh failed - keeping error banner")
                // Keep the error banner showing
            }
        }
    }

    /**
     * ✅ Hook called after successful form submission (review step complete)
     * This is called by BaseTransactionViewModel.submitForm() after strategy.submit() succeeds
     *
     * Sets a success flag that can be observed by UI or strategies.
     *
     * IMPORTANT: This does NOT navigate anywhere. Each strategy decides navigation by:
     * - Returning a specific step index from processStepData()
     * - Navigating to a conditional step
     * - Or letting the base flow handle it
     */
    override fun onFormSubmitSuccess() {
        println("✅ onFormSubmitSuccess called - transaction submitted successfully")

        // Set success flag (strategies can check this if needed)
        _transactionSubmitSuccess.value = true

        // No navigation here - let strategies control where to go next
    }

    /**
     * ✅ NEW: Find the correct resume step based on StepType instead of numeric index
     * This handles cases where step order changes during rebuild (e.g., conditional steps)
     */
    private fun findResumeStepByType(steps: List<StepData>, lastCompletedIndex: Int): Int {
        println("🔍 findResumeStepByType: lastCompletedIndex=$lastCompletedIndex, totalSteps=${steps.size}")
        println("🔍 Rebuilt step types: ${steps.mapIndexed { i, s -> "$i=${s.stepType}" }}")

        // The lastCompletedIndex refers to a step in the REBUILT list
        // Since we're resuming with the SAME form data, the steps should rebuild in the SAME order
        // So we can use lastCompletedIndex to find the completed step in the rebuilt list

        if (lastCompletedIndex !in steps.indices) {
            println("⚠️ Invalid lastCompletedIndex=$lastCompletedIndex (totalSteps=${steps.size})")
            // If index is beyond the list, it means Review was completed
            // Find the REVIEW step and look for what comes after it
            val reviewIndex = steps.indexOfFirst { it.stepType == StepType.REVIEW }
            if (reviewIndex >= 0 && lastCompletedIndex >= reviewIndex) {
                println("🔍 Index beyond list and >= review, looking for next step after REVIEW")
                return findStepAfterReview(steps, reviewIndex)
            }
            return 0 // Fallback to first step
        }

        // Get the StepType that was last completed from the rebuilt list
        val lastCompletedType = steps[lastCompletedIndex].stepType
        println("🔍 Last completed StepType at index $lastCompletedIndex: $lastCompletedType")

        // ✅ SMART: If last completed is REVIEW, find the actual next step in THIS strategy
        if (lastCompletedType == StepType.REVIEW) {
            println("🔍 Last completed step is REVIEW, finding next step based on strategy structure...")
            return findStepAfterReview(steps, lastCompletedIndex)
        }

        // Define the logical progression (what step comes AFTER each step)
        val nextStepMapping = mapOf(
            StepType.PERSON_TYPE to StepType.COMMERCIAL_REGISTRATION,
            StepType.COMMERCIAL_REGISTRATION to StepType.MARINE_UNIT_SELECTION,
            StepType.MARINE_UNIT_SELECTION to StepType.MARINE_UNIT_DATA,
            StepType.MARINE_UNIT_DATA to StepType.SHIP_DIMENSIONS,
            StepType.SHIP_DIMENSIONS to StepType.SHIP_WEIGHTS,
            StepType.SHIP_WEIGHTS to StepType.ENGINE_INFO,
            StepType.ENGINE_INFO to StepType.OWNER_INFO,
            StepType.OWNER_INFO to StepType.DOCUMENTS,
            StepType.DOCUMENTS to StepType.REVIEW,
            StepType.REVIEW to StepType.CUSTOM,  // ← CUSTOM after REVIEW is Marine Unit Name Selection
            StepType.MARINE_UNIT_NAME_SELECTION to StepType.PAYMENT,
            StepType.PAYMENT to StepType.PAYMENT_SUCCESS,
            StepType.CUSTOM to null  // Will be handled specially
        )

        // Special handling for CUSTOM steps (need to determine by position)
        if (lastCompletedType == StepType.CUSTOM) {
            println("🔍 Last completed step is CUSTOM, checking position...")
            // If CUSTOM step is before REVIEW, it's MARINE_UNIT_SELECTION, so next is MARINE_UNIT_DATA
            // If CUSTOM step is after REVIEW, it's MARINE_UNIT_NAME_SELECTION, so next is PAYMENT
            val reviewIndex = steps.indexOfFirst { it.stepType == StepType.REVIEW }
            if (reviewIndex >= 0) {
                if (lastCompletedIndex < reviewIndex) {
                    println("🔍 CUSTOM step before REVIEW -> Looking for next step")
                    return (lastCompletedIndex + 1).coerceIn(0, steps.size - 1)
                } else {
                    println("🔍 CUSTOM step after REVIEW (Marine Unit Name) -> Next is PAYMENT")
                    val paymentIndex = steps.indexOfFirst { it.stepType == StepType.PAYMENT }
                    if (paymentIndex >= 0) {
                        return paymentIndex
                    }
                }
            }
            // Fallback: just go to next step
            return (lastCompletedIndex + 1).coerceIn(0, steps.size - 1)
        }

        // Find what step should come NEXT after the completed step
        var nextExpectedType = nextStepMapping[lastCompletedType]

        if (nextExpectedType == null) {
            println("⚠️ No next step defined for $lastCompletedType, using next index")
            return (lastCompletedIndex + 1).coerceIn(0, steps.size - 1)
        }

        println("🔍 Next expected StepType: $nextExpectedType")

        // Try to find the next expected step in the rebuilt list
        // If not found, try the step after that, etc. (to handle conditional steps)
        var currentType: StepType? = nextExpectedType
        var attempts = 0
        val maxAttempts = 10  // Prevent infinite loop

        while (currentType != null && attempts < maxAttempts) {
            val foundIndex = steps.indexOfFirst { it.stepType == currentType }
            if (foundIndex >= 0) {
                println("✅ Found next step: $currentType at index $foundIndex")
                return foundIndex
            }

            println("⚠️ Step $currentType not found in rebuilt steps, trying next...")
            currentType = nextStepMapping[currentType]
            attempts++
        }

        // Fallback: just go to next index
        println("⚠️ Could not find expected next step, using next index")
        return (lastCompletedIndex + 1).coerceIn(0, steps.size - 1)
    }

    /**
     * ✅ NEW: Smart helper to find what step comes after REVIEW
     * Different strategies have different steps after review:
     * - Temp Registration & Change Name: CUSTOM (Marine Unit Name Selection)
     * - Others: PAYMENT
     */
    private fun findStepAfterReview(steps: List<StepData>, reviewIndex: Int): Int {
        println("🔍 Finding step after REVIEW (index $reviewIndex)...")

        // Look for next step after review
        val nextSteps = steps.drop(reviewIndex + 1)

        if (nextSteps.isEmpty()) {
            println("⚠️ No steps after REVIEW")
            return reviewIndex // Stay at review
        }

        // Check what type comes after REVIEW in this strategy
        val firstStepAfterReview = nextSteps.first()
        val nextStepIndex = reviewIndex + 1

        println("✅ First step after REVIEW is ${firstStepAfterReview.stepType} at index $nextStepIndex")

        when (firstStepAfterReview.stepType) {
            StepType.CUSTOM -> {
                // This is Marine Unit Name Selection (Temp Registration or Change Name)
                println("✅ Strategy has CUSTOM (Marine Unit Name) after REVIEW")
                return nextStepIndex
            }
            StepType.PAYMENT -> {
                // Direct to payment (other transactions)
                println("✅ Strategy has PAYMENT directly after REVIEW")
                return nextStepIndex
            }
            else -> {
                // Unexpected, but go to next step anyway
                println("⚠️ Unexpected step type after REVIEW: ${firstStepAfterReview.stepType}")
                return nextStepIndex
            }
        }
    }

    /**
     * ✅ NEW: Calculate which step was last completed in a draft based on available data
     * The backend doesn't track step completion for drafts, so we infer it from formData
     *
     * IMPORTANT: Backend only returns ship-related data (callSign, dimensions, etc.)
     * It does NOT return Person Type or Commercial Registration selections
     * So we automatically mark those as completed if ANY ship data exists
     */
    private fun calculateDraftLastCompletedStep(formData: Map<String, String>, steps: List<StepData>): Int {
        println("🔍 Calculating last completed step for draft...")
        println("📊 Available data keys: ${formData.keys.joinToString(", ")}")

        // ✅ FIX: Backend doesn't return Person Type or Commercial Registration
        // If ANY ship data exists, those early steps must have been completed
        val hasAnyShipData = formData.containsKey("callSign") ||
                            formData.containsKey("imoNumber") ||
                            formData.containsKey("shipInfoId") ||
                            formData.containsKey("isAddingNewUnit")

        // Check which ship-related data points exist (these ARE returned by backend)
        val hasMarineUnitSelection = formData.containsKey("isAddingNewUnit") && formData["isAddingNewUnit"] == "true"
        val hasBasicShipData = formData.containsKey("callSign") || formData.containsKey("imoNumber")
        val hasDimensions = formData.containsKey("overallLength") || formData.containsKey("overallWidth") || formData.containsKey("depth")
        val hasWeights = formData.containsKey("grossTonnage") || formData.containsKey("netTonnage") || formData.containsKey("staticLoad")
        val hasEngineData = formData.containsKey("engineManufacturer") || formData.containsKey("engines")
        val hasOwnerData = formData.containsKey("ownerName") || formData.containsKey("owners")
        val hasDocuments = formData["hasDocuments"]?.toString()?.toBoolean() == true

        println("📊 Data completeness:")
        println("   - Has Any Ship Data: $hasAnyShipData (if true, Person Type & Commercial Reg are auto-completed)")
        println("   - Marine Unit Selection: $hasMarineUnitSelection")
        println("   - Basic Ship Data: $hasBasicShipData")
        println("   - Dimensions: $hasDimensions")
        println("   - Weights: $hasWeights")
        println("   - Engine Data: $hasEngineData")
        println("   - Owner Data: $hasOwnerData")
        println("   - Documents Uploaded: $hasDocuments")

        // Find the last completed step by checking data availability
        // We map data availability to step types
        var lastCompletedStep = -1

        for ((index, step) in steps.withIndex()) {
            val isStepCompleted = when (step.stepType) {
                // ✅ FIX: Auto-complete these if ANY ship data exists (backend doesn't return them)
                StepType.PERSON_TYPE -> hasAnyShipData
                StepType.COMMERCIAL_REGISTRATION -> hasAnyShipData

                // These ARE returned by backend, so check actual data
                StepType.MARINE_UNIT_SELECTION -> hasMarineUnitSelection
                StepType.MARINE_UNIT_DATA -> hasBasicShipData
                StepType.SHIP_DIMENSIONS -> hasDimensions
                StepType.SHIP_WEIGHTS -> hasWeights
                StepType.ENGINE_INFO -> hasEngineData
                StepType.OWNER_INFO -> hasOwnerData
                StepType.DOCUMENTS -> hasDocuments  // ✅ Check if documents uploaded

                StepType.CUSTOM -> {
                    // Custom steps could be Marine Unit Selection (before ship data steps)
                    // Check if we have "isAddingNewUnit" flag
                    hasMarineUnitSelection
                }

                else -> false  // Unknown steps are not completed
            }

            if (isStepCompleted) {
                lastCompletedStep = index
                println("   ✅ Step $index (${step.stepType}) has data")
            } else {
                println("   ❌ Step $index (${step.stepType}) missing data - stopping here")
                break  // Stop at first incomplete step
            }
        }

        println("📝 Calculated last completed step: $lastCompletedStep")
        return lastCompletedStep
    }

    /**
     * Update engine immediately via API (for engines with dbId)
     * Called when user edits an engine from the UI
     */
    fun updateEngineImmediate(engine: com.informatique.mtcit.ui.components.EngineData) {
        viewModelScope.launch {
            try {
                val requestId = uiState.value.formData["requestId"]?.toIntOrNull()
                val engineId = engine.dbId

                if (requestId == null || engineId == null) {
                    println("❌ Cannot update engine: requestId=$requestId, engineId=$engineId")
                    return@launch
                }

                println("🔄 Updating engine via API: requestId=$requestId, engineId=$engineId")

                // Get strategy's registration manager
                val strategy = currentStrategy
                if (strategy is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.RequestInspectionStrategy) {

                    // Get the RegistrationRequestManager from strategy
                    val registrationManager = when (strategy) {
                        is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.RequestInspectionStrategy ->
                            strategy.getRegistrationRequestManager()
                        else -> {
                            println("❌ Strategy doesn't support engine update")
                            return@launch
                        }
                    }

                    // Call UPDATE API
                    val result = registrationManager.updateEngineImmediate(appContext, requestId, engine)

                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Success -> {
                            println("✅ Engine updated successfully via API")
                        }
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Error -> {
                            println("❌ Failed to update engine: ${result.message}")
                            // Could show error to user here if needed
                        }
                    }
                } else {
                    println("⚠️ Current strategy doesn't support engine operations")
                }
            } catch (e: Exception) {
                println("❌ Exception updating engine: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete engine immediately via API (for engines with dbId)
     * Called when user deletes an engine from the UI
     */
    fun deleteEngineImmediate(engine: com.informatique.mtcit.ui.components.EngineData) {
        viewModelScope.launch {
            try {
                val requestId = uiState.value.formData["requestId"]?.toIntOrNull()
                val engineId = engine.dbId

                if (requestId == null || engineId == null) {
                    println("❌ Cannot delete engine: requestId=$requestId, engineId=$engineId")
                    return@launch
                }

                println("🗑️ Deleting engine via API: requestId=$requestId, engineId=$engineId")

                // Get strategy's registration manager
                val strategy = currentStrategy
                if (strategy is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.RequestInspectionStrategy) {

                    // Get the RegistrationRequestManager from strategy
                    val registrationManager = when (strategy) {
                        is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.RequestInspectionStrategy ->
                            strategy.getRegistrationRequestManager()
                        else -> {
                            println("❌ Strategy doesn't support engine deletion")
                            return@launch
                        }
                    }

                    // Call DELETE API
                    val result = registrationManager.deleteEngineImmediate(requestId, engineId)

                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Success -> {
                            println("✅ Engine deleted successfully from API")
                        }
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Error -> {
                            println("❌ Failed to delete engine: ${result.message}")
                            // Could show error to user here if needed
                        }
                    }
                } else {
                    println("⚠️ Current strategy doesn't support engine operations")
                }
            } catch (e: Exception) {
                println("❌ Exception deleting engine: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Update owner immediately via API (for owners with dbId)
     * Called when user edits an owner from the UI
     */
    fun updateOwnerImmediate(owner: com.informatique.mtcit.ui.components.OwnerData) {
        viewModelScope.launch {
            try {
                val requestId = uiState.value.formData["requestId"]?.toIntOrNull()
                val ownerId = owner.dbId

                if (requestId == null || ownerId == null) {
                    println("❌ Cannot update owner: requestId=$requestId, ownerId=$ownerId")
                    return@launch
                }

                println("🔄 Updating owner via API: requestId=$requestId, ownerId=$ownerId")

                // Get strategy's registration manager
                val strategy = currentStrategy
                if (strategy is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.RequestInspectionStrategy) {

                    // Get the RegistrationRequestManager from strategy
                    val registrationManager = when (strategy) {
                        is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.RequestInspectionStrategy ->
                            strategy.getRegistrationRequestManager()
                        else -> {
                            println("❌ Strategy doesn't support owner update")
                            return@launch
                        }
                    }

                    // Call UPDATE API
                    val result = registrationManager.updateOwnerImmediate(appContext, requestId, owner)

                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Success -> {
                            println("✅ Owner updated successfully via API")
                        }
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Error -> {
                            println("❌ Failed to update owner: ${result.message}")
                            // Could show error to user here if needed
                        }
                    }
                } else {
                    println("⚠️ Current strategy doesn't support owner operations")
                }
            } catch (e: Exception) {
                println("❌ Exception updating owner: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete owner immediately via API (for owners with dbId)
     * Called when user deletes an owner from the UI
     */
    fun deleteOwnerImmediate(owner: com.informatique.mtcit.ui.components.OwnerData) {
        viewModelScope.launch {
            try {
                val requestId = uiState.value.formData["requestId"]?.toIntOrNull()
                val ownerId = owner.dbId

                if (requestId == null || ownerId == null) {
                    println("❌ Cannot delete owner: requestId=$requestId, ownerId=$ownerId")
                    return@launch
                }

                println("🗑️ Deleting owner via API: requestId=$requestId, ownerId=$ownerId")

                // Get strategy's registration manager
                val strategy = currentStrategy
                if (strategy is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ||
                    strategy is com.informatique.mtcit.business.transactions.RequestInspectionStrategy) {

                    // Get the RegistrationRequestManager from strategy
                    val registrationManager = when (strategy) {
                        is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy ->
                            strategy.getRegistrationRequestManager()
                        is com.informatique.mtcit.business.transactions.RequestInspectionStrategy ->
                            strategy.getRegistrationRequestManager()
                        else -> {
                            println("❌ Strategy doesn't support owner delete")
                            return@launch
                        }
                    }

                    // Call DELETE API
                    val result = registrationManager.deleteOwnerImmediate(requestId, ownerId)

                    when (result) {
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Success -> {
                            println("✅ Owner deleted successfully from API")
                            // Note: Local list removal is handled by OwnerListManager immediately
                        }
                        is com.informatique.mtcit.business.transactions.shared.UpdateResult.Error -> {
                            println("❌ Failed to delete owner: ${result.message}")
                            // Could show error to user here if needed
                        }
                    }
                } else {
                    println("⚠️ Current strategy doesn't support owner operations")
                }
            } catch (e: Exception) {
                println("❌ Exception deleting owner: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * View draft document by calling the file preview API
     * Gets the MinIO URL from the server for the given refNo
     */
    fun viewDraftDocument(refNo: String, callback: (fileUrl: String?, error: String?) -> Unit) {
        viewModelScope.launch {
            try {
                println("🔍 Calling getFilePreview API for refNo=$refNo")

                // Get the repository from the current strategy
                val repository = when (val strategy = currentStrategy) {
                    is com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy -> {
                        // Access via reflection (private field)
                        strategy::class.java.getDeclaredField("repository").apply {
                            isAccessible = true
                        }.get(strategy) as? com.informatique.mtcit.data.repository.ShipRegistrationRepository
                    }
                    is com.informatique.mtcit.business.transactions.PermanentRegistrationStrategy -> {
                        strategy::class.java.getDeclaredField("repository").apply {
                            isAccessible = true
                        }.get(strategy) as? com.informatique.mtcit.data.repository.ShipRegistrationRepository
                    }
                    is com.informatique.mtcit.business.transactions.RequestInspectionStrategy -> {
                        strategy::class.java.getDeclaredField("repository").apply {
                            isAccessible = true
                        }.get(strategy) as? com.informatique.mtcit.data.repository.ShipRegistrationRepository
                    }
                    else -> null
                }

                if (repository == null) {
                    println("❌ Could not get repository from strategy")
                    callback(null, "Unable to access file preview service")
                    return@launch
                }

                // Call the API through repository
                val result = repository.getFilePreview(refNo)

                result.fold(
                    onSuccess = { fileUrl ->
                        println("✅ File preview URL received: $fileUrl")
                        callback(fileUrl, null)
                    },
                    onFailure = { exception ->
                        println("❌ Failed to get file preview: ${exception.message}")
                        callback(null, exception.message ?: "Failed to load document")
                    }
                )
            } catch (e: Exception) {
                println("❌ Exception in viewDraftDocument: ${e.message}")
                e.printStackTrace()
                callback(null, "Failed to load document")
            }
        }
    }
}
