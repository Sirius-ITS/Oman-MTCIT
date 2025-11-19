package com.informatique.mtcit.business.validation.rules


import com.informatique.mtcit.common.FormField
import java.text.SimpleDateFormat
import java.util.*

/**
 * Validation rules for date fields
 */
object DateValidationRules {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Manufacturer year must be between 1900 and current year
     */
    fun manufacturerYearValidation() = ValidationRule.CustomValidation(
        fieldIds = listOf("manufacturerYear"),
        errorFieldId = "manufacturerYear",
        errorMessage = "Manufacturer year must be between 1900 and current year"
    ) { fields ->
        val year = (fields.find { it.id == "manufacturerYear" } as? FormField.TextField)
            ?.value?.toIntOrNull()

        if (year == null) return@CustomValidation true

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        year in 1900..currentYear
    }

    /**
     * Construction end date must be after construction start date
     */
    fun constructionDateRange() = ValidationRule.CustomValidation(
        fieldIds = listOf("constructionEndDate", "constructionStartDate"),
        errorFieldId = "constructionEndDate",
        errorMessage = "Construction end date must be after start date"
    ) { fields ->
        val endDate = (fields.find { it.id == "constructionEndDate" } as? FormField.DatePicker)?.value
        val startDate = (fields.find { it.id == "constructionStartDate" } as? FormField.DatePicker)?.value

        if (endDate.isNullOrBlank() || startDate.isNullOrBlank()) return@CustomValidation true

        try {
            val end = dateFormat.parse(endDate)
            val start = dateFormat.parse(startDate)
            end?.after(start) ?: true
        } catch (e: Exception) {
            true // If parsing fails, let basic validation handle it
        }
    }

    /**
     * First registration must be after construction end date
     */
    fun registrationAfterConstruction() = ValidationRule.CustomValidation(
        fieldIds = listOf("firstRegistrationDate", "constructionEndDate"),
        errorFieldId = "firstRegistrationDate",
        errorMessage = "Registration date must be after construction completion"
    ) { fields ->
        val registrationDate = (fields.find { it.id == "firstRegistrationDate" } as? FormField.DatePicker)?.value
        val constructionDate = (fields.find { it.id == "constructionEndDate" } as? FormField.DatePicker)?.value

        // firstRegistrationDate is optional
        if (registrationDate.isNullOrBlank()) return@CustomValidation true
        if (constructionDate.isNullOrBlank()) return@CustomValidation true

        try {
            val registration = dateFormat.parse(registrationDate)
            val construction = dateFormat.parse(constructionDate)
            registration?.after(construction) ?: true
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Get all date-related validation rules
     */
    fun getAllDateRules(): List<ValidationRule> = listOf(
        manufacturerYearValidation(),
        constructionDateRange(),
        registrationAfterConstruction()
    )
}