package com.ghostgram.domain.di

import AccountSession
import SessionManager
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import usecase.GenerateCatchUpSummaryUseCase
import usecase.GenerateChatSummaryUseCase
import usecase.GenerateSmartRepliesUseCase

val domainModule = module {
    /*single {
        SessionManager(
            sessionFactory = { accountId ->
                AccountSession(
                    accountId = accountId,
                    // 💥 ВОТ ОНО! Строго передаем accountId в репозитории!
                    chatRepository = get { parametersOf(accountId) },
                    authRepository = get { parametersOf(accountId) }
                )
            }
        )
    }*/

    single {
        SessionManager( appStorage = get(), sessionFactory = get())
    }

    // factory означает, что каждый раз при запросе будет создаваться новый экземпляр UseCase
    factory { GenerateChatSummaryUseCase(sessionManager = get(), aiRepository = get()) }
    factory { GenerateSmartRepliesUseCase(sessionManager = get(), aiRepository = get()) }
    factory { GenerateCatchUpSummaryUseCase(sessionManager = get(), aiRepository = get()) }
}