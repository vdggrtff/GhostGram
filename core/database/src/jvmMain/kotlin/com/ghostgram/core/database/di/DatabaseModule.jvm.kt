package com.ghostgram.core.database.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.ghostgram.core.database.GhostDatabase
import org.koin.dsl.module
import java.io.File

actual val platformDatabaseModule = module {
    factory<RoomDatabase.Builder<GhostDatabase>> { (accountId: String) ->
        val dbFile = File(System.getProperty("user.home"), "ghostgram_$accountId.db")
        Room.databaseBuilder<GhostDatabase>(dbFile.absolutePath)
    }
    /*single<RoomDatabase.Builder<GhostDatabase>> {
        val dbFile = File(System.getProperty("user.home"), "ghostgram.db")
        Room.databaseBuilder<GhostDatabase>(dbFile.absolutePath)
    }*/
}