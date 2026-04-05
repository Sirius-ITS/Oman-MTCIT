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

    // ✅ NEW: Track previous OAuth trigger state to prevent re-triggering
    private var previousOAuthTriggerState = false

    // OAuth URLs
    companion object {
        const val OAUTH_REDIRECT_URI = "https://mtimedev.mtcit.gov.om/auth/callback"

        /**
         * Builds a fresh OAuth authorization URL matching the web app's parameters.
         * Uses PKCE (S256), a random nonce, response_mode=fragment, and prompt=login.
         */
        fun buildAuthUrl(): String {
            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val nonce = java.util.UUID.randomUUID().toString().replace("-", "")
            val state = java.util.UUID.randomUUID().toString()
            // Store verifier so token exchange can use it
            _lastCodeVerifier = codeVerifier
            return buildString {
                append("https://mtimedevidp.mtcit.gov.om/realms/oman/protocol/openid-connect/auth")
                append("?client_id=front")
                append("&redirect_uri=${android.net.Uri.encode(OAUTH_REDIRECT_URI)}")
                append("&state=${android.net.Uri.encode(state)}")
                append("&response_mode=fragment")
                append("&response_type=code")
                append("&scope=openid")
                append("&nonce=${android.net.Uri.encode(nonce)}")
                append("&prompt=login")
                append("&code_challenge=${android.net.Uri.encode(codeChallenge)}")
                append("&code_challenge_method=S256")
            }
        }

        private var _lastCodeVerifier: String = ""
        val lastCodeVerifier: String get() = _lastCodeVerifier

        private fun generateCodeVerifier(): String {
            val bytes = ByteArray(32)
            java.security.SecureRandom().nextBytes(bytes)
            return android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
        }

        private fun generateCodeChallenge(verifier: String): String {
            val bytes = verifier.toByteArray(Charsets.US_ASCII)
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            return android.util.Base64.encodeToString(
                digest,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
        }
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
                val currentOAuthTriggerState = state.formData["_triggerOAuthFlow"] == "true"

                // ✅ CRITICAL: Only trigger if flag changed from false to true
                // This prevents re-triggering when formData changes for other reasons
                if (currentOAuthTriggerState && !previousOAuthTriggerState) {
                    println("🚀 LoginViewModel: OAuth trigger flag detected in uiState (changed from false to true)")

                    // Update previous state
                    previousOAuthTriggerState = true

                    // ✅ Clear the flag from the strategy's accumulated data to avoid re-triggering
                    (currentStrategy as? LoginStrategy)?.getFormData()?.let { formData ->
                        if (formData is MutableMap) {
                            formData.remove("_triggerOAuthFlow")
                            println("✅ OAuth trigger flag cleared from strategy")
                        }
                    }

                    // Trigger navigation to OAuth WebView
                    _navigateToOAuth.emit(Unit)
                } else if (!currentOAuthTriggerState && previousOAuthTriggerState) {
                    // Reset previous state when flag is cleared
                    previousOAuthTriggerState = false
                    println("🔄 OAuth trigger flag cleared, resetting previous state")
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

        val result = authRepository.exchangeCodeForToken(authorizationCode, lastCodeVerifier)

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

    /**
     * ✅ NEW: Reset OAuth trigger flags when user returns from OAuth without completing login
     */
    fun resetOAuthFlags() {
        viewModelScope.launch {
            // ✅ CRITICAL: Reset strategy first (this clears registrationMethod and OAuth flags)
            (currentStrategy as? LoginStrategy)?.resetOAuthTrigger()
            previousOAuthTriggerState = false
            println("✅ OAuth flags reset in LoginViewModel")

            // ✅ CRITICAL: Reload steps from strategy (will return only step 0 since registrationMethod is cleared)
            val steps = currentStrategy?.getSteps() ?: emptyList()
            println("🔄 Reloaded steps after reset: ${steps.size} steps")

            // ✅ CRITICAL: Force complete UI state reset
            // This will clear formData, fieldErrors, and reload step 0
            updateUiState { currentState ->
                currentState.copy(
                    currentStep = 0,
                    steps = steps, // Update steps to reflect cleared selection
                    formData = emptyMap(), // Clear all form data (including registrationMethod)
                    fieldErrors = emptyMap(),
                    isLoading = false,
                    canProceedToNext = false // Disable Next button until user selects again
                )
            }

            println("✅ UI state completely reset - user must select registration method again")
        }
    }
}
