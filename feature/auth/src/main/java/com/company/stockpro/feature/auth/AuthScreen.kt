package com.company.stockpro.feature.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val BrandGreen = Color(0xFF00875A)
private val BrandDarkGreen = Color(0xFF006D44)
private val BrandNavy = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF475569)
private val TextMuted = Color(0xFF64748B)
private val FieldBorder = Color(0xFFE2E8F0)
private val FieldBg = Color(0xFFF8FAFC)
private val CanvasMint = Color(0xFFF7FAF8)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthenticated()
        }
    }

    AuthScreen(
        uiState = uiState,
        onFullNameChanged = viewModel::onFullNameChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onPlantNameChanged = viewModel::onPlantNameChanged,
        onAgreedToTermsChanged = viewModel::onAgreedToTermsChanged,
        onSubmitClick = viewModel::submit,
        onToggleModeClick = viewModel::toggleAuthMode,
        onForgotPasswordClick = {
            viewModel.sendPasswordReset(
                onSuccess = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun AuthScreen(
    uiState: AuthUiState,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onPlantNameChanged: (String) -> Unit,
    onAgreedToTermsChanged: (Boolean) -> Unit,
    onSubmitClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasMint)
    ) {
        // Decorative soft green blurred circles in background
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(x = (-40).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color(0x35A7E3B2))
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 30.dp)
                .clip(CircleShape)
                .background(Color(0x35B2E8D0))
                .blur(60.dp)
        )

        // Bottom Industrial Warehouse Illustration
        Image(
            painter = painterResource(id = R.drawable.login_warehouse_illustration),
            contentDescription = "Warehouse illustration",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            contentScale = ContentScale.FillWidth
        )

        // Main Foreground Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // App Icon Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandDarkGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Factory,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // StockPro Brand Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Stock",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Pro",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                    letterSpacing = (-0.5).sp,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Manufacturing Inventory Management",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Heading & Subtitle
            Text(
                text = if (uiState.isSignUpMode) "Create Your Account" else "Welcome Back!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrandNavy,
                letterSpacing = (-0.5).sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (uiState.isSignUpMode) {
                    "Get started and take control of your\ninventory and operations."
                } else {
                    "Sign in to manage your inventory and keep your operations running smoothly."
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Login / Signup Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, FieldBorder),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    if (uiState.isSignUpMode) {
                        // ─── Sign Up Fields ───

                        // Full Name
                        OutlinedTextField(
                            value = uiState.fullName,
                            onValueChange = onFullNameChanged,
                            placeholder = {
                                Text(
                                    text = "Full Name",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Email Address
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = onEmailChanged,
                            placeholder = {
                                Text(
                                    text = "Email Address",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChanged,
                            placeholder = {
                                Text(
                                    text = "Password",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Confirm Password
                        OutlinedTextField(
                            value = uiState.confirmPassword,
                            onValueChange = onConfirmPasswordChanged,
                            placeholder = {
                                Text(
                                    text = "Confirm Password",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Plant / Company Name
                        OutlinedTextField(
                            value = uiState.plantName,
                            onValueChange = onPlantNameChanged,
                            placeholder = {
                                Text(
                                    text = "Plant / Company Name",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Apartment,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSubmitClick()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Checkbox for Terms of Service & Privacy Policy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onAgreedToTermsChanged(!uiState.agreedToTerms) },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (uiState.agreedToTerms) BrandGreen else Color.White)
                                    .border(
                                        BorderStroke(
                                            1.5.dp,
                                            if (uiState.agreedToTerms) BrandGreen else Color(0xFFCBD5E1)
                                        ),
                                        RoundedCornerShape(5.dp)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uiState.agreedToTerms) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Agreed",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            val termsAnnotated = buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(style = SpanStyle(color = BrandGreen, fontWeight = FontWeight.SemiBold)) {
                                    append("Terms of Service")
                                }
                                append(" and ")
                                withStyle(style = SpanStyle(color = BrandGreen, fontWeight = FontWeight.SemiBold)) {
                                    append("Privacy Policy")
                                }
                            }

                            Text(
                                text = termsAnnotated,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp,
                            )
                        }

                    } else {
                        // ─── Sign In Fields ───

                        // Email Field
                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = onEmailChanged,
                            placeholder = {
                                Text(
                                    text = "Enter your email",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = onPasswordChanged,
                            placeholder = {
                                Text(
                                    text = "Enter your password",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onSubmitClick()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FieldBg,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = FieldBorder,
                                focusedTextColor = BrandNavy,
                                unfocusedTextColor = BrandNavy,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Forgot Password
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandGreen,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onForgotPasswordClick,
                                    )
                                    .padding(vertical = 4.dp),
                            )
                        }
                    }

                    // Error Message Banner
                    AnimatedVisibility(
                        visible = uiState.errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        uiState.errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = ErrorRed,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSubmitClick()
                        },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White,
                            disabledContainerColor = BrandGreen.copy(alpha = 0.6f),
                            disabledContentColor = Color.White.copy(alpha = 0.8f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (uiState.isSignUpMode) "Creating account…" else "Signing in…",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = if (uiState.isSignUpMode) "Create Account" else "Sign In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFCBD5E1),
                            thickness = 1.dp,
                        )
                        Text(
                            text = "OR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFCBD5E1),
                            thickness = 1.dp,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle Mode (Sign In <-> Create Account)
                    val annotatedText = buildAnnotatedString {
                        if (uiState.isSignUpMode) {
                            append("Already have an account? ")
                            pushStringAnnotation(tag = "TOGGLE", annotation = "TOGGLE")
                            withStyle(
                                style = SpanStyle(
                                    color = BrandGreen,
                                    fontWeight = FontWeight.Bold,
                                )
                            ) {
                                append("Sign In")
                            }
                            pop()
                        } else {
                            append("Don't have an account? ")
                            pushStringAnnotation(tag = "TOGGLE", annotation = "TOGGLE")
                            withStyle(
                                style = SpanStyle(
                                    color = BrandGreen,
                                    fontWeight = FontWeight.Bold,
                                )
                            ) {
                                append("Create one")
                            }
                            pop()
                        }
                    }

                    Text(
                        text = annotatedText,
                        fontSize = 13.5.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleModeClick,
                            )
                            .padding(vertical = 4.dp),
                    )
                }
            }

            // Padding at bottom so scrollable content sits nicely above illustration
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
