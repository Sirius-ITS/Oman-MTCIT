package com.informatique.mtcit.ui.viewmodels

import androidx.lifecycle.viewModelScope
import com.informatique.mtcit.business.transactions.LoginStrategy
import com.informatique.mtcit.business.transactions.TransactionStrategy
import com.informatique.mtcit.business.transactions.TransactionType
import com.informatique.mtcit.business.usecases.StepNavigationUseCase
import com.informatique.mtcit.common.ResourceProvider
import com.informatique.mtcit.data.repository.AuthRepository
import com.informatique.mtcit.ui.base.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login/Registration ViewModel
 *
 * Handles user authentication flow before accessing transactions
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
    navigationUseCase: StepNavigationUseCase,
    private val loginStrategy: LoginStrategy,
    private val authRepository: AuthRepository
) : BaseTransactionViewModel(resourceProvider, navigationUseCase) {

    // Event to notify when login is complete
    private val _loginComplete = MutableSharedFlow<Boolean>()
    val loginComplete = _loginComplete.asSharedFlow()

    // Event to trigger OAuth WebView navigation
    private val _navigateToOAuth = MutableSharedFlow<Unit>()
    val navigateToOAuth = _navigateToOAuth.asSharedFlow()

    // OAuth URLs
    companion object {
        const val OAUTH_AUTH_URL = "https://omankeycloak.isfpegypt.com/realms/oman/protocol/openid-connect/auth?client_id=front&redirect_uri=https%3A%2F%2Fomankeycloak.isfpegypt.com%2Fstarter&response_type=code&scope=openid"
        const val OAUTH_REDIRECT_URI = "https://omankeycloak.isfpegypt.com/starter"
    }

    init {
        // Initialize login flow with a dummy transaction type
        initializeTransaction(TransactionType.TEMPORARY_REGISTRATION_CERTIFICATE)
    }

    /**
     * Create strategy - returns LoginStrategy
     */
    override suspend fun createStrategy(transactionType: TransactionType): TransactionStrategy {
        return loginStrategy
    }

    /**
     * Monitor submission state to detect login completion
     */
    init {
        viewModelScope.launch {
            submissionState.collect { state ->
                if (state is UIState.Success) {
                    println("✅ Login successful! Emitting completion event...")
                    _loginComplete.emit(true)
                }
            }
        }

        // ✅ PROPER SOLUTION: Observe uiState changes and trigger OAuth when flag is set
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.formData["_triggerOAuthFlow"] == "true") {
                    println("🚀 LoginViewModel: OAuth trigger flag detected in uiState")
                    // Clear the flag to avoid re-triggering
                    state.formData.toMutableMap().remove("_triggerOAuthFlow")
                    // Trigger navigation to OAuth WebView
                    _navigateToOAuth.emit(Unit)
                }
            }
        }
    }

    /**
     * ✅ Override nextStep to process the step normally
     * OAuth trigger is now handled by state observation above
     */
    override fun nextStep() {
        viewModelScope.launch {
            super.nextStep()
            // No need to check flag here - the init block observer handles it
        }
    }

    /**
     * Handle OAuth authorization code from WebView
     * Exchange code for access token
     * ✅ CHANGED: Made suspend function so caller can wait for completion
     */
    suspend fun handleOAuthCode(authorizationCode: String): Boolean {
        println("🔄 Exchanging OAuth code for token...")

        val result = authRepository.exchangeCodeForToken(authorizationCode)

        return result.fold(
            onSuccess = { tokenResponse ->
                println("✅ OAuth token received: ${tokenResponse.accessToken}")
                // Token is automatically saved in AuthRepository

                // ✅ Now call submitForm() to complete the login flow properly
                // This respects the architecture and uses the proper submission handling
                submitForm()

                // ✅ CRITICAL: Wait a bit for submitForm to process
                kotlinx.coroutines.delay(100)

                true // Return success
            },
            onFailure = { error ->
                println("❌ OAuth token exchange failed: ${error.message}")

                // Show error toast to user
                _showToastEvent.value = error.message ?: "فشل تسجيل الدخول. يرجى المحاولة مرة أخرى"

                // Navigate back or show error in UI
                // The user can try again from the login screen
                false // Return failure
            }
        )
    }
}
