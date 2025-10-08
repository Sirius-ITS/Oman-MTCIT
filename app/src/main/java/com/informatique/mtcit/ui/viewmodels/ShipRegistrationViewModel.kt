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
import com.informatique.mtcit.business.company.CompanyRepository
import com.informatique.mtcit.ui.repo.CompanyRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StepData(
    val titleRes: Int,
    val descriptionRes: Int,
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
    val canProceedToNext: Boolean = false,
    val selectedFiles: Map<String, String> = emptyMap() // fieldId -> fileUri
)

// Navigation events for file operations
sealed class FileNavigationEvent {
    data class OpenFilePicker(val fieldId: String, val allowedTypes: List<String>) : FileNavigationEvent()
    data class ViewFile(val fileUri: String, val fileType: String) : FileNavigationEvent()
    data class RemoveFile(val fieldId: String) : FileNavigationEvent()
}

@HiltViewModel
class ShipRegistrationViewModel @Inject constructor(
    // private val validator: FormValidator,
    // private val companyLookupUseCase: CompanyLookupUseCase
    private val companyRepository: CompanyRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShipRegistrationState())
    val uiState: StateFlow<ShipRegistrationState> = _uiState.asStateFlow()

    private val _submissionState = MutableStateFlow<UIState<Boolean>>(UIState.Empty)
    val submissionState: StateFlow<UIState<Boolean>> = _submissionState.asStateFlow()

    // Company lookup loading state
    private val _companyLookupLoading = MutableStateFlow<Set<String>>(emptySet())
    val companyLookupLoading: StateFlow<Set<String>> = _companyLookupLoading.asStateFlow()

    // File navigation events
    private val _fileNavigationEvent = MutableStateFlow<FileNavigationEvent?>(null)
    val fileNavigationEvent: StateFlow<FileNavigationEvent?> = _fileNavigationEvent.asStateFlow()

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
//            createMarineUnitDimensionsStep(),
            createOwnerInformationStep(),
            createDocumentationStep(),
            createReviewStep()
        )
    }

    private fun localizedString(resId: Int): String {
        return contextProvider?.invoke()?.getString(resId) ?: ""
    }

    private fun createUnitDataStep(): StepData = StepData(
        titleRes = R.string.unit_data,
        descriptionRes = R.string.unit_data_description,
        fields = listOf(
            FormField.DropDown(
                id = "unitType",
                labelRes = R.string.select_unit_type_placeholder,
                optionRes = listOf(
                    R.string.ship,
                    R.string.marine_unit,
                    R.string.yacht,
                    R.string.boat
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = "callSign",
                labelRes = R.string.call_sign,
                mandatory = true
            ),
            FormField.TextField(
                id = "imoNumber",
                labelRes = R.string.imo_number,
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "mmsi",
                labelRes = R.string.mmsi_number,
                isNumeric = true,
                mandatory = true
            ),
            FormField.DropDown(
                id = "registrationPort",
                labelRes = R.string.registration_port,
                optionRes = listOf(
                    R.string.sohar_port,
                    R.string.salalah_port,
                    R.string.muscat_port,
                    R.string.duqm_port,
                    R.string.shinas_port
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = "shipManufacturer",
                labelRes = R.string.ship_manufacturer,
                mandatory = true
            ),
            FormField.TextField(
                id = "manufacturerYear",
                labelRes = R.string.ship_manufacture_year,
                isNumeric = true,
                mandatory = true
            ),
            FormField.DropDown(
                id = "proofType",
                labelRes = R.string.proof_type,
                optionRes = listOf(
                    R.string.ownership_certificate,
                    R.string.sale_contract,
                    R.string.registration_document,
                    R.string.other
                ),
                mandatory = true
            ),
            FormField.FileUpload(
                id = "proofDocument",
                labelRes = R.string.proof_document,
                allowedTypes = listOf("pdf", "jpg", "png"),
                mandatory = true
            ),
            FormField.DatePicker(
                id = "constructionEndDate",
                labelRes = R.string.construction_end_date,
                allowPastDates = false,
                mandatory = true
            ),
            FormField.DatePicker(
                id = "constructionStartDate",
                labelRes = R.string.construction_start_date,
                allowPastDates = true,
                mandatory = true
            ),
            FormField.DatePicker(
                id = "firstRegistrationDate",
                labelRes = R.string.first_registration_date_optional,
                allowPastDates = false,
                mandatory = false
            ),
            FormField.DropDown(
                id = "registrationCountry",
                labelRes = R.string.building_country_optional,
                optionRes = listOf(
                    R.string.uae,
                    R.string.saudi,
                    R.string.kuwait,
                    R.string.bahrain,
                    R.string.qatar,
                    R.string.oman
                ),
                mandatory = false
            )
        )
    )

    private fun createMarineUnitDimensionsStep(): StepData = StepData(
        titleRes = R.string.marine_unit_Dimentions,
        descriptionRes = R.string.marine_unit_Dimentions,
        fields = emptyList() // Will be implemented later
    )

    private fun createOwnerInformationStep(): StepData = StepData(
        titleRes = R.string.owner_info,
        descriptionRes = R.string.owner_info_description,
        fields = listOf(
            FormField.CheckBox(
                id = "isCompany",
                labelRes = R.string.is_company,
                mandatory = false
            ),
            FormField.TextField(
                id = "ownerFullName",
                labelRes = R.string.owner_full_name,
                mandatory = true
            ),
            FormField.DropDown(
                id = "ownerNationality",
                labelRes = R.string.owner_nationality,
                optionRes = listOf(
                    R.string.uae,
                    R.string.saudi,
                    R.string.kuwait,
                    R.string.bahrain,
                    R.string.qatar,
                    R.string.oman,
                    R.string.other
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerIdNumber",
                labelRes = R.string.owner_id_number,
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerPassportNumber",
                labelRes = R.string.owner_passport_number,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerEmail",
                labelRes = R.string.email,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerMobile",
                labelRes = R.string.owner_mobile,
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerAddress",
                labelRes = R.string.owner_address,
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerCity",
                labelRes = R.string.owner_city,
                mandatory = true
            ),
            FormField.DropDown(
                id = "ownerCountry",
                labelRes = R.string.country,
                optionRes = listOf(
                    R.string.uae,
                    R.string.saudi,
                    R.string.kuwait,
                    R.string.bahrain,
                    R.string.qatar,
                    R.string.oman
                ),
                mandatory = true
            ),
            FormField.TextField(
                id = "ownerPostalCode",
                labelRes = R.string.owner_postal_code,
                isNumeric = true,
                mandatory = false
            ),
            FormField.TextField(
                id = "companyRegistrationNumber",
                labelRes = R.string.company_registration_number,
                isNumeric = true,
                mandatory = true
            ),
            FormField.TextField(
                id = "companyName",
                labelRes = R.string.company_name,
                mandatory = false
            ),
            FormField.TextField(
                id = "companyType",
                labelRes = R.string.owner_type,
                mandatory = false
            )
        )
    )

    private fun createDocumentationStep(): StepData = StepData(
        titleRes = R.string.documents,
        descriptionRes = R.string.documents_description,
        fields = listOf(
            FormField.FileUpload(
                id = "shipbuildingCertificate",
                labelRes = R.string.shipbuilding_certificate_or_sale_contract,
                allowedTypes = listOf("pdf", "jpg", "jpeg", "png"),
                maxSizeMB = 5,
                mandatory = true
            ),
            FormField.FileUpload(
                id = "inspectionDocuments",
                labelRes = R.string.inspection_documents,
                allowedTypes = listOf("pdf", "jpg", "jpeg", "png"),
                maxSizeMB = 5,
                mandatory = true
            )
        )
    )

    private fun createReviewStep(): StepData = StepData(
        titleRes = R.string.review,
        descriptionRes = R.string.step_placeholder_content,
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
        //if (registrationNumber.isBlank()) return

        // Validate registration number
        if (registrationNumber.isBlank()) {
            val newErrors = _uiState.value.fieldErrors.toMutableMap()
            newErrors["companyRegistrationNumber"] = "رقم السجل التجاري مطلوب"
            _uiState.value = _uiState.value.copy(fieldErrors = newErrors)
            return
        }

        if (registrationNumber.length < 3) {
            val newErrors = _uiState.value.fieldErrors.toMutableMap()
            newErrors["companyRegistrationNumber"] = "رقم السجل التجاري يجب أن يكون أكثر من 3 أرقام"
            _uiState.value = _uiState.value.copy(fieldErrors = newErrors)
            return
        }

        // Add loading state for this field
        _companyLookupLoading.value = _companyLookupLoading.value + "companyRegistrationNumber"

        // Clear previous errors for company fields
        val currentState = _uiState.value
        val newFieldErrors = currentState.fieldErrors.toMutableMap()
        newFieldErrors.remove("companyRegistrationNumber")
        newFieldErrors.remove("companyName")
        newFieldErrors.remove("companyType")

        _uiState.value = currentState.copy(fieldErrors = newFieldErrors)

        viewModelScope.launch {
            companyRepository.fetchCompanyLookup(registrationNumber)
                .flowOn(Dispatchers.IO)
                .catch { it ->
                    // Handle unexpected errors
                    val newErrors = _uiState.value.fieldErrors.toMutableMap()
                    newErrors["companyRegistrationNumber"] = "حدث خطأ أثناء البحث عن الشركة" + it.message
                    _uiState.value = _uiState.value.copy(fieldErrors = newErrors)

                    _companyLookupLoading.value = _companyLookupLoading.value - "companyRegistrationNumber"
                }
                .collect {
                    when (it) {
                        is BusinessState.Success -> {
                            val companyData = it.data.result
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

                                _companyLookupLoading.value = _companyLookupLoading.value - "companyRegistrationNumber"
                            }
                        }

                        is BusinessState.Error -> {
                            // Show error inline with the field
                            val newErrors = _uiState.value.fieldErrors.toMutableMap()
                            newErrors["companyRegistrationNumber"] = it.message

                            // Clear company fields if there was an error
                            val newFormData = _uiState.value.formData.toMutableMap()
                            newFormData.remove("companyName")
                            newFormData.remove("companyType")

                            _uiState.value = _uiState.value.copy(
                                fieldErrors = newErrors,
                                formData = newFormData
                            )

                            _companyLookupLoading.value = _companyLookupLoading.value - "companyRegistrationNumber"
                        }

                        is BusinessState.Loading -> {
                            // Loading state is already handled by _companyLookupLoading
                            // No additional action needed here
                        }
                    }
                }

//            try {
//                val result = companyLookupUseCase(
//                    CompanyLookupParams(registrationNumber)
//                )
//
//                when (result) {
//                    is BusinessState.Success -> {
//                        val companyData = result.data.result
//                        if (companyData != null) {
//                            // Update form data with company information
//                            val newFormData = currentState.formData.toMutableMap()
//                            newFormData["companyName"] = companyData.arabicCommercialName
//                            newFormData["companyType"] = companyData.commercialRegistrationEntityType
//
//                            _uiState.value = _uiState.value.copy(
//                                formData = newFormData,
//                                canProceedToNext = validateCurrentStep(
//                                    currentState.currentStep,
//                                    currentState.steps,
//                                    newFormData
//                                )
//                            )
//                        }
//                    }
//
//                    is BusinessState.Error -> {
//                        // Show error inline with the field
//                        val newErrors = _uiState.value.fieldErrors.toMutableMap()
//                        newErrors["companyRegistrationNumber"] = result.message
//
//                        // Clear company fields if there was an error
//                        val newFormData = _uiState.value.formData.toMutableMap()
//                        newFormData.remove("companyName")
//                        newFormData.remove("companyType")
//
//                        _uiState.value = _uiState.value.copy(
//                            fieldErrors = newErrors,
//                            formData = newFormData
//                        )
//                    }
//
//                    is BusinessState.Loading -> {
//                        // Loading state is already handled by _companyLookupLoading
//                        // No additional action needed here
//                    }
//                }
//            } catch (e: Exception) {
//                // Handle unexpected errors
//                val newErrors = _uiState.value.fieldErrors.toMutableMap()
//                newErrors["companyRegistrationNumber"] = "حدث خطأ أثناء البحث عن الشركة"
//
//                _uiState.value = _uiState.value.copy(fieldErrors = newErrors)
//            } finally {
//                // Remove loading state
//                _companyLookupLoading.value = _companyLookupLoading.value - "companyRegistrationNumber"
//            }
        }
    }

    fun isFieldLoading(fieldId: String): Boolean {
        return fieldId in _companyLookupLoading.value
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

    fun onFileSelected(fieldId: String, fileUri: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newSelectedFiles = currentState.selectedFiles.toMutableMap()

            // Update selected files
            newSelectedFiles[fieldId] = fileUri

            _uiState.value = currentState.copy(selectedFiles = newSelectedFiles)
        }
    }

    fun onFileRemoved(fieldId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val newSelectedFiles = currentState.selectedFiles.toMutableMap()

            // Remove the file
            newSelectedFiles.remove(fieldId)

            _uiState.value = currentState.copy(selectedFiles = newSelectedFiles)
        }
    }
}
