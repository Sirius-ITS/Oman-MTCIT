package com.informatique.mtcit.business.validation.rules

import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.common.util.AppLanguage

/**
 * Format validation rules based on regex patterns.
 * Each rule skips blank fields (required-checking is handled separately by mandatory=true).
 */
object FormatValidationRules {

    /** Digits only: 0-9 */
    fun numericOnly(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أرقام فقط" else "Must contain digits only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^\\d+$"))
        }

    /** Numeric decimal: digits with optional single decimal point (e.g. 12.5) */
    fun numericDecimal(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أرقام صحيحة أو عشرية فقط" else "Must be a valid number (e.g. 12 or 12.5)"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^\\d+(\\.\\d+)?$"))
        }

    /** Exactly `count` digits */
    fun exactDigits(fieldId: String, count: Int): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يتكون من $count أرقام بالضبط" else "Must be exactly $count digits"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^\\d{$count}$"))
        }

    /** Call sign: 4-7 characters — Arabic letters, English letters, or digits */
    fun callSignFormat(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "علامة النداء يجب أن تكون 4-7 أحرف (عربي / إنجليزي / أرقام)"
            else "Call sign must be 4-7 characters (Arabic/English/digits)"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[\u0600-\u06FFa-zA-Z0-9]{4,7}$"))
        }

    /** Arabic letters and spaces only */
    fun arabicLettersOnly(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف عربية فقط" else "Must contain Arabic letters only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[\u0600-\u06FF\\s]+$"))
        }

    /** English letters and spaces only */
    fun englishLettersOnly(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف إنجليزية فقط" else "Must contain English letters only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[a-zA-Z\\s]+$"))
        }

    /** Arabic letters, English letters, digits, spaces */
    fun arabicEnglishNumbers(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف عربية أو إنجليزية أو أرقام فقط"
            else "Must contain Arabic/English letters or digits only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[\u0600-\u06FFa-zA-Z0-9\\s]+$"))
        }

    /** English letters and digits only (no spaces, no Arabic) */
    fun englishAlphanumeric(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف إنجليزية وأرقام فقط"
            else "Must contain English letters and digits only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[a-zA-Z0-9]+$"))
        }

    /** Valid email address */
    fun emailFormat(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "البريد الإلكتروني غير صالح" else "Invalid email address"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()
        }

    /** Arabic letters, digits, dash, spaces (ship name Arabic) */
    fun arabicNumbersDash(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف عربية وأرقام وشرطة فقط"
            else "Must contain Arabic letters, digits, and dashes only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[\u0600-\u06FF0-9\\-\\s]+$"))
        }

    /** English letters, digits, dash (ship name English) */
    fun englishNumbersDash(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف إنجليزية وأرقام وشرطة فقط"
            else "Must contain English letters, digits, and dashes only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[a-zA-Z0-9\\-]+$"))
        }

    /** English letters, digits, spaces (no dash — captain name English) */
    fun englishNumbersNoDash(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف إنجليزية وأرقام فقط"
            else "Must contain English letters and digits only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[a-zA-Z0-9\\s]+$"))
        }

    /** Arabic letters, digits, dash, spaces (captain name Arabic) */
    fun arabicNumbersOptionalDash(fieldId: String): ValidationRule.CustomValidation =
        ValidationRule.CustomValidation(
            fieldIds = listOf(fieldId),
            errorFieldId = fieldId,
            errorMessage = if (AppLanguage.isArabic) "يجب أن يحتوي على أحرف عربية وأرقام فقط"
            else "Must contain Arabic letters and digits only"
        ) { fields ->
            val value = (fields.find { it.id == fieldId } as? FormField.TextField)?.value ?: return@CustomValidation true
            if (value.isBlank()) return@CustomValidation true
            value.matches(Regex("^[\u0600-\u06FF0-9\\-\\s]+$"))
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Centralized focus-loss validator
    // Used by BaseTransactionViewModel to run format checks on step fields when
    // the user leaves a field (onFocusLost), without touching any strategy file.
    // Returns an error message string, or null if the value is valid / empty.
    // ─────────────────────────────────────────────────────────────────────────
    fun validateFieldFormat(fieldId: String, value: String): String? {
        if (value.isBlank()) return null
        val isAr = AppLanguage.isArabic
        return when (fieldId) {
            "callSign" ->
                if (!value.matches(Regex("^[\u0600-\u06FFa-zA-Z0-9]{4,7}$")))
                    if (isAr) "علامة النداء يجب أن تكون 4-7 أحرف (عربي / إنجليزي / أرقام)"
                    else "Call sign must be 4-7 characters (Arabic/English/digits)"
                else null

            "mmsi" ->
                if (!value.matches(Regex("^\\d{9}$")))
                    if (isAr) "يجب أن يتكون من 9 أرقام بالضبط" else "Must be exactly 9 digits"
                else null

            "imoNumber" ->
                if (!value.matches(Regex("^\\d{7}$")))
                    if (isAr) "يجب أن يتكون من 7 أرقام بالضبط" else "Must be exactly 7 digits"
                else null

            "constructionpool", "buildingDock" ->
                if (!value.matches(Regex("^[\u0600-\u06FFa-zA-Z0-9\\s]+$")))
                    if (isAr) "يجب أن يحتوي على أحرف عربية أو إنجليزية أو أرقام فقط"
                    else "Must contain Arabic/English letters or digits only"
                else null

            "marineUnitName" ->
                if (!value.matches(Regex("^[\u0600-\u06FF\\s]+$")))
                    if (isAr) "يجب أن يحتوي على أحرف عربية فقط" else "Must contain Arabic letters only"
                else null

            "marineUnitNameEn" ->
                if (!value.matches(Regex("^[a-zA-Z\\s]+$")))
                    if (isAr) "يجب أن يحتوي على أحرف إنجليزية فقط" else "Must contain English letters only"
                else null

            "new_ship_name_ar" ->
                if (!value.matches(Regex("^[\u0600-\u06FF0-9\\-\\s]+$")))
                    if (isAr) "يجب أن يحتوي على أحرف عربية وأرقام وشرطة فقط"
                    else "Must contain Arabic letters, digits, and dashes only"
                else null

            "new_ship_name_en" ->
                if (!value.matches(Regex("^[a-zA-Z0-9\\-]+$")))
                    if (isAr) "يجب أن يحتوي على أحرف إنجليزية وأرقام وشرطة فقط"
                    else "Must contain English letters, digits, and dashes only"
                else null

            "mortgageContractNumber" ->
                if (!value.matches(Regex("^[a-zA-Z0-9]+$")))
                    if (isAr) "يجب أن يحتوي على أحرف إنجليزية وأرقام فقط"
                    else "Must contain English letters and digits only"
                else null

            "passengersNo", "agricultureRequestNumber", "decksCount" ->
                if (!value.matches(Regex("^\\d+$")))
                    if (isAr) "يجب أن يحتوي على أرقام فقط" else "Must contain digits only"
                else null

            // ── Dimension fields (overallLength, overallWidth, depth, height) ──────
            "overallLength", "overallWidth", "depth", "height" ->
                when {
                    !value.matches(Regex("^\\d+(\\.\\d+)?$")) ->
                        if (isAr) "يجب أن يحتوي على أرقام صحيحة أو عشرية فقط"
                        else "Must be a valid number (e.g. 12 or 12.5)"
                    (value.toDoubleOrNull() ?: 0.0) > 99.99 ->
                        if (isAr) "القيمة يجب ألا تتجاوز 99.99 متر"
                        else "Value must not exceed 99.99 meters"
                    else -> null
                }

            // ── Weight / tonnage fields ───────────────────────────────────────────
            "grossTonnage", "netTonnage", "staticLoad", "maxPermittedLoad" ->
                if (!value.matches(Regex("^\\d+(\\.\\d+)?$")))
                    if (isAr) "يجب أن يحتوي على أرقام صحيحة أو عشرية فقط"
                    else "Must be a valid number (e.g. 12 or 12.5)"
                else null

            else -> null
        }
    }
}

