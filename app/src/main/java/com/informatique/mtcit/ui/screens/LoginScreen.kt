package com.informatique.mtcit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.IconButton
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.TextField
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.informatique.mtcit.R
import com.informatique.mtcit.common.NoInternetException
import com.informatique.mtcit.data.model.loginModels.LoginResponse
import com.informatique.mtcit.ui.base.UIState
import com.informatique.mtcit.ui.components.AlertPopup
import com.informatique.mtcit.ui.components.CommonButton
import com.informatique.mtcit.ui.theme.LocalExtraColors
import com.informatique.mtcit.ui.viewmodels.LoginViewModel
import com.informatique.mtcit.ui.viewmodels.SharedUserViewModel

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
    val extraColors = LocalExtraColors.current
    val headerColor = extraColors.background

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogColor by remember { mutableStateOf(Color.Transparent) }
    val context = LocalContext.current

    SideEffect {
        systemUiController.setStatusBarColor(
            color = headerColor,
            darkIcons = true
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = headerColor
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
                label = {
                    Text(
                        mContext.getString(R.string.username),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUsernameFocused)
                            colorResource(id = R.color.text_field_label)
                        else Color.Gray
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isUsernameFocused = it.isFocused },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = colorResource(id = R.color.background),
                    focusedLabelColor = colorResource(id = R.color.text_field_label),
                    unfocusedLabelColor = Color.Gray,
                    focusedIndicatorColor = colorResource(id = R.color.text_field_label),
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = colorResource(id = R.color.text_field_label)
                ),
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(38.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        mContext.getString(R.string.password),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isPasswordFocused)
                            colorResource(id = R.color.text_field_label)
                        else Color.Gray
                    )
                },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isPasswordFocused = it.isFocused },
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
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(52.dp))

            CommonButton(
                text = mContext.getString(R.string.login),
                true,
                colorResource(id = R.color.login_button)
            ) {
                loginViewModel.login(username, password)
            }

            Spacer(modifier = Modifier.height(10.dp))

            CommonButton(text = "Theme option", true, Color.White) {
                navController.navigate("settings_screen")
            }

            Spacer(modifier = Modifier.height(10.dp))

            CommonButton(text = "Microsoft Azure", true, Color.White) {
                navController.navigate("microsoft_azure")
            }

            // ✅ معالجة حالات الواجهة بشكل ثابت
            LaunchedEffect(loginUiState) {
                when (loginUiState) {
                    is UIState.Failure -> {
                        val errorText =
                            if ((loginUiState as UIState.Failure<LoginResponse>).throwable is NoInternetException)
                                context.getString(R.string.no_internet_available)
                            else
                                context.getString(R.string.something_went_wrong)

                        dialogTitle = "Login Failed"
                        dialogMessage = errorText
                        dialogColor = extraColors.error

                        // ✅ إعادة إظهار الديالوج حتى لو نفس الخطأ
                        showDialog = false
                        showDialog = true
                    }

                    is UIState.Success -> {
                        val response = (loginUiState as UIState.Success<LoginResponse>).data
                        if (response.result != "Ok") {
                            dialogTitle = "Login Failed"
                            dialogMessage = response.details ?: "Username or password incorrect"
                            dialogColor = extraColors.error
                            showDialog = false
                            showDialog = true
                        } else {
                            response.cardProfile?.let { profile ->
                                sharedUserViewModel.setCardProfile(profile)
                                response.userMainData?.let {
                                    sharedUserViewModel.setUserMainData(it)
                                }

                                dialogTitle = "Login Success"
                                dialogMessage = "Welcome back!"
                                dialogColor = extraColors.success
                                showDialog = false
                                showDialog = true

                                // التنقل بعد نجاح الدخول
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } ?: run {
                                dialogTitle = "Login Failed"
                                dialogMessage = "لم يتم استلام بيانات المستخدم بشكل صحيح."
                                dialogColor = extraColors.error
                                showDialog = false
                                showDialog = true
                            }
                        }
                    }

                    is UIState.Error -> {
                        dialogTitle = "Login Failed"
                        dialogMessage = (loginUiState as UIState.Error<LoginResponse>).message
                        dialogColor = extraColors.error
                        showDialog = false
                        showDialog = true
                    }

                    else -> Unit
                }
            }

            // ✅ عرض الـ Dialog في واجهة UI
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
