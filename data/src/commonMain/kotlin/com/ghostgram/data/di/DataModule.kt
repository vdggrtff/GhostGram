package com.ghostgram.data.di

import com.ghostgram.core.database.GhostDatabase
import com.ghostgram.data.repository.AiRepositoryImpl
import com.ghostgram.data.repository.AuthRepositoryImpl
import com.ghostgram.data.repository.ChatRepositoryImpl
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import repository.AiRepository
import repository.AuthRepository
import repository.ChatRepository

val dataModule = module {
    // Указываем, что при запросе интерфейса (AiRepository) нужно отдать реализацию (AiRepositoryImpl)
    single<AiRepository> { AiRepositoryImpl(geminiClient = get()) }
    factory<ChatRepository> { (accountId: String) ->
        ChatRepositoryImpl(
            // Прокидываем accountId в TDLib и Dao
            tdlibClient = get { parametersOf(accountId) },
            messageDao = get { parametersOf(accountId) },
            cryptoLayer = get()
        )
    }
    factory<AuthRepository> { (accountId: String) ->
        AuthRepositoryImpl(
            tdlibClient = get { parametersOf(accountId) }
        )
    }

    factory { (accountId: String) ->
        get<GhostDatabase>(parameters = { parametersOf(accountId) }).messageDao()
    }
}