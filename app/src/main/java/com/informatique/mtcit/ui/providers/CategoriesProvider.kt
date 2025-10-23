package com.informatique.mtcit.ui.providers

import androidx.compose.runtime.compositionLocalOf
import com.informatique.mtcit.ui.models.MainCategory

/**
 * CompositionLocal for providing categories throughout the app
 * This allows any screen to access categories without passing them as parameters
 */
val LocalCategories = compositionLocalOf<List<MainCategory>> {
    emptyList()
}

