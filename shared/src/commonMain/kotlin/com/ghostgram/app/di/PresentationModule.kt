package com.ghostgram.app.di

import com.ghostgram.app.presentation.auth.AuthViewModel
import com.ghostgram.app.presentation.chats.ChatListViewModel
import com.ghostgram.app.presentation.chats.chat_details.ChatDetailsViewModel
import com.ghostgram.app.presentation.chats.chat_profile.ChatProfileViewModel
import com.ghostgram.app.presentation.contacts.ContactsViewModel
import com.ghostgram.app.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
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
            searchMessagesSemanticUseCase = get(),
            sessionManager = get()
        )
    }

    viewModel { AuthViewModel(sessionManager = get()) }

    viewModel { SettingsViewModel(sessionManager = get(), settingsManager = get()) }

    viewModel { ContactsViewModel(sessionManager = get()) }

    viewModel { ChatProfileViewModel(savedStateHandle =  get(), sessionManager = get()) }
}