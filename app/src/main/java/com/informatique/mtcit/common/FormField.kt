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
        val placeholder: String? = null,
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
        val placeholder: String? = null,
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
        val allowedTypes: List<String> = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"),
        val maxSizeMB: Int = 5,
        val selectedFiles: List<String> = emptyList(),
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)

    /**
     * Owner List Field - For managing multiple owners
     * Value is stored as JSON string containing list of owners
     *
     * @param nationalities List of nationality options for dropdown
     * @param countries List of country options for dropdown
     * @param includeCompanyFields Whether to show company-specific fields
     * @param totalCountFieldId ID of the field storing total owners count (optional)
     */
    data class OwnerList(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "[]", // JSON array of owners
        val nationalities: List<String> = emptyList(),
        val countries: List<String> = emptyList(),
        val includeCompanyFields: Boolean = true,
        val totalCountFieldId: String? = null, // Optional link to total count field
        val placeholder: String? = null,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)

    data class MarineUnitSelector(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "[]", // JSON array of selected unit IDs
        val units: List<com.informatique.mtcit.business.transactions.shared.MarineUnit> = emptyList(), // List of available units
        val allowMultipleSelection: Boolean = true, // Allow selecting multiple units
        val showOwnedUnitsWarning: Boolean = true, // Show warning for owned units
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)

    data class SelectableList<T>(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val options: List<T> = emptyList(),
        val selectedOption: T? = null,
        override val value: String = "",
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory)
}