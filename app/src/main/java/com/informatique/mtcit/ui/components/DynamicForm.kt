package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData
import com.informatique.mtcit.ui.components.FocusAwareTextField


@Composable
fun DynamicStepForm(
    stepData: StepData,
    onFieldChange: (String, String, Boolean?) -> Unit,
    onCompanyRegistrationFocusLost: (String) -> Unit = {},
    isFieldLoading: (String) -> Boolean = { false },
    showConditionalFields: (String) -> Boolean = { true }
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
                                onFocusLost = onCompanyRegistrationFocusLost,
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
                            mandatory = field.mandatory
                        )
                    }
                }
            }
        }
    }
}
