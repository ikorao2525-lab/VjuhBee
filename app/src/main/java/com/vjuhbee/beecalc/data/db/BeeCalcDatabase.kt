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
        TreatmentEntity::class,
        HarvestItemEntity::class,
        UserEntity::class,
        ApiaryEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class BeeCalcDatabase : RoomDatabase() {

    abstract fun calendarTaskDao(): CalendarTaskDao
    abstract fun hiveDao(): HiveDao
    abstract fun harvestItemDao(): HarvestItemDao
    abstract fun userDao(): UserDao
    abstract fun apiaryDao(): ApiaryDao

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

        /**
         * v6 → v7: ключи и метки для синхронизации (SPEC.md §9, v0.4).
         * Каждому синхронизируемому объекту — стабильный uuid (переживает
         * слияние баз) и updatedAt (для разрешения конфликтов и экспорта).
         * Существующим строкам генерим UUID v4 на месте и ставим updatedAt = now.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("ALTER TABLE hives ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE hives SET updatedAt = $now")

                db.execSQL("ALTER TABLE inspections ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inspections ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE inspections SET uuid = " +
                        "lower(hex(randomblob(4)) || '-' || " +
                        "hex(randomblob(2)) || '-' || " +
                        "'4' || substr(hex(randomblob(2)), 2) || '-' || " +
                        "substr('89ab', 1 + (abs(random()) % 4), 1) || " +
                        "substr(hex(randomblob(2)), 2) || '-' || " +
                        "hex(randomblob(6)))"
                )
                db.execSQL("UPDATE inspections SET updatedAt = $now")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inspections_uuid ON inspections(uuid)")

                db.execSQL("ALTER TABLE treatments ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE treatments ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE treatments SET uuid = " +
                        "lower(hex(randomblob(4)) || '-' || " +
                        "hex(randomblob(2)) || '-' || " +
                        "'4' || substr(hex(randomblob(2)), 2) || '-' || " +
                        "substr('89ab', 1 + (abs(random()) % 4), 1) || " +
                        "substr(hex(randomblob(2)), 2) || '-' || " +
                        "hex(randomblob(6)))"
                )
                db.execSQL("UPDATE treatments SET updatedAt = $now")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_treatments_uuid ON treatments(uuid)")

                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE calendar_tasks SET uuid = " +
                        "lower(hex(randomblob(4)) || '-' || " +
                        "hex(randomblob(2)) || '-' || " +
                        "'4' || substr(hex(randomblob(2)), 2) || '-' || " +
                        "substr('89ab', 1 + (abs(random()) % 4), 1) || " +
                        "substr(hex(randomblob(2)), 2) || '-' || " +
                        "hex(randomblob(6)))"
                )
                db.execSQL("UPDATE calendar_tasks SET updatedAt = $now")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_calendar_tasks_uuid ON calendar_tasks(uuid)")
            }
        }

        // v7 → v8: привязка работ календаря к ульям (SPEC.md §5.2, v0.5).
        // Список uuid ульев через запятую (как теги), пустая строка — без привязки.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN linkedHiveUuids TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v8 → v9: отдельные позиции собранной продукции. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS harvest_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, taskUuid TEXT NOT NULL, product TEXT NOT NULL, amount REAL NOT NULL, unit TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_harvest_items_taskUuid ON harvest_items(taskUuid)")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'honey', honeyKg, 'kg' FROM calendar_tasks WHERE honeyKg IS NOT NULL AND honeyKg > 0")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'honey', honeyLiters, 'l' FROM calendar_tasks WHERE honeyLiters IS NOT NULL AND honeyLiters > 0")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'pollen', pollenKg, 'kg' FROM calendar_tasks WHERE pollenKg IS NOT NULL AND pollenKg > 0")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'bee_bread', beeBreadKg, 'kg' FROM calendar_tasks WHERE beeBreadKg IS NOT NULL AND beeBreadKg > 0")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'propolis', propolisGrams, 'g' FROM calendar_tasks WHERE propolisGrams IS NOT NULL AND propolisGrams > 0")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'wax', waxKg, 'kg' FROM calendar_tasks WHERE waxKg IS NOT NULL AND waxKg > 0")
                db.execSQL("INSERT INTO harvest_items (taskUuid, product, amount, unit) SELECT uuid, 'royal_jelly', royalJellyGrams, 'g' FROM calendar_tasks WHERE royalJellyGrams IS NOT NULL AND royalJellyGrams > 0")
            }
        }
        /** v9 → v10: необязательная точная дата выполнения работы. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN dueDateMillis INTEGER")
            }
        }

        /**
         * v10 → v11: мультипользователи и мультипасеки (SPEC.md §9, v0.9.0).
         * Таблицы users и apiaries; в hives и calendar_tasks добавляется apiaryUuid.
         * Существующие данные связываются с профилем по умолчанию и основной пасекой.
         */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                // 1. Создание таблицы пользователей
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "uuid TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "type TEXT NOT NULL DEFAULT 'individual', " +
                        "createdAt INTEGER NOT NULL DEFAULT 0, " +
                        "updatedAt INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_uuid ON users(uuid)")

                // 2. Создание таблицы пасек
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS apiaries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "uuid TEXT NOT NULL, " +
                        "userUuid TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "note TEXT NOT NULL DEFAULT '', " +
                        "address TEXT NOT NULL DEFAULT '', " +
                        "createdAt INTEGER NOT NULL DEFAULT 0, " +
                        "updatedAt INTEGER NOT NULL DEFAULT 0, " +
                        "FOREIGN KEY(userUuid) REFERENCES users(uuid) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_apiaries_uuid ON apiaries(uuid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_apiaries_userUuid ON apiaries(userUuid)")

                // 3. Создание пользователя по умолчанию и основной пасеки со стабильными UUID
                val defaultUserUuid = java.util.UUID.randomUUID().toString()
                val defaultApiaryUuid = java.util.UUID.randomUUID().toString()

                db.execSQL(
                    "INSERT INTO users (uuid, name, type, createdAt, updatedAt) " +
                        "VALUES ('$defaultUserUuid', 'Мой профиль', 'individual', $now, $now)"
                )
                db.execSQL(
                    "INSERT INTO apiaries (uuid, userUuid, name, note, address, createdAt, updatedAt) " +
                        "VALUES ('$defaultApiaryUuid', '$defaultUserUuid', 'Основная пасека', '', '', $now, $now)"
                )

                // 4. Добавление apiaryUuid в hives и calendar_tasks
                db.execSQL("ALTER TABLE hives ADD COLUMN apiaryUuid TEXT NOT NULL DEFAULT '$defaultApiaryUuid'")
                db.execSQL("UPDATE hives SET apiaryUuid = '$defaultApiaryUuid'")

                db.execSQL("ALTER TABLE calendar_tasks ADD COLUMN apiaryUuid TEXT NOT NULL DEFAULT '$defaultApiaryUuid'")
                db.execSQL("UPDATE calendar_tasks SET apiaryUuid = '$defaultApiaryUuid'")
            }
        }

        fun build(context: Context): BeeCalcDatabase =
            Room.databaseBuilder(context, BeeCalcDatabase::class.java, "beecalc.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11
                )
                .build()
    }
}
