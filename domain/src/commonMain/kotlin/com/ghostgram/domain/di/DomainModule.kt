package com.ghostgram.domain.di

import org.koin.dsl.module
import session.SessionManager
import usecase.GenerateCatchUpSummaryUseCase
import usecase.GenerateChatSummaryUseCase
import usecase.GenerateSmartRepliesUseCase
import usecase.SearchMessagesSemanticUseCase

val domainModule = module {
    single {
        SessionManager( appStorage = get(), sessionFactory = get())
    }

    // factory означает, что каждый раз при запросе будет создаваться новый экземпляр UseCase
    factory { GenerateChatSummaryUseCase(sessionManager = get(), aiRepository = get()) }
    factory { GenerateSmartRepliesUseCase(sessionManager = get(), aiRepository = get()) }
    factory { GenerateCatchUpSummaryUseCase(sessionManager = get(), aiRepository = get()) }
    factory { SearchMessagesSemanticUseCase(get()) }
}