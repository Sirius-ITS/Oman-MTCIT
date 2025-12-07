package com.informatique.mtcit.business.validation.rules


import com.informatique.mtcit.common.FormField

/**
 * Base class for cross-field validation rules
 */
sealed class ValidationRule {
    abstract fun validate(fields: List<FormField>): ValidationResult


    /**
     * ✅ NEW: Cross-Step Validation
     * Validates fields across multiple steps using accumulated form data
     */
    /**
     * Cross-Step Validation
     */
    data class CrossStepValidation(
        val triggerFieldId: String,
        val triggerCondition: (String?) -> Boolean,
        val requiredFieldId: String,
        val errorFieldId: String,
        val errorMessage: String
    ) : ValidationRule() {
        override fun validate(fields: List<FormField>): ValidationResult {
            return ValidationResult.Valid
        }
        fun validateWithAccumulatedData(accumulatedData: Map<String, String>): ValidationResult {

            val triggerValue = accumulatedData[triggerFieldId]
            val requiredValue = accumulatedData[requiredFieldId]

            if (triggerCondition(triggerValue)) {
                println("🔍 Condition met! Checking required field...")
                if (requiredValue.isNullOrBlank()) {
                    println("🔍 ❌ Required field is blank! Returning error.")
                    return ValidationResult.Invalid(errorFieldId, errorMessage)
                } else {
                    println("🔍 ✅ Required field has mortgageValue: $requiredValue")
                }
            } else {
                println("🔍 ✅ Condition not met, validation passes")
            }

            return ValidationResult.Valid
        }
    }

    /**
     * Conditional Required: Field becomes required when condition is met
     * Example: IMO required when grossTonnage > 500
     */
    data class ConditionalRequired(
        val triggerFieldId: String,
        val triggerCondition: (FormField) -> Boolean,
        val requiredFieldId: String,
        val errorMessage: String,
        val checkInAllData : Boolean = false
    ) : ValidationRule() {
        override fun validate(fields: List<FormField>): ValidationResult {
            val triggerField = fields.find { it.id == triggerFieldId }
            val requiredField = fields.find { it.id == requiredFieldId }

            if (triggerField == null || requiredField == null) {
                return ValidationResult.Valid
            }

            // Check if condition is met
            if (triggerCondition(triggerField)) {
                val isEmpty = when (requiredField) {
                    is FormField.TextField -> requiredField.value.isBlank()
                    is FormField.DropDown -> requiredField.value.isBlank()
                    else -> false
                }

                if (isEmpty) {
                    return ValidationResult.Invalid(requiredFieldId, errorMessage)
                }
            }

            return ValidationResult.Valid
        }
    }

    /**
     * Numeric Comparison: Compare two numeric fields
     * Example: netTonnage must be <= grossTonnage
     */
    data class NumericComparison(
        val field1Id: String,
        val field2Id: String,
        val comparison: Comparison,
        val errorFieldId: String,
        val errorMessage: String
    ) : ValidationRule() {
        override fun validate(fields: List<FormField>): ValidationResult {
            val field1 = fields.find { it.id == field1Id } as? FormField.TextField
            val field2 = fields.find { it.id == field2Id } as? FormField.TextField

            if (field1 == null || field2 == null) return ValidationResult.Valid
            if (field1.value.isBlank() || field2.value.isBlank()) return ValidationResult.Valid

            val value1 = field1.value.toDoubleOrNull() ?: return ValidationResult.Valid
            val value2 = field2.value.toDoubleOrNull() ?: return ValidationResult.Valid

            val isValid = when (comparison) {
                Comparison.GREATER_THAN -> value1 > value2
                Comparison.LESS_THAN -> value1 < value2
                Comparison.GREATER_THAN_OR_EQUAL -> value1 >= value2
                Comparison.LESS_THAN_OR_EQUAL -> value1 <= value2
                Comparison.EQUAL -> value1 == value2
                Comparison.NOT_EQUAL -> value1 != value2
            }

            return if (!isValid) {
                ValidationResult.Invalid(errorFieldId, errorMessage)
            } else {
                ValidationResult.Valid
            }
        }
    }

    /**
     * Custom Validation: Custom logic using lambda
     */
    data class CustomValidation(
        val fieldIds: List<String>,
        val errorFieldId: String,
        val errorMessage: String,
        val validationLogic: (List<FormField>) -> Boolean
    ) : ValidationRule() {
        override fun validate(fields: List<FormField>): ValidationResult {
            val relevantFields = fields.filter { it.id in fieldIds }

            if (relevantFields.size != fieldIds.size) return ValidationResult.Valid

            return if (!validationLogic(relevantFields)) {
                ValidationResult.Invalid(errorFieldId, errorMessage)
            } else {
                ValidationResult.Valid
            }
        }
    }

    /**
     * Multiple Conditions: Multiple fields must meet conditions
     */
    data class MultiFieldCondition(
        val conditions: Map<String, (FormField) -> Boolean>,
        val requiredFieldId: String,
        val errorMessage: String,
        val allMustMatch: Boolean = true
    ) : ValidationRule() {
        override fun validate(fields: List<FormField>): ValidationResult {
            val results = conditions.map { (fieldId, condition) ->
                fields.find { it.id == fieldId }?.let { condition(it) } ?: false
            }

            val shouldRequire = if (allMustMatch) {
                results.all { it }
            } else {
                results.any { it }
            }

            if (shouldRequire) {
                val requiredField = fields.find { it.id == requiredFieldId }
                val isEmpty = when (requiredField) {
                    is FormField.TextField -> requiredField?.value?.isBlank() ?: true
                    is FormField.DropDown -> requiredField?.value?.isBlank() ?: true
                    else -> false
                }

                if (isEmpty) {
                    return ValidationResult.Invalid(requiredFieldId, errorMessage)
                }
            }

            return ValidationResult.Valid
        }
    }
}

enum class Comparison {
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    NOT_EQUAL
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val fieldId: String, val error: String) : ValidationResult()
}