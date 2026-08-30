package com.ghostgram.core.tdlib.di

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.TelegramNativeClient
import org.koin.dsl.module

val tdlibModule = module {
    single { TelegramNativeClient() }
    single { TelegramFlowClient(nativeClient = get()) }
}