//package com.informatique.mtcit.ui.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
///**
// * Generic Owner List Manager Component
// * Can be used in any form field that needs to manage a list of owners
// * Works seamlessly with FormField.OwnerList
// */
//@Composable
//fun OwnerListManager(
//    modifier: Modifier = Modifier,
//    owners: List<OwnerData>,
//    nationalities: List<String>,
//    countries: List<String>,
//    includeCompanyFields: Boolean = true,
//    totalOwnersCount: String? = null,
//    onOwnersChange: (List<OwnerData>) -> Unit,
//    onTotalCountChange: ((String) -> Unit)? = null
//) {
//    val extraColors = LocalExtraColors.current
//    var showBottomSheet by remember { mutableStateOf(false) }
//    var editingOwner by remember { mutableStateOf<OwnerData?>(null) }
//
//    // Auto-update the total count whenever owners list changes
//    LaunchedEffect(owners.size) {
//        onTotalCountChange?.invoke(owners.size.toString())
//    }
//
//    Column(
//        modifier = modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Total Owners Count (read-only, auto-updated)
//        if (onTotalCountChange != null) {
//            CustomTextField(
//                value = owners.size.toString(), // Always show actual count
//                onValueChange = { /* Read-only - no changes allowed */ },
//                label = localizedApp(R.string.total_owners_count),
//                isNumeric = true,
//                mandatory = false
//            )
//        }
//
//        // Add Owner Button
//        Button(
//            onClick = {
//                editingOwner = null
//                showBottomSheet = true
//            },
//            modifier = Modifier.fillMaxWidth(),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.blue1
//            )
//        ) {
//            Icon(
//                imageVector = Icons.Default.Add,
//                contentDescription = null,
//                modifier = Modifier.size(20.dp)
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(localizedApp(R.string.add_owner))
//        }
//
//        // List of Added Owners
//        if (owners.isNotEmpty()) {
//            Text(
//                text = localizedApp(R.string.added_owners),
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = extraColors.blue1
//            )
//
//            owners.forEach { owner ->
//                OwnerCard(
//                    owner = owner,
//                    onEdit = {
//                        editingOwner = owner
//                        showBottomSheet = true
//                    },
//                    onDelete = {
//                        onOwnersChange(owners.filter { it.id != owner.id })
//                    }
//                )
//            }
//        }
//    }
//
//    // Bottom Sheet for Owner Form
//    if (showBottomSheet) {
//        OwnerFormBottomSheet(
//            owner = editingOwner,
//            nationalities = nationalities,
//            countries = countries,
//            includeCompanyFields = includeCompanyFields,
//            onDismiss = { showBottomSheet = false },
//            onSave = { ownerData ->
//                if (editingOwner != null) {
//                    // Edit existing owner
//                    onOwnersChange(owners.map { if (it.id == editingOwner!!.id) ownerData else it })
//                } else {
//                    // Add new owner
//                    onOwnersChange(owners + ownerData)
//                }
//                showBottomSheet = false
//            }
//        )
//    }
//}
//
//@Composable
//fun OwnerCard(
//    owner: OwnerData,
//    onEdit: () -> Unit,
//    onDelete: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.grayCard
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = owner.fullName,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = extraColors.blue1
//                )
//                Text(
//                    text = "${localizedApp(R.string.owner_nationality)}: ${owner.nationality}",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = extraColors.blue2
//                )
//                Text(
//                    text = "${localizedApp(R.string.owner_id_number)}: ${owner.idNumber}",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = extraColors.blue2
//                )
//                if (owner.isCompany) {
//                    Text(
//                        text = "${localizedApp(R.string.company_name)}: ${owner.companyName}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = extraColors.blue2
//                    )
//                }
//            }
//
//            Row {
//                IconButton(onClick = onEdit) {
//                    Icon(
//                        imageVector = Icons.Default.Edit,
//                        contentDescription = localizedApp(R.string.edit_owner),
//                        tint = extraColors.blue1
//                    )
//                }
//                IconButton(onClick = onDelete) {
//                    Icon(
//                        imageVector = Icons.Default.Delete,
//                        contentDescription = localizedApp(R.string.delete_owner),
//                        tint = MaterialTheme.colorScheme.error
//                    )
//                }
//            }
//        }
//    }
//}

package com.informatique.mtcit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.ExtraColors
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * Generic Owner List Manager Component
 * Can be used in any form field that needs to manage a list of owners
 * Works seamlessly with FormField.OwnerList
 */
@Composable
fun OwnerListManager(
    modifier: Modifier = Modifier,
    owners: List<OwnerData>,
    nationalities: List<String>,
    countries: List<String>,
    includeCompanyFields: Boolean = true,
    totalOwnersCount: String? = null,
    onOwnersChange: (List<OwnerData>) -> Unit,
    onTotalCountChange: ((String) -> Unit)? = null
) {
    val extraColors = LocalExtraColors.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingOwner by remember { mutableStateOf<OwnerData?>(null) }

    // Auto-update the total count whenever owners list changes
    LaunchedEffect(owners.size) {
        onTotalCountChange?.invoke(owners.size.toString())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Owners Count (read-only, auto-updated)
        if (onTotalCountChange != null) {
            CustomTextField(
                value = owners.size.toString(),
                onValueChange = { /* Read-only */ },
                label = localizedApp(R.string.total_owners_count),
                isNumeric = true,
                mandatory = false
            )
        }

        // List of Added Owners
        if (owners.isNotEmpty()) {
            owners.forEach { owner ->
                ModernOwnerCard(
                    owner = owner,
                    onEdit = {
                        editingOwner = owner
                        showBottomSheet = true
                    },
                    onDelete = {
                        onOwnersChange(owners.filter { it.id != owner.id })
                    }
                )
            }
        }

        // Add Owner Button
        Button(
            onClick = {
                editingOwner = null
                showBottomSheet = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = extraColors.blue6
            ),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = extraColors.white
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = localizedApp(R.string.add_owner),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.white
            )
        }
    }

    // Bottom Sheet for Owner Form
    if (showBottomSheet) {
        OwnerFormBottomSheet(
            owner = editingOwner,
            nationalities = nationalities,
            countries = countries,
            includeCompanyFields = includeCompanyFields,
            onDismiss = { showBottomSheet = false },
            onSave = { ownerData ->
                if (editingOwner != null) {
                    onOwnersChange(owners.map { if (it.id == editingOwner!!.id) ownerData else it })
                } else {
                    onOwnersChange(owners + ownerData)
                }
                showBottomSheet = false
            }
        )
    }
}

@Composable
fun ModernOwnerCard(
    owner: OwnerData,
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
                        text = owner.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.white,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = owner.nationality,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.white.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Expand/Collapse Icon with Circle Background
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = extraColors.white,
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
                        color = extraColors.white.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // ID Number
                    InfoRow(
                        label = localizedApp(R.string.owner_id_number),
                        value = owner.idNumber,
                        extraColors = extraColors
                    )

                    // Company Info if applicable
                    if (owner.isCompany && owner.companyName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow(
                            label = localizedApp(R.string.company_name),
                            value = owner.companyName,
                            extraColors = extraColors
                        )
                    }

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
                                contentColor = extraColors.white
                            ),
                            border = BorderStroke(1.dp, extraColors.blue1.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = extraColors.white
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = localizedApp(R.string.edit_owner),
                                fontSize = 12.sp,
                                color = extraColors.white
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
                                text = localizedApp(R.string.delete_owner),
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
            color = extraColors.white.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = extraColors.white,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
