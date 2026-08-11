package com.studenthub.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.studenthub.app.ui.theme.glassMorphism

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit, 
    onGoRegister: () -> Unit, 
    onGoForgotPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .glassMorphism(), // Glassmorphism Theme
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Student Hub", style = MaterialTheme.typography.headlineMedium)
            Text("Web-Synced Login", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = email, 
                onValueChange = { email = it }, 
                label = { Text("Email Address") }, 
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = password, 
                onValueChange = { password = it }, 
                label = { Text("Password") }, 
                visualTransformation = PasswordVisualTransformation(), 
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onLoggedIn, modifier = Modifier.fillMaxWidth()) { 
                Text("Login") 
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = onGoRegister) { 
                Text("Don't have an account? Sign up") 
            }
            TextButton(onClick = onGoForgotPassword) { 
                Text("Forgot Password?") 
            }
        }
    }
}
