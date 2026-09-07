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

    factory { (accountId: String) ->
        get<androidx.room.RoomDatabase.Builder<GhostDatabase>>(
            parameters = { org.koin.core.parameter.parametersOf(accountId) }
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    factory { (accountId: String) ->
        get<GhostDatabase>(parameters = { org.koin.core.parameter.parametersOf(accountId) }).messageDao()
    }
}