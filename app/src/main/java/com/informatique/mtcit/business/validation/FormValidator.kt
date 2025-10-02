package com.informatique.mtcit.business.validation

import com.informatique.mtcit.common.FormField
import javax.inject.Inject

class FormValidator @Inject constructor() {

    fun validate(field: FormField): FormField {
        return when (field) {
            is FormField.TextField -> {
                val error = when {
                    field.value.isBlank() -> {
                        if (field.mandatory) "${field.label} is required" else null
                    }
                    field.isNumeric && field.value.any { !it.isDigit() } -> "Must be numeric"
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
        }
    }

    fun validateAll(fields: List<FormField>): List<FormField> {
        return fields.map { validate(it) }
    }

    fun isFormValid(fields: List<FormField>): Boolean {
        return fields.all { it.error == null }
    }
}
