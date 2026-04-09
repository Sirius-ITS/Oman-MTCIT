package com.informatique.mtcit.business.validation.rules


import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.common.util.AppLanguage

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
        errorMessage = if (AppLanguage.isArabic) "العرض لا يمكن أن يتجاوز الطول" else "Width cannot exceed length"
    )

    /**
     * ✅ Per-field dimension max value validation (≤ 99.99).
     * Use one rule per field so the error appears on the correct field.
     * Non-numeric values are skipped here — numericDecimal() handles that separately.
     */
    fun dimensionMaxValueRule(fieldId: String) = ValidationRule.CustomValidation(
        fieldIds = listOf(fieldId),
        errorFieldId = fieldId,
        errorMessage = if (AppLanguage.isArabic) "القيمة يجب ألا تتجاوز 99.99 متر" else "Value must not exceed 99.99 meters"
    ) { fields ->
        val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value
            ?: return@CustomValidation true
        if (value.isBlank()) return@CustomValidation true
        // If not a valid number, let numericDecimal() handle the format error
        val num = value.toDoubleOrNull() ?: return@CustomValidation true
        println("🔍 dimensionMaxValueRule: $fieldId = $num")
        num <= 99.99
    }

    /**
     * @deprecated Use dimensionMaxValueRule(fieldId) per-field instead.
     * Kept for backward compatibility — PermanentRegistration / RequestInspection still reference it.
     * BUG: always attaches error to "overallLength" regardless of which field fails.
     */
    fun dimensionMaxValueValidation() = ValidationRule.CustomValidation(
        fieldIds = listOf("overallLength", "overallWidth", "depth", "height"),
        errorFieldId = "overallLength",
        errorMessage = if (AppLanguage.isArabic) "القيمة يجب ألا تتجاوز 99.99 متر" else "Value must not exceed 99.99 meters"
    ) { fields ->
        for (fieldId in listOf("overallLength", "overallWidth", "depth", "height")) {
            val field = fields.find { it.id == fieldId } as? FormField.TextField
            val valueStr = field?.value
            if (valueStr.isNullOrBlank()) continue
            val value = valueStr.toDoubleOrNull() ?: continue // skip non-numeric (handled by numericDecimal)
            if (value > 99.99) {
                println("❌ Value $value exceeds 99.99 for field $fieldId")
                return@CustomValidation false
            }
        }
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
        dimensionMaxValueValidation(), // ✅ NEW: Check max mortgageValue 99.99
        lengthGreaterThanWidth(),
        heightValidation(),
        deckCountValidation()
    )
}