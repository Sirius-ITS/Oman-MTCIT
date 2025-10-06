package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.validation.FormValidator
import com.informatique.mtcit.business.company.CompanyLookupUseCase
import com.informatique.mtcit.business.company.CompanyLookupParams
import com.informatique.mtcit.business.BusinessState
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StepData(
    val titleRes: String,
    val descriptionRes: String,
    val fields: List<FormField>
)

data class ShipRegistrationState(
    val currentStep: Int = 0,
    val steps: List<StepData> = emptyList(),
    val completedSteps: Set<Int> = emptySet(),
    val formData: Map<String, String> = emptyMap(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val canProceedToNext: Boolean = false
)

@HiltViewModel
class ShipRegistrationViewModel @Inject constructor(
    private val validator: FormValidator,
    private val companyLookupUseCase: CompanyLookupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShipRegistrationState())
    val uiState: StateFlow<ShipRegistrationState> = _uiState.asStateFlow()

    private val _submissionState = MutableStateFlow<UIState<Boolean>>(UIState.Empty)
    val submissionState: StateFlow<UIState<Boolean>> = _submissionState.asStateFlow()

    // Company lookup loading state
    private val _companyLookupLoading = MutableStateFlow<Set<String>>(emptySet())
    val companyLookupLoading: StateFlow<Set<String>> = _companyLookupLoading.asStateFlow()

    // We need access to context for localization
    private var contextProvider: (() -> android.content.Context)? = null

    fun setContextProvider(provider: () -> android.content.Context) {
        contextProvider = provider
        initializeShipRegistration()
    }

    private fun initializeShipRegistration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Initialize all steps with localized labels
            val steps = createAllRegistrationSteps()

            _uiState.value = _uiState.value.copy(
                steps = steps,
                isLoading = false,
                isInitialized = true,
                canProceedToNext = validateCurrentStep(0, steps, emptyMap())
            )
        }
    }

    private fun createAllRegistrationSteps(): List<StepData> {
        return listOf(
            createUnitDataStep(),
            createOwnerInformationStep(),
            createDocumentationStep(),
            createReviewStep()
        )
    }

    private fun localizedString(resId: Int): String {
        return contextProvider?.invoke()?.getString(resId) ?: ""
    }

    private fun createUnitDataStep(): StepData = StepData(
        titleRes = localizedString(R.string.unit_data),
        descriptionRes = localizedString(R.string.unit_data_description),
        fields = listOf(
            FormField.DropDown(
                id = localizedString(R.string.select_unit_type),
                label = localizedString(R.string.select_unit_type_placeholder),
                options = listOf(
                    localizedString(R.string.ship),
                    localizedString(R.string.marine_unit),
                    localizedString(R.string.yacht),
                    localizedString(R.string.boat)
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = localizedString(R.string.call_sign),
                label = localizedString(R.string.call_sign),
                mandatory = true
            ),
            FormField.TextField(
                id = localizedString(R.string.imo_number),
                label = localizedString(R.string.imo_number),
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = localizedString(R.string.mmsi_number),
                label = localizedString(R.string.mmsi_number),
                isNumeric = true,
                mandatory = true
            ),
            FormField.DropDown(
                id = localizedString(R.string.registration_port),
                label = localizedString(R.string.registration_port),
                options = listOf(
                    localizedString(R.string.sohar_port),
                    localizedString(R.string.salalah_port),
                    localizedString(R.string.muscat_port),
                    localizedString(R.string.duqm_port),
                    localizedString(R.string.shinas_port)
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = localizedString(R.string.ship_manufacturer),
                label = localizedString(R.string.ship_manufacturer),
                mandatory = true
            ),
            FormField.TextField(
                id = localizedString(R.string.ship_manufacture_year),
                label = localizedString(R.string.ship_manufacture_year),
                isNumeric = true,
                mandatory = true
            ),
            FormField.DropDown(
                id = localizedString(R.string.proof_type),
                label = localizedString(R.string.proof_type),
                options = listOf(
                    localizedString(R.string.ownership_certificate),
                    localizedString(R.string.sale_contract),
                    localizedString(R.string.registration_document),
                    localizedString(R.string.other)
                ),
                mandatory = true
            ),
            FormField.FileUpload(
                id = localizedString(R.string.proof_document),
                label = localizedString(R.string.proof_document),
                allowedTypes = listOf("pdf", "jpg", "png"),
                mandatory = true
            ),
            FormField.DatePicker(
                id = localizedString(R.string.construction_end_date),
                label = localizedString(R.string.construction_end_date),
                allowPastDates = true,
                mandatory = true
            ),
            FormField.DatePicker(
                id = localizedString(R.string.construction_start_date),
                label = localizedString(R.string.construction_start_date),
                allowPastDates = true,
                mandatory = true
            ),
            FormField.DatePicker(
                id = localizedString(R.string.first_registration_date_optional),
                label = localizedString(R.string.first_registration_date_optional),
                allowPastDates = true,
                mandatory = true
            ),
            FormField.DropDown(
                id = localizedString(R.string.building_country_optional),
                label = localizedString(R.string.building_country_optional),
                options = listOf(
                    localizedString(R.string.uae),
                    localizedString(R.string.saudi),
                    localizedString(R.string.kuwait),
                    localizedString(R.string.bahrain),
                    localizedString(R.string.qatar),
                    localizedString(R.string.oman)
                ),
                mandatory = true
            )
        )
    )

    private fun createOwnerInformationStep(): StepData = StepData(
        titleRes = localizedString(R.string.owner_info),
        descriptionRes = localizedString(R.string.owner_info_description),
        fields = listOf(
            FormField.CheckBox(
                id = "isCompany",
                label = localizedString(R.string.is_company),
                mandatory = false
            ),
            FormField.TextField(
                id = "ownerFullName",
                label = localizedString(R.string.owner_full_name),
                mandatory = true
            ),
            FormField.DropDown(
                id = "ownerNationality",
                label = localizedString(R.string.owner_nationality),
                options = listOf(
                    localizedString(R.string.uae),
                    localizedString(R.string.saudi),
                    localizedString(R.string.kuwait),
                    localizedString(R.string.bahrain),
                    localizedString(R.string.qatar),
                    localizedString(R.string.oman),
                    localizedString(R.string.other)
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerIdNumber",
                label = localizedString(R.string.owner_id_number),
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerPassportNumber",
                label = localizedString(R.string.owner_passport_number),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerEmail",
                label = localizedString(R.string.email),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerMobile",
                label = localizedString(R.string.owner_mobile),
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerAddress",
                label = localizedString(R.string.owner_address),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerCity",
                label = localizedString(R.string.owner_city),
                mandatory = true
            ),
            FormField.DropDown(
                id = "ownerCountry",
                label = localizedString(R.string.country),
                options = listOf(
                    localizedString(R.string.uae),
                    localizedString(R.string.saudi),
                    localizedString(R.string.kuwait),
                    localizedString(R.string.bahrain),
                    localizedString(R.string.qatar),
                    localizedString(R.string.oman)
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerPostalCode",
                label = localizedString(R.string.owner_postal_code),
                isNumeric = true,
                mandatory = false
            ),
            FormField.TextField(
                id = "companyRegistrationNumber",
                label = localizedString(R.string.company_registration_number),
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "companyName",
                label = localizedString(R.string.company_name),
                mandatory = false
            ),
            FormField.TextField(
                id = "companyType",
                label = localizedString(R.string.owner_type),
                mandatory = false
            )
        )
    )

    private fun createDocumentationStep(): StepData = StepData(
        titleRes = localizedString(R.string.documents),
        descriptionRes = localizedString(R.string.documents_description),
        fields = emptyList() // Will be implemented later
    )

    private fun createReviewStep(): StepData = StepData(
        titleRes = localizedString(R.string.review),
        descriptionRes = localizedString(R.string.step_placeholder_content),
        fields = emptyList() // Will be implemented later
    )

    fun onFieldValueChange(fieldId: String, value: String, checked: Boolean? = null) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newFormData = currentState.formData.toMutableMap()

            // Update form data
            newFormData[fieldId] = checked?.toString() ?: value

            // Clear field error
            val newFieldErrors = currentState.fieldErrors.toMutableMap()
            newFieldErrors.remove(fieldId)

            // Update state
            _uiState.value = currentState.copy(
                formData = newFormData,
                fieldErrors = newFieldErrors,
                canProceedToNext = validateCurrentStep(
                    currentState.currentStep,
                    currentState.steps,
                    newFormData
                )
            )

            // Handle dynamic fields based on owner type
            if (fieldId == "owner_type") {
                handleOwnerTypeChange(value, newFormData)
            }
        }
    }

    private fun handleOwnerTypeChange(ownerType: String, formData: MutableMap<String, String>) {
        when (ownerType) {
            "فرد" -> {
                // Clear company fields
                formData.remove("companyName")
                formData.remove("companyRegistrationNumber")
            }
            "شركة" -> {
                // Company fields will be shown and are required
            }
            "شراكة" -> {
                // Clear company fields
                formData.remove("companyName")
                formData.remove("companyRegistrationNumber")
            }
        }

        _uiState.value = _uiState.value.copy(formData = formData)
    }

    fun nextStep() {
        viewModelScope.launch {
            val currentState = _uiState.value

            if (validateAndCompleteCurrentStep()) {
                val nextStep = currentState.currentStep + 1
                val newCompletedSteps = currentState.completedSteps + currentState.currentStep

                _uiState.value = currentState.copy(
                    currentStep = nextStep,
                    completedSteps = newCompletedSteps,
                    canProceedToNext = if (nextStep < currentState.steps.size) {
                        validateCurrentStep(nextStep, currentState.steps, currentState.formData)
                    } else false
                )
            }
        }
    }

    fun previousStep() {
        val currentState = _uiState.value
        if (currentState.currentStep > 0) {
            val prevStep = currentState.currentStep - 1
            _uiState.value = currentState.copy(
                currentStep = prevStep,
                canProceedToNext = validateCurrentStep(
                    prevStep,
                    currentState.steps,
                    currentState.formData
                )
            )
        }
    }

    fun goToStep(stepIndex: Int) {
        val currentState = _uiState.value
        if (stepIndex in 0 until currentState.steps.size &&
            (stepIndex <= currentState.currentStep || stepIndex in currentState.completedSteps)) {
            _uiState.value = currentState.copy(
                currentStep = stepIndex,
                canProceedToNext = validateCurrentStep(
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
        val currentStepData = currentState.steps.getOrNull(currentState.currentStep) ?: return false

        val (isValid, errors) = validateStepFields(currentStepData, currentState.formData)

        _uiState.value = currentState.copy(fieldErrors = errors)

        return isValid
    }

    private fun validateCurrentStep(stepIndex: Int, steps: List<StepData>, formData: Map<String, String>): Boolean {
        val stepData = steps.getOrNull(stepIndex) ?: return false
        val (isValid, _) = validateStepFields(stepData, formData)
        return isValid
    }

    private fun validateStepFields(stepData: StepData, formData: Map<String, String>): Pair<Boolean, Map<String, String>> {
        val errors = mutableMapOf<String, String>()
        var isValid = true

        stepData.fields.forEach { field ->
            if (field.mandatory && shouldValidateField(field, formData)) {
                val value = formData[field.id] ?: ""

                when {
                    value.isEmpty() || (field is FormField.CheckBox && value != "true") -> {
                        errors[field.id] = "هذا الحقل مطلوب"
                        isValid = false
                    }
                    field.id == "email" && value.isNotEmpty() && !isValidEmail(value) -> {
                        errors[field.id] = "يرجى إدخال بريد إلكتروني صحيح"
                        isValid = false
                    }
                    field.id == "phone" && !isValidPhone(value) -> {
                        errors[field.id] = "يرجى إدخال رقم هاتف صحيح"
                        isValid = false
                    }
                }
            }
        }

        return isValid to errors
    }

    private fun shouldValidateField(field: FormField, formData: Map<String, String>): Boolean {
        return when (field.id) {
            "companyName", "companyRegistrationNumber" -> formData["owner_type"] == "شركة"
            else -> true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 8 && phone.all { it.isDigit() }
    }

    fun submitForm() {
        viewModelScope.launch {
            _submissionState.value = UIState.Loading

            try {
                // Simulate form submission
                kotlinx.coroutines.delay(2000)

                val currentState = _uiState.value
                val allFormData = currentState.formData

                // Here you would call your actual API to submit the form
                // val result = shipRegistrationRepository.submitRegistration(allFormData)

                _submissionState.value = UIState.Success(true)
            } catch (e: Exception) {
                _submissionState.value = UIState.Failure(e)
            }
        }
    }

    fun resetSubmissionState() {
        _submissionState.value = UIState.Empty
    }

    fun isStepCompleted(stepIndex: Int): Boolean {
        return stepIndex in _uiState.value.completedSteps
    }

    fun getTotalSteps(): Int = _uiState.value.steps.size

    fun getCurrentStep(): Int = _uiState.value.currentStep

    fun getAllFormData(): Map<String, String> = _uiState.value.formData

    fun onCompanyRegistrationNumberFocusLost(registrationNumber: String) {
        if (registrationNumber.isBlank()) return

        viewModelScope.launch {
            // Add loading state for this field
            _companyLookupLoading.value = _companyLookupLoading.value + "companyRegistrationNumber"

            // Clear previous errors for company fields
            val currentState = _uiState.value
            val newFieldErrors = currentState.fieldErrors.toMutableMap()
            newFieldErrors.remove("companyRegistrationNumber")
            newFieldErrors.remove("companyName")
            newFieldErrors.remove("companyType")

            _uiState.value = currentState.copy(fieldErrors = newFieldErrors)

            try {
                val result = companyLookupUseCase(
                    CompanyLookupParams(registrationNumber)
                )

                when (result) {
                    is BusinessState.Success -> {
                        val companyData = result.data.result
                        if (companyData != null) {
                            // Update form data with company information
                            val newFormData = currentState.formData.toMutableMap()
                            newFormData["companyName"] = companyData.arabicCommercialName
                            newFormData["companyType"] = companyData.commercialRegistrationEntityType

                            _uiState.value = _uiState.value.copy(
                                formData = newFormData,
                                canProceedToNext = validateCurrentStep(
                                    currentState.currentStep,
                                    currentState.steps,
                                    newFormData
                                )
                            )
                        }
                    }

                    is BusinessState.Error -> {
                        // Show error inline with the field
                        val newErrors = _uiState.value.fieldErrors.toMutableMap()
                        newErrors["companyRegistrationNumber"] = result.message

                        // Clear company fields if there was an error
                        val newFormData = _uiState.value.formData.toMutableMap()
                        newFormData.remove("companyName")
                        newFormData.remove("companyType")

                        _uiState.value = _uiState.value.copy(
                            fieldErrors = newErrors,
                            formData = newFormData
                        )
                    }

                    is BusinessState.Loading -> {
                        // Loading state is already handled by _companyLookupLoading
                        // No additional action needed here
                    }
                }
            } catch (e: Exception) {
                // Handle unexpected errors
                val newErrors = _uiState.value.fieldErrors.toMutableMap()
                newErrors["companyRegistrationNumber"] = "حدث خطأ أثناء البحث عن الشركة"

                _uiState.value = _uiState.value.copy(fieldErrors = newErrors)
            } finally {
                // Remove loading state
                _companyLookupLoading.value = _companyLookupLoading.value - "companyRegistrationNumber"
            }
        }
    }

    fun isFieldLoading(fieldId: String): Boolean {
        return fieldId in _companyLookupLoading.value
    }
}
