package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.informatique.mtcit.R
import com.informatique.mtcit.common.NoInternetException
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.AlertPopup
import com.informatique.mtcit.ui.components.localizedApp
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.LoginViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    sharedUserViewModel: SharedUserViewModel
) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginUiState: UIState<LoginResponse> by loginViewModel.loginState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogColor by remember { mutableStateOf(Color.Transparent) }
    val extraColors = LocalExtraColors.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = localizedApp(R.string.login),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(80.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings_screen") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = extraColors.blue1,
                            modifier = Modifier.size(24.dp)
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Username Field
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(localizedApp(R.string.username)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Username",
                            tint = extraColors.blue1
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.blue1,
                        unfocusedBorderColor = colorResource(id = R.color.grey),
                        focusedLabelColor = extraColors.blue1,
                        unfocusedLabelColor = colorResource(id = R.color.grey),
                        cursorColor = extraColors.blue1
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(localizedApp(R.string.password)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = extraColors.blue1
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = colorResource(id = R.color.grey)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.blue1,
                        unfocusedBorderColor = colorResource(id = R.color.grey),
                        focusedLabelColor = extraColors.blue1,
                        unfocusedLabelColor = colorResource(id = R.color.grey),
                        cursorColor = extraColors.blue1
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Login Button
                Button(
                    onClick = { loginViewModel.login(username, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.blue1,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (loginUiState is UIState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = localizedApp(R.string.login),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Forgot Password
                TextButton(onClick = { /* TODO */ }) {
                    Text(
                        text = localizedApp(R.string.forgotpass),
                        color = extraColors.blue2,
                        fontSize = 14.sp
                    )
                }
            }

            // Handle Login States
            LaunchedEffect(loginUiState) {
                when (loginUiState) {
                    is UIState.Failure -> {
                        val errorText = if ((loginUiState as UIState.Failure<LoginResponse>).throwable is NoInternetException)
                            context.getString(R.string.no_internet_available)
                        else
                            context.getString(R.string.something_went_wrong)

                        dialogTitle = "Login Failed"
                        dialogMessage = errorText
                        dialogColor = extraColors.blue1
                        showDialog = true
                    }

                    is UIState.Success -> {
                        val response = (loginUiState as UIState.Success<LoginResponse>).data
                        if (response.result != "Ok") {
                            dialogTitle = "Login Failed"
                            dialogMessage = response.details ?: "Username or password incorrect"
                            dialogColor = extraColors.blue1
                            showDialog = true
                        } else {
                            response.cardProfile?.let { profile ->
                                sharedUserViewModel.setCardProfile(profile)
                                response.userMainData?.let {
                                    sharedUserViewModel.setUserMainData(it)
                                }
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    }

                    is UIState.Error -> {
                        dialogTitle = "Login Failed"
                        dialogMessage = (loginUiState as UIState.Error<LoginResponse>).message
                        dialogColor = extraColors.blue1
                        showDialog = true
                    }

                    else -> Unit
                }
            }

            if (showDialog) {
                AlertPopup(
                    title = dialogTitle,
                    desc = dialogMessage,
                    color = dialogColor,
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}
