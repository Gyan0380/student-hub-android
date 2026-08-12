package com.studenthub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studenthub.app.ui.viewmodel.AuthUiState
import com.studenthub.app.ui.viewmodel.AuthViewModel

private val classOptions = (1..12).map { "Class $it" } + "12th Pass / College"

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var classLevel by remember { mutableStateOf(classOptions.first()) }
    var classMenuExpanded by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            onRegisterSuccess()
            viewModel.resetState()
        }
    }

    fun fieldColors() = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(24.dp))

            Text("FULL NAME", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = fullName, onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("USERNAME", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("PASSWORD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("DATE OF BIRTH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = dob, onValueChange = { dob = it },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("SCHOOL NAME", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = schoolName, onValueChange = { schoolName = it },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(16.dp), colors = fieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("CLASS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(
                    onClick = { classMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(classLevel, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                    classOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            classLevel = option
                            classMenuExpanded = false
                        })
                    }
                }
            }

            if (state is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text((state as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.register(
                        username.trim(), password, fullName.trim(), dob.trim(), classLevel, schoolName.trim()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = state !is AuthUiState.Loading
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("CREATE ACCOUNT", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already have an account? Log in", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
