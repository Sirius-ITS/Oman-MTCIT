package com.informatique.educationComposeVersion.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.educationComposeVersion.R

@Composable
fun ProfileHeader(title : String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(color = colorResource(id = R.color.system_bar) , RoundedCornerShape(bottomStart = 45.dp, bottomEnd = 45.dp)), // Blue
    ) {
        Row(modifier = Modifier.padding(start = 40.dp, top = 25.dp)) {
            Text(title, color = Color.White, fontSize = 20.sp , modifier = Modifier.align(alignment = Alignment.CenterVertically) , fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notification",
            tint = Color.White,
            modifier = Modifier
                .padding(16.dp)
                .size(35.dp)
        )
    }
}