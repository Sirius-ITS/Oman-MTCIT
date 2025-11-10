package com.informatique.mtcit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtraColors(
    val background: Color,
    val cardBackground : Color,
    val whiteInDarkMode: Color,
    val viewAll :Color,
    val iconBlueBackground :Color,
    val textSubTitle: Color,
    val showDetials : Color,
    val viewAllText : Color,
    val textBlueSubTitle : Color,
    val iconBackBackground : Color,
    val iconBackBackground2 : Color,
    val cardBackground2: Color,
    val iconLightBlueBackground: Color,
    val iconLightBlue : Color,
    val iconBack :Color,
    val iconBack2 :Color,
    val cardBacground3 : Color,
    val SelectedCustomTab : Color,
    val startServiceButton : Color,
    val iconGreyBackground : Color,
    val iconBlueGrey : Color,
    val buttonLightBlue : Color,





    val success: Color,
    val warning: Color,
    val error: Color,
    val blue1: Color,
    val blue2: Color,
    val gray0: Color,
    val accent: Color,
    val steppunselected: Color,
    val grayCard: Color,
    val blue3: Color,
    val surface: Color,
    val navy18223B : Color
)
val LightExtraColors = ExtraColors(
    background = BackgroundLight,
    cardBackground = cardBackgroundLight,
    whiteInDarkMode = White,
    viewAll = ViewAllLight,
    iconBlueBackground = Blue4,
    textSubTitle = Gray1Light,
    showDetials = ShowDetailsLight,
    viewAllText = Blue2Light,
    textBlueSubTitle = Blue2Light,
    iconBackBackground = IconBackgroundLight,
    iconBackBackground2 = IconBackBackground2Light,
    cardBackground2 = cardbackground2Light,
    iconLightBlueBackground = LightBlueGray,
    iconLightBlue = blue5Light,
    iconBack = BackIconLight,
    iconBack2 = White,
    cardBacground3 = CardBackground3Ligt,
    SelectedCustomTab = SelectedCustomTabLight,
    startServiceButton = blue6Light,
    iconGreyBackground = IconGreyBackgroundLight,
    iconBlueGrey = IconBlueGreyLight,
    buttonLightBlue = ButtonLightBlueLight,



    success = SuccessLight,
    warning = WarningLight,
    error = ErrorLight,
    blue1 = Blue1Light,
    blue2 = Blue2Light,
    gray0 = Gray0Light,
    accent = AccentLight,
    steppunselected = SteppunselectedLight,
    grayCard = GrayCardLight,
    blue3 = Blue3Light,
    surface = SurfaceLight,
    navy18223B = Black
)

val DarkExtraColors = ExtraColors(
    background = BackgroundDark,
    cardBackground = cardBackgroundDark,
    whiteInDarkMode = Black,
    viewAll = ViewAllDark,
    iconBlueBackground = Blue4,
    textSubTitle = Gray1Dark,
    showDetials = ShowDetailsDark,
    viewAllText = Blue2Dark,
    textBlueSubTitle = Blue2Dark,
    iconBackBackground = IconBackgroundDark,
    iconBackBackground2 = IconBackBackground2Dark,
    cardBackground2 = cardbackground2Dark,
    iconLightBlueBackground = DarkBlueGray,
    iconLightBlue = blue5Dark,
    iconBack = BackIconDark,
    iconBack2 = BackIconDark,
    cardBacground3 = CardBackground3Dark,
    SelectedCustomTab = SelectedCustomTabDark,
    startServiceButton = blue6Dark,
    iconGreyBackground = IconGreyBackgroundDark,
    iconBlueGrey = IconBlueGreyDark,
    buttonLightBlue = ButtonLightBlueDark,


    success = SuccessDark,
    warning = WarningDark,
    error = ErrorDark,
    blue1 = Blue1Dark,
    blue2 = Blue2Dark,
    gray0 = Gray0Dark,

    accent = AccentDark,
    steppunselected = SteppunselectedDark,
    grayCard = GrayCardDark,
    blue3 = Blue3Dark,
    surface = SurfaceDark,
    navy18223B = Navy18223B

)

val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }