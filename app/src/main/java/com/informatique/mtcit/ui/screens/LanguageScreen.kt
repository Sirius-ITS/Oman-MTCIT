package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.CommonButton
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.LanguageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    navController: NavController,
    languageViewModel: LanguageViewModel = hiltViewModel()
) {
    val isLoading = languageViewModel.isLoading.value
    val extraColors = LocalExtraColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = localizedApp(R.string.language_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = extraColors.blue1
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = extraColors.blue1
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    modifier = Modifier.size(80.dp),
                    tint = extraColors.blue1
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = localizedApp(R.string.change_language),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.blue1
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Arabic Button
                LanguageOptionCard(
                    languageName = "العربية",
                    languageCode = "Arabic",
                    isSelected = false,
                    onClick = {
                        languageViewModel.saveLanguage("ar")
                        navController.popBackStack()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // English Button
                LanguageOptionCard(
                    languageName = "English",
                    languageCode = "English",
                    isSelected = false,
                    onClick = {
                        languageViewModel.saveLanguage("en")
                        navController.popBackStack()
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = extraColors.blue1
                )
            }
        }
    }
}

@Composable
fun LanguageOptionCard(
    languageName: String,
    languageCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extraColors = LocalExtraColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                extraColors.blue1.copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = languageName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.blue1
                )
                Text(
                    text = languageCode,
                    fontSize = 13.sp,
                    color = extraColors.grayCard
                )
            }

            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = extraColors.blue1,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
