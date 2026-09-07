package com.ghostgram.app.di

import com.ghostgram.core.crypto.di.cryptoModule
import com.ghostgram.core.database.di.databaseModule
import com.ghostgram.core.network.di.networkModule
import com.ghostgram.core.tdlib.di.tdlibModule
import com.ghostgram.data.di.dataModule
import com.ghostgram.domain.di.domainModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            networkModule,
            tdlibModule,
            dataModule,
            domainModule,
            presentationModule,
            databaseModule,
            cryptoModule
        )
    }
}