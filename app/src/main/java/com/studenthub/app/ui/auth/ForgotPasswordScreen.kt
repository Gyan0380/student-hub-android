package com.studenthub.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ForgotPasswordScreen(
    onGoLogin: () -> Unit,
    vm: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reset password", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Apna email daalo, hum reset link bhej denge.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        when (state) {
            is AuthUiState.Error -> Text(
                (state as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
            is AuthUiState.ResetEmailSent -> Text(
                "Reset email bhej diya — apna inbox check karo.",
                color = MaterialTheme.colorScheme.primary
            )
            else -> {}
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { vm.sendPasswordReset(email) },
            enabled = state !is AuthUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state is AuthUiState.Loading) "Bhej rahe hain…" else "Send reset email") }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { vm.resetState(); onGoLogin() }) { Text("Back to login") }
    }
}
