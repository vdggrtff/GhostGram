package com.ghostgram.core.database.di

import androidx.room.RoomDatabase.Builder
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ghostgram.core.database.GhostDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

expect val platformDatabaseModule: Module

val databaseModule = module {
    includes(platformDatabaseModule) // Подтягиваем платформенный билдер

    factory { (accountId: String) ->
        get<Builder<GhostDatabase>>(
            parameters = { parametersOf(accountId) }
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    factory { (accountId: String) ->
        get<GhostDatabase>(parameters = { parametersOf(accountId) }).messageDao()
    }
}