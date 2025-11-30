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
     * ✅ NEW: Validate dimension fields don't exceed 99.99 meters
     * Format: Maximum 2 digits before decimal point, 2 digits after (XX.XX)
     */
    fun dimensionMaxValueValidation() = ValidationRule.CustomValidation(
        fieldIds = listOf("overallLength", "overallWidth", "depth", "height"),
        errorFieldId = "overallLength", // Default, will be overridden in loop
        errorMessage = "القيمة يجب ألا تتجاوز 99.99 متر" // Value must not exceed 99.99 meters
    ) { fields ->
        for (fieldId in listOf("overallLength", "overallWidth", "depth", "height")) {
            val field = fields.find { it.id == fieldId } as? FormField.TextField
            val valueStr = field?.value

            // Skip empty optional fields
            if (valueStr.isNullOrBlank()) continue

            println("🔍 Validating dimension field: $fieldId with value: $valueStr")

            // Parse value
            val value = valueStr.toDoubleOrNull()

            // Check if value is invalid
            if (value == null) {
                println("❌ Invalid value for $fieldId: $valueStr")
                return@CustomValidation false
            }

            // Check if value exceeds 99.99
            if (value > 99.99) {
                println("❌ Value $value exceeds 99.99 for field $fieldId")
                return@CustomValidation false
            }

            // ✅ Check integer part doesn't exceed 2 digits (prevents 100, 9999, etc.)
            val integerPart = value.toInt()
            if (integerPart > 99) {
                println("❌ Integer part $integerPart exceeds 99 for field $fieldId (value: $value)")
                return@CustomValidation false
            }

            println("✅ Field $fieldId passed validation with value: $value")
        }

        println("✅ All dimension fields passed validation")
        true
    }

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
        dimensionMaxValueValidation(), // ✅ NEW: Check max value 99.99
        lengthGreaterThanWidth(),
        heightValidation(),
        deckCountValidation()
    )
}