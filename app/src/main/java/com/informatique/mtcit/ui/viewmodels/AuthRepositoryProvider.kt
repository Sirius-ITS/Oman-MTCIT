package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.informatique.mtcit.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Simple ViewModel wrapper to provide AuthRepository via Hilt DI
 * Used in navigation scenarios where we need AuthRepository at Composable level
 */
@HiltViewModel
class AuthRepositoryProvider @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel()

