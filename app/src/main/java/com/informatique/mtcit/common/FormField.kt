package com.informatique.mtcit.common


sealed class FormField(
    open val id: String,
    open val label: String,
    open val value: String = "",
    open val error: String? = null,
    open val mandatory: Boolean = false


) {
    data class TextField(
        override val id: String,
        override val label: String,
        override val value: String = "",
        val isPassword: Boolean = false,
        val isNumeric: Boolean = false,
        override val error: String? = null,
        override val mandatory: Boolean = false

    ) : FormField(id, label, value, error)

    data class DropDown(
        override val id: String,
        override val label: String,
        val options: List<String>,
        val selectedOption: String? = null,
        override val value: String = "",
        override val error: String? = null ,
        override val mandatory: Boolean = false
    ) : FormField(id, label, value, error)

    data class CheckBox(
        override val id: String,
        override val label: String,
        val checked: Boolean = false,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, value = if (checked) "true" else "false", error = error)

    data class DatePicker(
        override val id: String,
        override val label: String,
        override val value: String = "",
        val allowPastDates: Boolean = true,
        override val error: String? = null,
        override val mandatory: Boolean = false
    ) : FormField(id, label, value, error)
}

