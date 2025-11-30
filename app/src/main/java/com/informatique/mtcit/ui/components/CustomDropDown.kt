package com.informatique.mtcit.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdown(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    error: String? = null,
    mandatory: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    // ✅ NEW: Shimmer loading parameters
    isLoading: Boolean = false,
    loadingMessage: String? = "جاري التحميل...",
    maxLength: Int? = null, // ✅ NEW: Maximum character length (for validation)
    minLength: Int? = null // ✅ NEW: Minimum character length (for validation)
) {
    var showSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val extraColors = LocalExtraColors.current

    val filteredOptions = options.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    // ✅ Disable interaction when loading
    val isInteractionEnabled = enabled && !isLoading

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = extraColors.whiteInDarkMode)) {
                    append(label)
                }
                if (mandatory) {
                    append(" ")
                    withStyle(style = SpanStyle(color = Color.Red)) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isInteractionEnabled) {
                    showSheet = true
                }
        ) {
            OutlinedTextField(
                value = selectedOption ?: "",
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
                    if ((selectedOption == null || selectedOption.isEmpty()) && placeholder != null) {
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
                    // ✅ Show different icon based on state
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
                                else extraColors.gray0.copy(alpha = 0.3f)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null,
                singleLine = true
            )

            // ✅ Shimmer effect overlay when loading
            if (isLoading) {
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(1.dp)
                )
            }
        }

        // ✅ Show error or loading message
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search...",
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
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
                        focusedContainerColor = extraColors.cardBackground,
                        unfocusedContainerColor = extraColors.cardBackground,
                        disabledContainerColor = extraColors.cardBackground,
                        cursorColor = extraColors.blue1,
                        focusedTextColor = extraColors.whiteInDarkMode,
                        unfocusedTextColor = extraColors.whiteInDarkMode
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    items(filteredOptions) { option ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    option,
                                    color = extraColors.whiteInDarkMode
                                )
                            },
                            modifier = Modifier
                                .clickable(enabled = enabled) {
                                    onOptionSelected(option)
                                    searchQuery = ""
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        showSheet = false
                                    }
                                }
                                .padding(vertical = 4.dp),
                            trailingContent = {
                                if (option == selectedOption) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = extraColors.blue1
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (option == selectedOption)
                                    extraColors.blue1.copy(alpha = 0.2f)
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
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "No results found",
                                        color = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ Shimmer effect with animation for loading states
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.3f),
        Color.Gray.copy(alpha = 0.5f),
        Color.Gray.copy(alpha = 0.3f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
    )
}
