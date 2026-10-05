package com.example.comp90018.ui.screens.login

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.comp90018.ui.theme.ForestDark
import com.example.comp90018.ui.theme.TrailwiseTheme

@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onContinueAsGuest: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val fieldShape = RoundedCornerShape(14.dp)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 48.dp),
    ) {
        // Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(ForestDark, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "OA",
                style = type.titleLarge,
                color = colors.onPrimary,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Outdoor Activity",
                style = type.headlineSmall,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = "& Health",
                style = type.headlineSmall,
                color = colors.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Track outdoor activity with context-aware health and weather guidance.",
                style = type.bodySmall,
                color = colors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))

        // Form
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Welcome back",
                style = type.titleLarge,
                color = colors.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sign in to continue, or use Guest Mode for a local demo.",
                style = type.bodySmall,
                color = colors.onSurface.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Email")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text("name@example.com", modifier = Modifier.alpha(0.6f))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            FieldLabel("Password")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = type.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = "Forgot password?",
                        style = type.labelMedium,
                        color = colors.primary,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { onSignIn(email, password) },
                enabled = !isLoading,
                shape = fieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (isLoading) "Signing in..." else "Sign in", style = type.titleMedium)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinueAsGuest,
                shape = fieldShape,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = "Continue as Guest",
                    style = type.titleMedium,
                    color = colors.onSurface,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Create account
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("New here?", style = type.labelLarge, color = colors.primary)
            TextButton(onClick = onCreateAccount) {
                Text("Create account", style = type.labelLarge, color = colors.primary)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Local-first note
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Local-first MVP",
                style = type.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.onPrimaryContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Guest Mode keeps routes, meals and health notes on this device.",
                style = type.bodySmall,
                color = colors.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun LoginScreenPreview() {
    TrailwiseTheme { LoginScreen() }
}