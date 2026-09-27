package com.ghostgram.app

import android.app.Application
import android.content.Context
import com.ghostgram.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class GhostGramApp: Application() {

    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this

        //ЗАПУСКАЕМ KOIN И ПЕРЕДАЕМ ЕМУ НАШУ БАЗУ И DATASTORE
        initKoin(appStoragePath = this@GhostGramApp.filesDir.absolutePath) {
            androidContext(this@GhostGramApp)
        }
    }
}