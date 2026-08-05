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
    /** Действующие работы всех лет (текущий год + архив прошлых). */
    fun observeTasks(): Flow<List<CalendarTask>>

    /** Корзина: удалённые работы. */
    fun observeDeleted(): Flow<List<CalendarTask>>

    suspend fun addTask(task: CalendarTask)
    suspend fun updateTask(task: CalendarTask)

    /** «Удалить» = отправить в корзину, откуда работу можно вернуть. */
    suspend fun moveToTrash(task: CalendarTask)
    suspend fun restoreFromTrash(task: CalendarTask)
    suspend fun deleteForever(task: CalendarTask)

    /**
     * Готовит текущий год:
     * - пустая база → заполняется стартовым списком;
     * - наступил новый год → работы копируются из последнего года
     *   со сброшенными отметками, прошлый год остаётся архивом.
     */
    suspend fun prepareYear(year: Int)
}

/** Реализация на Room (с v0.1.1): все работы лежат в базе и редактируются. */
class RoomCalendarRepository(private val dao: CalendarTaskDao) : CalendarRepository {

    override fun observeTasks(): Flow<List<CalendarTask>> =
        dao.observeActive().map { entities -> entities.map { it.toModel() } }

    override fun observeDeleted(): Flow<List<CalendarTask>> =
        dao.observeDeleted().map { entities -> entities.map { it.toModel() } }

    override suspend fun addTask(task: CalendarTask) {
        dao.insert(task.toEntity())
    }

    override suspend fun updateTask(task: CalendarTask) {
        dao.update(task.toEntity())
    }

    override suspend fun moveToTrash(task: CalendarTask) {
        dao.update(task.copy(isDeleted = true).toEntity())
    }

    override suspend fun restoreFromTrash(task: CalendarTask) {
        dao.update(task.copy(isDeleted = false).toEntity())
    }

    override suspend fun deleteForever(task: CalendarTask) {
        dao.delete(task.toEntity())
    }

    override suspend fun prepareYear(year: Int) {
        if (dao.count() == 0) {
            dao.insertAll(DefaultCalendarTasks.tasks.map { it.copy(year = year).toEntity() })
            return
        }
        if (dao.countForYear(year) > 0) return

        val lastYear = dao.latestYear() ?: return
        // Отметки и собранный мёд принадлежат своему году — в новый не переносятся.
        val carriedOver = dao.activeTasksForYear(lastYear).map { entity ->
            entity.copy(id = 0, year = year, isDone = false, honeyLiters = null)
        }
        dao.insertAll(carriedOver)
    }
}
