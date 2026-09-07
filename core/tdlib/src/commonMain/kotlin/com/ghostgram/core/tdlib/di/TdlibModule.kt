package com.ghostgram.core.tdlib.di

import com.ghostgram.core.tdlib.TelegramFlowClient
import com.ghostgram.core.tdlib.TelegramNativeClient
import org.koin.core.module.Module
import org.koin.dsl.module

data class TdlibConfig(val databasePath: String)

expect fun platformTdlibModule(): Module

val tdlibModule = module {
    includes(platformTdlibModule())
    factory { TelegramNativeClient() }
    factory { (accountId: String) ->
        TelegramFlowClient(
            nativeClient = get(),
            config = get { org.koin.core.parameter.parametersOf(accountId) }
        )
    }
}