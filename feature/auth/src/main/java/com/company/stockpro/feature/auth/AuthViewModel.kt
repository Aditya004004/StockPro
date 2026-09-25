package com.company.stockpro.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.stockpro.core.domain.util.DataResult
import com.company.stockpro.core.domain.util.UiText
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val confirmPassword: String = "",
    val plantName: String = "",
    val agreedToTerms: Boolean = true,
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // If already signed in, skip login
        try {
            if (auth.currentUser != null) {
                _uiState.update { it.copy(isAuthenticated = true) }
            }
        } catch (e: Exception) {
            // Safe offline fallback
        }
    }

    fun toggleAuthMode() {
        _uiState.update { it.copy(isSignUpMode = !it.isSignUpMode, errorMessage = null) }
    }

    fun sendPasswordReset(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            val msg = "Please enter your email to reset password."
            _uiState.update { it.copy(errorMessage = msg) }
            onError(msg)
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onSuccess("Password reset link sent to $email")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val msg = e.localizedMessage ?: "Failed to send reset email."
                _uiState.update { it.copy(errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun onPlantNameChanged(plant: String) {
        _uiState.update { it.copy(plantName = plant, errorMessage = null) }
    }

    fun onAgreedToTermsChanged(agreed: Boolean) {
        _uiState.update { it.copy(agreedToTerms = agreed, errorMessage = null) }
    }

    fun submit() {
        if (_uiState.value.isSignUpMode) {
            signUp()
        } else {
            signIn()
        }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(state.email.trim(), state.password)
                    .await()
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Authentication failed.",
                    )
                }
            }
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name.") }
            return
        }
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email address is required.") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }
        if (state.plantName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Plant or company name is required.") }
            return
        }
        if (!state.agreedToTerms) {
            _uiState.update { it.copy(errorMessage = "Please accept the Terms of Service to continue.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(state.email.trim(), state.password).await()
                result.user?.let { user ->
                    try {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(state.fullName.trim())
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        // Profile display name fallback
                    }
                }
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Account creation failed.",
                    )
                }
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                user?.delete()?.await()
                _uiState.update { AuthUiState() }
                onSuccess()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                onError(e.localizedMessage ?: "Failed to delete account.")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update {
            AuthUiState() // Reset to default
        }
    }
}
