package com.ghostgram.app.di

import AppStorageConfig
import com.ghostgram.core.crypto.di.cryptoModule
import com.ghostgram.core.database.di.databaseModule
import com.ghostgram.core.network.di.networkModule
import com.ghostgram.core.tdlib.di.tdlibModule
import com.ghostgram.data.di.dataModule
import com.ghostgram.domain.di.domainModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.plugin.module.dsl.module

fun initKoin(appStoragePath: String, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            module { single { AppStorageConfig(appStoragePath) } },
            networkModule,
            tdlibModule,
            dataModule,
            domainModule,
            presentationModule,
            databaseModule,
        )
    }
}