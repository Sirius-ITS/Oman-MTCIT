package com.informatique.educationComposeVersion.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.informatique.educationComposeVersion.R
import com.informatique.educationComposeVersion.common.NoInternetException
import com.informatique.educationComposeVersion.data.model.loginModels.LoginResponse
import com.informatique.educationComposeVersion.ui.base.ShowError
import com.informatique.educationComposeVersion.ui.base.ShowLoading
import com.informatique.educationComposeVersion.ui.base.UIState
import com.informatique.educationComposeVersion.ui.components.CommonButton
import com.informatique.educationComposeVersion.ui.components.localizedApp
import com.informatique.educationComposeVersion.ui.viewmodels.LoginViewModel
import com.informatique.educationComposeVersion.ui.viewmodels.SharedUserViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    sharedUserViewModel: SharedUserViewModel
) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginUiState: UIState<LoginResponse> by loginViewModel.loginState.collectAsStateWithLifecycle()
    val mContext = LocalContext.current
    var isUsernameFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val showPasswordIcon = painterResource(id = R.drawable.baseline_visibility_24)
    val hidePasswordIcon = painterResource(id = R.drawable.outline_visibility_off_24)
    val systemUiController = rememberSystemUiController()
    val headerColor = colorResource(id = R.color.background)

    SideEffect {
        systemUiController.setStatusBarColor(
            color = headerColor,
            darkIcons = true
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorResource(id = R.color.background)
    ) {
        Column(
            modifier = Modifier
                .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 15.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.moodle),
                contentDescription = null,
                modifier = Modifier.size(260.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(mContext.getString(R.string.username), style = MaterialTheme.typography.labelLarge, color = if (isUsernameFocused) colorResource(id = R.color.text_field_label) else Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        isUsernameFocused = it.isFocused
                    },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = colorResource(id = R.color.background),
                    focusedLabelColor = colorResource(id = R.color.text_field_label),
                    unfocusedLabelColor = Color.Gray,
                    focusedIndicatorColor = colorResource(id = R.color.text_field_label),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = colorResource(id = R.color.text_field_label)
                ),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(38.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(mContext.getString(R.string.password), style = MaterialTheme.typography.labelLarge, color = if (isPasswordFocused) colorResource(id = R.color.text_field_label) else Color.Gray) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        isPasswordFocused = it.isFocused
                    },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = colorResource(id = R.color.background),
                    focusedLabelColor = colorResource(id = R.color.text_field_label),
                    unfocusedLabelColor = Color.Gray,
                    focusedIndicatorColor = colorResource(id = R.color.text_field_label),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = colorResource(id = R.color.text_field_label),
                    textColor = colorResource(id = R.color.text_field_label)
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Image(
                            painter = if (passwordVisible) hidePasswordIcon else showPasswordIcon,
                            contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(colorResource(id = R.color.grey))
                        )
                    }
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(52.dp))

            CommonButton(text = mContext.getString(R.string.login), true, colorResource(id = R.color.login_button)) {
                loginViewModel.login(username, password)
            }

            Spacer(modifier = Modifier.height(10.dp))

            CommonButton(text = mContext.getString(R.string.forgotpass), true, Color.White) {
                navController.navigate("forgot_password")
            }

            Spacer(modifier = Modifier.height(10.dp))

            CommonButton(text = "Microsoft Azure", true, Color.White) {
                navController.navigate("microsoft_azure")
            }

            when (loginUiState) {
                is UIState.Loading -> {
                    ShowLoading()
                }
                is UIState.Failure -> {
                    var errorText = localizedApp(R.string.something_went_wrong)
                    if ((loginUiState as UIState.Failure<LoginResponse>).throwable is NoInternetException) {
                        errorText = localizedApp(R.string.no_internet_available)
                    }
                    ShowError(
                        text = errorText,
                        retryEnabled = true
                    ) {
                        loginViewModel.login(username, password)
                    }
                }
                is UIState.Success -> {
                    val response = (loginUiState as UIState.Success<LoginResponse>).data
                    if (response.result != "Ok") {
                        ShowError(text = response.details.toString())
                    } else {
                        response.cardProfile?.let { profile ->
                            sharedUserViewModel.setCardProfile(profile)

                            response.userMainData?.let { userMainData ->
                                sharedUserViewModel.setUserMainData(userMainData)
                            }
                            LaunchedEffect(profile) {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } ?: run {
                            ShowError(text = "لم يتم استلام بيانات المستخدم بشكل صحيح.")
                        }
                    }
                }

                is UIState.Empty -> {
                    // Optionally show something if needed
                }

                is UIState.Error -> {
                    ShowError(text = (loginUiState as UIState.Error).message)
                }
            }
        }
    }
}