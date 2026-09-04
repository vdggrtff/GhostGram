package com.ghostgram.domain.di

import org.koin.dsl.module
import usecase.GenerateChatSummaryUseCase

val domainModule = module {
    // factory означает, что каждый раз при запросе будет создаваться новый экземпляр UseCase
    factory { GenerateChatSummaryUseCase(chatRepository = get(), aiRepository = get()) }
}