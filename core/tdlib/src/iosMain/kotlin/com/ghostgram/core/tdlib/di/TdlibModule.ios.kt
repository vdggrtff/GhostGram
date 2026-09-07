package com.ghostgram.core.tdlib.di

import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual fun platformTdlibModule(): Module = module {
    factory<TdlibConfig> { (accountId: String) ->
        val path = NSHomeDirectory() + "/ghostgram_tdlib_data_$accountId"
        TdlibConfig(databasePath = path)
    }
    /*single<TdlibConfig> {
        val path = NSHomeDirectory() + "/ghostgram_tdlib_data"
        TdlibConfig(databasePath = path)
    }*/
}