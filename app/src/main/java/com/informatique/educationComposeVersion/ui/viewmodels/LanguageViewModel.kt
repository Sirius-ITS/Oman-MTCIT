package com.informatique.educationComposeVersion.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.informatique.educationComposeVersion.common.dataStores.LanguageDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageDataStore: LanguageDataStore // Inject DataStore directly
) : ViewModel() {

    val languageFlow = languageDataStore.languageFlow
    var isLoading = mutableStateOf(false)
        private set

    fun saveLanguage(langCode: String) {
        viewModelScope.launch {
            isLoading.value = true
            languageDataStore.saveLanguage(langCode)
            delay(300) // tiny buffer to let config refresh
            isLoading.value = false
        }
    }
}