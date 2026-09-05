package com.ghostgram.domain.di

import org.koin.dsl.module
import usecase.GenerateCatchUpSummaryUseCase
import usecase.GenerateChatSummaryUseCase
import usecase.GenerateSmartRepliesUseCase

val domainModule = module {
    // factory означает, что каждый раз при запросе будет создаваться новый экземпляр UseCase
    factory { GenerateChatSummaryUseCase(chatRepository = get(), aiRepository = get()) }

    factory { GenerateSmartRepliesUseCase(chatRepository = get(), aiRepository = get()) }

    factory { GenerateCatchUpSummaryUseCase(chatRepository = get(), aiRepository = get()) }
}