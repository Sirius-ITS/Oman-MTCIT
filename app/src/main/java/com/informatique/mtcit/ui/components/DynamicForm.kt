package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.navigation.NavRoutes
import com.informatique.mtcit.ui.screens.RequestDetail.CheckShipCondition
import com.informatique.mtcit.ui.viewmodels.StepData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


@Composable
fun DynamicStepForm(
    stepData: StepData,
    formData: Map<String, String> = emptyMap(), // Add formData parameter
    onFieldChange: (String, String, Boolean?) -> Unit,
    onFieldFocusLost: (String, String) -> Unit = { _, _ -> },
    isFieldLoading: (String) -> Boolean = { false },
    showConditionalFields: (String) -> Boolean = { true },
    onOpenFilePicker: ((String, List<String>) -> Unit)? = null,
    onViewFile: ((String, String) -> Unit)? = null,
    onRemoveFile: ((String) -> Unit)? = null,
    allSteps: List<StepData> = emptyList(), // Add parameter to pass all steps for review
    onDeclarationChange: ((Boolean) -> Unit)? = null, // Changed to declaration callback
    onTriggerNext: () -> Unit, // ✅ أضف الـ parameter ده
    // NEW: Validation parameters
    validationState: com.informatique.mtcit.ui.viewmodels.ValidationState = com.informatique.mtcit.ui.viewmodels.ValidationState.Idle,
    onMarineUnitSelected: ((String) -> Unit)? = null,
    // ✅ NEW: Lookup loading state parameters
    lookupLoadingStates: Map<String, Boolean> = emptyMap(),
    loadedLookupData: Map<String, Pair<List<String>, Boolean>> = emptyMap()
) {

    // ✅ Helper function to map field ID to lookup key
    fun getLookupKeyForField(fieldId: String): String? {
        return when (fieldId) {
            "registrationPort" -> "ports"
            "buildingCountry", "productionCountry", "ownerCountry" -> "countries"
            "ownerNationality" -> "nationalities"
            "unitType" -> "shipTypes"
            "unitClassification" -> "shipCategories"
            "maritimeActivity" -> "marineActivities"
            "proofType" -> "proofTypes"
            "engineCondition" -> "engineStatuses"
            "fuelTypes" -> "engineFuelTypes"
            "buildingMaterial" -> "buildMaterials"
            "bankName" -> "bankName"
            "mortgagePurpose" -> "mortgagePurpose"
            "sailingRegions" -> "sailingRegions"
            else -> null
        }
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedPersonId by remember { mutableStateOf("فرد") }

    // Detect Review Step: If no fields, show ReviewStepContent
    if (stepData.fields.isEmpty() && allSteps.isNotEmpty()) {
        // This is the review step - show summary of all collected data
        ReviewStepContent(
            steps = allSteps,
            formData = formData,
            onDeclarationChange = onDeclarationChange,
            onViewFile = onViewFile
        )
    } else {
        // Regular step - show form fields
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            stepData.fields.forEach { field ->
                // Only show field if it passes conditional visibility check
                if (showConditionalFields(field.id)) {
                    when (field) {
                        is FormField.TextField -> {
                            // Special handling for company registration number field
                            if (field.id == "companyRegistrationNumber") {
                                FocusAwareTextField(
                                    value = field.value,
                                    onValueChange = { onFieldChange(field.id, it, null) },
                                    onFocusLost = { value -> onFieldFocusLost(field.id, value) },
                                    label = field.label,
                                    isPassword = field.isPassword,
                                    isNumeric = field.isNumeric,
                                    isDecimal = field.isDecimal, // ✅ Pass isDecimal
                                    error = field.error,
                                    mandatory = field.mandatory,
                                    isLoading = isFieldLoading(field.id),
                                    maxLength = field.maxLength, // ✅ Pass maxLength
                                    minLength = field.minLength // ✅ Pass minLength
                                )
                            }
                            // ✅ Special handling for agriculture request number field (fishing boats)
                            else if (field.id == "agricultureRequestNumber") {
                                FocusAwareTextField(
                                    value = field.value,
                                    onValueChange = { onFieldChange(field.id, it, null) },
                                    onFocusLost = { value -> onFieldFocusLost(field.id, value) },
                                    label = field.label,
                                    isPassword = field.isPassword,
                                    isNumeric = field.isNumeric,
                                    isDecimal = field.isDecimal, // ✅ Pass isDecimal
                                    error = field.error,
                                    mandatory = field.mandatory,
                                    isLoading = isFieldLoading(field.id),
                                    maxLength = field.maxLength, // ✅ Pass maxLength
                                    minLength = field.minLength // ✅ Pass minLength
                                )
                            }
                            // Make company name and type fields read-only
                            else if (field.id == "companyName" || field.id == "companyType") {
                                FocusAwareTextField(
                                    value = field.value,
                                    onValueChange = { /* Read-only, no change allowed */ },
                                    label = field.label,
                                    isDecimal = field.isDecimal, // ✅ Pass isDecimal
                                    error = field.error,
                                    mandatory = field.mandatory,
                                    readOnly = true,
                                    maxLength = field.maxLength, // ✅ Pass maxLength
                                    minLength = field.minLength // ✅ Pass minLength
                                )
                            }
                            // Regular text fields
                            else {
                                CustomTextField(
                                    value = field.value,
                                    onValueChange = { onFieldChange(field.id, it, null) },
                                    label = field.label,
                                    isPassword = field.isPassword,
                                    isNumeric = field.isNumeric,
                                    isDecimal = field.isDecimal, // ✅ Pass isDecimal
                                    error = field.error,
                                    mandatory = field.mandatory,
                                    placeholder = field.label,
                                    enabled = field.enabled, // ✅ Use the enabled property
                                    maxLength = field.maxLength, // ✅ Pass maxLength
                                    minLength = field.minLength // ✅ Pass minLength
                                )
                            }
                        }

                        is FormField.DropDown -> {
                            // Get the lookup key for the field
                            val lookupKey = getLookupKeyForField(field.id)

                            // Determine if shimmer loading should be shown
                            val isShimmerLoading = lookupKey != null && lookupLoadingStates[lookupKey] == true

                            CustomDropdown(
                                label = field.label,
                                options = if (isShimmerLoading) emptyList() else field.options,
                                selectedOption = field.selectedOption,
                                onOptionSelected = { onFieldChange(field.id, it, null) },
                                error = field.error,
                                mandatory = field.mandatory,
                                placeholder = field.label,
                                isLoading = isShimmerLoading, // Pass loading state to dropdown
                                maxLength = field.maxLength, // ✅ Pass maxLength
                                minLength = field.minLength, // ✅ Pass minLength
                                enableSections = field.enableSections,
                                sections = if (isShimmerLoading) emptyList() else field.sections
                            )
                        }

                        is FormField.CheckBox -> {
                            CustomCheckBox(
                                checked = field.checked,
                                onCheckedChange = { onFieldChange(field.id, "", it) },
                                label = field.label,
                                error = field.error
                            )
                        }

                        is FormField.DatePicker -> {
                            CustomDatePicker(
                                value = field.value,
                                onValueChange = { onFieldChange(field.id, it, null) },
                                label = field.label,
                                error = field.error,
                                allowPastDates = field.allowPastDates,
                                mandatory = field.mandatory
                            )
                        }

                        is FormField.FileUpload -> {
                            CustomFileUpload(
                                value = field.value,
                                onValueChange = { onFieldChange(field.id, it, null) },
                                label = field.label,
                                error = field.error,
                                allowedTypes = field.allowedTypes,
                                maxSizeMB = field.maxSizeMB,
                                mandatory = field.mandatory,
                                fieldId = field.id,
                                onOpenFilePicker = onOpenFilePicker,
                                onViewFile = onViewFile,
                                onRemoveFile = onRemoveFile
                            )
                          }

                        is FormField.OwnerList -> {
                            // Parse owners from JSON mortgageValue
                            val owners = remember(field.value) {
                                try {
                                    Json.decodeFromString<List<OwnerData>>(
                                        field.value
                                    )
                                    Json.decodeFromString<List<OwnerData>>(field.value)
                                } catch (_: Exception) {
                                    emptyList<OwnerData>()
                                }
                            }

                            // Get total count from form data if totalCountFieldId is specified
                            val totalCount = field.totalCountFieldId?.let { countFieldId ->
                                formData[countFieldId] // Retrieve from form data
                            }

                            OwnerListManager(
                                owners = owners,
                                nationalities = field.nationalities,
                                countries = field.countries,
                                includeCompanyFields = field.includeCompanyFields,
                                totalOwnersCount = totalCount,
                                onOwnersChange = { updatedOwners ->
                                    val json = Json.encodeToString(updatedOwners)
                                    onFieldChange(field.id, json, null)
                                },
                                onTotalCountChange = field.totalCountFieldId?.let { countFieldId ->
                                    { count -> onFieldChange(countFieldId, count, null) }
                                }
                            )
                        }
                        is FormField.EngineList -> {
                            val engines = remember(field.value) {
                                try {
                                    Json.decodeFromString<List<EngineData>>(
                                        field.value
                                    )
                                    Json.decodeFromString<List<EngineData>>(field.value)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }

                            EngineListManager(
                                engines = engines,
                                engineTypes = field.engineTypes, // ✅ Pass engineTypes
                                manufacturers = field.manufacturers,
                                countries = field.countries,
                                fuelTypes = field.fuelTypes,
                                conditions = field.engineConditions,
                                onEnginesChange = { updatedEngines ->
                                    val json = Json.encodeToString(updatedEngines)
                                    onFieldChange(field.id, json, null)
                                }
                            )
                        }
                        is FormField.MarineUnitSelector -> {
                            // Parse selected unit IDs from JSON
                            val selectedIds = remember(field.value) {
                                try {
                                    Json.decodeFromString<List<String>>(field.value)
                                } catch (_: Exception) {
                                    emptyList<String>()
                                }
                            }
                            var shouldTriggerNext by remember { mutableStateOf(false) }
                            LaunchedEffect(shouldTriggerNext) {
                                if (shouldTriggerNext) {
                                    onTriggerNext()
                                    shouldTriggerNext = false
                                }
                            }

                            MarineUnitSelectorManager(
                                units = field.units,
                                selectedUnitIds = selectedIds,
                                allowMultipleSelection = field.allowMultipleSelection,
                                showOwnedUnitsWarning = field.showOwnedUnitsWarning,
                                showAddNewButton = field.showAddNewButton,
                                addNewUnit = {
                                    // Clear selection and set flag
                                    onFieldChange(field.id, "[]", null)
                                    onFieldChange("isAddingNewUnit", "true", null)

                                    // Trigger next step immediately (no need to wait for Next button)
                                    onTriggerNext()
                                },
                                onSelectionChange = { updatedSelection ->
                                    val json = kotlinx.serialization.json.Json.encodeToString(updatedSelection)
                                    onFieldChange(field.id, json, null)
                                    // Remove hardcoded navigation - let validation handle it
                                },
                                // Pass validation parameters down to MarineUnitSelectorManager
                                validationState = validationState,
                                onMarineUnitSelected = onMarineUnitSelected
                            )
                        }

                        is FormField.SelectableList<*> -> {
                            when(field.id){
                                "selectionPersonType" -> {
                                    SelectableList(
                                        items = field.options,
                                        uiItem = { item ->
                                            PersonTypeCard(
                                                item = item as PersonType,
                                                defaultValue = selectedPersonId == item.title.toString(),
                                                isSelected = selectedPersonId == item.title.toString(),
                                                onClick = {
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                    println("🎯 PersonType clicked")
                                                    println("📝 Field ID: ${field.id}")
                                                    println("🆔 Item ID: ${item.id}")
                                                    println("📊 Item Title: ${item.title}")
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                                    selectedPersonId = item.title.toString()

                                                    // ✅ الحل: ابعت الـ title مش الـ JSON
                                                    onFieldChange(field.id, item.title.toString(), null)
                                                }
                                            )
                                        }
                                    )
                                }

                                "selectionData" -> {
                                    SelectableList(
                                        items = field.options,
                                        uiItem = { item ->
                                            SelectableItemCard(
                                                item = item as SelectableItem,
                                                isSelected = selectedId == item.id,
                                                onClick = {
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                    println("🎯 SelectableItem clicked")
                                                    println("📝 Field ID: ${field.id}")
                                                    println("🆔 Item ID (CR Number): ${item.id}")
                                                    println("📊 Item Title (Company Name): ${item.title}")
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                                    selectedId = item.id

                                                    // ✅ FIXED: Send the CR Number (item.id) instead of company name (item.title)
                                                    // This ensures the API receives the commercialNumber correctly
                                                    onFieldChange(field.id, item.id, null)
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        is FormField.RadioGroup -> {
                            // ✅ Create a local state to track selection
                            var localSelection by remember {
                                mutableStateOf(field.selectedValue)
                            }

                            RadioGroupManager(
                                field = field as FormField.RadioGroup,
                                selectedValue = localSelection, // ✅ Use local state
                                onValueChange = { newValue ->
                                    localSelection = newValue // ✅ Update local state first
                                    onFieldChange(field.id, newValue, null) // Then notify parent
                                }
                            )
                        }
                        is FormField.InfoCard -> {
                            InfoCardComponent(
                                items = field.items,
                                showCheckmarks = field.showCheckmarks
                            )
                        }
                        is FormField.PaymentDetails -> {
                            PaymentDetailsComponent(
                                arabicValue = field.arabicValue,
                                lineItems = field.lineItems,
                                totalCost = field.totalCost,
                                totalTax = field.totalTax,
                                finalTotal = field.finalTotal
                            )
                        }
                        is FormField.PhoneNumberField -> {
                            // Get current country code from formData or use default
                            val currentCountryCode = formData["${field.id}_countryCode"] ?: field.selectedCountryCode

                            PhoneNumberComponent(
                                value = field.value,
                                onValueChange = { onFieldChange(field.id, it, null) },
                                label = field.label,
                                countryCodes = field.countryCodes,
                                selectedCountryCode = currentCountryCode,
                                onCountryCodeChange = { newCode ->
                                    onFieldChange("${field.id}_countryCode", newCode, null)
                                },
                                placeholder = field.placeholder,
                                error = field.error,
                                mandatory = field.mandatory
                            )
                        }
                        is FormField.OTPField -> {
                            OTPComponent(
                                value = field.value,
                                onValueChange = { onFieldChange(field.id, it, null) },
                                label = field.label,
                                phoneNumber = field.phoneNumber,
                                otpLength = field.otpLength,
                                remainingTime = field.remainingTime,
                                onResendOTP = {
                                    // Trigger resend OTP action
                                    // You can call your API here
                                },
                                error = field.error)
                        }

                        is FormField.SailorList -> {
                            val sailors = remember(field.value) {
                                try {
                                    Json.decodeFromString<List<SailorData>>(
                                        field.value
                                    )
                                    Json.decodeFromString<List<SailorData>>(field.value)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }

                            SailorListManager(
                                sailors = sailors,
                                jobs = field.jobs,
                                onSailorChange = { updatedSailors ->
                                    val json = Json.encodeToString(updatedSailors)
                                    onFieldChange(field.id, json, null)
                                }
                            )
                        }

                        is FormField.MultiSelectDropDown -> {
                            // Parse selected options from JSON array
                            val selectedOptions = remember(field.value) {
                                try {
                                    Json.decodeFromString<List<String>>(field.value)
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }

                            // Get the lookup key for the field
                            val lookupKey = getLookupKeyForField(field.id)

                            // Determine if shimmer loading should be shown
                            val isShimmerLoading = lookupKey != null && lookupLoadingStates[lookupKey] == true

                            CustomMultiSelectDropdown(
                                label = field.label,
                                options = if (isShimmerLoading) emptyList() else field.options,
                                selectedOptions = selectedOptions,
                                onOptionsSelected = { updatedSelection ->
                                    val json = Json.encodeToString(updatedSelection)
                                    onFieldChange(field.id, json, null)
                                },
                                error = field.error,
                                mandatory = field.mandatory,
                                placeholder = field.placeholder ?: field.label,
                                isLoading = isShimmerLoading,
                                maxSelection = field.maxSelection,
                                showSelectionCount = field.showSelectionCount
                            )
                        }
                    }
                }
            }
        }
    }
}
