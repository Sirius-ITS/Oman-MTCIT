package com.informatique.mtcit.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.ui.components.DynamicForm
import com.informatique.mtcit.ui.components.HomeFeatureList
import com.informatique.mtcit.ui.components.HomeHeader
import com.informatique.mtcit.ui.components.HomeHorizontal2
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.viewmodels.FormViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

@SuppressLint("SuspiciousIndentation")
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun HomeScreen(navController: NavController, sharedUserViewModel: SharedUserViewModel , viewModel: FormViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.initForm()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DynamicForm(
            viewModel = viewModel,
            onSubmit = { fields ->
                // ✅ هنا تحط لوجيك الـ submit
                println("Form submitted: $fields")
            }
        )
    }

        }