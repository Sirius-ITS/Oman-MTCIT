//package com.informatique.mtcit.ui.components
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.expandVertically
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.shrinkVertically
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material.icons.filled.KeyboardArrowDown
//import androidx.compose.material.icons.filled.KeyboardArrowUp
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.informatique.mtcit.R
//import com.informatique.mtcit.ui.theme.ExtraColors
//import com.informatique.mtcit.ui.theme.LocalExtraColors
//
//@Composable
//fun SailorListManager(
//    modifier: Modifier = Modifier,
//    sailors: List<SailorData>,
//    jobs: List<String>,
//    onSailorChange: (List<SailorData>) -> Unit,
//    onTotalCountChange: ((String) -> Unit)? = null
//) {
//    val extraColors = LocalExtraColors.current
//    var showBottomSheet by remember { mutableStateOf(false) }
//    var editingSailor by remember { mutableStateOf<SailorData?>(null) }
//
//    // Auto-update the total count whenever engines list changes
//    LaunchedEffect(sailors.size) {
//        onTotalCountChange?.invoke(sailors.size.toString())
//    }
//
//    Column(
//        modifier = modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        // Total Engines Count (read-only, auto-updated)
//        CustomTextField(
//            value = sailors.size.toString(),
//            onValueChange = { /* Read-only */ },
//            label = localizedApp(R.string.sailor_count),
//            isNumeric = true,
//            mandatory = false,
//            placeholder = localizedApp(R.string.sailor_count),
//        )
//
//
//        // List of Added Sailors
//        if (sailors.isNotEmpty()) {
//            sailors.forEach { sailor ->
//                ModernSailorCard(
//                    sailor = sailor,
//                    onEdit = {
//                        editingSailor = sailor
//                        showBottomSheet = true
//                    },
//                    onDelete = {
//                        onSailorChange(sailors.filter { it.id != sailor.id })
//                    }
//                )
//            }
//        }
//
//        // Add Sailor Button
//        Button(
//            onClick = {
//                editingSailor = null
//                showBottomSheet = true
//            },
//            modifier = Modifier.fillMaxWidth(),
//            shape = RoundedCornerShape(14.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = extraColors.startServiceButton
//            ),
//            contentPadding = PaddingValues(vertical = 14.dp)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Add,
//                contentDescription = null,
//                modifier = Modifier.size(22.dp),
//                tint = Color.White
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = localizedApp(R.string.sailor_add_title),
//                fontSize = 16.sp,
//                fontWeight = FontWeight.SemiBold,
//                color = Color.White
//            )
//        }
//    }
//
//    // Bottom Sheet for Sailor Form
//    if (showBottomSheet) {
//        SailorFormBottomSheet(
//            sailor = editingSailor,
//            jobs = jobs,
//            onDismiss = { showBottomSheet = false },
//            onSave = { sailorData ->
//                if (editingSailor != null) {
//                    onSailorChange(sailors.map { if (it.id == editingSailor!!.id) sailorData else it })
//                } else {
//                    onSailorChange(sailors + sailorData)
//                }
//
//                showBottomSheet = false
//            }
//        )
//    }
//}
//
//@Composable
//fun ModernSailorCard(
//    sailor: SailorData,
//    onEdit: () -> Unit,
//    onDelete: () -> Unit
//) {
//    val extraColors = LocalExtraColors.current
//    var isExpanded by remember { mutableStateOf(false) }
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(14.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = extraColors.cardBackground
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            // Clickable Header Section
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { isExpanded = !isExpanded }
//                    .padding(16.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = sailor.fullName,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        color = extraColors.whiteInDarkMode,
//                        fontSize = 16.sp
//                    )
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Text(
//                        text = sailor.job,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
//                        fontSize = 14.sp
//                    )
//                }
//
//                Spacer(modifier = Modifier.width(12.dp))
//
//                // Expand/Collapse Icon with Circle Background
//                Icon(
//                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                    contentDescription = if (isExpanded) "Collapse" else "Expand",
//                    tint = extraColors.whiteInDarkMode,
//                    modifier = Modifier.padding(6.dp)
//                )
//            }
//
//            // Animated Expandable Details
//            AnimatedVisibility(
//                visible = isExpanded,
//                enter = expandVertically() + fadeIn(),
//                exit = shrinkVertically() + fadeOut()
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
//                ) {
//                    HorizontalDivider(
//                        modifier = Modifier.padding(bottom = 12.dp),
//                        color = extraColors.whiteInDarkMode.copy(alpha = 0.2f),
//                        thickness = 1.dp
//                    )
//
//                    // ID Number
//                    InfoRow(
//                        label = localizedApp(R.string.sailor_identity_number),
//                        value = sailor.identityNumber,
//                        extraColors = extraColors
//                    )
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    InfoRow(
//                        label = localizedApp(R.string.sailor_seaman_passport_number),
//                        value = sailor.seamanPassportNumber,
//                        extraColors = extraColors
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Action Buttons
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        // Edit Button
//                        OutlinedButton(
//                            onClick = onEdit,
//                            modifier = Modifier.weight(1f),
//                            shape = RoundedCornerShape(8.dp),
//                            colors = ButtonDefaults.outlinedButtonColors(
//                                contentColor = extraColors.whiteInDarkMode
//                            ),
//                            border = BorderStroke(1.dp, extraColors.whiteInDarkMode.copy(alpha = 0.5f))
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Edit,
//                                contentDescription = null,
//                                modifier = Modifier.size(18.dp),
//                                tint = extraColors.whiteInDarkMode
//                            )
//                            Spacer(modifier = Modifier.width(6.dp))
//                            Text(
//                                text = localizedApp(R.string.sailor_edit_title),
//                                fontSize = 12.sp,
//                                color = extraColors.whiteInDarkMode
//                            )
//                        }
//
//                        // Delete Button
//                        OutlinedButton(
//                            onClick = onDelete,
//                            modifier = Modifier.weight(1f),
//                            shape = RoundedCornerShape(8.dp),
//                            colors = ButtonDefaults.outlinedButtonColors(
//                                contentColor = MaterialTheme.colorScheme.error
//                            ),
//                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Delete,
//                                contentDescription = null,
//                                modifier = Modifier.size(18.dp)
//                            )
//                            Spacer(modifier = Modifier.width(6.dp))
//                            Text(
//                                text = localizedApp(R.string.sailor_delete_title),
//                                fontSize = 12.sp
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun InfoRow(
//    label: String,
//    value: String,
//    extraColors: ExtraColors
//) {
//    Column {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodySmall,
//            color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
//            fontSize = 12.sp
//        )
//        Spacer(modifier = Modifier.height(2.dp))
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyMedium,
//            color = extraColors.whiteInDarkMode,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Medium
//        )
//    }
//}
package com.informatique.mtcit.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.ExtraColors
import com.informatique.mtcit.ui.theme.LocalExtraColors

@Composable
fun SailorListManager(
    modifier: Modifier = Modifier,
    sailors: List<SailorData>,
    jobs: List<String>,
    nationalities: List<String> = emptyList(),
    hasExcelFile: Boolean = false, // ✅ NEW: Disable add sailor button if Excel file is uploaded
    onSailorChange: (List<SailorData>) -> Unit,
    onTotalCountChange: ((String) -> Unit)? = null
) {
    val extraColors = LocalExtraColors.current
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingSailor by remember { mutableStateOf<SailorData?>(null) }

    // Launcher for file download
    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            downloadExcelFile(context, uri)
        }
    }

    // Auto-update the total count whenever engines list changes
    LaunchedEffect(sailors.size) {
        onTotalCountChange?.invoke(sailors.size.toString())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total Sailors Count (read-only, auto-updated)
//        CustomTextField(
//            value = sailors.size.toString(),
//            onValueChange = { /* Read-only */ },
//            label = localizedApp(R.string.sailor_count),
//            isNumeric = true,
//            mandatory = false,
//            placeholder = localizedApp(R.string.sailor_count),
//        )

        // Download Template Button
//        OutlinedButton(
//            onClick = {
//                downloadLauncher.launch("نموذج_البحارة.xlsx")
//            },
//            modifier = Modifier.fillMaxWidth(),
//            shape = RoundedCornerShape(12.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = extraColors.whiteInDarkMode
//            ),
//            border = BorderStroke(1.dp, extraColors.whiteInDarkMode.copy(alpha = 0.3f)),
//            contentPadding = PaddingValues(vertical = 14.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Download,
//                    contentDescription = null,
//                    modifier = Modifier.size(20.dp),
//                    tint = extraColors.whiteInDarkMode
//                )
//
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text(
//                        text = "تنزيل نموذج الإدخال",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Medium,
//                        color = extraColors.whiteInDarkMode
//                    )
//                    Text(
//                        text = "5.3MB",
//                        fontSize = 12.sp,
//                        color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
//                    )
//                }
//
//                Surface(
//                    shape = RoundedCornerShape(50),
//                    color = extraColors.startServiceButton.copy(alpha = 0.2f),
//                    modifier = Modifier.size(36.dp)
//                ) {
//                    Box(
//                        contentAlignment = Alignment.Center,
//                        modifier = Modifier.fillMaxSize()
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Download,
//                            contentDescription = null,
//                            modifier = Modifier.size(20.dp),
//                            tint = extraColors.startServiceButton
//                        )
//                    }
//                }
//            }
//        }
        // Download Template Button - Modern Card Design
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { downloadLauncher.launch("sailors_template.xlsx") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = extraColors.cardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extraColors.startServiceButton.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = extraColors.whiteInDarkMode.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "تنزيل نموذج الإدخال",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = extraColors.whiteInDarkMode
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = extraColors.startServiceButton.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Excel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = extraColors.whiteInDarkMode.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "5.3 MB",
                            fontSize = 13.sp,
                            color = extraColors.whiteInDarkMode.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

//                // Arrow Icon
//                Icon(
//                    imageVector = Icons.Default.Download,
//                    contentDescription = null,
//                    modifier = Modifier.size(24.dp),
//                    tint = extraColors.whiteInDarkMode.copy(alpha = 0.4f)
//                )
            }
        }

        // List of Added Sailors
        if (sailors.isNotEmpty()) {
            sailors.forEach { sailor ->
                ModernSailorCard(
                    sailor = sailor,
                    onEdit = {
                        editingSailor = sailor
                        showBottomSheet = true
                    },
                    onDelete = {
                        onSailorChange(sailors.filter { it.id != sailor.id })
                    }
                )
            }
        }

        // Add Sailor Button
        Button(
            onClick = {
                editingSailor = null
                showBottomSheet = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasExcelFile) extraColors.startServiceButton.copy(alpha = 0.5f) else extraColors.startServiceButton,
                disabledContainerColor = extraColors.startServiceButton.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(vertical = 14.dp),
            enabled = !hasExcelFile // ✅ Disable if Excel file is uploaded
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (hasExcelFile) Color.White.copy(alpha = 0.5f) else Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = localizedApp(R.string.sailor_add_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (hasExcelFile) Color.White.copy(alpha = 0.5f) else Color.White
            )
        }
    }

    // Bottom Sheet for Sailor Form
    if (showBottomSheet) {
        SailorFormBottomSheet(
            sailor = editingSailor,
            jobs = jobs,
            nationalities = nationalities,
            onDismiss = { showBottomSheet = false },
            onSave = { sailorData ->
                if (editingSailor != null) {
                    onSailorChange(sailors.map { if (it.id == editingSailor!!.id) sailorData else it })
                } else {
                    onSailorChange(sailors + sailorData)
                }

                showBottomSheet = false
            }
        )
    }
}

private fun downloadExcelFile(context: Context, uri: Uri) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            context.resources.openRawResource(R.raw.sailors_template).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

            Toast.makeText(
                context,
                "تم تحميل الملف بنجاح",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "حدث خطأ أثناء تحميل الملف: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Composable
    fun ModernSailorCard(
        sailor: SailorData,
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
                            text = sailor.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extraColors.whiteInDarkMode,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sailor.job,
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
                            label = localizedApp(R.string.sailor_identity_number),
                            value = sailor.identityNumber,
                            extraColors = extraColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        InfoRow(
                            label = localizedApp(R.string.sailor_seaman_passport_number),
                            value = sailor.seamanPassportNumber,
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
                                    text = localizedApp(R.string.sailor_edit_title),
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
                                    text = localizedApp(R.string.sailor_delete_title),
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