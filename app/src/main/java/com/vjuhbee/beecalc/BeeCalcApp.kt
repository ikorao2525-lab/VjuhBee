package com.vjuhbee.beecalc

import android.app.Application
import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.data.HiveRepository
import com.vjuhbee.beecalc.data.RoomCalendarRepository
import com.vjuhbee.beecalc.data.RoomHiveRepository
import com.vjuhbee.beecalc.data.RoomUserAndApiaryRepository
import com.vjuhbee.beecalc.data.UserAndApiaryRepository
import com.vjuhbee.beecalc.data.db.BeeCalcDatabase
import com.vjuhbee.beecalc.data.sync.SyncRepository
import com.vjuhbee.beecalc.diagnostics.DiagnosticLogger

/** Application-класс: держит единственные экземпляры базы и репозиториев. */
class BeeCalcApp : Application() {
    val diagnosticLogger: DiagnosticLogger by lazy { DiagnosticLogger(this) }

    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (diagnosticLogger.consumeInterruptedStartup()) {
            diagnosticLogger.warning("Previous startup did not complete")
        }
        diagnosticLogger.markStartupStarted()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                diagnosticLogger.error("Uncaught exception on ${thread.name}", throwable)
                diagnosticLogger.markStartupStarted()
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
        diagnosticLogger.info("Application started")
    }
    val database: BeeCalcDatabase by lazy { BeeCalcDatabase.build(this) }
    val userAndApiaryRepository: UserAndApiaryRepository by lazy {
        RoomUserAndApiaryRepository(this, database.userDao(), database.apiaryDao())
    }
    val calendarRepository: CalendarRepository by lazy {
        RoomCalendarRepository(database.calendarTaskDao(), database.harvestItemDao())
    }
    val hiveRepository: HiveRepository by lazy { RoomHiveRepository(database.hiveDao()) }
    val syncRepository: SyncRepository by lazy { SyncRepository(hiveRepository, calendarRepository, database) }
}
