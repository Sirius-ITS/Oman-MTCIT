package com.informatique.mtcit.business.validation.rules


import com.informatique.mtcit.common.FormField

/**
 * Validation rules specific to marine unit dimensions
 */
object DimensionValidationRules {

    /**
     * Overall length must be greater than width
     */
    fun lengthGreaterThanWidth() = ValidationRule.NumericComparison(
        field1Id = "overallLength",
        field2Id = "overallWidth",
        comparison = Comparison.GREATER_THAN,
        errorFieldId = "overallWidth",
        errorMessage = "Width cannot exceed length"
    )

    /**
     * Validate height based on vessel size
     */
    fun heightValidation() = ValidationRule.CustomValidation(
        fieldIds = listOf("height", "grossTonnage"),
        errorFieldId = "height",
        errorMessage = "Height seems unusually large for this vessel size"
    ) { fields ->
        val height = (fields.find { it.id == "height" } as? FormField.TextField)
            ?.value?.toDoubleOrNull()
        val tonnage = (fields.find { it.id == "grossTonnage" } as? FormField.TextField)
            ?.value?.toDoubleOrNull()

        // If height is empty (optional), it's valid
        if (height == null) return@CustomValidation true
        if (tonnage == null) return@CustomValidation true

        // Simple heuristic: height should be reasonable for tonnage
        when {
            tonnage < 100 -> height <= 25.0
            tonnage < 500 -> height <= 40.0
            tonnage < 1000 -> height <= 50.0
            else -> height <= 70.0
        }
    }

    /**
     * Validate deck count based on vessel size
     */
    fun deckCountValidation() = ValidationRule.CustomValidation(
        fieldIds = listOf("decksCount", "grossTonnage"),
        errorFieldId = "decksCount",
        errorMessage = "Number of decks seems unusual for this vessel size"
    ) { fields ->
        val deckCount = (fields.find { it.id == "decksCount" } as? FormField.TextField)
            ?.value?.toIntOrNull()
        val tonnage = (fields.find { it.id == "grossTonnage" } as? FormField.TextField)
            ?.value?.toDoubleOrNull()

        // If decksCount is empty (optional), it's valid
        if (deckCount == null) return@CustomValidation true
        if (tonnage == null) return@CustomValidation true

        when {
            tonnage < 100 -> deckCount <= 2
            tonnage < 500 -> deckCount <= 4
            tonnage < 1000 -> deckCount <= 6
            tonnage < 5000 -> deckCount <= 8
            else -> deckCount <= 12
        }
    }

    /**
     * Get all dimension-related validation rules
     */
    fun getAllDimensionRules(): List<ValidationRule> = listOf(
        lengthGreaterThanWidth(),
        heightValidation(),
        deckCountValidation()
    )
}