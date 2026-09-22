package com.optiontracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PositionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class OptionDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao

    companion object {
        fun create(context: Context): OptionDatabase =
            Room.databaseBuilder(context, OptionDatabase::class.java, "option_tracker.db")
                .build()
    }
}
