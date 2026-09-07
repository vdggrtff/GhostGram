package com.ghostgram.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ghostgram.core.database.GhostDatabase
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

actual val platformDatabaseModule = module {
    factory<RoomDatabase.Builder<GhostDatabase>> { (accountId: String) ->
        // На iOS файл базы кладется в домашнюю директорию песочницы приложения
        val dbFilePath = NSHomeDirectory() + "/ghostgram_$accountId.db"
        Room.databaseBuilder<GhostDatabase>(name = dbFilePath)
    }
}