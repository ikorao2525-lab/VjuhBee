package com.vjuhbee.beecalc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Calendar

/**
 * База приложения (SPEC.md §7). Пока одна таблица — календарь.
 * Таблицы ульев (Hive, Inspection, Treatment) добавятся в v0.2
 * миграцией с повышением version.
 */
@Database(entities = [CalendarTaskEntity::class], version = 2, exportSchema = false)
abstract class BeeCalcDatabase : RoomDatabase() {

    abstract fun calendarTaskDao(): CalendarTaskDao

    companion object {

        /** v1 → v2: годовые снимки календаря (year) и корзина (isDeleted). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN year INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                // Все уже существующие работы относим к текущему году.
                db.execSQL("UPDATE calendar_tasks SET year = $currentYear")
            }
        }

        fun build(context: Context): BeeCalcDatabase =
            Room.databaseBuilder(context, BeeCalcDatabase::class.java, "beecalc.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
