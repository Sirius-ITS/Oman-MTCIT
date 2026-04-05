package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors

/**
 * Read-only card that shows the current value of a field before a change operation.
 * Matches the iOS PreviousMarineActivityCard design:
 *   - Ferry icon in a soft circle
 *   - Small secondary label above the value
 *   - Semi-transparent rounded card background
 */
@Composable
fun CurrentValueCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = extraColors.textSubTitle.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Ferry icon inside a soft circle ──────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = extraColors.textSubTitle.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBoat,
                contentDescription = null,
                tint = extraColors.textSubTitle,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── Label + Value ─────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = extraColors.textSubTitle
            )
            Text(
                text = value.ifEmpty { "—" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.whiteInDarkMode
            )
        }
    }
}

