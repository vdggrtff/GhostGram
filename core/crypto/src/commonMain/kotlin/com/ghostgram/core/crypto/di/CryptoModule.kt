package com.ghostgram.core.crypto.di

import com.ghostgram.core.crypto.CryptoLayer
import org.koin.dsl.module

val cryptoModule = module {
    single { CryptoLayer() }
}