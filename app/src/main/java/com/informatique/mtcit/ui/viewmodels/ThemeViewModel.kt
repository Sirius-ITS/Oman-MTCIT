package com.informatique.mtcit.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.data.preferences.ThemePreferences
import com.informatique.mtcit.ui.theme.ThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val app: Application
) : ViewModel() {

    private val _theme = MutableStateFlow(ThemeOption.SYSTEM_DEFAULT)
    val theme: StateFlow<ThemeOption> = _theme.asStateFlow()

    init {
        viewModelScope.launch {
            ThemePreferences.getTheme(app).collect {
                _theme.value = it
            }
        }
    }

    fun setTheme(option: ThemeOption) {
        viewModelScope.launch {
            ThemePreferences.saveTheme(app, option)
        }
    }
}
