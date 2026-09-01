package com.ghostgram.data.di

import com.ghostgram.data.repository.AiRepositoryImpl
import com.ghostgram.data.repository.AuthRepositoryImpl
import com.ghostgram.data.repository.ChatRepositoryImpl
import org.koin.dsl.module
import repository.AiRepository
import repository.AuthRepository
import repository.ChatRepository

val dataModule = module {
    // Указываем, что при запросе интерфейса (AiRepository) нужно отдать реализацию (AiRepositoryImpl)
    single<AiRepository> { AiRepositoryImpl(geminiClient = get()) }
    single<ChatRepository> { ChatRepositoryImpl(tdlibClient = get(), messageDao = get()) }
    single<AuthRepository> { AuthRepositoryImpl(tdlibClient = get()) }
}