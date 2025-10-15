package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R
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
                value = owners.size.toString(), // Always show actual count
                onValueChange = { /* Read-only - no changes allowed */ },
                label = localizedApp(R.string.total_owners_count),
                isNumeric = true,
                mandatory = false
            )
        }

        // Add Owner Button
        Button(
            onClick = {
                editingOwner = null
                showBottomSheet = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = extraColors.blue1
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(localizedApp(R.string.add_owner))
        }

        // List of Added Owners
        if (owners.isNotEmpty()) {
            Text(
                text = localizedApp(R.string.added_owners),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extraColors.blue1
            )

            owners.forEach { owner ->
                OwnerCard(
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
                    // Edit existing owner
                    onOwnersChange(owners.map { if (it.id == editingOwner!!.id) ownerData else it })
                } else {
                    // Add new owner
                    onOwnersChange(owners + ownerData)
                }
                showBottomSheet = false
            }
        )
    }
}

@Composable
fun OwnerCard(
    owner: OwnerData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = extraColors.grayCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = owner.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.blue1
                )
                Text(
                    text = "${localizedApp(R.string.owner_nationality)}: ${owner.nationality}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extraColors.blue2
                )
                Text(
                    text = "${localizedApp(R.string.owner_id_number)}: ${owner.idNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extraColors.blue2
                )
                if (owner.isCompany) {
                    Text(
                        text = "${localizedApp(R.string.company_name)}: ${owner.companyName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extraColors.blue2
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = localizedApp(R.string.edit_owner),
                        tint = extraColors.blue1
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = localizedApp(R.string.delete_owner),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
