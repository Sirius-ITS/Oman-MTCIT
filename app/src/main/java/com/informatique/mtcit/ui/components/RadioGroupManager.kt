 package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.common.FormField
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * Radio Group Manager Component
 * Main entry point for rendering radio button groups
 *
 * Features:
 * - Single selection from multiple options
 * - Vertical/Horizontal orientation support
 * - Optional descriptions per option
 * - Animated selection states
 * - Theme-aware colors
 */
@Composable
fun RadioGroupManager(
    modifier: Modifier = Modifier,
    field: FormField.RadioGroup,
    selectedValue: String? = null, // Make it optional with default
    onValueChange: (String) -> Unit
) {
    val extraColors = LocalExtraColors.current

    // ✅ Use field.selectedValue if selectedValue parameter is null
    val currentSelection = selectedValue ?: field.selectedValue

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label with mandatory indicator
        RadioGroupLabel(
            labelRes = field.labelRes,
            isMandatory = field.mandatory
        )

        // Optional description
        field.descriptionRes?.let { descRes ->
            RadioGroupDescription(descriptionRes = descRes)
        }

        // Radio Options based on orientation
        when (field.orientation) {
            FormField.RadioOrientation.VERTICAL -> {
                VerticalRadioOptions(
                    options = field.options,
                    selectedValue = currentSelection,
                    onSelect = onValueChange
                )
            }
            FormField.RadioOrientation.HORIZONTAL -> {
                HorizontalRadioOptions(
                    options = field.options,
                    selectedValue = currentSelection,
                    onSelect = onValueChange
                )
            }
        }
    }
}

/**
 * Radio Group Label Component
 * Displays the main label with optional mandatory indicator
 */
@Composable
private fun RadioGroupLabel(
    labelRes: Int,
    isMandatory: Boolean
) {
    val extraColors = LocalExtraColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = localizedApp(labelRes),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = extraColors.whiteInDarkMode
        )

        if (isMandatory) {
            Text(
                text = "*",
                color = Color(0xFFEF4444),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Radio Group Description Component
 * Displays optional helper text
 */
@Composable
private fun RadioGroupDescription(descriptionRes: Int) {
    val extraColors = LocalExtraColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = extraColors.cardBackground2.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = extraColors.textBlueSubTitle,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = localizedApp(descriptionRes),
            fontSize = 14.sp,
            color = extraColors.textSubTitle,
            lineHeight = 20.sp
        )
    }
}

/**
 * Vertical Radio Options Layout
 * Stacks options vertically
 */
@Composable
private fun VerticalRadioOptions(
    options: List<FormField.RadioOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            RadioOptionCard(
                option = option,
                isSelected = selectedValue == option.value,
                onSelect = { onSelect(option.value) }
            )
        }
    }
}

/**
 * Horizontal Radio Options Layout
 * Arranges options side by side
 */
@Composable
private fun HorizontalRadioOptions(
    options: List<FormField.RadioOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            RadioOptionCard(
                option = option,
                isSelected = selectedValue == option.value,
                onSelect = { onSelect(option.value) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Radio Option Card Component
 * Individual selectable option with animations
 */
@Composable
private fun RadioOptionCard(
    option: FormField.RadioOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    val backgroundColor = if (isSelected) {
        Color(0xFF1E3A5F).copy(alpha = 0.1f)
    } else {
        extraColors.cardBackground
    }

    val borderColor = if (isSelected) {
        Color(0xFF1E3A5F)
    } else {
        extraColors.cardBackground2.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = option.isEnabled) {
                    onSelect()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Custom Radio Button
            CustomRadioButton(
                isSelected = isSelected,
                onClick = onSelect
            )

            // Text Column (Label + Description)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Option Label
                Text(
                    text = localizedApp(option.labelRes),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (option.isEnabled) {
                        extraColors.whiteInDarkMode
                    } else {
                        extraColors.textSubTitle
                    }
                )

                // Description (if available)
                option.descriptionRes?.let { descRes ->
                    Text(
                        text = localizedApp(descRes),
                        fontSize = 13.sp,
                        color = extraColors.textSubTitle,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Custom Radio Button Component
 * Circular button with check mark when selected
 */
@Composable
private fun CustomRadioButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .border(
                width = 2.dp,
                color = if (isSelected) {
                    Color(0xFF1E3A5F)
                } else {
                    extraColors.cardBackground2.copy(alpha = 0.5f)
                },
                shape = CircleShape
            )
            .background(
                color = if (isSelected) {
                    Color(0xFF1E3A5F)
                } else {
                    Color.Transparent
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}