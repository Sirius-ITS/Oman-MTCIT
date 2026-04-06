package com.informatique.mtcit.common

import com.informatique.mtcit.common.util.AppLanguage

/**
 * Centralized error handling for the application.
 *
 * Each subclass stores both [arabicMessage] and [englishMessage] so the UI can pick
 * the correct string reactively using [getMessage] with [LocalAppLocale.current].
 *
 * **Do NOT use [message] in Compose UI** – it reads [Locale.getDefault()] which may be
 * stale after a runtime language change. Use `error.getMessage(LocalAppLocale.current.language == "ar")` instead.
 */
sealed class AppError {

    /**
     * Returns the correctly localised message for display.
     * In @Composable contexts pass `LocalAppLocale.current.language == "ar"` so the text
     * updates immediately when the user switches language without restarting the app.
     */
    abstract fun getMessage(isArabic: Boolean): String

    /**
     * Backward-compat accessor – reads [Locale.getDefault()] which may be stale after a
     * runtime locale change. Use [getMessage] in composables.
     */
    val message: String get() = getMessage(AppLanguage.isArabic)

    data class Network(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    data class Validation(val fieldErrors: Map<String, String>) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) "خطأ في التحقق" else "Validation error"
    }

    data class FileUpload(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    data class CompanyLookup(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    data class Submission(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    data class Initialization(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    /** HTTP error with a status [code]. */
    data class ApiError(val code: Int, val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    /** 401 Unauthorized – token expired or missing. */
    data class Unauthorized(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }

    data class Unknown(val arabicMessage: String, val englishMessage: String) : AppError() {
        override fun getMessage(isArabic: Boolean) = if (isArabic) arabicMessage else englishMessage
    }
}
