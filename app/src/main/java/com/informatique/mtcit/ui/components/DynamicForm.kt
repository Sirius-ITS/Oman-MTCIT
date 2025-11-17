package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.StepData
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
    onDeclarationChange: ((Boolean) -> Unit)? = null // Changed to declaration callback
    onTriggerNext: () -> Unit // ✅ أضف الـ parameter ده
) {

    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedPersonId by remember { mutableStateOf("PT-2024-001") }

    // Detect Review Step: If no fields, show ReviewStepContent
    if (stepData.fields.isEmpty() && allSteps.isNotEmpty()) {
        // This is the review step - show summary of all collected data
        ReviewStepContent(
            steps = allSteps,
            formData = formData,
//            onDeclarationChange = onDeclarationChange
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
                                    mandatory = field.mandatory,
                                    placeholder = field.label,
                                    enabled = true
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
                                mandatory = field.mandatory,
                                placeholder = field.label
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
                                    kotlinx.serialization.json.Json.decodeFromString<List<OwnerData>>(
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
                                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(field.value)
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
                                showAddNewButton = field.showAddNewButton, // ✅ مرر القيمة
                                addNewUnit = {
                                    // ✅ نحط flag أن المستخدم عايز يضيف سفينة جديدة
                                    onFieldChange(field.id, "[]", null) // نفضي الـ selection
                                    onFieldChange("isAddingNewUnit", "true", null) // نحط flag

                                    shouldTriggerNext = true // ✅ هنا
                                },
                                onSelectionChange = { updatedSelection ->
                                    val json = kotlinx.serialization.json.Json.encodeToString(updatedSelection)
                                    onFieldChange(field.id, json, null)
                                }
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
                                                isSelected = selectedPersonId == item.id,
                                                onClick = {
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                                    println("🎯 PersonType clicked")
                                                    println("📝 Field ID: ${field.id}")
                                                    println("🆔 Item ID: ${item.id}")
                                                    println("📊 Item Title: ${item.title}")
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                                    selectedPersonId = item.id

                                                    // ✅ الحل: ابعت الـ title مش الـ JSON
                                                    onFieldChange(field.id, item.title, null)
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
                                                    println("🆔 Item ID: ${item.id}")
                                                    println("📊 Item Title: ${item.title}")
                                                    println("━━━━━━━━━━━━━━━━━━━━━━━━━━")

                                                    selectedId = item.id

                                                    // ✅ الحل: ابعت الـ title أو id حسب احتياجك
                                                    onFieldChange(field.id, item.title, null)
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
                    }
                }
            }
        }
    }
}
