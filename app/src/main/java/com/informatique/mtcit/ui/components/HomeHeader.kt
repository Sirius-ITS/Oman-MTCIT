package com.informatique.mtcit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.informatique.mtcit.R

@Composable
fun HomeHeader(title: String , personVictor : ImageVector , notificationVector : ImageVector){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(color = colorResource(id = R.color.system_bar) , RoundedCornerShape(bottomStart = 42.dp, bottomEnd = 42.dp))
            .windowInsetsPadding(WindowInsets.statusBars), // Add status bar padding
    ) {
        Row(modifier = Modifier.padding(start = 20.dp, top = 8.dp)) { // Reduced top padding from 20dp to 8dp
            Icon(
                imageVector = notificationVector,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(title, color = Color.White, fontSize = 14.sp , modifier = Modifier.align(alignment = Alignment.CenterVertically).width(220.dp) , maxLines = 2)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = personVictor,
            contentDescription = "Notification",
            tint = Color.White,
            modifier = Modifier
                .padding(16.dp)
                .size(35.dp)
        )
    }
}