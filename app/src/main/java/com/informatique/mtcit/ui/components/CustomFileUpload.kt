package com.informatique.mtcit.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.theme.LocalExtraColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomFileUpload(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    allowedTypes: List<String> = listOf("pdf", "jpg", "jpeg", "png", "doc", "docx", "xls", "xlsx", "txt"),
    maxSizeMB: Int = 5,
    mandatory: Boolean = false,
    fieldId: String = "",
    onOpenFilePicker: ((String, List<String>) -> Unit)? = null,
    onViewFile: ((String, String) -> Unit)? = null,
    onRemoveFile: ((String) -> Unit)? = null,
    disabled: Boolean = false, // ✅ Disable file upload when sailors are manually entered
    draftDocumentRefNum: String? = null, // ✅ NEW: For draft documents from API
    draftDocumentFileName: String? = null // ✅ NEW: Display name for draft documents
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMimeType by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // ✅ Check if this is a draft document
    val isDraftDocument = remember(value, draftDocumentRefNum) {
        value.startsWith("draft:") && draftDocumentRefNum != null
    }

    // Ensure Excel extensions are always considered allowed when opening picker
    val pickerAllowedTypes = remember(allowedTypes) {
        (allowedTypes + listOf("xls", "xlsx")).map { it.lowercase() }.distinct()
    }

    // Update selectedUri when value changes (for persistence)
    LaunchedEffect(value, draftDocumentFileName) {
        if (isDraftDocument) {
            // ✅ Draft document - use filename from API
            selectedUri = null // No local URI
            selectedMimeType = null
            fileName = draftDocumentFileName ?: "Document"
        } else if (value.isNotEmpty() && value.startsWith("content://")) {
            // Local file URI
            selectedUri = Uri.parse(value)
            selectedMimeType = context.contentResolver.getType(selectedUri!!)
            fileName = getFileName(context, selectedUri!!) ?: "unknown_file"
        } else if (value.isEmpty()) {
            selectedUri = null
            selectedMimeType = null
            fileName = ""
        }
    }

    // Determine if current file is an Excel file (by MIME type or filename/URL extension)
    val isExcel by remember(selectedMimeType, fileName, value) {
        mutableStateOf(
            when {
                // MIME types that often indicate Excel
                selectedMimeType != null && (
                        selectedMimeType!!.contains("spreadsheet", ignoreCase = true) ||
                                selectedMimeType!!.contains("excel", ignoreCase = true) ||
                                selectedMimeType!!.equals("application/vnd.ms-excel", ignoreCase = true) ||
                                selectedMimeType!!.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ignoreCase = true)
                        ) -> true

                // Check filename or value (could be a URL or stored filename)
                fileName.isNotBlank() && (fileName.lowercase().endsWith(".xls") || fileName.lowercase().endsWith(".xlsx")) -> true

                value.isNotBlank() && (value.lowercase().endsWith(".xls") || value.lowercase().endsWith(".xlsx")) -> true

                else -> false
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Label
        if (label.isNotEmpty()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = LocalExtraColors.current.whiteInDarkMode)) {
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
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }

        if ((selectedUri != null || isDraftDocument) && value.isNotEmpty()) {
            // File selected - show file card with dropdown menu
            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { showMenu = true },
                            onLongClick = { showMenu = true }
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (error != null)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                        else
                            Color(0x3300C853) // light green background when a file is present
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (error != null)
                            MaterialTheme.colorScheme.error
                        else
                            Color(0xFF4CAF50) // green border to highlight success
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status icon
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )

                        // File icon
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        // File name
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(0.dp, 4.dp)
                ) {
                    // View option - disabled for Excel files, enabled for draft documents
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(localizedApp(R.string.view_file))
                            }
                        },
                        onClick = {
                            showMenu = false
                            if (isDraftDocument) {
                                // ✅ For draft documents, open preview URL
                                onViewFile?.invoke("draft:$draftDocumentRefNum", "application/octet-stream")
                            } else {
                                // For local files, use URI
                                onViewFile?.invoke(value, selectedMimeType ?: "application/*")
                            }
                        },
                        enabled = !isExcel || isDraftDocument // ✅ Enable for draft documents even if Excel
                    )

                    // Replace option
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(localizedApp(R.string.replace_file))
                            }
                        },
                        onClick = {
                            showMenu = false
                            // Use enriched list that always contains Excel extensions
                            onOpenFilePicker?.invoke(fieldId, pickerAllowedTypes)
                        }
                    )

                    // Delete option
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = localizedApp(R.string.delete_file),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onRemoveFile?.invoke(fieldId)
                            onValueChange("")
                        }
                    )
                }
            }
        } else {
            // No file selected - show upload button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !disabled) {
                        // Use enriched list that always contains Excel extensions
                        onOpenFilePicker?.invoke(fieldId, pickerAllowedTypes)
                    },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (error != null)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    else if (disabled)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) // ✅ Dimmed when disabled
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (error != null)
                        MaterialTheme.colorScheme.error
                    else if (disabled)
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) // ✅ Lighter border when disabled
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        tint = if (disabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) // ✅ Dimmed icon when disabled
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = localizedApp(R.string.choose_file),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (disabled)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) // ✅ Dimmed text when disabled
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Helper text
        if (error == null && value.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = localizedApp(R.string.file_format_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Error text
        error?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// Helper function to get file name from URI
private fun getFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    } catch (e: Exception) {
        uri.lastPathSegment
    }
}
