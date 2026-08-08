package com.vjuhbee.beecalc.data

import com.vjuhbee.beecalc.data.db.CalendarTaskDao
import com.vjuhbee.beecalc.data.db.HarvestItemDao
import com.vjuhbee.beecalc.data.db.toEntity
import com.vjuhbee.beecalc.data.db.toModel
import com.vjuhbee.beecalc.model.CalendarTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface CalendarRepository {
    fun observeTasks(): Flow<List<CalendarTask>>
    fun observeDeleted(): Flow<List<CalendarTask>>
    suspend fun addTask(task: CalendarTask)
    suspend fun updateTask(task: CalendarTask)
    suspend fun allTasks(): List<CalendarTask>
    suspend fun moveToTrash(task: CalendarTask)
    suspend fun restoreFromTrash(task: CalendarTask)
    suspend fun deleteForever(task: CalendarTask)
    suspend fun prepareYear(year: Int)
}

class RoomCalendarRepository(
    private val dao: CalendarTaskDao,
    private val harvestDao: HarvestItemDao
) : CalendarRepository {
    private fun mergeHarvest(tasks: List<com.vjuhbee.beecalc.data.db.CalendarTaskEntity>, items: List<com.vjuhbee.beecalc.data.db.HarvestItemEntity>) =
        tasks.map { entity ->
            entity.toModel().copy(harvestItems = items.filter { it.taskUuid == entity.uuid }.map { it.toModel() })
        }

    override fun observeTasks(): Flow<List<CalendarTask>> =
        combine(dao.observeActive(), harvestDao.observeAll()) { tasks, items -> mergeHarvest(tasks, items) }

    override fun observeDeleted(): Flow<List<CalendarTask>> =
        combine(dao.observeDeleted(), harvestDao.observeAll()) { tasks, items -> mergeHarvest(tasks, items) }

    private suspend fun saveHarvest(task: CalendarTask) {
        harvestDao.deleteForTask(task.uuid)
        harvestDao.insertAll(task.harvestItems.map { it.toEntity(task.uuid) })
    }

    override suspend fun addTask(task: CalendarTask) {
        dao.insert(task.toEntity())
        saveHarvest(task)
    }

    override suspend fun updateTask(task: CalendarTask) {
        dao.update(task.toEntity())
        saveHarvest(task)
    }

    override suspend fun allTasks(): List<CalendarTask> =
        mergeHarvest(dao.allTasks(), harvestDao.all()).toList()

    override suspend fun moveToTrash(task: CalendarTask) {
        dao.update(task.copy(isDeleted = true).toEntity())
    }

    override suspend fun restoreFromTrash(task: CalendarTask) {
        dao.update(task.copy(isDeleted = false).toEntity())
    }

    override suspend fun deleteForever(task: CalendarTask) {
        dao.delete(task.toEntity())
        harvestDao.deleteForTask(task.uuid)
    }

    override suspend fun prepareYear(year: Int) {
        if (dao.count() == 0) {
            dao.insertAll(DefaultCalendarTasks.tasks.map { it.copy(year = year).toEntity() })
            return
        }
        if (dao.countForYear(year) > 0) return
        val lastYear = dao.latestYear() ?: return
        val carriedOver = dao.activeTasksForYear(lastYear).map { entity ->
            entity.copy(
                id = 0,
                year = year,
                isDone = false,
                honeyKg = null,
                honeyLiters = null,
                pollenKg = null,
                beeBreadKg = null,
                propolisGrams = null,
                waxKg = null,
                royalJellyGrams = null
            )
        }
        dao.insertAll(carriedOver)
    }
}