package com.ghostgram.app.di

import AccountSession
import SessionManager
import com.ghostgram.app.presentation.auth.AuthViewModel
import com.ghostgram.app.presentation.chats.ChatListViewModel
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsViewModel
import com.ghostgram.app.presentation.settings.SettingsViewModel
import com.ghostgram.core.database.GhostDatabase
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.data.repository.AuthRepositoryImpl
import com.ghostgram.data.repository.ChatRepositoryImpl
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        ChatListViewModel(
            generateChatSummaryUseCase = get(),
            sessionManager = get()
        )
    }

    viewModel {
        ChatDetailsViewModel(
            savedStateHandle = get(),
            generateSmartRepliesUseCase = get(),
            generateCatchUpSummaryUseCase = get(),
            sessionManager = get()
        )
    }

    viewModel { AuthViewModel(sessionManager = get()) }

    viewModel { SettingsViewModel(sessionManager = get()) }
}