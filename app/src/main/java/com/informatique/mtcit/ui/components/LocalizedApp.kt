package com.informatique.mtcit.ui.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.informatique.mtcit.common.util.LocalAppLocale


@SuppressLint("LocalContextConfigurationRead")
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

@SuppressLint("LocalContextConfigurationRead")
@Composable
fun localizedApp(@StringRes resId: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val locale = LocalAppLocale.current

    val config = context.resources.configuration.apply {
        setLocale(locale)
    }
    val localizedContext: Context = context.createConfigurationContext(config)

    return localizedContext.resources.getString(resId, *formatArgs)
}

@SuppressLint("LocalContextConfigurationRead")
@Composable
fun localizedPluralsApp(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val locale = LocalAppLocale.current

    val config = context.resources.configuration.apply {
        setLocale(locale)
    }
    val localizedContext: Context = context.createConfigurationContext(config)

    return localizedContext.resources.getQuantityString(resId, quantity, *formatArgs)
}