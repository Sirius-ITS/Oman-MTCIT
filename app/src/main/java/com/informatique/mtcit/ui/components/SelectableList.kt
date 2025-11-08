package com.informatique.mtcit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PersonType(
    val id: String,
    val title: String,
    val code: String,
    @Transient val icon: @Composable () -> Unit = { DefaultBusinessIcon() }
)

// Generic data class for selectable items
@Serializable
data class SelectableItem(
    val id: String,
    val title: String,
    val code: String,
    val description: String,
    @Transient val icon: @Composable () -> Unit = { DefaultBusinessIcon() }
)

// Reusable Selectable List Component
@Composable
fun <T> SelectableList(
    items: List<T>,
    uiItem: @Composable (ColumnScope.(T) -> Unit),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // List Items

        if (items.isNotEmpty()) {
            items.forEach { item ->
                Spacer(modifier = Modifier.height(16.dp))
                uiItem(item)
            }
        }

    }
}

@Composable
fun SelectableItemCard(
    item: SelectableItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.LightGray
    }

    val backgroundColor = if (isSelected) {
        LocalExtraColors.current.cardBackground
    } else {
        LocalExtraColors.current.cardBackground
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icon
            item.icon()

            Spacer(modifier = Modifier.width(16.dp))

            // Content Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = LocalExtraColors.current.iconGreyBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.code,
                        fontSize = 14.sp,
                        color = LocalExtraColors.current.textSubTitle,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = LocalExtraColors.current.textSubTitle,
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Selection Indicator
            SelectionIndicator(isSelected = isSelected)
        }
    }
}

@Composable
fun PersonTypeCard(
    item: PersonType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    val borderColor = if (isSelected) {
       extraColors.iconBlueGrey
    } else {
        extraColors.background
    }

    val backgroundColor = if (isSelected) {
       extraColors.cardBackground
    } else {
        extraColors.cardBackground
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icon
            item.icon()

            Spacer(modifier = Modifier.width(16.dp))

            // Content Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Selection Indicator
            SelectionIndicator(isSelected = isSelected)
        }
    }
}

@Composable
private fun SelectionIndicator(isSelected: Boolean) {
    if (isSelected) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    } else {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, Color.LightGray),
            color = Color.Transparent
        ) {}
    }
}

@Composable
fun DefaultBusinessIcon(default: Boolean = true) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = LocalExtraColors.current.iconGreyBackground,
    )
    {
        Box(contentAlignment = Alignment.Center) {
            if (default) {
                Text(
                    text = "🏢",
                    fontSize = 28.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(32.dp),
                    tint = LocalExtraColors.current.iconBlueGrey
                )
            }
        }
    }
}

