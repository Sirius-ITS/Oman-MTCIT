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
import com.informatique.mtcit.business.transactions.RequestInspectionStrategy
import com.informatique.mtcit.business.transactions.TemporaryRegistrationStrategy
import com.informatique.mtcit.business.transactions.ValidationResult
import com.informatique.mtcit.business.transactions.marineunit.MarineUnitNavigationAction

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
    private val strategyFactory: TransactionStrategyFactory
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    // NEW: Validation state for marine unit selection
    private val _validationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val validationState: StateFlow<ValidationState> = _validationState.asStateFlow()

    // NEW: Store validation result for later use on Next button click
    private var _storedValidationResult: ValidationResult? = null

    // NEW: Navigation to compliance detail screen (removed error dialog state)
    private val _navigationToComplianceDetail = MutableStateFlow<MarineUnitNavigationAction.ShowComplianceDetailScreen?>(null)
    val navigationToComplianceDetail: StateFlow<MarineUnitNavigationAction.ShowComplianceDetailScreen?> = _navigationToComplianceDetail.asStateFlow()

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
        strategy: RequestInspectionStrategy,
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

    /**
     * NEW: Validate and submit form for Temporary Registration
     * This method should be called from UI instead of submitForm() for Temporary Registration
     * Validates inspection status before actual submission
     */
    fun validateAndSubmit() {
        val currentState = uiState.value
        val transactionType = currentState.transactionType

        // Only intercept for Temporary Registration Certificate
        if (transactionType != TransactionType.REQUEST_FOR_INSPECTION) {
            println("📤 Not Temporary Registration, calling submitForm()")
            submitForm()
            return
        }

        println("🔍 Temporary Registration: Validating inspection before submit")

        viewModelScope.launch {
            try {
                // Get selected marine unit ID from form data
                val selectedUnitsJson = currentState.formData["selectedMarineUnits"]
                println("🔍 Selected units JSON: $selectedUnitsJson")

                if (selectedUnitsJson.isNullOrEmpty() || selectedUnitsJson == "[]") {
                    println("❌ No marine unit selected")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown("الرجاء اختيار وحدة بحرية")
                    return@launch
                }

                // Parse selected unit ID (maritimeId from JSON)
                val selectedMaritimeIds = try {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(selectedUnitsJson)
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

                // Get the strategy
                val strategy = currentStrategy as? RequestInspectionStrategy
                if (strategy == null) {
                    println("❌ Strategy not found")
                    submitForm()
                    return@launch
                }

                // Get marine units and find the selected one
                val dynamicOptions = strategy.loadDynamicOptions()
                val marineUnits = dynamicOptions["marineUnits"] as? List<*>

                val selectedUnit = marineUnits?.firstOrNull {
                    (it as? com.informatique.mtcit.business.transactions.shared.MarineUnit)?.maritimeId == selectedMaritimeId
                } as? com.informatique.mtcit.business.transactions.shared.MarineUnit

                if (selectedUnit == null) {
                    println("❌ Selected unit not found")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown("الوحدة البحرية المختارة غير موجودة")
                    return@launch
                }

                println("✅ Found selected unit: ${selectedUnit.name}, id: ${selectedUnit.id}")

                // Now validate the unit's inspection status
                val userId = getCurrentUserId()
                val validationResult = validateTemporaryRegistrationUnit(strategy, selectedUnit.id, userId)

                if (validationResult == null) {
                    println("❌ Validation returned null")
                    _error.value = com.informatique.mtcit.common.AppError.Unknown("فشل التحقق من حالة الفحص")
                    return@launch
                }

                // Handle validation result
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
                                println("⚠️ Inspection validation failed, showing RequestDetailScreen")
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
        if (transactionType == TransactionType.REQUEST_FOR_INSPECTION) {
            println("🔍 Review Step: Validating inspection for Temporary Registration")
            validateAndSubmit()
        } else {
            // For other transactions, just proceed to next step
            println("➡️ Review Step: Proceeding to next step for other transaction")
            nextStep()
        }
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
            TransactionType.RELEASE_MORTGAGE -> true
            else -> false
        }
    }
}
