package com.ghostgram.app.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostgram.app.ui.theme.GhostBackground
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthRoute(
    viewModel: AuthViewModel = koinViewModel(),
    onAuthSuccess: () -> Unit,
    onNavigateBack: () -> Unit
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
        onIntent = { intent -> viewModel.onIntent(intent, onNavigateBack) }
    )
}

@Composable
fun AuthScreen(
    state: AuthScreenState,
    onIntent: (AuthIntent) -> Unit
) {
    Scaffold(
        containerColor = GhostBackground, // Из твоей темы
        topBar = {
            /*TopAppBar(
                title = { Text("Добавление аккаунта", color = Color.White, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostBackground),
                navigationIcon = {
                    // 💥 КНОПКА ОТМЕНЫ (НАЗАД)
                    IconButton(onClick = { onIntent(AuthIntent.OnCancelClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                }
            )*/
            TopAppBar(
                title = { Text(if (state.isFirstAccount) "Вход в GhostGRAM" else "Добавление аккаунта", color = Color.White, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GhostBackground),
                // 💥 РИСУЕМ КНОПКУ "НАЗАД" ТОЛЬКО ЕСЛИ ЕСТЬ КУДА ВОЗВРАЩАТЬСЯ!
                navigationIcon = if (!state.isFirstAccount) {
                    {
                        IconButton(onClick = { onIntent(AuthIntent.OnCancelClick) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                        }
                    }
                } else {
                    {} // Пустая заглушка для первого входа
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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