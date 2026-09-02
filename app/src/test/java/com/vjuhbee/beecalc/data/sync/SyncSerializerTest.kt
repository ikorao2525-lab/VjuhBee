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
    @Test
    fun taskMappingPreservesDueDate() {
        val source = CalendarTask(
            id = 0,
            month = 8,
            title = "Due date",
            shortDescription = "",
            category = TaskCategory.OTHER,
            dueDateMillis = 1_754_000_000_000L
        )

        assertEquals(source.dueDateMillis, source.toSync().dueDateMillis)
    }

    @Test
    fun serializerEncodesAndDecodesUsersAndApiaries() {
        val user = SyncUser(uuid = "user-1", name = "Иван", type = "individual", createdAt = 100L, updatedAt = 100L)
        val apiary = SyncApiary(uuid = "ap-1", userUuid = "user-1", name = "Лесная", note = "Точок у леса", address = "д. Сосновка", createdAt = 100L, updatedAt = 100L)
        val hive = SyncHive(uuid = "h-1", name = "Улей №1", note = "Сильный", apiaryUuid = "ap-1", updatedAt = 100L)
        val task = SyncTask(
            uuid = "t-1", year = 2026, month = 8, apiaryUuid = "ap-1", title = "Осмотр",
            shortDescription = "Проверка", fullDescription = null, category = "INSPECTION",
            importance = "NORMAL", tags = "", isDone = false, isDeleted = false,
            honeyKg = null, honeyLiters = null, pollenKg = null, beeBreadKg = null,
            propolisGrams = null, waxKg = null, royalJellyGrams = null,
            harvestItems = "", linkedHiveUuids = "h-1", updatedAt = 100L
        )

        val file = SyncFile(
            version = 2,
            scope = SyncScope.ACTIVE_APIARY,
            exportedAt = 200L,
            appVersion = "0.9.0",
            source = "BeeCalc",
            users = listOf(user),
            apiaries = listOf(apiary),
            hives = listOf(hive),
            inspections = emptyList(),
            treatments = emptyList(),
            tasks = listOf(task)
        )

        val json = SyncSerializer.encode(file)
        val decoded = SyncSerializer.decode(json)

        assertEquals(2, decoded.version)
        assertEquals(SyncScope.ACTIVE_APIARY, decoded.scope)
        assertEquals(1, decoded.users.size)
        assertEquals("Иван", decoded.users.first().name)
        assertEquals(1, decoded.apiaries.size)
        assertEquals("Лесная", decoded.apiaries.first().name)
        assertEquals("ap-1", decoded.hives.first().apiaryUuid)
        assertEquals("ap-1", decoded.tasks.first().apiaryUuid)
    }

    @Test
    fun decodeRejectsUnknownTaskEnumBeforeImport() {
        val json = """{
            "version": 1, "exportedAt": 0, "appVersion": "test", "source": "test",
            "hives": [], "inspections": [], "treatments": [],
            "tasks": [{
                "uuid": "task-1", "year": 2026, "month": 1, "title": "Task",
                "shortDescription": "", "category": "UNKNOWN", "importance": "LOW",
                "tags": "", "isDone": false, "isDeleted": false, "harvestItems": "",
                "linkedHiveUuids": "", "updatedAt": 1
            }]
        }""".trimIndent()

        try {
            SyncSerializer.decode(json)
            org.junit.Assert.fail("Unknown enum must be rejected while decoding")
        } catch (error: RuntimeException) {
            assertTrue(error.message.orEmpty().isNotBlank() || error.cause?.message.orEmpty().isNotBlank())
        }
    }
}
