package com.vjuhbee.beecalc.data.sync

import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncModelsTest {
    @Test
    fun taskMappingPreservesLinksStatusAndTimestamp() {
        val source = CalendarTask(
            id = 0,
            year = 2026,
            month = 8,
            title = "Sync test",
            shortDescription = "Two hives",
            fullDescription = "Full description",
            category = TaskCategory.MAINTENANCE,
            importance = Importance.HIGH,
            isDone = true,
            honeyKg = 12.5,
            linkedHiveUuids = listOf("hive-a", "hive-b"),
            uuid = "task-1",
            updatedAt = 123456789L
        )

        val sync = source.toSync()

        assertEquals("task-1", sync.uuid)
        assertTrue(sync.isDone)
        assertEquals(123456789L, sync.updatedAt)
        assertEquals("hive-a,hive-b", sync.linkedHiveUuids)
        assertEquals(12.5, sync.honeyKg ?: -1.0, 0.0)
        assertEquals("MAINTENANCE", sync.category)
        assertEquals("HIGH", sync.importance)
    }

    @Test
    fun syncFileMetadataIsStoredInDto() {
        val file = SyncFile(exportedAt = 42L, appVersion = "0.6.3", source = "test")
        assertEquals(42L, file.exportedAt)
        assertEquals("0.6.3", file.appVersion)
        assertEquals("test", file.source)
    }

    @Test
    fun taskWithoutLinksMapsToEmptyString() {
        val task = CalendarTask(
            id = 0,
            month = 1,
            title = "Legacy",
            shortDescription = "",
            category = TaskCategory.OTHER
        )

        assertEquals("", task.toSync().linkedHiveUuids)
    }
}
