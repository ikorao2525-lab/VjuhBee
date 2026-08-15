package com.vjuhbee.beecalc.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarTaskDao {

    /** Действующие работы всех лет. Flow: UI обновится при любом изменении. */
    @Query("SELECT * FROM calendar_tasks WHERE isDeleted = 0 ORDER BY year DESC, month, id")
    fun observeActive(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE isDeleted = 0 AND (apiaryUuid = :apiaryUuid OR apiaryUuid = '') ORDER BY year DESC, month, id")
    fun observeActiveByApiary(apiaryUuid: String): Flow<List<CalendarTaskEntity>>

    /** Корзина: удалённые работы, их можно вернуть. */
    @Query("SELECT * FROM calendar_tasks WHERE isDeleted = 1 ORDER BY year DESC, month, id")
    fun observeDeleted(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE isDeleted = 1 AND (apiaryUuid = :apiaryUuid OR apiaryUuid = '') ORDER BY year DESC, month, id")
    fun observeDeletedByApiary(apiaryUuid: String): Flow<List<CalendarTaskEntity>>

    @Query("SELECT COUNT(*) FROM calendar_tasks")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM calendar_tasks WHERE (apiaryUuid = :apiaryUuid OR apiaryUuid = '')")
    suspend fun countByApiary(apiaryUuid: String): Int

    @Query("SELECT COUNT(*) FROM calendar_tasks WHERE year = :year")
    suspend fun countForYear(year: Int): Int

    @Query("SELECT COUNT(*) FROM calendar_tasks WHERE year = :year AND (apiaryUuid = :apiaryUuid OR apiaryUuid = '')")
    suspend fun countForYearByApiary(year: Int, apiaryUuid: String): Int

    @Query("SELECT MAX(year) FROM calendar_tasks")
    suspend fun latestYear(): Int?

    @Query("SELECT MAX(year) FROM calendar_tasks WHERE (apiaryUuid = :apiaryUuid OR apiaryUuid = '')")
    suspend fun latestYearByApiary(apiaryUuid: String): Int?

    @Query("SELECT * FROM calendar_tasks WHERE year = :year AND isDeleted = 0")
    suspend fun activeTasksForYear(year: Int): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks WHERE year = :year AND isDeleted = 0 AND (apiaryUuid = :apiaryUuid OR apiaryUuid = '')")
    suspend fun activeTasksForYearByApiary(year: Int, apiaryUuid: String): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks")
    suspend fun allTasks(): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks WHERE (apiaryUuid = :apiaryUuid OR apiaryUuid = '')")
    suspend fun allTasksByApiary(apiaryUuid: String): List<CalendarTaskEntity>

    @Query("SELECT * FROM calendar_tasks")
    suspend fun allTasksWithHarvest(): List<CalendarTaskEntity>

    @Insert
    suspend fun insert(task: CalendarTaskEntity)

    @Insert
    suspend fun insertAll(tasks: List<CalendarTaskEntity>)

    @Update
    suspend fun update(task: CalendarTaskEntity)

    /** Окончательное удаление строки (из корзины). */
    @Delete
    suspend fun delete(task: CalendarTaskEntity)
}
