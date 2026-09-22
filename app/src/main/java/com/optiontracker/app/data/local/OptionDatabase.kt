package com.optiontracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PositionEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class OptionDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE positions ADD COLUMN account TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE positions ADD COLUMN realizedOverrideCents INTEGER")
            }
        }

        fun create(context: Context): OptionDatabase =
            Room.databaseBuilder(context, OptionDatabase::class.java, "option_tracker.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
