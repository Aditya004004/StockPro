package com.company.stockpro.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.stockpro.core.designsystem.component.ButtonVariant
import com.company.stockpro.core.designsystem.component.StockProButton
import com.company.stockpro.core.designsystem.component.StockProTextField
import com.company.stockpro.core.designsystem.theme.Radius
import com.company.stockpro.core.designsystem.theme.Spacing
import com.company.stockpro.core.designsystem.theme.StockProColors

@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticated()
        }
    }

    AuthScreen(
        uiState = uiState,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSubmitClick = viewModel::submit,
        onToggleModeClick = viewModel::toggleAuthMode,
        onDemoSignInClick = viewModel::signInAsDemo,
        modifier = modifier,
    )
}

@Composable
private fun AuthScreen(
    uiState: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onDemoSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockProColors.Canvas)
            .padding(horizontal = Spacing.screenGutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(Radius.lg)
                .background(StockProColors.BrandMark),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Factory,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x3))

        Row {
            Text(
                text = "Stock",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = StockProColors.TextPrimary,
            )
            Text(
                text = "Pro",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = StockProColors.BrandMark,
            )
        }

        Text(
            text = "Manufacturing Inventory Management",
            style = MaterialTheme.typography.bodyMedium,
            color = StockProColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(Spacing.x6))

        // Email Field
        StockProTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            label = "Email",
            placeholder = "name@company.com",
        )

        Spacer(modifier = Modifier.height(Spacing.x3))

        // Password Field
        StockProTextField(
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = "Password",
            placeholder = "Enter your password",
        )

        // Error Message
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(Spacing.x2))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = StockProColors.Danger,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x4))

        // Submit Button (Sign In / Create Account)
        StockProButton(
            text = when {
                uiState.isLoading && uiState.isSignUpMode -> "Creating account…"
                uiState.isLoading -> "Signing in…"
                uiState.isSignUpMode -> "Create Account"
                else -> "Sign In"
            },
            onClick = onSubmitClick,
            variant = ButtonVariant.PRIMARY,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.x2))

        // Toggle Sign In / Create Account
        androidx.compose.material3.TextButton(
            onClick = onToggleModeClick,
            enabled = !uiState.isLoading,
        ) {
            Text(
                text = if (uiState.isSignUpMode) "Already have an account? Sign In" else "Don't have an account? Create one",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = StockProColors.BrandMark,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x1))

        // Demo / Offline Mode Button
        StockProButton(
            text = "Continue as Demo / Offline Mode",
            onClick = onDemoSignInClick,
            variant = ButtonVariant.SECONDARY,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(Spacing.x3))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = StockProColors.BrandMark,
                strokeWidth = 2.dp,
            )
        }
    }
}
