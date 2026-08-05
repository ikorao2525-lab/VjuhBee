package com.vjuhbee.beecalc.data

import com.vjuhbee.beecalc.data.db.CalendarTaskDao
import com.vjuhbee.beecalc.data.db.toEntity
import com.vjuhbee.beecalc.data.db.toModel
import com.vjuhbee.beecalc.model.CalendarTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Источник данных календаря (SPEC.md §5.2).
 * UI работает только с интерфейсом — реализацию можно менять,
 * не трогая экраны.
 */
interface CalendarRepository {
    fun observeTasks(): Flow<List<CalendarTask>>
    suspend fun addTask(task: CalendarTask)
    suspend fun updateTask(task: CalendarTask)
    suspend fun deleteTask(task: CalendarTask)

    /** При первом запуске заполняет пустую базу стартовым списком работ. */
    suspend fun seedDefaultsIfEmpty()
}

/** Реализация на Room (с v0.1.1): все работы лежат в базе и редактируются. */
class RoomCalendarRepository(private val dao: CalendarTaskDao) : CalendarRepository {

    override fun observeTasks(): Flow<List<CalendarTask>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun addTask(task: CalendarTask) {
        dao.insert(task.toEntity())
    }

    override suspend fun updateTask(task: CalendarTask) {
        dao.update(task.toEntity())
    }

    override suspend fun deleteTask(task: CalendarTask) {
        dao.delete(task.toEntity())
    }

    override suspend fun seedDefaultsIfEmpty() {
        if (dao.count() == 0) {
            dao.insertAll(DefaultCalendarTasks.tasks.map { it.toEntity() })
        }
    }
}
