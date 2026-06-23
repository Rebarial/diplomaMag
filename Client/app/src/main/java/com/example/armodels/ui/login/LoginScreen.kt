package com.example.armodels.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.armodels.navigation.Screen

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToLibrary) {
        if (state.navigateToLibrary) {
            viewModel.onNavigated()

            // Сигналим предыдущему экрану (Library или Generate) что логин прошёл
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("logged_in", true)

            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "AR Models",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Логин") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = viewModel::login,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Войти")
                }

                TextButton(
                    onClick = viewModel::register,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зарегистрироваться")
                }
            }
        }
    }
}