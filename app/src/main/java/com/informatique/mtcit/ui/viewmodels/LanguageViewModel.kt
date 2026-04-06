package com.informatique.mtcit.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.common.dataStores.LanguageDataStore
import com.informatique.mtcit.data.repository.LookupRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageDataStore: LanguageDataStore,
    private val lookupRepository: LookupRepository  // ✅ Inject to clear localized cache on language change
) : ViewModel() {

    val languageFlow = languageDataStore.languageFlow
    var isLoading = mutableStateOf(false)
        private set

    fun saveLanguage(langCode: String) {
        viewModelScope.launch {
            isLoading.value = true
            // ✅ Clear all localized lookup caches so the next transaction loads fresh
            // data in the new language (person types, ports, countries, ship types, etc.)
            lookupRepository.clearCache()
            languageDataStore.saveLanguage(langCode)
            delay(300) // tiny buffer to let config refresh
            isLoading.value = false
        }
    }
}