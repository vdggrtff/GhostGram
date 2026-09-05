package com.ghostgram.app.di

import com.ghostgram.app.presentation.auth.AuthViewModel
import com.ghostgram.app.presentation.chats.ChatListViewModel
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        ChatListViewModel(
            chatRepository = get(),
            generateChatSummaryUseCase = get()
        )
    }

    viewModel {
        ChatDetailsViewModel(
            savedStateHandle = get(),
            chatRepository = get(),
            generateSmartRepliesUseCase = get(),
            generateCatchUpSummaryUseCase = get()
        )
    }

    viewModel { AuthViewModel(authRepository = get()) }
}