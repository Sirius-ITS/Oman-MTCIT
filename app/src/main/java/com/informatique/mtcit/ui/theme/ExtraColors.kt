package com.informatique.mtcit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.informatique.mtcit.ui.theme.cardBackgroundDark

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
    val white: Color,
    val cardBackground : Color,
    val cardBackground2: Color,
    val blue4 :Color,
    val surface: Color,
    val bluegray: Color,
    val blue5 : Color,
    val navy18223B : Color,
    val blue6 : Color
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
    white = White,
    cardBackground = cardBackgroundLight,
    blue4 = Blue4,
    surface = SurfaceLight,
    cardBackground2 = cardbackground2Light,
    bluegray = LightBlueGray,
    blue5 = blue5Light,
    navy18223B = Black,
    blue6 = blue6Light
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
    white = Black,
    cardBackground = cardBackgroundDark,
    blue4 = Blue4,
    surface = SurfaceDark,
    cardBackground2 = cardbackground2Dark,
    bluegray = DarkBlueGray,
    blue5 = blue5Dark,
    navy18223B = Navy18223B,
    blue6 = blue6Dark

)

val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }