package com.ghostgram.data.di

import AccountSession
import com.ghostgram.core.crypto.CryptoLayer
import com.ghostgram.core.database.GhostDatabase
import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.di.TdlibConfig
import com.ghostgram.data.repository.AiRepositoryImpl
import com.ghostgram.data.repository.AuthRepositoryImpl
import com.ghostgram.data.repository.ChatRepositoryImpl
import com.ghostgram.data.repository.ContactRepositoryImpl
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import repository.AiRepository
import repository.AuthRepository
import repository.ChatRepository

val dataModule = module {
    // Указываем, что при запросе интерфейса (AiRepository) нужно отдать реализацию (AiRepositoryImpl)
    single<AiRepository> { AiRepositoryImpl(geminiClient = get()) }

    single<(String) -> AccountSession> {
        { accountId ->
            val tdlibClient: TelegramFlowClient = get { parametersOf(accountId) }
            val database: GhostDatabase = get { parametersOf(accountId) }
            val tdlibConfig: TdlibConfig = get { parametersOf(accountId) }

            AccountSession(
                accountId = accountId,
                // 💥 Отдаем ОДИН И ТОТ ЖЕ tdlibClient обоим репозиториям!
                chatRepository = ChatRepositoryImpl(
                    tdlibClient = tdlibClient,
                    messageDao = database.messageDao(),
                    cryptoLayer = CryptoLayer(databasePath = tdlibConfig.databasePath)
                ),
                authRepository = AuthRepositoryImpl(
                    tdlibClient = tdlibClient
                ),
                contactRepository = ContactRepositoryImpl( // 💥 Добавили контакт-репозиторий!
                    tdlibClient = tdlibClient
                )
            )
        }
    }

    factory<ChatRepository> { (accountId: String) ->
        val tdlibConfig: TdlibConfig = get { parametersOf(accountId) }
        val database: GhostDatabase = get { parametersOf(accountId) }
        ChatRepositoryImpl(
            // Прокидываем accountId в TDLib и Dao
            tdlibClient = get { parametersOf(accountId) },
            messageDao = database.messageDao(),
            cryptoLayer = CryptoLayer(databasePath = tdlibConfig.databasePath)
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