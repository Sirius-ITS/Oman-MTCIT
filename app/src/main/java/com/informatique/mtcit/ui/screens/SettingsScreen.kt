package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.theme.ThemeOption
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel
import com.informatique.mtcit.viewmodel.ThemeViewModel

@Composable
fun SettingsScreen(
    viewModel: ThemeViewModel,
    navController: NavController,
    sharedUserViewModel: SharedUserViewModel,
    languageViewModel: LanguageViewModel = hiltViewModel()
) {
    val extraColors = LocalExtraColors.current
    val theme by viewModel.theme.collectAsState()
    val currentLanguage by languageViewModel.languageFlow.collectAsState(initial = "ar")
    val isLoading = languageViewModel.isLoading.value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.background)
            .padding(top = 40.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar with Title centered and Done Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                // Title centered
                Text(
                    text = localizedApp(R.string.settings_title),
                    fontSize = 22.sp,
                    color = extraColors.whiteInDarkMode,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Done Button (will be on right in LTR, left in RTL)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(extraColors.cardBackground, CircleShape)
                        .border(1.5.dp, extraColors.iconLightBlueBackground, CircleShape)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedApp(R.string.done),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.whiteInDarkMode
                    )
                }
            }

            // Language Section Title
            Text(
                text = localizedApp(R.string.settings_language_section),
                fontSize = 20.sp,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // English Language Card
            LanguageCardEnhanced(
                languageName = "English",
                flagEmoji = "🇺🇸",
                isSelected = currentLanguage == "en",
                onClick = { languageViewModel.saveLanguage("en") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic Language Card
            LanguageCardEnhanced(
                languageName = "العربية",
                flagEmoji = "🇴🇲",
                isSelected = currentLanguage == "ar",
                onClick = { languageViewModel.saveLanguage("ar") }
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Theme Section Title
            Text(
                text = localizedApp(R.string.settings_theme_section),
                fontSize = 20.sp,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // Theme Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Light Theme Card
                ThemeCardEnhanced(
                    themeIcon = "☀️",
                    themeName = localizedApp(R.string.theme_light),
                    isSelected = theme == ThemeOption.LIGHT,
                    onClick = { viewModel.setTheme(ThemeOption.LIGHT) },
                    modifier = Modifier.weight(1f)
                )

                // Dark Theme Card
                ThemeCardEnhanced(
                    themeIcon = "🌙",
                    themeName = localizedApp(R.string.theme_dark),
                    isSelected = theme == ThemeOption.DARK,
                    onClick = { viewModel.setTheme(ThemeOption.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // About App Section Title
            Text(
                text = localizedApp(R.string.about_app),
                fontSize = 20.sp,
                color = extraColors.whiteInDarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // Version Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(extraColors.grayCard.copy(alpha = 0.25f))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedApp(R.string.version),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = extraColors.whiteInDarkMode
                    )
                    Text(
                        text = "1.0",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = extraColors.whiteInDarkMode.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = extraColors.blue1
            )
        }
    }
}

@Composable
fun LanguageCardEnhanced(
    languageName: String,
    flagEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, extraColors.blue1, RoundedCornerShape(16.dp)).background(extraColors.iconLightBlueBackground)
                else
                    Modifier.border(1.dp, extraColors.grayCard.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language name and flag (will be on left in LTR, right in RTL)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = languageName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) extraColors.whiteInDarkMode else extraColors.whiteInDarkMode.copy(alpha = 0.9f)
                )
                Text(
                    text = flagEmoji,
                    fontSize = 26.sp
                )
            }

            // Checkmark (will be on right in LTR, left in RTL - only when selected)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = extraColors.blue1.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(22.dp))
            }
        }
    }
}

@Composable
fun ThemeCardEnhanced(
    themeIcon: String,
    themeName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraColors = LocalExtraColors.current

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .then(
                if (isSelected)
                    Modifier.border(2.dp, extraColors.blue1, RoundedCornerShape(16.dp)).background(extraColors.iconLightBlueBackground)
                else
                    Modifier.border(1.dp, extraColors.grayCard.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Theme Icon in Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(extraColors.grayCard.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = themeIcon,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = themeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.whiteInDarkMode
            )
        }
    }
}
