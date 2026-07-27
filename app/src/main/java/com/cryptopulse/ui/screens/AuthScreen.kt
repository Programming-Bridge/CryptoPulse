package com.cryptopulse.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cryptopulse.ui.components.CryptoButton
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.cryptopulse.ui.theme.CryptoPulseTheme
import com.cryptopulse.ui.theme.Typography
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Use LocalInspectionMode to avoid initializing Firebase in Previews
    val isPreview = LocalInspectionMode.current
    val auth = remember { if (isPreview) null else FirebaseAuth.getInstance() }
    
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Text(
                text = when {
                    isForgotPasswordMode -> "Reset Password"
                    isSignUp -> "Create Account"
                    else -> "Welcome Back!"
                },
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontSize = if (isSignUp || isForgotPasswordMode) Typography.headlineLarge.fontSize else Typography.displayLarge.fontSize
            )
            
            Text(
                text = when {
                    isForgotPasswordMode -> "Enter your email to receive a reset link."
                    isSignUp -> "Start your crypto journey."
                    else -> "Securely access your portfolio."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            AuthForm(
                isSignUp = isSignUp,
                isForgotPasswordMode = isForgotPasswordMode,
                onAuthClick = { email, password, name ->
                    if (auth == null) return@AuthForm // Skip Firebase calls in Preview mode
                    scope.launch {
                        loading = true
                        errorMessage = null
                        successMessage = null
                        try {
                            when {
                                isForgotPasswordMode -> {
                                    if (email.isEmpty()) {
                                        errorMessage = "Please enter your email address"
                                    } else {
                                        auth.sendPasswordResetEmail(email).await()
                                        successMessage = "Reset link sent! Check your inbox."
                                    }
                                }
                                isSignUp -> {
                                    val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                    // Save the display name immediately after signup
                                    if (name.isNotEmpty()) {
                                        val profileUpdates = userProfileChangeRequest {
                                            displayName = name
                                        }
                                        authResult.user?.updateProfile(profileUpdates)?.await()
                                    }
                                    onAuthSuccess()
                                }
                                else -> {
                                    auth.signInWithEmailAndPassword(email, password).await()
                                    onAuthSuccess()
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Operation failed"
                        } finally {
                            loading = false
                        }
                    }
                },
                onForgotPasswordClick = {
                    isForgotPasswordMode = true
                    errorMessage = null
                    successMessage = null
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val promptText = when {
                    isForgotPasswordMode -> "Remember your password? "
                    isSignUp -> "Already have an account? "
                    else -> "Don't have an account? "
                }
                val actionText = when {
                    isForgotPasswordMode -> "Sign In"
                    isSignUp -> "Sign In"
                    else -> "Sign Up"
                }

                Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { 
                    if (isForgotPasswordMode) {
                        isForgotPasswordMode = false
                        isSignUp = false
                    } else {
                        isSignUp = !isSignUp 
                    }
                    errorMessage = null
                    successMessage = null
                }) {
                    Text(
                        text = actionText,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AuthForm(
    isSignUp: Boolean,
    isForgotPasswordMode: Boolean,
    onAuthClick: (String, String, String) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (!isForgotPasswordMode) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        if (!isSignUp && !isForgotPasswordMode) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onForgotPasswordClick) {
                    Text(
                        text = "Forgot Password?",
                        style = Typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        CryptoButton(
            text = when {
                isForgotPasswordMode -> "Send Reset Link"
                isSignUp -> "Sign Up"
                else -> "Login"
            },
            onClick = { onAuthClick(email, password, name) },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    CryptoPulseTheme {
        AuthScreen(onAuthSuccess = {})
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    CryptoPulseTheme {
        // Just previewing the screen; interactivity is limited in standard @Preview
        AuthScreen(onAuthSuccess = {})
    }
}
