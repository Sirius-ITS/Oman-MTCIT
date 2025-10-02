package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.viewmodels.FormViewModel

@Composable
fun DynamicForm(
    viewModel: FormViewModel,
    onSubmit: (List<FormField>) -> Unit
) {
    // نجمع الـ state بتاع الفورم من الـ ViewModel
    val fields by viewModel.fields.collectAsState()

    Column {
        fields.forEach { field ->
            when (field) {
                is FormField.TextField -> {
                    CustomTextField(
                        value = field.value,
                        onValueChange = { viewModel.onValueChange(field.id, it) },
                        label = field.label,
                        isPassword = field.isPassword,
                        isNumeric = field.isNumeric,
                        error = field.error
                    )
                }

                is FormField.DropDown -> {
                    CustomDropdown(
                        label = field.label,
                        options = field.options,
                        selectedOption = field.selectedOption,
                        onOptionSelected = { viewModel.onValueChange(field.id, it) }
                    )
                }

                is FormField.CheckBox -> {
                    CustomCheckBox(
                        checked = field.checked,
                        onCheckedChange = { viewModel.onValueChange(field.id, "", it) },
                        label = field.label,
                        error = field.error
                    )
                }

                is FormField.DatePicker -> {
                    CustomDatePicker(
                        value = field.value,
                        onValueChange = { viewModel.onValueChange(field.id, it) },
                        label = field.label,
                        error = field.error,
                        allowPastDates = field.allowPastDates
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.validateForm()
                if (viewModel.isFormValid()) {
                    onSubmit(fields) // يرجع الفورم كله بعد الفاليديشن
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Submit")
        }
    }
}
