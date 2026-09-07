package com.ghostgram.core.tdlib.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformTdlibModule(): Module = module {
    factory<TdlibConfig> { (accountId: String) ->
        val context = androidContext()
        // 💥 Разные папки кэша для разных аккаунтов!
        val path = context.filesDir.absolutePath + "/ghostgram_tdlib_data_$accountId"
        TdlibConfig(databasePath = path)
    }
    /*single<TdlibConfig> { // 💥 Явно указываем тип
        val context = androidContext()
        val path = context.filesDir.absolutePath + "/ghostgram_tdlib_data"
        TdlibConfig(databasePath = path)
    }*/
}