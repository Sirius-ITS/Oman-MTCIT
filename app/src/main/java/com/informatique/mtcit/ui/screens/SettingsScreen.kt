package com.informatique.mtcit.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.informatique.mtcit.ui.theme.ThemeOption
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel

@Composable
fun SettingsScreen(viewModel: ThemeViewModel , navController: NavController, sharedUserViewModel: SharedUserViewModel) {
    val theme by viewModel.theme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Choose App Theme",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        ThemeOption.values().forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTheme(option) }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = theme == option,
                    onClick = { viewModel.setTheme(option) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (option) {
                        ThemeOption.LIGHT -> "Light"
                        ThemeOption.DARK -> "Dark"
                        ThemeOption.SYSTEM_DEFAULT -> "System Default"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
