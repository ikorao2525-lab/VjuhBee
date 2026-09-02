package com.vjuhbee.beecalc.data

import androidx.room.withTransaction
import com.vjuhbee.beecalc.data.db.BeeCalcDatabase
import com.vjuhbee.beecalc.data.db.CalendarTaskDao
import com.vjuhbee.beecalc.data.db.HarvestItemDao
import com.vjuhbee.beecalc.data.db.toEntity
import com.vjuhbee.beecalc.data.db.toModel
import com.vjuhbee.beecalc.model.CalendarTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface CalendarRepository {
    fun observeTasks(): Flow<List<CalendarTask>>
    fun observeTasksForApiary(apiaryUuid: String): Flow<List<CalendarTask>>
    fun observeDeleted(): Flow<List<CalendarTask>>
    fun observeDeletedForApiary(apiaryUuid: String): Flow<List<CalendarTask>>
    suspend fun addTask(task: CalendarTask)
    suspend fun updateTask(task: CalendarTask)
    suspend fun allTasks(): List<CalendarTask>
    suspend fun allTasksForApiary(apiaryUuid: String): List<CalendarTask>
    suspend fun moveToTrash(task: CalendarTask)
    suspend fun restoreFromTrash(task: CalendarTask)
    suspend fun deleteForever(task: CalendarTask)
    suspend fun prepareYear(year: Int, apiaryUuid: String = "")
}

class RoomCalendarRepository(
    private val database: BeeCalcDatabase,
    private val dao: CalendarTaskDao,
    private val harvestDao: HarvestItemDao
) : CalendarRepository {
    private fun mergeHarvest(tasks: List<com.vjuhbee.beecalc.data.db.CalendarTaskEntity>, items: List<com.vjuhbee.beecalc.data.db.HarvestItemEntity>) =
        tasks.map { entity ->
            entity.toModel().copy(harvestItems = items.filter { it.taskUuid == entity.uuid }.map { it.toModel() })
        }

    override fun observeTasks(): Flow<List<CalendarTask>> =
        combine(dao.observeActive(), harvestDao.observeAll()) { tasks, items -> mergeHarvest(tasks, items) }

    override fun observeTasksForApiary(apiaryUuid: String): Flow<List<CalendarTask>> =
        combine(dao.observeActiveByApiary(apiaryUuid), harvestDao.observeAll()) { tasks, items -> mergeHarvest(tasks, items) }

    override fun observeDeleted(): Flow<List<CalendarTask>> =
        combine(dao.observeDeleted(), harvestDao.observeAll()) { tasks, items -> mergeHarvest(tasks, items) }

    override fun observeDeletedForApiary(apiaryUuid: String): Flow<List<CalendarTask>> =
        combine(dao.observeDeletedByApiary(apiaryUuid), harvestDao.observeAll()) { tasks, items -> mergeHarvest(tasks, items) }

    private suspend fun saveHarvest(task: CalendarTask) {
        harvestDao.deleteForTask(task.uuid)
        harvestDao.insertAll(task.harvestItems.map { it.toEntity(task.uuid) })
    }

    override suspend fun addTask(task: CalendarTask) = database.withTransaction {
        dao.insert(task.toEntity())
        saveHarvest(task)
    }

    override suspend fun updateTask(task: CalendarTask) = database.withTransaction {
        dao.update(task.toEntity())
        saveHarvest(task)
    }

    override suspend fun allTasks(): List<CalendarTask> =
        mergeHarvest(dao.allTasks(), harvestDao.all()).toList()

    override suspend fun allTasksForApiary(apiaryUuid: String): List<CalendarTask> =
        mergeHarvest(dao.allTasksByApiary(apiaryUuid), harvestDao.all()).toList()

    override suspend fun moveToTrash(task: CalendarTask) {
        dao.update(task.copy(isDeleted = true).toEntity())
    }

    override suspend fun restoreFromTrash(task: CalendarTask) {
        dao.update(task.copy(isDeleted = false).toEntity())
    }

    override suspend fun deleteForever(task: CalendarTask) = database.withTransaction {
        harvestDao.deleteForTask(task.uuid)
        dao.delete(task.toEntity())
    }

    override suspend fun prepareYear(year: Int, apiaryUuid: String) {
        if (dao.countByApiary(apiaryUuid) == 0) {
            val now = System.currentTimeMillis()
            dao.insertAll(DefaultCalendarTasks.tasks.map {
                it.copy(
                    year = year,
                    apiaryUuid = apiaryUuid,
                    uuid = java.util.UUID.randomUUID().toString(),
                    updatedAt = now
                ).toEntity()
            })
            return
        }
        if (dao.countForYearByApiary(year, apiaryUuid) > 0) return
        val lastYear = dao.latestYearByApiary(apiaryUuid) ?: return
        val now = System.currentTimeMillis()
        val carriedOver = dao.activeTasksForYearByApiary(lastYear, apiaryUuid).map { entity ->
            entity.copy(
                id = 0,
                year = year,
                apiaryUuid = apiaryUuid,
                uuid = java.util.UUID.randomUUID().toString(),
                updatedAt = now,
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