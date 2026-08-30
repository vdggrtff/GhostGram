package com.ghostgram.app.di

import com.ghostgram.core.network.di.networkModule
import com.ghostgram.core.tdlib.di.tdlibModule
import com.ghostgram.data.di.dataModule
import com.ghostgram.domain.di.domainModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(
            networkModule,
            tdlibModule,
            dataModule,
            domainModule,
            presentationModule
        )
    }
}