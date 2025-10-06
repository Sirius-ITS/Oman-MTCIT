package com.informatique.mtcit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtraColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val background: Color
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
