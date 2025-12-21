//package com.informatique.mtcit.business.validation
//
//
//import com.informatique.mtcit.business.validation.rules.ValidationResult
//import com.informatique.mtcit.business.validation.rules.ValidationRule
//import com.informatique.mtcit.common.FormField
//import javax.inject.Inject
//
///**
// * Handles cross-field validation using defined rules
// */
//class CrossFieldValidator @Inject constructor() {
//
//    /**
//     * Validate fields using provided rules
//     * Returns updated fields with error messages
//     */
//    fun validateWithRules(
//        fields: List<FormField>,
//        rules: List<ValidationRule>
//    ): List<FormField> {
//        var validatedFields = fields
//
//        rules.forEach { rule ->
//            val result = rule.validate(validatedFields)
//
//            if (result is ValidationResult.Invalid) {
//                validatedFields = validatedFields.map { field ->
//                    if (field.id == result.fieldId) {
//                        applyError(field, result.error)
//                    } else {
//                        field
//                    }
//                }
//            }
//        }
//
//        return validatedFields
//    }
//
//    /**
//     * Apply error to specific field type
//     */
//    private fun applyError(field: FormField, error: String): FormField {
//        return when (field) {
//            is FormField.TextField -> field.copy(error = error)
//            is FormField.DropDown -> field.copy(error = error)
//            is FormField.CheckBox -> field.copy(error = error)
//            is FormField.DatePicker -> field.copy(error = error)
//            is FormField.FileUpload -> field.copy(error = error)
//            is FormField.OwnerList -> field.copy(error = error)
//            is FormField.EngineList -> field.copy(error = error)
//            is FormField.SelectableList<*> -> field.copy(error = error)
//            is FormField.MarineUnitSelector -> field.copy(error = error)
//            is FormField.RadioGroup -> field.copy(error = error)
//        }
//    }
//}

package com.informatique.mtcit.business.validation

import com.informatique.mtcit.business.validation.rules.ValidationResult
import com.informatique.mtcit.business.validation.rules.ValidationRule
import com.informatique.mtcit.common.FormField
import java.text.Normalizer
import javax.inject.Inject

class CrossFieldValidator @Inject constructor() {

    /**
     * Validate fields using provided rules (same-step validation)
     */
    fun validateWithRules(
        fields: List<FormField>,
        rules: List<ValidationRule>
    ): List<FormField> {
        var validatedFields = fields

        rules.forEach { rule ->
            // Skip CrossStepValidation rules - they need accumulated data
            if (rule is ValidationRule.CrossStepValidation) {
                return@forEach
            }

            val result = rule.validate(validatedFields)

            if (result is ValidationResult.Invalid) {
                validatedFields = validatedFields.map { field ->
                    if (field.id == result.fieldId) {
                        applyError(field, result.error)
                    } else {
                        field
                    }
                }
            }
        }

        return validatedFields
    }

    /**
     * ✅ NEW: Validate fields with accumulated data from all steps
     */
    fun validateWithAccumulatedData(
        currentStepFields: List<FormField>,
        allFormData: Map<String, String>,
        rules: List<ValidationRule>
    ): List<FormField> {
        println("🔍 CrossFieldValidator.validateWithAccumulatedData called")
        println("🔍 Rules: ${rules.map { it::class.simpleName }}")

        var validatedFields = currentStepFields

        rules.forEach { rule ->
            println("🔍 Processing rule: ${rule::class.simpleName}")

            when (rule) {
                is ValidationRule.CrossStepValidation -> {
                    println("🔍 CrossStepValidation - triggerField: ${rule.triggerFieldId}, requiredField: ${rule.requiredFieldId}")
                    println("🔍 Accumulated data has triggerField? ${allFormData.containsKey(rule.triggerFieldId)}")
                    println("🔍 Accumulated data has requiredField? ${allFormData.containsKey(rule.requiredFieldId)}")
                    println("🔍 Trigger mortgageValue: ${allFormData[rule.triggerFieldId]}")
                    println("🔍 Required mortgageValue: ${allFormData[rule.requiredFieldId]}")

                    val result = rule.validateWithAccumulatedData(allFormData)
                    println("🔍 Validation result: $result")

                    if (result is ValidationResult.Invalid) {
                        println("🔍 ❌ Validation failed! Field: ${result.fieldId}, Error: ${result.error}")
                        validatedFields = validatedFields.map { field ->
                            if (field.id == result.fieldId) {
                                val errorField = applyError(field, result.error)
                                println("🔍 Applied error to field ${field.id}")
                                errorField
                            } else {
                                field
                            }
                        }
                    } else {
                        println("🔍 ✅ Validation passed")
                    }
                }
                else -> {
                    val result = rule.validate(validatedFields)

                    if (result is ValidationResult.Invalid) {
                        validatedFields = validatedFields.map { field ->
                            if (field.id == result.fieldId) {
                                applyError(field, result.error)
                            } else {
                                field
                            }
                        }
                    }
                }
            }
        }

        return validatedFields
    }

    private fun applyError(field: FormField, error: String): FormField {
        return when (field) {
            is FormField.TextField -> field.copy(error = error)
            is FormField.DropDown -> field.copy(error = error)
            is FormField.CheckBox -> field.copy(error = error)
            is FormField.DatePicker -> field.copy(error = error)
            is FormField.FileUpload -> field.copy(error = error)
            is FormField.OwnerList -> field.copy(error = error)
            is FormField.EngineList -> field.copy(error = error)
            is FormField.SelectableList<*> -> field.copy(error = error)
            is FormField.MarineUnitSelector -> field.copy(error = error)
            is FormField.RadioGroup -> field.copy(error = error)
            is FormField.InfoCard -> field.copy(error = error)
            is FormField.PhoneNumberField -> field.copy(error = error)
            is FormField.OTPField -> field.copy(error = error)
            is FormField.SailorList -> field.copy(error = error)
            is FormField.MultiSelectDropDown -> field.copy(error = error)
            is FormField.PaymentDetails -> field.copy(value = error)
        }
    }
}