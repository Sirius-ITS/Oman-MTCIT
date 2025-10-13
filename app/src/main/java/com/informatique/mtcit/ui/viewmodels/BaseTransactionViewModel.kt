package com.informatique.mtcit.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.transactions.FieldFocusResult
import com.informatique.mtcit.business.transactions.TransactionState
import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.AppError
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.common.ResourceProvider
import com.informatique.mtcit.ui.base.UIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared data classes used by ViewModels
 */
data class StepData(
    val titleRes: Int,
    val descriptionRes: Int,
    val fields: List<FormField>
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

    // File navigation events
    private val _fileNavigationEvent = MutableStateFlow<FileNavigationEvent?>(null)
    val fileNavigationEvent: StateFlow<FileNavigationEvent?> = _fileNavigationEvent.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    // Current transaction strategy
    protected var currentStrategy: TransactionStrategy? = null

    /**
     * Abstract method to create strategy for specific transaction type
     * Each category ViewModel implements this to create its own strategies
     */
    protected abstract suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy

    /**
     * Initialize transaction with specific type
     * This must be called before using the ViewModel
     */
    fun initializeTransaction(transactionType: TransactionType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Create category-specific strategy
                currentStrategy = createStrategy(transactionType)

                // Load dynamic options FIRST (before getting steps)
                val dynamicOptions = currentStrategy?.loadDynamicOptions() ?: emptyMap()

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

    // Helper function to open file with external app
    fun openFileOutsideApp(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    fun onFieldValueChange(fieldId: String, value: String, checked: Boolean? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val strategy = currentStrategy ?: return@launch

            val newFormData = currentState.formData.toMutableMap()

            // Update form data
            newFormData[fieldId] = checked?.toString() ?: value

            // Clear field error
            val newFieldErrors = currentState.fieldErrors.toMutableMap()
            newFieldErrors.remove(fieldId)

            // Handle dynamic field changes via strategy
            val updatedFormData = strategy.handleFieldChange(fieldId, value, newFormData)

            // Update state
            _uiState.value = currentState.copy(
                formData = updatedFormData,
                fieldErrors = newFieldErrors,
                canProceedToNext = navigationUseCase.canProceedToNext(
                    currentState.currentStep,
                    currentState.steps,
                    updatedFormData
                )
            )
        }
    }

    fun nextStep() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (validateAndCompleteCurrentStep()) {
                navigationUseCase.getNextStep(currentState.currentStep, currentState.steps.size)?.let { nextStep ->
                    val newCompletedSteps = currentState.completedSteps + currentState.currentStep

                    _uiState.value = currentState.copy(
                        currentStep = nextStep,
                        completedSteps = newCompletedSteps,
                        canProceedToNext = navigationUseCase.canProceedToNext(
                            nextStep,
                            currentState.steps,
                            currentState.formData
                        )
                    )
                }
            }
        }
    }

    fun previousStep() {
        val currentState = _uiState.value
        navigationUseCase.getPreviousStep(currentState.currentStep)?.let { prevStep ->
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

    fun goToStep(stepIndex: Int) {
        val currentState = _uiState.value
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

                val result = strategy.submit(currentState.formData)

                result.fold(
                    onSuccess = {
                        _submissionState.value = UIState.Success(true)
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
}
