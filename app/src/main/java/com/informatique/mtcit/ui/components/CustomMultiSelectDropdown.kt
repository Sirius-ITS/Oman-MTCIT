package com.informatique.mtcit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomMultiSelectDropdown(
    label: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionsSelected: (List<String>) -> Unit,
    error: String? = null,
    mandatory: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    isLoading: Boolean = false,
    loadingMessage: String? = "جاري التحميل...",
    maxSelection: Int? = null,
    showSelectionCount: Boolean = true
) {
    var showSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val extraColors = LocalExtraColors.current

    val filteredOptions = options.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    val isInteractionEnabled = enabled && !isLoading

    Column(modifier = Modifier.fillMaxWidth()) {
        // Label
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = extraColors.whiteInDarkMode)) {
                    append(label)
                }
                if (mandatory) {
                    append(" ")
                    withStyle(style = SpanStyle(color = Color(0xFFE74C3C))) {
                        append("*")
                    }
                }
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                error != null -> Color(0xFFE74C3C)
                !isInteractionEnabled -> extraColors.whiteInDarkMode.copy(alpha = 0.38f)
                else -> extraColors.whiteInDarkMode
            },
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        // Main Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isInteractionEnabled) {
                    showSheet = true
                }
        ) {
            OutlinedTextField(
                value = if (showSelectionCount && selectedOptions.isNotEmpty()) {
                    "${selectedOptions.size} selected"
                } else {
                    ""
                },
                onValueChange = {},
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = extraColors.cardBackground,
                    unfocusedContainerColor = extraColors.cardBackground,
                    disabledContainerColor = extraColors.cardBackground,
                    focusedBorderColor = if (error != null) Color(0xFFE74C3C) else Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedTextColor = extraColors.whiteInDarkMode,
                    unfocusedTextColor = extraColors.whiteInDarkMode,
                    disabledTextColor = extraColors.whiteInDarkMode
                ),
                readOnly = true,
                enabled = false,
                placeholder = {
                    if (selectedOptions.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            color = extraColors.textSubTitle,
                            fontSize = 16.sp
                        )
                    }
                },
                leadingIcon = if (leadingIcon != null) {
                    {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = extraColors.blue1,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else null,
                trailingIcon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = extraColors.blue1
                                )
                            }
                            error != null -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Color(0xFFE74C3C)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Dropdown Icon",
                                    tint = if (isInteractionEnabled) extraColors.textSubTitle
                                    else extraColors.textSubTitle.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                singleLine = true
            )

            if (isLoading) {
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(1.dp)
                )
            }
        }

        // Selected Items Chips
        if (selectedOptions.isNotEmpty() && !isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedOptions.forEach { option ->
                    SelectedChip(
                        text = option,
                        onRemove = {
                            onOptionsSelected(selectedOptions - option)
                        },
                        enabled = enabled
                    )
                }
            }
        }

        // Error or Loading Message
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = Color(0xFFE74C3C),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        } else if (isLoading && loadingMessage != null) {
            Text(
                text = loadingMessage,
                fontSize = 12.sp,
                color = extraColors.blue1,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }

    // Bottom Sheet for Selection
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                searchQuery = ""
            },
            sheetState = sheetState,
            containerColor = extraColors.cardBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = extraColors.blue1,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = if (mandatory) "$label *" else label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.whiteInDarkMode
                        )
                    }

                    // Selection Count
                    if (showSelectionCount && selectedOptions.isNotEmpty()) {
                        Text(
                            text = "${selectedOptions.size} selected",
                            fontSize = 14.sp,
                            color = extraColors.blue1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Search Field
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "بحث...",
                            color = extraColors.textSubTitle
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search Icon",
                            tint = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = extraColors.background,
                        unfocusedContainerColor = extraColors.background,
                        disabledContainerColor = extraColors.background,
                        cursorColor = extraColors.blue1,
                        focusedTextColor = extraColors.whiteInDarkMode,
                        unfocusedTextColor = extraColors.whiteInDarkMode,
                        focusedPlaceholderColor = extraColors.textSubTitle,
                        unfocusedPlaceholderColor = extraColors.textSubTitle,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                // Options List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    items(filteredOptions) { option ->
                        val isSelected = selectedOptions.contains(option)
                        val isMaxReached = maxSelection != null &&
                                selectedOptions.size >= maxSelection &&
                                !isSelected

                        ListItem(
                            headlineContent = {
                                Text(
                                    option,
                                    color = if (isMaxReached)
                                        extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                                    else
                                        extraColors.whiteInDarkMode,
                                    fontSize = 15.sp
                                )
                            },
                            modifier = Modifier
                                .clickable(enabled = enabled && !isMaxReached) {
                                    val newSelection = if (isSelected) {
                                        selectedOptions - option
                                    } else {
                                        selectedOptions + option
                                    }
                                    onOptionsSelected(newSelection)
                                }
                                .padding(vertical = 4.dp),
                            leadingContent = {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    enabled = !isMaxReached,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = extraColors.startServiceButton,
                                        uncheckedColor = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                                        checkmarkColor = Color.White
                                    )
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected)
                                    extraColors.startServiceButton.copy(alpha = 0.15f)
                                else
                                    Color.Transparent
                            )
                        )

                        if (option != filteredOptions.last()) {
                            HorizontalDivider(
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.1f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (filteredOptions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "لا توجد نتائج",
                                    color = extraColors.whiteInDarkMode.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clear All
                    if (selectedOptions.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                onOptionsSelected(emptyList())
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = extraColors.startServiceButton,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("مسح الكل", color = Color.White)
                        }
                    }

                    // Done Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                sheetState.hide()
                                showSheet = false
                                searchQuery = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extraColors.startServiceButton,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("تم", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedChip(
    text: String,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    val extraColors = LocalExtraColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = extraColors.cardBackground.copy(alpha = 0.2f),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = extraColors.whiteInDarkMode,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )

            if (enabled) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = extraColors.whiteInDarkMode,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}