package com.ghostgram.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ghostgram.core.database.GhostDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformDatabaseModule = module {
    single<RoomDatabase.Builder<GhostDatabase>> {
        val context = androidContext()
        val dbFile = context.getDatabasePath("ghostgram.db")
        Room.databaseBuilder<GhostDatabase>(context, dbFile.absolutePath)
    }
}