package com.informatique.mtcit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtraColors(
    val success: Color,
    val warning: Color,
    val error: Color,
    val background: Color,
    val blue1: Color ,
    val blue2: Color ,
    val gray0: Color ,
    val gray1: Color ,
    val accent: Color,
    val steppunselected: Color,
    val grayCard: Color,
    val blue3: Color,
    val white: Color
)
val LightExtraColors = ExtraColors(
    success = SuccessLight,
    warning = WarningLight,
    error = ErrorLight,
    background = BackgroundLight,
    blue1 = Blue1Light,
    blue2 = Blue2Light,
    gray0 = Gray0Light,
    gray1 = Gray1Light,
    accent = AccentLight,
    steppunselected = SteppunselectedLight,
    grayCard = GrayCardLight,
    blue3 = Blue3Light,
    white = White
)

val DarkExtraColors = ExtraColors(
    success = SuccessDark,
    warning = WarningDark,
    error = ErrorDark,
    background = BackgroundDark,
    blue1 = Blue1Dark,
    blue2 = Blue2Dark,
    gray0 = Gray0Dark,
    gray1 = Gray1Dark,
    accent = AccentDark,
    steppunselected = SteppunselectedDark,
    grayCard = GrayCardDark,
    blue3 = Blue3Dark,
    white = White
)

val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }