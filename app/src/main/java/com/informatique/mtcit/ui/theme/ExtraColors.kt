package com.informatique.mtcit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

data class ExtraColors(
    val success: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color,
    val error: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color
)
val LightExtraColors = ExtraColors(
    success = SuccessLight,
    warning = WarningLight,
    error = ErrorLight,
    background = backgroundLightt
)

val DarkExtraColors = ExtraColors(
    success = SuccessDark,
    warning = WarningDark,
    error = ErrorDark,
    background = backgroundDarkk
)

val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }
