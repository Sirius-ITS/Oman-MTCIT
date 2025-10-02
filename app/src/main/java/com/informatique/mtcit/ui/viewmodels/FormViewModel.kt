package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.validation.FormValidator
import com.informatique.mtcit.common.FormField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormViewModel @Inject constructor(
    private val validator: FormValidator
) : ViewModel() {

    private val _fields = MutableStateFlow<List<FormField>>(emptyList())
    val fields: StateFlow<List<FormField>> = _fields.asStateFlow()

    fun initForm() {
        _fields.value = listOf(
            FormField.TextField(
                id = "username",
                label = "Username",
                mandatory = true // ✅ إجباري
            ),
            FormField.TextField(
                id = "password",
                label = "Password",
                isPassword = true,
                mandatory = false // ✅ إجباري
            ),
            FormField.DropDown(
                id = "gender",
                label = "Gender",
                options = listOf("Male", "Female"),
                mandatory = false // ✅ اختياري
            ),
            FormField.CheckBox(
                id = "terms",
                label = "Terms & Conditions",
                mandatory = true // ✅ إجباري
            ),
            FormField.DatePicker(
                id = "dob",
                label = "Date of Birth",
                allowPastDates = true,
                mandatory = true // ✅ إجباري
            ),
            FormField.DatePicker(
                id = "appointment",
                label = "Appointment Date",
                allowPastDates = false,
                mandatory = false // ✅ اختياري
            )
        )
    }

    fun onValueChange(id: String, value: String, checked: Boolean? = null) {
        _fields.value = _fields.value.map { field ->
            when {
                field.id == id && field is FormField.TextField -> field.copy(value = value, error = null)
                field.id == id && field is FormField.DropDown -> field.copy(value = value, error = null)
                field.id == id && field is FormField.CheckBox -> field.copy(
                    checked = checked ?: field.checked,
                    error = null
                )
                field.id == id && field is FormField.DatePicker -> field.copy(value = value, error = null)
                else -> field
            }
        }
    }

    fun validateForm() {
        viewModelScope.launch {
            _fields.value = validator.validateAll(_fields.value)
        }
    }

    fun isFormValid(): Boolean {
        return validator.isFormValid(_fields.value)
    }
}
