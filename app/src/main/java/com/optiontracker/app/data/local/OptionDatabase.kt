package com.optiontracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PositionEntity::class, AssignedLotEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class OptionDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
    abstract fun assignedLotDao(): AssignedLotDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE positions ADD COLUMN account TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE positions ADD COLUMN realizedOverrideCents INTEGER")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assigned_lots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ticker` TEXT NOT NULL, `costBasisCents` INTEGER NOT NULL, `shares` INTEGER NOT NULL, `assignedEpochDay` INTEGER NOT NULL, `sourcePositionId` INTEGER, `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL)",
                )
            }
        }

        fun create(context: Context): OptionDatabase =
            Room.databaseBuilder(context, OptionDatabase::class.java, "option_tracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
