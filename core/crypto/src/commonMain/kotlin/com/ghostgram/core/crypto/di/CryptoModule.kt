package com.ghostgram.core.crypto.di

import com.ghostgram.core.crypto.CryptoLayer
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val cryptoModule = module {
    factory { (accountId: String) ->
        CryptoLayer(databasePath = "ghostgram_crypto_$accountId")
    }
}