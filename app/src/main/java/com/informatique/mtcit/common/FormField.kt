package com.informatique.mtcit.common


sealed class FormField(
    open val id: String,
    open val label: String = "",
    open val labelRes: Int = 0, // String resource ID for localization
    open val value: String = "",
    open val error: String? = null,
    open val mandatory: Boolean = false // ✅ الفلاج الجديد (true = إجباري, false = اختياري)


) {
    data class TextField(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val isPassword: Boolean = false,
        val isNumeric: Boolean = false,
        override val error: String? = null,
        override val mandatory: Boolean = false

    ) : FormField(id, label, labelRes, value, error, mandatory)

    data class DropDown(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val options: List<String> = emptyList(),
        val optionRes: List<Int> = emptyList(), // String resource IDs for options
        val selectedOption: String? = null,
        override val value: String = "",
        override val error: String? = null ,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)

    data class CheckBox(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val checked: Boolean = false,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value = if (checked) "true" else "false", error = error, mandatory = mandatory)

    data class DatePicker(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val allowPastDates: Boolean = true, // ✅ الفلاج الجديد
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)

    data class FileUpload(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val allowedTypes: List<String> = listOf("pdf", "jpg", "png"),
        val maxSizeMB: Int = 5,
        val selectedFiles: List<String> = emptyList(),
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)
}