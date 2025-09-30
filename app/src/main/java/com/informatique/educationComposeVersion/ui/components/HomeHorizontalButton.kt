package com.informatique.educationComposeVersion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun HomeHorizontalButton(
    title: String,
    subtitle: String,
    background: Color,
    icon: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(background)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 5.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(35.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(title, color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleMedium , fontWeight = FontWeight.SemiBold , maxLines = 1)
                Spacer(modifier = Modifier.height(8.dp))
                Text(subtitle, color = Color.White , style = androidx.compose.material3.MaterialTheme.typography.labelSmall , maxLines = 1)
            }
        }
    }
}


//@Composable
//fun HomeHorizontalButton(title: String, subtitle: String, background: Color, icon: Int , onClick: ()-> Unit ) {
//    Row(
//        modifier = Modifier
//            .background(background, RoundedCornerShape(14.dp))
//            .padding(start = 5.dp , end = 24.dp , top = 13.dp , bottom = 13.dp )
//            .width(125.dp)
//            .clickable { onClick() },
//        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
//    ) {
//        Image(
//            painter = painterResource(id = icon),
//            contentDescription = title,
//            colorFilter = ColorFilter.tint(Color.White),
//            modifier = Modifier.size(35.dp))
//        Spacer( modifier = Modifier.width(5.dp))
//        Column{
//            Text(title, color = Color.White, fontWeight = FontWeight.Bold , fontSize = 18.sp)
//            Spacer( modifier = Modifier.height(10.dp))
//            Text(subtitle, color = Color.White)
//        }
//    }
//}