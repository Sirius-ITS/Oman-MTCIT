package com.informatique.mtcit.data.helpers

import android.content.Context
import java.util.Locale

object LocaleHelper {
    fun wrapContext(context: Context, languageCode: String?): Context {
        if (languageCode.isNullOrEmpty()) return context

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}