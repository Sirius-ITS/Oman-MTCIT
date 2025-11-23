package com.informatique.mtcit.business.validation.rules

/**
 * Validation rules for documents based on marine unit characteristics
 */
object DocumentValidationRules {

    /**
     * Inspection documents are mandatory only when overallLength <= 24 meters
     * For vessels > 24 meters, inspection documents are optional
     */
    fun inspectionDocumentBasedOnLength(accumulatedData: Map<String, String>) = ValidationRule.CrossStepValidation(
        triggerFieldId = "overallLength",
        triggerCondition = { lengthValue ->
            // If overallLength <= 24, inspection document is mandatory
            lengthValue?.toDoubleOrNull()?.let { it <= 24.0 } ?: false
        },
        requiredFieldId = "inspectionDocuments",
        errorFieldId = "inspectionDocuments",
        errorMessage = "مستندات الفحص مطلوبة للوحدات البحرية التي يقل طولها عن أو يساوي 24 متر"
    )

    /**
     * Get all document validation rules
     */
    fun getAllDocumentRules(accumulatedData: Map<String, String>): List<ValidationRule> = listOf(
        inspectionDocumentBasedOnLength(accumulatedData)
    )
}
