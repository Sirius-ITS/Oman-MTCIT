package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.ExtraColors
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * Generic Engine List Manager Component
 * Can be used in any form field that needs to manage a list of engines
 * Works seamlessly with FormField.EngineList
 */
@Composable
fun EngineListManager(
    modifier: Modifier = Modifier,
    engines: List<EngineData>,
    manufacturers: List<String>,
    countries: List<String>,
    fuelTypes: List<String>,
    conditions: List<String>,
    onEnginesChange: (List<EngineData>) -> Unit,
    onTotalCountChange: ((String) -> Unit)? = null
) {
    val extraColors = LocalExtraColors.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingEngine by remember { mutableStateOf<EngineData?>(null) }

    // Auto-update the total count whenever engines list changes
    LaunchedEffect(engines.size) {
        onTotalCountChange?.invoke(engines.size.toString())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Engines Count (read-only, auto-updated)
        CustomTextField(
            value = engines.size.toString(),
            onValueChange = { /* Read-only */ },
            label = localizedApp(R.string.engine_count),
            isNumeric = true,
            mandatory = false,
            placeholder = localizedApp(R.string.engine_count),
            enabled = true
        )


        // List of Added Engines
        if (engines.isNotEmpty()) {
            engines.forEach { engine ->
                ModernEngineCard(
                    engine = engine,
                    onEdit = {
                        editingEngine = engine
                        showBottomSheet = true
                    },
                    onDelete = {
                        onEnginesChange(engines.filter { it.id != engine.id })
                    }
                )
            }
        }

        // Add Engine Button
        Button(
            onClick = {
                editingEngine = null
                showBottomSheet = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extraColors.startServiceButton
            ),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = localizedApp(R.string.engine_add_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }

    // Bottom Sheet for Engine Form
    if (showBottomSheet) {
        EngineFormBottomSheet(
            engine = editingEngine,
            manufacturers = manufacturers,
            countries = countries,
            fuelTypes = fuelTypes,
            engineConditions = conditions,
            onDismiss = { showBottomSheet = false },
            onSave = { engineData ->
                if (editingEngine != null) {
                    onEnginesChange(engines.map { if (it.id == editingEngine!!.id) engineData else it })
                } else {
                    onEnginesChange(engines + engineData)
                }

                showBottomSheet = false
            }
        )
    }
}

@Composable
fun ModernEngineCard(
    engine: EngineData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clickable Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = engine.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.whiteInDarkMode,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = engine.condition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Expand/Collapse Icon with Circle Background
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = extraColors.whiteInDarkMode,
                    modifier = Modifier.padding(6.dp)
                )
            }

            // Animated Expandable Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // ID Number
                    InfoRow(
                        label = localizedApp(R.string.engine_fuel_type),
                        value = engine.fuelType,
                        extraColors = extraColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow(
                        label = localizedApp(R.string.engine_power),
                        value = engine.power,
                        extraColors = extraColors
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Edit Button
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = extraColors.whiteInDarkMode
                            ),
                            border = BorderStroke(1.dp, extraColors.whiteInDarkMode.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = extraColors.whiteInDarkMode
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = localizedApp(R.string.engine_edit_title),
                                fontSize = 12.sp,
                                color = extraColors.whiteInDarkMode
                            )
                        }

                        // Delete Button
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = localizedApp(R.string.engine_delete_title),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    extraColors: ExtraColors
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.whiteInDarkMode,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
