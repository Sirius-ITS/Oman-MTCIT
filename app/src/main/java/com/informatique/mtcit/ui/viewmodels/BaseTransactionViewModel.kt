package com.informatique.mtcit.ui.viewmodels

import com.informatique.mtcit.business.transactions.shared.StepType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.transactions.FieldFocusResult
import com.informatique.mtcit.business.transactions.TransactionState
import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.ApiException
import com.informatique.mtcit.common.AppError
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.common.ResourceProvider
import com.informatique.mtcit.ui.base.UIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared data classes used by ViewModels
 */
data class StepData(
    val stepType: StepType = StepType.CUSTOM,  // ✅ NEW: Type-safe step identification
    val titleRes: Int,
    val descriptionRes: Int,
    val fields: List<FormField>,
    val requiredLookups: List<String> = emptyList() // List of lookup keys to fetch when step is opened
)

/**
 * Navigation events for file operations
 */
sealed class FileNavigationEvent {
    data class OpenFilePicker(val fieldId: String, val allowedTypes: List<String>) : FileNavigationEvent()
    data class ViewFile(val fileUri: String, val fileType: String) : FileNavigationEvent()
    data class RemoveFile(val fieldId: String) : FileNavigationEvent()
}

/**
 * ✅ NEW: File viewer state for displaying files in a dialog overlay
 * This preserves form state instead of navigating to a separate screen
 */
data class FileViewerState(
    val isOpen: Boolean = false,
    val fileUri: String = "",
    val fileName: String = "",
    val mimeType: String = ""
)

/**
 * Base Transaction ViewModel - Abstract base class for category-specific ViewModels
 * Contains common transaction logic shared across all categories
 *
 * Category-specific ViewModels extend this:
 * - ShipTransactionViewModel (handles all ship transactions)
 * - VehicleTransactionViewModel (handles all vehicle transactions)
 * - BuildingTransactionViewModel (handles all building transactions)
 */
abstract class BaseTransactionViewModel(
    protected val resourceProvider: ResourceProvider,
    protected val navigationUseCase: StepNavigationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionState())
    val uiState: StateFlow<TransactionState> = _uiState.asStateFlow()

    private val _submissionState = MutableStateFlow<UIState<Boolean>>(UIState.Empty)
    val submissionState: StateFlow<UIState<Boolean>> = _submissionState.asStateFlow()

    // Field-specific loading states (e.g., company lookup)
    private val _fieldLoadingStates = MutableStateFlow<Set<String>>(emptySet())
    val fieldLoadingStates: StateFlow<Set<String>> = _fieldLoadingStates.asStateFlow()

    // ✅ NEW: Per-lookup loading states (e.g., "ports", "countries", "shipTypes")
    // Map<lookupKey, Boolean> - true = loading, false = loaded
    private val _lookupLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val lookupLoadingStates: StateFlow<Map<String, Boolean>> = _lookupLoadingStates.asStateFlow()

    // ✅ NEW: Loaded lookup data with success status
    // Map<lookupKey, Pair<data, success>>
    private val _loadedLookupData = MutableStateFlow<Map<String, Pair<List<String>, Boolean>>>(emptyMap())
    val loadedLookupData: StateFlow<Map<String, Pair<List<String>, Boolean>>> = _loadedLookupData.asStateFlow()

    // File navigation events
    private val _fileNavigationEvent = MutableStateFlow<FileNavigationEvent?>(null)
    val fileNavigationEvent: StateFlow<FileNavigationEvent?> = _fileNavigationEvent.asStateFlow()

    // ✅ NEW: File viewer state for dialog overlay (preserves form state)
    private val _fileViewerState = MutableStateFlow(FileViewerState())
    val fileViewerState: StateFlow<FileViewerState> = _fileViewerState.asStateFlow()

    // Error state
    protected val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    // Current transaction strategy
    protected var currentStrategy: TransactionStrategy? = null

    // Flag indicating we're processing the Next action to prevent duplicate clicks / UI freezes
    private val _isProcessingNext = MutableStateFlow(false)
    val isProcessingNext: StateFlow<Boolean> = _isProcessingNext.asStateFlow()

    val _showToastEvent = MutableStateFlow<String?>(null)
    val showToastEvent: StateFlow<String?> = _showToastEvent.asStateFlow()


    // ✅ Success dialog state (for transaction completion)
    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog: StateFlow<Boolean> = _showSuccessDialog.asStateFlow()

    private val _successMessage = MutableStateFlow("")
    val successMessage: StateFlow<String> = _successMessage.asStateFlow()

    /**
     * ✅ NEW: Protected method to update UI state from child classes
     * This allows child classes to update state without accessing private _uiState
     */
    protected fun updateUiState(update: (TransactionState) -> TransactionState) {
        _uiState.value = update(_uiState.value)
    }

    /**
     * Abstract method to create strategy for specific transaction type
     * Each category ViewModel implements this to create its own strategies
     */
    protected abstract suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy

    /**
     * Initialize transaction with specific type
     * This must be called before using the ViewModel
     */
    fun  initializeTransaction(transactionType: TransactionType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Create category-specific strategy
                currentStrategy = createStrategy(transactionType)
                // ✅ No longer needed - strategies get context via Hilt @ApplicationContext injection

                // ✅ Set up callback to rebuild steps when lookups are loaded (generic for all strategies)
                currentStrategy?.onStepsNeedRebuild = {
                    viewModelScope.launch {
                        println("🔄 Rebuilding steps after loading lookups...")
                        val rebuiltSteps = currentStrategy?.getSteps() ?: emptyList()

                        // Merge any strategy formData (pre-populated values) into UI state so fields pick up values
                        val strategyFormData = currentStrategy?.getFormData() ?: emptyMap()
                        val mergedFormData = _uiState.value.formData.toMutableMap().apply { putAll(strategyFormData) }

                        // ✅ CRITICAL FIX: Recalculate canProceedToNext after rebuilding steps
                        // This ensures the "Pay" button is enabled when payment data is loaded via onStepOpened
                        val currentStep = _uiState.value.currentStep
                        val updatedCanProceed = navigationUseCase.canProceedToNext(
                            currentStep,
                            rebuiltSteps,
                            mergedFormData
                        )
                        println("🔄 Recalculated canProceedToNext after step rebuild: $updatedCanProceed")

                        _uiState.value = _uiState.value.copy(
                            steps = rebuiltSteps,
                            formData = mergedFormData,
                            canProceedToNext = updatedCanProceed  // ✅ Update canProceedToNext
                        )
                        println("✅ Steps rebuilt with ${rebuiltSteps.size} steps")
                        println("   Merged strategy formData keys: ${strategyFormData.keys}")
                     }
                }

                // ✅ NEW: Set up callback for when a lookup starts loading
                currentStrategy?.onLookupStarted = { lookupKey ->
                    viewModelScope.launch {
                        println("📥 Lookup started: $lookupKey")
                        _lookupLoadingStates.value = _lookupLoadingStates.value + (lookupKey to true)
                    }
                }

                // ✅ NEW: Set up callback for when a lookup completes (success or failure)
                currentStrategy?.onLookupCompleted = { lookupKey, data, success ->
                    viewModelScope.launch {
                        println("✅ Lookup completed: $lookupKey (success=$success, items=${data.size})")
                        _lookupLoadingStates.value = _lookupLoadingStates.value + (lookupKey to false)
                        _loadedLookupData.value = _loadedLookupData.value + (lookupKey to (data to success))
                    }
                }

                // Load dynamic options FIRST (before getting steps)
                currentStrategy?.loadDynamicOptions() ?: emptyMap()

                // Now get steps (which will use the loaded options)
                val steps = currentStrategy?.getSteps() ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    steps = steps,
                    isLoading = false,
                    isInitialized = true,
                    transactionType = transactionType,
                    canProceedToNext = navigationUseCase.canProceedToNext(0, steps, emptyMap())
                )
            } catch (e: Exception) {
                _error.value = AppError.Initialization(e.message ?: "Failed to initialize transaction")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialized = false
                )
            }
        }
    }

    fun onFieldValueChange(fieldId: String, value: String, checked: Boolean? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val strategy = currentStrategy ?: return@launch

            val newFormData = currentState.formData.toMutableMap()

            // ✅ If person type is changing, clear loaded ships, commercial reg data, AND refresh steps
            var shouldRefreshStepsForPersonType = false
            if (fieldId == "selectionPersonType") {
                val oldPersonType = currentState.formData["selectionPersonType"]
                if (oldPersonType != null && oldPersonType != value) {
                    println("🔄 Person type changed from $oldPersonType to $value - clearing ships and refreshing steps")
                    strategy.clearLoadedShips()

                    // ✅ Clear commercial registration data if changing from "شركة" to "فرد"
                    if (oldPersonType == "شركة" && value == "فرد") {
                        println("🧹 Changing from شركة to فرد - clearing commercial registration data")
                        // ✅ FIXED: The actual field ID is "selectionData" not "commercialRegistration"
                        newFormData.remove("selectionData")
                        newFormData.remove("companyName")
                        newFormData.remove("companyType")

                        // ✅ Also update strategy's accumulated data
                        val clearedData = mapOf(
                            "selectionData" to "",
                            "companyName" to "",
                            "companyType" to ""
                        )
                        strategy.updateAccumulatedData(clearedData)
                    }

                    shouldRefreshStepsForPersonType = true
                }
            }

            // Update form data
            newFormData[fieldId] = checked?.toString() ?: value

            // ✅ Update accumulated data in strategy immediately
            strategy.updateAccumulatedData(newFormData)

            // Clear field error
            val newFieldErrors = currentState.fieldErrors.toMutableMap()
            newFieldErrors.remove(fieldId)

            // Handle dynamic field changes via strategy
            val updatedFormData = strategy.handleFieldChange(fieldId, value, newFormData)

            // ✅ Check if we need to refresh steps
            val shouldRefreshStepsForTriggerFlag = updatedFormData.containsKey("_triggerRefresh")
            val shouldRefreshSteps = shouldRefreshStepsForPersonType || shouldRefreshStepsForTriggerFlag

            // Remove the trigger flag from form data if present
            val cleanedFormData = updatedFormData.toMutableMap().apply {
                remove("_triggerRefresh")
            }

            // ✅ Refresh steps if needed
            val updatedSteps = if (shouldRefreshSteps) {
                println("🔄 Refreshing steps because field '$fieldId' changed and triggered refresh")
                strategy.getSteps()
            } else {
                currentState.steps
            }

            // Update state
            _uiState.value = currentState.copy(
                formData = cleanedFormData,
                fieldErrors = newFieldErrors,
                steps = updatedSteps, // ✅ Update steps
                canProceedToNext = navigationUseCase.canProceedToNext(
                    currentState.currentStep,
                    updatedSteps, // ✅ Use updated steps
                    cleanedFormData
                )
            )
        }
    }

    open fun nextStep() {
        viewModelScope.launch {
            // prevent re-entry
            if (_isProcessingNext.value) return@launch
            _isProcessingNext.value = true
            try {
                val currentState = _uiState.value

                if (validateAndCompleteCurrentStep()) {
                    val currentStepIndex = currentState.currentStep
                    val currentStep = currentState.steps.getOrNull(currentStepIndex) ?: return@launch

                    // 🔹 تحديد الـ fields الخاصة بالـ step الحالي
                    val currentStepFields = currentStep.fields.map { it.id }

                    // 🔹 فلترة الداتا اللي تخص الـ step الحالي فقط
                    val currentStepData = currentState.formData.filterKeys { it in currentStepFields }

                    // ✅ NEW: Check if we just completed person type or commercial registration step
                    // If so, load ships before moving to next step
                    val personType = currentState.formData["selectionPersonType"]
                    val isPersonTypeStep = currentStepFields.contains("selectionPersonType")
                    val isCommercialRegStep = currentStepFields.contains("selectionData")

                    // ✅ IMPORTANT: Merge current step data with existing form data to get complete picture
                    val mergedFormData = currentState.formData.toMutableMap().apply {
                        putAll(currentStepData)
                    }

                    val commercialRegValue = mergedFormData["selectionData"]
                    val hasCommercialRegData = !commercialRegValue.isNullOrEmpty()

                    println("🔍 DEBUG: Checking shouldLoadShips")
                    println("   personType = $personType")
                    println("   isPersonTypeStep = $isPersonTypeStep")
                    println("   isCommercialRegStep = $isCommercialRegStep")
                    println("   hasCommercialRegData = $hasCommercialRegData")

                    val shouldLoadShips = when {
                        isPersonTypeStep && personType == "فرد" -> {
                            println("✅ Should load ships: Individual person type selected")
                            true
                        }
                        isPersonTypeStep && personType == "شركة" -> {
                            println("ℹ️ Company selected - will load ships after commercial reg step")
                            false
                        }
                        isCommercialRegStep && hasCommercialRegData -> {
                            println("✅ Should load ships: Commercial registration data entered")
                            true
                        }
                        else -> {
                            println("❌ Should NOT load ships - conditions not met")
                            false
                        }
                    }

                    val strategy = currentStrategy
                    if (strategy != null) {
                        // Process the data (may be CPU/IO heavy) off the main dispatcher
                        val requiredNextStep = try {
                            withContext(Dispatchers.IO) {
                                strategy.processStepData(currentStepIndex, currentStepData)
                            }
                        } catch (e: ApiException) {
                            // ✅ Handle API errors with proper error code and message
                            println("❌ API Error in processStepData: ${e.code} - ${e.message}")

                            // ✅ Special handling for 401 Unauthorized errors
                            if (e.code == 401) {
                                println("🔐 401 Unauthorized - Token expired or invalid")
                                _error.value = AppError.Unauthorized(e.message ?: "انتهت صلاحية الجلسة. الرجاء تحديث الرمز للمتابعة")
                            } else {
                                _error.value = AppError.ApiError(e.code, e.message ?: "حدث خطأ في الخادم")
                            }

                            // ✅ Only show banner, no toast
                            // Toast removed - banner is sufficient for error display

                            // Don't proceed to next step
                            return@launch
                        } catch (e: Exception) {
                            println("❌ Exception in processStepData: ${e.message}")
                            e.printStackTrace()

                            // Show error to user - banner only, no toast
                            _error.value = AppError.Unknown(e.message ?: "حدث خطأ أثناء معالجة البيانات")
                            // Toast removed - banner is sufficient for error display

                            // Don't proceed to next step
                            return@launch
                        }

                        // ✅ Handle return -1 (error/validation failure) BEFORE loading ships
                        if (requiredNextStep == -1) {
                            println("=" .repeat(80))
                            println("⛔⛔⛔ processStepData returned -1 (BLOCKING NAVIGATION)")
                            println("=" .repeat(80))

                            // ✅ Refresh steps to show any UI changes + merge formData for dialogs
                            val updatedSteps = strategy.getSteps()
                            val strategyFormData = strategy.getFormData()

                            _uiState.value = currentState.copy(
                                steps = updatedSteps,
                                formData = currentState.formData.toMutableMap().apply {
                                    putAll(strategyFormData)
                                }
                            )

                            println("✅ UI State updated after error (including formData)")
                            println("=" .repeat(80))

                            return@launch
                        }

                        // ✅ NEW: Handle return -2 (success but show dialog and stop - used for review step)
                        if (requiredNextStep == -2) {
                            println("=" .repeat(80))
                            println("✅✅✅ processStepData returned -2 (SHOW SUCCESS DIALOG & STOP)")
                            println("=" .repeat(80))

                            // Strategy has set success data in formData, just refresh UI
                            val updatedSteps = strategy.getSteps()
                            val strategyFormData = strategy.getFormData()

                            _uiState.value = currentState.copy(
                                steps = updatedSteps,
                                formData = currentState.formData.toMutableMap().apply {
                                    putAll(strategyFormData)
                                }
                            )

                            println("✅ UI State updated, success dialog should be shown by parent ViewModel")
                            println("=" .repeat(80))

                            return@launch
                        }

                        if (shouldLoadShips) {
                            try {
                                // run loadShips on IO dispatcher to avoid blocking UI
                                val loadedShips = withContext(Dispatchers.IO) {
                                    strategy.loadShipsForSelectedType(mergedFormData)
                                }
                                println("✅ Loaded ${loadedShips.size} ships successfully")
                            } catch (e: Exception) {
                                println("❌ Failed to load ships: ${e.message}")
                                e.printStackTrace()
                            }
                        }

                        // Refresh steps
                        val updatedSteps = strategy.getSteps()

                        // ✅ Merge strategy's formData (which may contain apiErrorCode/apiErrorMessage)
                        val strategyFormData = strategy.getFormData()
                        println("🔍 ViewModel: Merging strategy formData into uiState")
                        println("   Current formData size: ${currentState.formData.size}")
                        println("   Strategy formData size: ${strategyFormData.size}")
                        println("   Strategy formData keys: ${strategyFormData.keys}")

                        val mergedFormData = currentState.formData.toMutableMap().apply {
                            putAll(strategyFormData)
                        }

                        println("   Merged formData size: ${mergedFormData.size}")
                        println("   apiErrorCode in merged: ${mergedFormData["apiErrorCode"]}")
                        println("   apiErrorMessage in merged: ${mergedFormData["apiErrorMessage"]}")

                        val updatedState = currentState.copy(
                            steps = updatedSteps,
                            formData = mergedFormData
                        )


                        // ✅ Update UI state immediately to show error banner if present
                        _uiState.value = updatedState
                        println("✅ ViewModel: UI state updated with merged formData")

                        navigationUseCase.getNextStep(currentStepIndex, updatedSteps.size)?.let { nextStep ->
                            val newCompletedSteps = updatedState.completedSteps + currentStepIndex

                            // ✅ CRITICAL FIX: Use updatedState (which has merged formData) instead of _uiState.value
                            _uiState.value = updatedState.copy(
                                currentStep = if (requiredNextStep == currentStepIndex) nextStep else requiredNextStep,
                                completedSteps = newCompletedSteps,
                                canProceedToNext = navigationUseCase.canProceedToNext(
                                    nextStep,
                                    updatedSteps,
                                    updatedState.formData
                                )
                            )

                            println("✅✅✅ FINAL UI State updated:")
                            println("   formData.size = ${_uiState.value.formData.size}")
                            println("   apiErrorCode = ${_uiState.value.formData["apiErrorCode"]}")
                            println("   apiErrorMessage = ${_uiState.value.formData["apiErrorMessage"]}")

                            val targetStep = if (requiredNextStep == currentStepIndex) nextStep else requiredNextStep
                            strategy.onStepOpened(targetStep)
                        }
                    }

                    // ✅ REMOVED: Debug toast that showed step data after every step
                    // SharedSteps.saveStepData("Step_${currentStepIndex + 1}", currentStepData)
                    // val dataSummary = currentStepData.entries.joinToString("\n") { (key, value) -> "$key: $value" }
                    // _showToastEvent.value = "Step ${currentStepIndex + 1} Data:\n$dataSummary"
                }
            } finally {
                _isProcessingNext.value = false
            }
        }
    }

    fun previousStep() {
        viewModelScope.launch {
            val currentState = _uiState.value

            // ✅ NEW: Prevent back navigation if current step is locked (resumed transaction)
            if (currentState.isResumedTransaction) {
                val prevStep = navigationUseCase.getPreviousStep(currentState.currentStep)
                if (prevStep != null && currentState.lockedSteps.contains(prevStep)) {
                    println("🔒 Cannot go back to locked step $prevStep (resumed transaction)")
                    _showToastEvent.value =
                        "لا يمكن الرجوع إلى الخطوات السابقة في المعاملات المستأنفة"
                    return@launch
                }
            }

            viewModelScope.launch {
                navigationUseCase.getPreviousStep(currentState.currentStep)?.let { prevStep ->
                    // ✅ Check if we're going back FROM marine unit selection step
                    // If so, we need to check if we should clear ships and refresh steps
                    val currentStepFields =
                        currentState.steps.getOrNull(currentState.currentStep)?.fields?.map { it.id }
                            ?: emptyList()
                    val isLeavingMarineUnitStep = currentStepFields.contains("selectedMarineUnits")

                    val prevStepFields =
                        currentState.steps.getOrNull(prevStep)?.fields?.map { it.id } ?: emptyList()
                    val isGoingToPersonTypeStep = prevStepFields.contains("selectionPersonType")
                    // ✅ FIXED: Use correct field ID "selectionData" instead of "commercialRegistration"
                    val isGoingToCommercialRegStep = prevStepFields.contains("selectionData")

                    // ✅ Clear ships ONLY if going back to person type or commercial reg step
                    if (isLeavingMarineUnitStep && (isGoingToPersonTypeStep || isGoingToCommercialRegStep)) {
                        println("🧹 Going back from marine unit selection to person type/commercial reg - clearing ships")
                        val strategy = currentStrategy
                        strategy?.clearLoadedShips()

                        // ✅ Refresh steps to reflect cleared ships
                        val updatedSteps = strategy?.getSteps() ?: currentState.steps

                        _uiState.value = currentState.copy(
                            currentStep = prevStep,
                            steps = updatedSteps,
                            canProceedToNext = navigationUseCase.canProceedToNext(
                                prevStep,
                                updatedSteps,
                                currentState.formData
                            )
                        )
                    } else {
                        // ✅ Normal back navigation - keep ships cached (DON'T clear them!)
                        println("⬅️ Normal back navigation - keeping ships cached")
                        _uiState.value = currentState.copy(
                            currentStep = prevStep,
                            canProceedToNext = navigationUseCase.canProceedToNext(
                                prevStep,
                                currentState.steps,
                                currentState.formData
                            )
                        )
                    }
                }
            }
        }
    }

    fun goToStep(stepIndex: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // ✅ NEW: Prevent navigation to locked steps (resumed transaction)
            if (currentState.isResumedTransaction && currentState.lockedSteps.contains(stepIndex)) {
                println("🔒 Cannot navigate to locked step $stepIndex (resumed transaction)")
                _showToastEvent.value = "لا يمكن الوصول إلى هذه الخطوة في المعاملات المستأنفة"
                return@launch
            }

            if (navigationUseCase.canJumpToStep(
                    stepIndex,
                    currentState.currentStep,
                    currentState.completedSteps,
                    currentState.steps.size
                )
            ) {
                _uiState.value = currentState.copy(
                    currentStep = stepIndex,
                    canProceedToNext = navigationUseCase.canProceedToNext(
                        stepIndex,
                        currentState.steps,
                        currentState.formData
                    )
                )

                // ✅ NEW: Load lookups for the target step
                currentStrategy?.let { strategy ->
                    launch {
                        strategy.onStepOpened(stepIndex)
                    }
                }
            }
        }
    }

    fun getCurrentStepData(): StepData? {
        val currentState = _uiState.value
        return currentState.steps.getOrNull(currentState.currentStep)
    }

    private fun validateAndCompleteCurrentStep(): Boolean {
        val currentState = _uiState.value
        val strategy = currentStrategy ?: return false

        val (isValid, errors) = strategy.validateStep(
            currentState.currentStep,
            currentState.formData
        )

        _uiState.value = currentState.copy(fieldErrors = errors)

        return isValid
    }

    fun submitForm() {
        viewModelScope.launch {
            _submissionState.value = UIState.Loading

            try {
                val currentState = _uiState.value
                val strategy = currentStrategy

                if (strategy == null) {
                    _submissionState.value = UIState.Failure(Exception("Transaction not initialized"))
                    return@launch
                }

                // ✅ Step 1: Call strategy.submit() for validation
                val result = strategy.submit(currentState.formData)

                result.fold(
                    onSuccess = {
                        println("✅ Strategy validation successful")
                        _submissionState.value = UIState.Success(true)

                        // ✅ Step 2: Call the hook for child ViewModels to handle final submission
                        onFormSubmitSuccess()
                    },
                    onFailure = { exception ->
                        _submissionState.value = UIState.Failure(exception)
                        _error.value = AppError.Submission(exception.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _submissionState.value = UIState.Failure(e)
                _error.value = AppError.Submission(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * ✅ Hook called after strategy.submit() succeeds
     * Child ViewModels can override this to handle final API submission
     */
    protected open fun onFormSubmitSuccess() {
        // Default: do nothing
        // MarineRegistrationViewModel will override to call submitOnReview()
    }

    fun resetSubmissionState() {
        _submissionState.value = UIState.Empty
    }

    /**
     * Handle field focus lost events
     * Delegates to strategy for transaction-specific behavior (e.g., company lookup)
     */
    fun onFieldFocusLost(fieldId: String, value: String) {
        viewModelScope.launch {
            val strategy = currentStrategy ?: return@launch

            // Add loading state for this field
            _fieldLoadingStates.value = _fieldLoadingStates.value + fieldId

            try {
                val result = strategy.onFieldFocusLost(fieldId, value)

                when (result) {
                    is FieldFocusResult.UpdateFields -> {
                        // Update form data with results
                        val currentState = _uiState.value
                        val newFormData = currentState.formData.toMutableMap()
                        newFormData.putAll(result.updates)

                        _uiState.value = currentState.copy(
                            formData = newFormData,
                            canProceedToNext = navigationUseCase.canProceedToNext(
                                currentState.currentStep,
                                currentState.steps,
                                newFormData
                            )
                        )
                    }

                    is FieldFocusResult.Error -> {
                        // Show error for the field
                        val currentState = _uiState.value
                        val newErrors = currentState.fieldErrors.toMutableMap()
                        newErrors[result.fieldId] = result.message

                        _uiState.value = currentState.copy(fieldErrors = newErrors)
                    }

                    is FieldFocusResult.NoAction -> {
                        // Nothing to do
                    }
                }
            } catch (e: Exception) {
                val currentState = _uiState.value
                val newErrors = currentState.fieldErrors.toMutableMap()
                newErrors[fieldId] = e.message ?: "حدث خطأ غير متوقع"
                _uiState.value = currentState.copy(fieldErrors = newErrors)
            } finally {
                // Remove loading state
                _fieldLoadingStates.value = _fieldLoadingStates.value - fieldId
            }
        }
    }

    fun isFieldLoading(fieldId: String): Boolean {
        return fieldId in _fieldLoadingStates.value
    }

    /**
     * ✅ NEW: Check if a specific lookup is currently loading
     * @param lookupKey The lookup identifier (e.g., "ports", "countries")
     * @return true if loading, false if loaded or not started
     */
    fun isLookupLoading(lookupKey: String): Boolean {
        return _lookupLoadingStates.value[lookupKey] == true
    }

    /**
     * ✅ NEW: Get loaded data for a specific lookup
     * @param lookupKey The lookup identifier
     * @return Pair of (data, success) or null if not loaded yet
     */
    fun getLookupData(lookupKey: String): Pair<List<String>, Boolean>? {
        return _loadedLookupData.value[lookupKey]
    }

    // File navigation methods
    fun openFilePicker(fieldId: String, allowedTypes: List<String>) {
        _fileNavigationEvent.value = FileNavigationEvent.OpenFilePicker(fieldId, allowedTypes)
    }

    fun viewFile(fileUri: String, fileType: String) {
        _fileNavigationEvent.value = FileNavigationEvent.ViewFile(fileUri, fileType)
    }

    fun removeFile(fieldId: String) {
        _fileNavigationEvent.value = FileNavigationEvent.RemoveFile(fieldId)
    }

    fun clearFileNavigationEvent() {
        _fileNavigationEvent.value = null
    }

    /**
     * ✅ NEW: Open file viewer dialog (preserves form state)
     */
    fun openFileViewerDialog(fileUri: String, fileName: String, mimeType: String) {
        _fileViewerState.value = FileViewerState(
            isOpen = true,
            fileUri = fileUri,
            fileName = fileName,
            mimeType = mimeType
        )
    }

    /**
     * ✅ NEW: Close file viewer dialog
     */
    fun closeFileViewerDialog() {
        _fileViewerState.value = FileViewerState(isOpen = false)
    }

    /**
     * ✅ NEW: Clear API error dialog
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * ✅ NEW: Refresh expired token
     * This should be called by child ViewModels when user clicks refresh token button
     * Child ViewModels must inject AuthRepository and call this method
     */
    protected suspend fun handleTokenRefresh(authRepository: com.informatique.mtcit.data.repository.AuthRepository): Boolean {
        return try {
            println("🔄 BaseTransactionViewModel: Attempting to refresh token...")
            val result = authRepository.refreshAccessToken()

            result.fold(
                onSuccess = { tokenResponse ->
                    println("✅ Token refreshed successfully")
                    // Clear the error state
                    _error.value = null
                    true
                },
                onFailure = { error ->
                    println("❌ Token refresh failed: ${error.message}")
                    // Show error that refresh failed - user needs to login again
                    _error.value = AppError.Unknown("فشل تحديث الرمز. يرجى تسجيل الدخول مرة أخرى")
                    false
                }
            )
        } catch (e: Exception) {
            println("❌ Exception during token refresh: ${e.message}")
            _error.value = AppError.Unknown("حدث خطأ أثناء تحديث الرمز: ${e.message}")
            false
        }
    }

    /**
     * Clear toast event after it's been shown
     */
    fun clearToastEvent() {
        _showToastEvent.value = null
    }

    /**
     * ✅ Generic function to update transaction status
     * Can be used by any transaction type (mortgage, registration, etc.)
     *
     * @param apiService The API service instance (e.g., MortgageApiService)
     * @param requestId The request ID
     * @param statusId The new status ID
     * @param updateStatusCall Lambda function that calls the specific API service method
     */
    protected suspend fun updateTransactionStatus(
        requestId: Int,
        statusId: Int,
        updateStatusCall: suspend (Int, Int) -> Result<Boolean>
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔄 BaseTransactionViewModel: Updating transaction status...")
                println("   Request ID: $requestId")
                println("   Status ID: $statusId")

                val result = updateStatusCall(requestId, statusId)

                result.onSuccess {
                    println("✅ Transaction status updated successfully")
                }
                result.onFailure { error ->
                    println("❌ Failed to update transaction status: ${error.message}")
                }

                result
            } catch (e: Exception) {
                println("❌ Exception in updateTransactionStatus: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
// ****************************************************
object SharedSteps {
    val stepDataMap = mutableMapOf<String, Map<String, String>>() // كل step فيها key/mortgageValue

    fun saveStepData(stepName: String, fields: Map<String, String>) {
        stepDataMap[stepName] = fields
    }

    fun reviewStep(): StepData {
        val reviewFields = stepDataMap.flatMap { (stepName, fields) ->
            fields.map { (key, value) ->
                FormField.TextField(
                    id = "$stepName-$key",
                    label = "$stepName - $key",
                    value = value
                )
            }
        }

        return StepData(
            titleRes = com.informatique.mtcit.R.string.review,
            descriptionRes = com.informatique.mtcit.R.string.step_placeholder_content,
            fields = reviewFields
        )
    }
}
