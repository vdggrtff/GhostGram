package com.ghostgram.core.tdlib.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformTdlibModule(): Module = module {
    single<TdlibConfig> { // 💥 Явно указываем тип
        val context = androidContext()
        val path = context.filesDir.absolutePath + "/ghostgram_tdlib_data"
        TdlibConfig(databasePath = path)
    }
}