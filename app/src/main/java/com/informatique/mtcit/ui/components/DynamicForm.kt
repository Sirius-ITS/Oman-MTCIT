package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData


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
) {
    // Detect Review Step: If no fields, show ReviewStepContent
    if (stepData.fields.isEmpty() && allSteps.isNotEmpty()) {
        // This is the review step - show summary of all collected data
        ReviewStepContent(
            steps = allSteps,
            formData = formData
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
                                    error = field.error,
                                    mandatory = field.mandatory,
                                    isLoading = isFieldLoading(field.id)
                                )
                            }
                            // Make company name and type fields read-only
                            else if (field.id == "companyName" || field.id == "companyType") {
                                FocusAwareTextField(
                                    value = field.value,
                                    onValueChange = { /* Read-only, no change allowed */ },
                                    label = field.label,
                                    error = field.error,
                                    mandatory = field.mandatory,
                                    readOnly = true
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
                                    error = field.error,
                                    mandatory = field.mandatory
                                )
                            }
                        }

                        is FormField.DropDown -> {
                            CustomDropdown(
                                label = field.label,
                                options = field.options,
                                selectedOption = field.selectedOption,
                                onOptionSelected = { onFieldChange(field.id, it, null) },
                                error = field.error,
                                mandatory = field.mandatory
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
                            // Parse owners from JSON value
                            val owners = remember(field.value) {
                                try {
                                    kotlinx.serialization.json.Json.decodeFromString<List<OwnerData>>(field.value)
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
                                    val json = kotlinx.serialization.json.Json.encodeToString(updatedOwners)
                                    onFieldChange(field.id, json, null)
                                },
                                onTotalCountChange = field.totalCountFieldId?.let { countFieldId ->
                                    { count -> onFieldChange(countFieldId, count, null) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
