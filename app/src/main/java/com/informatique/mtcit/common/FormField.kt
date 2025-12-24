package com.informatique.mtcit.common


sealed class FormField(
    open val id: String,
    open val label: String = "",
    open val labelRes: Int = 0, // String resource ID for localization
    open val value: String = "",
    open val error: String? = null,
    open val mandatory: Boolean = false // ✅ الفلاج الجديد (true = إجباري, false = اختياري)


) {

    abstract fun copyWithValue(newValue: String): FormField
    abstract fun copyWithError(newError: String?): FormField


    data class TextField(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val isPassword: Boolean = false,
        val isNumeric: Boolean = false,
        val isDecimal: Boolean = false, // ✅ NEW: For decimal numbers like 18.5, 3.14
        val placeholder: String? = null,
        override val error: String? = null,
        val initialValue: String = "",
        val enabled: Boolean = true, // ✅ إضافة الخاصية دي
        override val mandatory: Boolean = false,
        val maxLength: Int? = null, // ✅ NEW: Maximum character length for validation
        val minLength: Int? = null // ✅ NEW: Minimum character length for validation

    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

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
        override val mandatory: Boolean = false,
        val maxLength: Int? = null, // ✅ NEW: Maximum character length for selected option
        val minLength: Int? = null, // ✅ NEW: Minimum character length for selected option
        val enableSections: Boolean = false, // ✅ NEW: allow grouped sections
        val sections: List<com.informatique.mtcit.ui.components.DropdownSection> = emptyList()

    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, selectedOption = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class CheckBox(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val checked: Boolean = false,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value = if (checked) "true" else "false", error = error, mandatory = mandatory){

        override fun copyWithValue(newValue: String) =
            copy(checked = newValue == "true")

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class DatePicker(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val allowPastDates: Boolean = true, // ✅ الفلاج الجديد
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

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
    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

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
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class MarineUnitSelector(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "[]", // JSON array of selected unit IDs
        val units: List<com.informatique.mtcit.business.transactions.shared.MarineUnit> = emptyList(), // List of available units
        val allowMultipleSelection: Boolean = true, // Allow selecting multiple units
        val showOwnedUnitsWarning: Boolean = true, // Show warning for owned units
        override val error: String? = null,
        val showAddNewButton: Boolean = true, // ✅ أضف الـ parameter ده
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }


    data class SelectableList<T>(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val options: List<T> = emptyList(),
        val selectedOption: T? = null,
        override val value: String = "",
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class EngineList(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "[]",
        val engineTypes: List<String> = emptyList(),
        val manufacturers: List<String> = emptyList(),
        val countries: List<String> = emptyList(),
        val fuelTypes: List<String> = emptyList(),
        val engineConditions: List<String> = emptyList(),
        val placeholder: String? = null,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class SailorList(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "[]",
        val jobs: List<String> = emptyList(),
        val placeholder: String? = null,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory) {
        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class RadioGroup(
        override val id: String,
        override val labelRes: Int,
        override val label: String = "",
        val options: List<RadioOption>,
        override val value: String = "[]",
        val selectedValue: String? = null,
        val descriptionRes: Int? = null,
        val orientation: RadioOrientation = RadioOrientation.VERTICAL,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory){

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, selectedValue = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class InfoCard(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val items: List<Int> = emptyList(), // List of string resource IDs
        val iconRes: Int? = null, // Optional icon for each item
        val showCheckmarks: Boolean = true, // Show ✓ before each item
        override val value: String = "",
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }
    data class PhoneNumberField(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val countryCodes: List<String> = listOf("+968", "+966", "+971", "+974", "+965", "+973"), // Available country codes
        val selectedCountryCode: String = "+968", // Default selected
        val placeholder: Int? = null,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)

        // Helper to update country code
        fun copyWithCountryCode(newCode: String) =
            copy(selectedCountryCode = newCode)
    }
    data class OTPField(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        override val value: String = "",
        val phoneNumber: String = "", // Display the phone number user entered
        val otpLength: Int = 6, // Number of OTP digits
        val resendEnabled: Boolean = false, // Can resend OTP?
        val remainingTime: Int = 33, // Countdown timer in seconds
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    data class MultiSelectDropDown(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val options: List<String> = emptyList(),
        val selectedOptions: List<String> = emptyList(),
        val placeholder: String? = null,
        override val value: String = "[]", // JSON array of selected options
        override val error: String? = null,
        override val mandatory: Boolean = false,
        val maxSelection: Int? = null, // Maximum number of selections allowed
        val showSelectionCount: Boolean = true, // Show count of selected items
        val maxLength: Int? = null, // Maximum character length for validation
        val minLength: Int? = null // Minimum character length for validation
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)

        // Helper to update selected options
        fun copyWithSelectedOptions(newOptions: List<String>) =
            copy(selectedOptions = newOptions, value = newOptions.toString(), error = null)
    }

    data class RadioOption(
        val value: String,
        val labelRes: Int,
        val descriptionRes: Int? = null,
        val isEnabled: Boolean = true,
        val error: String? = null
    )
    enum class RadioOrientation {
        VERTICAL,   // خيارات عمودية (الافتراضي)
        HORIZONTAL  // خيارات أفقية
    }

    /**
     * Payment Details Field - Displays payment breakdown from API response
     * Shows Arabic value, line items, and total amount
     */
    data class PaymentDetails(
        override val id: String,
        override val label: String = "",
        override val labelRes: Int = 0,
        val arabicValue: String = "", // e.g., "مائةوعشرون ريالاًعمانى فقط لا غير"
        val lineItems: List<PaymentLineItem> = emptyList(), // List of payment items
        val totalCost: Double = 0.0,
        val totalTax: Double = 0.0,
        val finalTotal: Double = 0.0,
        override val value: String = "",
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, labelRes, value, error, mandatory) {

        override fun copyWithValue(newValue: String) =
            copy(value = newValue, error = null)

        override fun copyWithError(newError: String?) =
            copy(error = newError)
    }

    /**
     * Represents a single line item in the payment breakdown
     */
    data class PaymentLineItem(
        val name: String,
        val amount: Double
    )

}
