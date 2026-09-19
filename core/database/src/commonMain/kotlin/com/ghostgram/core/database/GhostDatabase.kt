package com.ghostgram.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ghostgram.core.database.dao.MessageDao
import com.ghostgram.core.database.entity.MessageEntity

@Database(
    entities = [MessageEntity::class],
    version = 7
)
abstract class GhostDatabase : RoomDatabase() {
    // KSP сгенерирует реализацию этого метода
    abstract fun messageDao(): MessageDao
}