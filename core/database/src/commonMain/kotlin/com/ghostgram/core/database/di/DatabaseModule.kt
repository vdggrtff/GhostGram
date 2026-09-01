package com.ghostgram.core.database.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ghostgram.core.database.GhostDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformDatabaseModule: Module

val databaseModule = module {
    includes(platformDatabaseModule) // Подтягиваем платформенный билдер

    // Собираем готовую БД
    single {
        get<androidx.room.RoomDatabase.Builder<GhostDatabase>>()
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    // Раздаем DAO по всему приложению
    single { get<GhostDatabase>().messageDao() }
}