package com.ghostgram.app

import android.app.Application
import com.ghostgram.app.di.initKoin

class GhostGramApp: Application() {

    override fun onCreate() {
        super.onCreate()

        // 💥 ЗАПУСКАЕМ KOIN И ПЕРЕДАЕМ ЕМУ НАШУ БАЗУ И DATASTORE
        initKoin()
    }
}