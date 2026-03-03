package com.minar.birday.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minar.birday.model.Event


@Database(entities = [Event::class], version = 13, exportSchema = false)
@TypeConverters(LocalDateTypeConverter::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        // Migration strategy to add two columns from version 9 to 10
        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE Event ADD COLUMN notes TEXT DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE Event ADD COLUMN image BLOB"
                )
            }
        }
        // Migration strategy to uppercase the type from version 10 to 11
        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE Event SET type = UPPER(type)"
                )
            }
        }
        // Migration strategy to add dayOfWeek column from version 11 to 12
        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE Event ADD COLUMN dayOfWeek INTEGER DEFAULT NULL"
                )
            }
        }
        // Migration strategy to remap Sunday from Java convention (7) to SQLite convention (0)
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE Event SET dayOfWeek = 0 WHERE dayOfWeek = 7")
            }
        }
        fun getUnbirdayDatabase(context: Context): EventDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "UnbirdayDB"
                )
                    .addMigrations(MIGRATION_9_10)
                    .addMigrations(MIGRATION_10_11)
                    .addMigrations(MIGRATION_11_12)
                    .addMigrations(MIGRATION_12_13)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        fun destroyInstance() {
            if (INSTANCE?.isOpen == true) {
                INSTANCE?.close()
            }
            INSTANCE = null
        }
    }
}