package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.data.model.ChecklistAnswer
import com.informatique.mtcit.data.model.ChecklistItem
import com.informatique.mtcit.ui.theme.LocalExtraColors
import java.util.Locale

/**
 * Dynamic Checklist Form — matches iOS checklistItemView exactly
 * id=1 → True/False buttons with circle/checkmark icons
 * id=2 → Written note multiline (TextEditor)
 * id=4 → Multiple choice list with circle/checkmark icons
 *
 * @param showItemOrder Show item order number prefix (e.g. "1.")
 * @param isReadOnly When true, disables all inputs
 */
@Composable
fun DynamicChecklistForm(
    modifier: Modifier = Modifier,
    checklistItems: List<ChecklistItem>,
    existingAnswers: List<ChecklistAnswer>? = null,
    onAnswersChanged: ((Map<Int, String>) -> Unit)? = null,
    showItemOrder: Boolean = false,
    isReadOnly: Boolean = (existingAnswers != null && onAnswersChanged == null)
) {
    val extraColors = LocalExtraColors.current
    val isArabic = Locale.getDefault().language == "ar"

    // State for answers (checklistItemId -> answer value)
    val answers = remember(existingAnswers) {
        mutableStateMapOf<Int, String>().apply {
            existingAnswers?.forEach { put(it.checklistItemId, it.answer) }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        checklistItems.forEach { item ->
            val currentAnswer = answers[item.id] ?: ""
            val isMandatoryUnanswered = item.isMandatory && currentAnswer.isBlank() && !isReadOnly

            // iOS: white1/light-gray card + red border when mandatory & unanswered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = if (isMandatoryUnanswered) Color.Red.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Question row: "N." (blue) + question + "*" (red)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (showItemOrder && item.itemOrder > 0) {
                            Text(
                                "${item.itemOrder}.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = extraColors.blue1
                            )
                        }
                        Text(
                            text = item.question,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = extraColors.whiteInDarkMode,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.isMandatory) {
                            Text("*", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        }
                    }

                    // Input based on checklistType.id
                    val typeId = item.checklistType.id
                    val typeName = item.checklistType.nameEn.uppercase()

                    when {
                        // id=1 or name contains TRUE/FALSE/BOOLEAN
                        typeId == 1 || typeName.contains("TRUE") || typeName.contains("BOOL") -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    "true" to if (isArabic) "نعم" else "True",
                                    "false" to if (isArabic) "لا" else "False"
                                ).forEach { (option, label) ->
                                    val isSelected = currentAnswer == option
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) extraColors.blue1.copy(alpha = 0.1f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) extraColors.blue1 else Color.Gray.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(
                                                enabled = !isReadOnly,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                answers[item.id] = option
                                                onAnswersChanged?.invoke(answers.toMap())
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) extraColors.blue1 else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            label, fontSize = 14.sp,
                                            color = if (isSelected) extraColors.blue1 else extraColors.whiteInDarkMode
                                        )
                                    }
                                }
                            }
                        }

                        // id=4 or name contains MULTIPLE/SELECT/DROPDOWN/LIST/CHOICE
                        typeId == 4 || typeName.contains("MULTIPLE") || typeName.contains("CHOICE") ||
                        typeName.contains("SELECT") || typeName.contains("LIST") || typeName.contains("DROPDOWN") -> {
                            val choices = item.choices.filter { it.isActive }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                choices.forEach { choice ->
                                    val isSelected = currentAnswer == choice.id.toString() ||
                                                     currentAnswer == choice.answer
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelected) extraColors.blue1.copy(alpha = 0.1f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable(
                                                enabled = !isReadOnly,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                answers[item.id] = choice.id.toString()
                                                onAnswersChanged?.invoke(answers.toMap())
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) extraColors.blue1 else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            choice.answer, fontSize = 14.sp,
                                            color = extraColors.whiteInDarkMode,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // id=2 or textarea/multiline — Written note
                        else -> {
                            OutlinedTextField(
                                value = currentAnswer,
                                onValueChange = { newVal ->
                                    if (!isReadOnly) {
                                        answers[item.id] = newVal
                                        onAnswersChanged?.invoke(answers.toMap())
                                    }
                                },
                                enabled = !isReadOnly,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 80.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = extraColors.blue1,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.25f),
                                    disabledBorderColor = Color.Gray.copy(alpha = 0.15f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedTextColor = extraColors.whiteInDarkMode,
                                    unfocusedTextColor = extraColors.whiteInDarkMode,
                                    disabledTextColor = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 8,
                                placeholder = {
                                    Text(
                                        if (isArabic) "اكتب هنا..." else "Write here...",
                                        color = Color.Gray.copy(alpha = 0.5f), fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }
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

