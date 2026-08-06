package com.vjuhbee.beecalc

import android.app.Application
import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.data.HiveRepository
import com.vjuhbee.beecalc.data.RoomCalendarRepository
import com.vjuhbee.beecalc.data.RoomHiveRepository
import com.vjuhbee.beecalc.data.db.BeeCalcDatabase
import com.vjuhbee.beecalc.data.sync.SyncRepository

/**
 * Application-класс: держит единственные экземпляры базы и репозиториев.
 * Простая замена DI-фреймворку — без магии (SPEC.md §11).
 */
class BeeCalcApp : Application() {

    val database: BeeCalcDatabase by lazy { BeeCalcDatabase.build(this) }

    val calendarRepository: CalendarRepository by lazy {
        RoomCalendarRepository(database.calendarTaskDao())
    }

    val hiveRepository: HiveRepository by lazy {
        RoomHiveRepository(database.hiveDao())
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(hiveRepository, calendarRepository)
    }
}
