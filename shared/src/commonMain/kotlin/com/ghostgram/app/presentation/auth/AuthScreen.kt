package com.ghostgram.app.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthRoute(
    viewModel: AuthViewModel = koinViewModel(),
    onAuthSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Как только авторизовались — перекидываем на список чатов
    LaunchedEffect(state.step) {
        if (state.step == AuthStep.Authorized) {
            onAuthSuccess()
        }
    }

    AuthScreen(
        state = state,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun AuthScreen(
    state: AuthScreenState,
    onIntent: (AuthIntent) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GhostGRAM 👻",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AnimatedContent(targetState = state.step) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val title = when (step) {
                        AuthStep.WaitPhoneNumber -> "Введите номер телефона"
                        AuthStep.WaitCode -> "Введите код из Telegram"
                        AuthStep.WaitPassword -> "Введите облачный пароль"
                        AuthStep.Authorized -> "Вход выполнен!"
                    }

                    Text(text = title, style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (step != AuthStep.Authorized) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = { onIntent(AuthIntent.OnInputChanged(it)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (step == AuthStep.WaitPassword) KeyboardType.Password else KeyboardType.Phone
                            ),
                            visualTransformation = if (step == AuthStep.WaitPassword) PasswordVisualTransformation() else VisualTransformation.None,
                            placeholder = { Text(if (step == AuthStep.WaitPhoneNumber) "+79991234567" else "") },
                            enabled = !state.isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onIntent(AuthIntent.OnSubmit) },
                            enabled = !state.isLoading && state.inputText.isNotBlank()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Продолжить")
                            }
                        }
                    }
                }
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}