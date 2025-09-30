package com.informatique.educationComposeVersion.ui.base

import android.content.Context
import androidx.activity.ComponentActivity
import com.informatique.educationComposeVersion.common.dataStores.LanguageDataStore
import com.informatique.educationComposeVersion.data.helpers.LocaleHelper
import com.informatique.educationComposeVersion.ui.BaseActivityEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

abstract class BaseActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // ✅ Fetch dependency manually because Hilt injection hasn't happened yet
        val entryPoint = EntryPointAccessors.fromApplication(
            newBase.applicationContext,
            BaseActivityEntryPoint::class.java
        )

        val languageDataStore = entryPoint.languageDataStore()

        val lang = runBlocking { languageDataStore.languageFlow.first() }
        val localizedContext = LocaleHelper.wrapContext(newBase, lang)
        super.attachBaseContext(localizedContext)
    }
}
