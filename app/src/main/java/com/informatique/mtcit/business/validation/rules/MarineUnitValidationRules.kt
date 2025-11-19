//package com.informatique.mtcit.business.validation.rules
//
//
//import com.informatique.mtcit.common.FormField
//
///**
// * Validation rules specific to marine unit weights and loads
// */
//object MarineUnitValidationRules {
//
//    /**
//     * IMO is required when gross tonnage > 500
//     */
//    fun imoRequiredForLargeVessels() = ValidationRule.ConditionalRequired(
//        triggerFieldId = "grossTonnage",
//        triggerCondition = { value ->
//            value.toString().toDoubleOrNull()?.let { it > 500 } ?: false
//        },
//        requiredFieldId = "imoNumber",
//        errorMessage = "IMO number is required for vessels over 500 gross tonnage",
//        checkInAllData = true
//    )
//
//
//    /**
//     * MMSI is required when gross tonnage > 300
//     */
//    fun mmsiRequiredForMediumVessels() = ValidationRule.ConditionalRequired(
//        triggerFieldId = "grossTonnage",
//        triggerCondition = { field ->
//            if (field is FormField.TextField) {
//                field.value.toDoubleOrNull()?.let { it > 300 } ?: false
//            } else false
//        },
//        requiredFieldId = "mmsi",
//        errorMessage = "MMSI number is required for vessels over 300 gross tonnage"
//    )
//
//    /**
//     * Net tonnage must be less than or equal to gross tonnage
//     */
//    fun netTonnageLessThanOrEqualGross() = ValidationRule.NumericComparison(
//        field1Id = "netTonnage",
//        field2Id = "grossTonnage",
//        comparison = Comparison.LESS_THAN_OR_EQUAL,
//        errorFieldId = "netTonnage",
//        errorMessage = "Net tonnage must be less than or equal to gross tonnage"
//    )
//
//    /**
//     * Static load (DWT) should be reasonable relative to gross tonnage
//     */
//    fun staticLoadValidation() = ValidationRule.NumericComparison(
//        field1Id = "staticLoad",
//        field2Id = "grossTonnage",
//        comparison = Comparison.LESS_THAN_OR_EQUAL,
//        errorFieldId = "staticLoad",
//        errorMessage = "Static load cannot exceed gross tonnage"
//    )
//
//    /**
//     * Max permitted load should be >= static load
//     */
//    fun maxPermittedLoadValidation() = ValidationRule.CustomValidation(
//        fieldIds = listOf("maxPermittedLoad", "staticLoad"),
//        errorFieldId = "maxPermittedLoad",
//        errorMessage = "Maximum permitted load must be greater than or equal to static load"
//    ) { fields ->
//        val maxLoad = (fields.find { it.id == "maxPermittedLoad" } as? FormField.TextField)
//            ?.value?.toDoubleOrNull()
//        val staticLoad = (fields.find { it.id == "staticLoad" } as? FormField.TextField)
//            ?.value?.toDoubleOrNull()
//
//        // If maxPermittedLoad is empty (optional), it's valid
//        if (maxLoad == null) return@CustomValidation true
//        if (staticLoad == null) return@CustomValidation true
//
//        maxLoad >= staticLoad
//    }
//
//    /**
//     * Get all weight-related validation rules
//     */
//    fun getAllWeightRules(): List<ValidationRule> = listOf(
//        imoRequiredForLargeVessels(),
//        mmsiRequiredForMediumVessels(),
//        netTonnageLessThanOrEqualGross(),
//        staticLoadValidation(),
//        maxPermittedLoadValidation()
//    )
//}

package com.informatique.mtcit.business.validation.rules

import com.informatique.mtcit.common.FormField

object MarineUnitValidationRules {

    /**
     * ✅ IMO is required when gross tonnage > 500
     * Uses CustomValidation to access accumulated data from previous steps
     */
    fun imoRequiredForLargeVessels(accumulatedData: Map<String, Any?>) =
        ValidationRule.CustomValidation(
            fieldIds = listOf("grossTonnage"),
            errorFieldId = "grossTonnage",
            errorMessage = "IMO number is required for vessels over 500 gross tonnage. Please go back and enter IMO number."
        ) { fields ->
            val grossTonnage = (fields.find { it.id == "grossTonnage" } as? FormField.TextField)
                ?.value?.toDoubleOrNull()

            // If gross tonnage <= 500 or empty, validation passes
            if (grossTonnage == null || grossTonnage <= 500) {
                return@CustomValidation true
            }

            // If gross tonnage > 500, check if IMO exists in accumulated data
            val imoNumber = accumulatedData["imoNumber"]?.toString()
            !imoNumber.isNullOrBlank()
        }

    /**
     * ✅ MMSI is required when gross tonnage > 300
     */
    fun mmsiRequiredForMediumVessels(accumulatedData: Map<String, Any?>) =
        ValidationRule.CustomValidation(
            fieldIds = listOf("grossTonnage"),
            errorFieldId = "grossTonnage",
            errorMessage = "MMSI number is required for vessels over 300 gross tonnage. Please go back and enter MMSI number."
        ) { fields ->
            val grossTonnage = (fields.find { it.id == "grossTonnage" } as? FormField.TextField)
                ?.value?.toDoubleOrNull()

            if (grossTonnage == null || grossTonnage <= 300) {
                return@CustomValidation true
            }

            val mmsi = accumulatedData["mmsi"]?.toString()
            !mmsi.isNullOrBlank()
        }

    /**
     * Net tonnage must be less than or equal to gross tonnage
     */
    fun netTonnageLessThanOrEqualGross() = ValidationRule.NumericComparison(
        field1Id = "netTonnage",
        field2Id = "grossTonnage",
        comparison = Comparison.LESS_THAN_OR_EQUAL,
        errorFieldId = "netTonnage",
        errorMessage = "Net tonnage must be less than or equal to gross tonnage"
    )

    /**
     * Static load (DWT) should be reasonable relative to gross tonnage
     */
    fun staticLoadValidation() = ValidationRule.NumericComparison(
        field1Id = "staticLoad",
        field2Id = "grossTonnage",
        comparison = Comparison.LESS_THAN_OR_EQUAL,
        errorFieldId = "staticLoad",
        errorMessage = "Static load cannot exceed gross tonnage"
    )

    /**
     * Max permitted load should be >= static load
     */
    fun maxPermittedLoadValidation() = ValidationRule.CustomValidation(
        fieldIds = listOf("maxPermittedLoad", "staticLoad"),
        errorFieldId = "maxPermittedLoad",
        errorMessage = "Maximum permitted load must be greater than or equal to static load"
    ) { fields ->
        val maxLoad = (fields.find { it.id == "maxPermittedLoad" } as? FormField.TextField)
            ?.value?.toDoubleOrNull()
        val staticLoad = (fields.find { it.id == "staticLoad" } as? FormField.TextField)
            ?.value?.toDoubleOrNull()

        if (maxLoad == null) return@CustomValidation true
        if (staticLoad == null) return@CustomValidation true

        maxLoad >= staticLoad
    }

    /**
     * Get all weight-related validation rules
     * IMPORTANT: Pass accumulatedFormData for cross-step validation
     */
    fun getAllWeightRules(accumulatedData: Map<String, Any?>): List<ValidationRule> = listOf(
        imoRequiredForLargeVessels(accumulatedData),
        mmsiRequiredForMediumVessels(accumulatedData),
        netTonnageLessThanOrEqualGross(),
        staticLoadValidation(),
        maxPermittedLoadValidation()
    )
}