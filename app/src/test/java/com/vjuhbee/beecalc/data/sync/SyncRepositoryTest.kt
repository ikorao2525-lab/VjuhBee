package com.vjuhbee.beecalc.data.sync

import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.data.HiveRepository
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.TaskCategory
import com.vjuhbee.beecalc.model.Treatment
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRepositoryTest {
    @Test
    fun activeImportedTaskRestoresLocalTaskFromTrash() = runBlocking {
        val local = CalendarTask(
            id = 7,
            year = 2026,
            month = 8,
            title = "Deleted locally",
            shortDescription = "",
            category = TaskCategory.OTHER,
            isDeleted = true,
            linkedHiveUuids = emptyList(),
            uuid = "task-1",
            updatedAt = 999L
        )
        val remote = local.copy(
            isDeleted = false,
            isDone = true,
            linkedHiveUuids = listOf("hive-a", "hive-b"),
            updatedAt = 100L
        )
        val repository = SyncRepository(
            FakeHiveRepository(),
            FakeCalendarRepository(listOf(local))
        )

        val plan = repository.buildPlan(SyncFile(tasks = listOf(remote.toSync())))

        assertEquals(1, plan.updatedTasks.size)
        assertTrue(plan.updatedTasks.single().linkedHiveUuids.contains("hive-a"))
        assertTrue(!plan.updatedTasks.single().isDeleted)
    }

    private class FakeCalendarRepository(
        private val tasks: List<CalendarTask>
    ) : CalendarRepository {
        override fun observeTasks(): Flow<List<CalendarTask>> = emptyFlow()
        override fun observeDeleted(): Flow<List<CalendarTask>> = emptyFlow()
        override suspend fun addTask(task: CalendarTask) = Unit
        override suspend fun updateTask(task: CalendarTask) = Unit
        override suspend fun allTasks(): List<CalendarTask> = tasks
        override suspend fun moveToTrash(task: CalendarTask) = Unit
        override suspend fun restoreFromTrash(task: CalendarTask) = Unit
        override suspend fun deleteForever(task: CalendarTask) = Unit
        override suspend fun prepareYear(year: Int) = Unit
    }

    private class FakeHiveRepository : HiveRepository {
        override fun observeHives(): Flow<List<Hive>> = emptyFlow()
        override fun observeHive(id: Int): Flow<Hive?> = emptyFlow()
        override suspend fun findHiveByUuid(uuid: String): Hive? = null
        override suspend fun addHive(hive: Hive): Long = 0L
        override suspend fun updateHive(hive: Hive) = Unit
        override suspend fun allHives(): List<Hive> = emptyList()
        override suspend fun allInspections(): List<Inspection> = emptyList()
        override suspend fun allTreatments(): List<Treatment> = emptyList()
        override suspend fun deleteHive(hive: Hive) = Unit
        override fun observeInspections(hiveId: Int): Flow<List<Inspection>> = emptyFlow()
        override suspend fun addInspection(inspection: Inspection) = Unit
        override suspend fun updateInspection(inspection: Inspection) = Unit
        override suspend fun deleteInspection(inspection: Inspection) = Unit
        override fun observeTreatments(hiveId: Int): Flow<List<Treatment>> = emptyFlow()
        override suspend fun addTreatment(treatment: Treatment) = Unit
        override suspend fun updateTreatment(treatment: Treatment) = Unit
        override suspend fun deleteTreatment(treatment: Treatment) = Unit
    }
}