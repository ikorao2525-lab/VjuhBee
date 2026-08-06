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
@Database(
    entities = [
        CalendarTaskEntity::class,
        HiveEntity::class,
        InspectionEntity::class,
        TreatmentEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class BeeCalcDatabase : RoomDatabase() {

    abstract fun calendarTaskDao(): CalendarTaskDao
    abstract fun hiveDao(): HiveDao

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

        /** v2 → v3: таблицы учёта ульев (SPEC.md §7). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS hives (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "note TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS inspections (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "hiveId INTEGER NOT NULL, " +
                        "date INTEGER NOT NULL, " +
                        "frames INTEGER NOT NULL, " +
                        "brood INTEGER NOT NULL, " +
                        "queenSeen INTEGER NOT NULL, " +
                        "note TEXT NOT NULL, " +
                        "FOREIGN KEY(hiveId) REFERENCES hives(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_inspections_hiveId ON inspections(hiveId)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS treatments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "hiveId INTEGER NOT NULL, " +
                        "date INTEGER NOT NULL, " +
                        "medicine TEXT NOT NULL, " +
                        "dose TEXT NOT NULL, " +
                        "note TEXT NOT NULL, " +
                        "FOREIGN KEY(hiveId) REFERENCES hives(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_treatments_hiveId ON treatments(hiveId)")
            }
        }

        /** v3 → v4: мёдооткачка — поле «собрано, л» у работ календаря. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN honeyLiters REAL")
            }
        }

        /**
         * v4 → v5: полный учёт продукции пасеки.
         * Мёд — кг и л (оба, без автоконвертации: плотность зависит от сорта).
         * Пыльца/перга/воск — кг; прополис и маточное молочко — граммы.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN honeyKg REAL")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN pollenKg REAL")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN beeBreadKg REAL")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN propolisGrams REAL")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN waxKg REAL")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN royalJellyGrams REAL")
            }
        }

        /**
         * v5 → v6: постоянный публичный id улья — uuid (SPEC.md §9, v0.4).
         * Нужен для QR-кодов и переноса базы на другой телефон:
         * не зависит от внутреннего id базы (он меняется при восстановлении).
         * Существующим ульям генерим UUID v4 на месте через SQLite randomblob.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE hives ADD COLUMN uuid TEXT NOT NULL DEFAULT ''"
                )
                // UUID v4: 8-4-4-4-12 hex, version 4, variant bits '89ab'.
                db.execSQL(
                    "UPDATE hives SET uuid = " +
                        "lower(hex(randomblob(4)) || '-' || " +
                        "hex(randomblob(2)) || '-' || " +
                        "'4' || substr(hex(randomblob(2)), 2) || '-' || " +
                        "substr('89ab', 1 + (abs(random()) % 4), 1) || " +
                        "substr(hex(randomblob(2)), 2) || '-' || " +
                        "hex(randomblob(6)))"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_hives_uuid ON hives(uuid)")
            }
        }

        fun build(context: Context): BeeCalcDatabase =
            Room.databaseBuilder(context, BeeCalcDatabase::class.java, "beecalc.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
    }
}
