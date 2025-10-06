package com.informatique.mtcit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.informatique.mtcit.R

@Composable
fun CustomFileUpload(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    allowedTypes: List<String> = listOf("pdf", "jpg", "png"),
    maxSizeMB: Int = 5,
    mandatory: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (error != null)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (error != null)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (value.isNotEmpty()) Icons.Default.Description else Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = if (error != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mandatory) "$label *" else label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(localizedApp(R.string.remove_file))
                    }
                } else {
                    Text(
                        text = localizedApp(R.string.file_upload_instructions)
                            .format(allowedTypes.joinToString(", ").uppercase(), maxSizeMB),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // Here you would implement file picker logic
                            // For now, we'll simulate file selection
                            onValueChange("selected_file.pdf")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(localizedApp(R.string.choose_file))
                    }
                }
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
