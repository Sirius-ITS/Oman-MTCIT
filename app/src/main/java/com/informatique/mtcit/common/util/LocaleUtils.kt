package com.informatique.mtcit.common.util

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.*

val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault() }

/**
 * App-level language accessor for non-Composable contexts (data layer, repositories, etc.).
 * Delegates to Locale.getDefault() which is correctly updated by LocaleManager.applyLocale().
 * For @Composable functions, use LocalAppLocale.current instead to get live recomposition.
 */
object AppLanguage {
    /** True when the current app language is Arabic. */
    val isArabic: Boolean get() = Locale.getDefault().language == "ar"
    /** Current language code, e.g. "ar" or "en". */
    val code: String get() = Locale.getDefault().language
}
