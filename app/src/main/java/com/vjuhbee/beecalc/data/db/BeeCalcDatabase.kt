package com.vjuhbee.beecalc.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * База приложения (SPEC.md §7). Пока одна таблица — календарь.
 * Таблицы ульев (Hive, Inspection, Treatment) добавятся в v0.2
 * миграцией с повышением version.
 */
@Database(entities = [CalendarTaskEntity::class], version = 1, exportSchema = false)
abstract class BeeCalcDatabase : RoomDatabase() {

    abstract fun calendarTaskDao(): CalendarTaskDao

    companion object {
        fun build(context: Context): BeeCalcDatabase =
            Room.databaseBuilder(context, BeeCalcDatabase::class.java, "beecalc.db").build()
    }
}
