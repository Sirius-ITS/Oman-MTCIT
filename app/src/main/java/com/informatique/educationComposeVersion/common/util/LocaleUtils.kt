package com.informatique.educationComposeVersion.common.util

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.*

val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault() }
