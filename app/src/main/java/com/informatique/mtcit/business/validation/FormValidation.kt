package com.informatique.mtcit.business.validation

import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.common.FormField
import javax.inject.Inject

class FormValidator @Inject constructor(
    private val crossFieldValidator: CrossFieldValidator
) {

    fun validate(field: FormField): FormField {
        return when (field) {
            is FormField.TextField -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    field.isNumeric && !field.isDecimal && field.value.any { !it.isDigit() } -> "Must be numeric"
                    field.isPassword && field.value.length < 6 -> "Password too short"
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.DropDown -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} must be selected" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.CheckBox -> {
                val error = when {
                    !field.checked && field.mandatory -> "You must accept ${field.label}"
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.DatePicker -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.FileUpload -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.OwnerList -> {
                // Validate that at least one owner is added if mandatory
                val error = when {
                    field.value == "[]" || field.value.isBlank() -> {
                        if (field.mandatory) "At least one owner must be added" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.EngineList -> {
                // Validate that at least one owner is added if mandatory
                val error = when {
                    field.value == "[]" || field.value.isBlank() -> {
                        if (field.mandatory) "At least one engine must be added" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.SelectableList<*> -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "At least one commercial registration must be selected" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }
            is FormField.MarineUnitSelector -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }
            is FormField.RadioGroup-> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }
            is FormField.InfoCard -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }
            is FormField.PhoneNumberField -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }
            is FormField.OTPField -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }


            is FormField.SailorList -> {
                val error = when {
                    field.value == "[]" || field.value.isBlank() -> {
                        if (field.mandatory) "At least one sailor must be added" else null
                    }
                    else -> null
                }
                field.copy(error = error)
            }

            is FormField.MultiSelectDropDown -> {
                val error = when {
                    field.value == "[]" || field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required - please select at least one option" else null
                    }
                    field.maxSelection != null && field.selectedOptions.size > field.maxSelection -> {
                        "Maximum ${field.maxSelection} selections allowed"
                    }
                    else -> null
                }
                field.copy(error = error)
            }
        }
    }
    /**
     * Validate with cross-field rules (same step only)
     */
    fun validateWithRules(
        fields: List<FormField>,
        rules: List<ValidationRule>
    ): List<FormField> {
        val basicValidated = validateAll(fields)
        return crossFieldValidator.validateWithRules(basicValidated, rules)
    }

    /**
     * ✅ NEW: Validate with accumulated data from all steps
     */
    fun validateWithAccumulatedData(
        currentStepFields: List<FormField>,
        allFormData: Map<String, String>,
        rules: List<ValidationRule>
    ): List<FormField> {
        println("🔍 FormValidator.validateWithAccumulatedData called")
        println("🔍 Current step fields: ${currentStepFields.map { it.id }}")
        println("🔍 All form data: $allFormData")
        println("🔍 Rules count: ${rules.size}")

        // First apply basic validation
        val basicValidated = currentStepFields.map { field ->
            val value = allFormData[field.id] ?: field.value
            val updatedField = when (field) {
                is FormField.TextField -> field.copy(value = value)
                is FormField.DropDown -> field.copy(value = value)
                is FormField.DatePicker -> field.copy(value = value)
                is FormField.MultiSelectDropDown -> field.copy(value = value)
                else -> field
            }
            validate(updatedField)
        }

        println("🔍 After basic validation: ${basicValidated.map { "${it.id}: error=${it.error}" }}")

        // Then apply cross-field validation with accumulated data
        val result = crossFieldValidator.validateWithAccumulatedData(
            basicValidated,
            allFormData,
            rules
        )

        println("🔍 After cross-field validation: ${result.map { "${it.id}: error=${it.error}" }}")

        return result
    }

    fun isFormValid(fields: List<FormField>): Boolean {
        return fields.all { it.error == null }
    }

    fun validateAll(fields: List<FormField>): List<FormField> {
        return fields.map { validate(it) }
    }


}