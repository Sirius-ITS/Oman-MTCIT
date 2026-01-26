package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.data.model.ChecklistAnswer
import com.informatique.mtcit.data.model.ChecklistItem
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.util.Locale

/**
 * Dynamic Checklist Form Builder
 * Renders checklist items dynamically based on field types
 *
 * @param checklistItems List of checklist items to render
 * @param existingAnswers Existing answers (for completed inspections) - makes fields read-only
 * @param onAnswersChanged Callback when answers are modified (null for read-only mode)
 */
@Composable
fun DynamicChecklistForm(
    modifier: Modifier = Modifier,
    checklistItems: List<ChecklistItem>,
    existingAnswers: List<ChecklistAnswer>? = null,
    onAnswersChanged: ((Map<Int, String>) -> Unit)? = null
) {
    val extraColors = LocalExtraColors.current
    val isArabic = Locale.getDefault().language == "ar"
    val isReadOnly = existingAnswers != null && onAnswersChanged == null

    // State for answers (checklistItemId -> answer value)
    val answers = remember {
        mutableStateMapOf<Int, String>().apply {
            // Pre-fill existing answers if provided
            existingAnswers?.forEach { answer ->
                put(answer.checklistItemId, answer.answer)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Render each checklist item
        checklistItems.forEach { item ->
            ChecklistItemField(
                item = item,
                value = answers[item.id] ?: "",
                isReadOnly = isReadOnly,
                onValueChange = { newValue ->
                    answers[item.id] = newValue
                    onAnswersChanged?.invoke(answers.toMap())
                },
                extraColors = extraColors,
                isArabic = isArabic
            )
        }
    }
}

/**
 * Renders a single checklist item field based on its type
 * ✅ Now uses FormField components matching transaction UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistItemField(
    item: ChecklistItem,
    value: String,
    isReadOnly: Boolean,
    onValueChange: (String) -> Unit,
    extraColors: com.informatique.mtcit.ui.theme.ExtraColors,
    isArabic: Boolean
) {
    val label = item.question
    val fieldTypeName = item.checklistType.nameEn.uppercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Show note if available (above the field)
        item.note?.let { note ->
            Text(
                text = note,
                fontSize = 12.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // ✅ Field based on type - using FormField components
        when (fieldTypeName) {
            "TEXT", "STRING", "WRITTEN NOTE" -> {
                CustomTextField(
                    value = value,
                    onValueChange = if (isReadOnly) { _ -> } else onValueChange,
                    label = label,
                    placeholder = if (isArabic) "أدخل الإجابة" else "Enter answer",
                    mandatory = item.isMandatory,
                    enabled = !isReadOnly
                )
            }

            "NUMBER", "INTEGER" -> {
                CustomTextField(
                    value = value,
                    onValueChange = if (isReadOnly) { _ -> } else onValueChange,
                    label = label,
                    isNumeric = true,
                    placeholder = if (isArabic) "أدخل رقم" else "Enter number",
                    mandatory = item.isMandatory,
                    enabled = !isReadOnly
                )
            }

            "DECIMAL" -> {
                CustomTextField(
                    value = value,
                    onValueChange = if (isReadOnly) { _ -> } else onValueChange,
                    label = label,
                    isDecimal = true,
                    placeholder = if (isArabic) "أدخل رقم عشري" else "Enter decimal number",
                    mandatory = item.isMandatory,
                    enabled = !isReadOnly
                )
            }

            "TEXTAREA", "LONGTEXT" -> {
                // For textarea, use OutlinedTextField with height modifier
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (item.isMandatory) "$label *" else label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = extraColors.whiteInDarkMode,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = if (isReadOnly) { _ -> } else onValueChange,
                        enabled = !isReadOnly,
                        placeholder = {
                            Text(
                                if (isArabic) "أدخل النص" else "Enter text",
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = extraColors.blue1,
                            unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                            disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                            disabledTextColor = extraColors.whiteInDarkMode,
                            focusedContainerColor = extraColors.cardBackground,
                            unfocusedContainerColor = extraColors.cardBackground,
                            disabledContainerColor = extraColors.cardBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        maxLines = 5
                    )
                }
            }

            "DROPDOWN", "SELECT", "LIST" -> {
                val choices = item.choices.filter { it.isActive }

                // ✅ Find selected choice text from ID for display
                val selectedChoiceText = value.toIntOrNull()?.let { choiceId ->
                    choices.find { it.id == choiceId }?.answer
                } ?: value

                CustomDropdown(
                    selectedOption = selectedChoiceText,
                    onOptionSelected = { selectedText: String ->
                        // Find the choice ID from the selected text
                        val choiceId = choices.find { it.answer == selectedText }?.id
                        if (choiceId != null) {
                            onValueChange(choiceId.toString())
                        }
                    },
                    options = choices.map { it.answer },
                    label = label,
                    placeholder = if (isArabic) "اختر من القائمة" else "Select from list",
                    mandatory = item.isMandatory,
                    isLoading = false,
                    enabled = !isReadOnly
                )
            }

            "CHECKBOX", "BOOLEAN" -> {
                CustomCheckBox(
                    checked = value.lowercase() in listOf("true", "yes", "نعم", "1"),
                    onCheckedChange = { checked ->
                        if (!isReadOnly) {
                            onValueChange(if (checked) "true" else "false")
                        }
                    },
                    label = label,
                    error = null
                )
            }

            "DATE" -> {
                CustomDatePicker(
                    value = value,
                    onValueChange = if (isReadOnly) { _ -> } else onValueChange,
                    label = label,
                    mandatory = item.isMandatory,
                    allowPastDates = true
                )
            }

            else -> {
                // Default to text field for unknown types
                CustomTextField(
                    value = value,
                    onValueChange = if (isReadOnly) { _ -> } else onValueChange,
                    label = label,
                    placeholder = if (isArabic) "أدخل الإجابة" else "Enter answer",
                    mandatory = item.isMandatory,
                    enabled = !isReadOnly
                )
            }
        }
    }
}

