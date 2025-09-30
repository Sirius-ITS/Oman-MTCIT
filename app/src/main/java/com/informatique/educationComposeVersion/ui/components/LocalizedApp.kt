package com.informatique.educationComposeVersion.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.informatique.educationComposeVersion.common.util.LocalAppLocale


@Composable
fun localizedApp(@StringRes resId: Int): String {
    val context = LocalContext.current
    val locale = LocalAppLocale.current

    val config = context.resources.configuration.apply {
        setLocale(locale)
    }
    val localizedContext: Context = context.createConfigurationContext(config)

    return localizedContext.resources.getString(resId)
}