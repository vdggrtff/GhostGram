package com.ghostgram.core.tdlib.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformTdlibModule(): Module = module {
    single<TdlibConfig> {
        TdlibConfig(databasePath = "ghostgram_tdlib_data")
    }
}