package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = if (isArabic) "قائمة الفحص" else "Checklist",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.padding(vertical = 8.dp)
        )

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

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Renders a single checklist item field based on its type
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
    val mandatoryIndicator = if (item.isMandatory) " *" else ""
    val fieldTypeName = item.checklistType.nameEn.uppercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(extraColors.cardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Label
        Text(
            text = "$label$mandatoryIndicator",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Show note if available
        item.note?.let { note ->
            Text(
                text = note,
                fontSize = 12.sp,
                color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Field based on type
        when (fieldTypeName) {
            "TEXT", "STRING", "WRITTEN NOTE" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isReadOnly,
                    placeholder = {
                        Text(
                            if (isArabic) "أدخل الإجابة" else "Enter answer",
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.blue1,
                        unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledTextColor = extraColors.whiteInDarkMode
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            "NUMBER", "INTEGER", "DECIMAL" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isReadOnly,
                    placeholder = {
                        Text(
                            if (isArabic) "أدخل رقم" else "Enter number",
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.blue1,
                        unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledTextColor = extraColors.whiteInDarkMode
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            "TEXTAREA", "LONGTEXT" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
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
                        disabledTextColor = extraColors.whiteInDarkMode
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 5
                )
            }

            "DROPDOWN", "SELECT", "LIST" -> {
                var expanded by remember { mutableStateOf(false) }
                val choices = item.choices.filter { it.isActive }

                // ✅ Find selected choice text from ID for display
                val selectedChoiceText = value.toIntOrNull()?.let { choiceId ->
                    choices.find { it.id == choiceId }?.answer
                } ?: value

                ExposedDropdownMenuBox(
                    expanded = expanded && !isReadOnly,
                    onExpandedChange = { if (!isReadOnly) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedChoiceText,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isReadOnly,
                        placeholder = {
                            Text(
                                if (isArabic) "اختر من القائمة" else "Select from list",
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            if (!isReadOnly) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, !isReadOnly),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = extraColors.blue1,
                            unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                            disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                            disabledTextColor = extraColors.whiteInDarkMode
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        choices.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.answer) },
                                onClick = {
                                    // ✅ Save choice ID, not answer text
                                    onValueChange(choice.id.toString())
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            "CHECKBOX", "BOOLEAN" -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = value.lowercase() in listOf("true", "yes", "نعم", "1"),
                        onCheckedChange = {
                            if (!isReadOnly) {
                                onValueChange(if (it) "true" else "false")
                            }
                        },
                        enabled = !isReadOnly,
                        colors = CheckboxDefaults.colors(
                            checkedColor = extraColors.blue1,
                            uncheckedColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "نعم" else "Yes",
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            "DATE" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isReadOnly,
                    placeholder = {
                        Text(
                            if (isArabic) "YYYY-MM-DD" else "YYYY-MM-DD",
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.blue1,
                        unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledTextColor = extraColors.whiteInDarkMode
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            else -> {
                // Default to text field for unknown types
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isReadOnly,
                    placeholder = {
                        Text(
                            if (isArabic) "أدخل الإجابة" else "Enter answer",
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.blue1,
                        unfocusedBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledBorderColor = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        disabledTextColor = extraColors.whiteInDarkMode
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        }
    }
}

