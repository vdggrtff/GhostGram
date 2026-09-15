package com.ghostgram.domain.di

import SessionManager
import entity.LocalSettingsManager
import org.koin.dsl.module
import usecase.GenerateCatchUpSummaryUseCase
import usecase.GenerateChatSummaryUseCase
import usecase.GenerateSmartRepliesUseCase

val domainModule = module {
    single {
        SessionManager( appStorage = get(), sessionFactory = get())
    }

    single { LocalSettingsManager(appStorage = get()) }

    // factory означает, что каждый раз при запросе будет создаваться новый экземпляр UseCase
    factory { GenerateChatSummaryUseCase(sessionManager = get(), aiRepository = get()) }
    factory { GenerateSmartRepliesUseCase(sessionManager = get(), aiRepository = get()) }
    factory { GenerateCatchUpSummaryUseCase(sessionManager = get(), aiRepository = get()) }
}