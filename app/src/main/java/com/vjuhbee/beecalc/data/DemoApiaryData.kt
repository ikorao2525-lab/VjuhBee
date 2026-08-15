package com.vjuhbee.beecalc.data

import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.HarvestItem
import com.vjuhbee.beecalc.model.HarvestProduct
import com.vjuhbee.beecalc.model.HarvestUnit
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.TaskCategory
import com.vjuhbee.beecalc.model.Treatment
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Небольшая независимая демонстрационная пасека для первого запуска. */
object DemoApiaryData {
    suspend fun seed(hiveRepository: HiveRepository, calendarRepository: CalendarRepository, apiaryUuid: String = "") {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val hives = listOf(
            Hive(0, "Улей №1 — Сильная семья", "Сильная семья, основная линия", apiaryUuid, UUID.randomUUID().toString(), now),
            Hive(0, "Улей №2 — Молодая матка", "Молодая матка, наблюдать засев", apiaryUuid, UUID.randomUUID().toString(), now),
            Hive(0, "Улей №3 — Отводок", "Отводок этого сезона", apiaryUuid, UUID.randomUUID().toString(), now),
            Hive(0, "Улей №4 — На наблюдении", "Требует дополнительного контроля", apiaryUuid, UUID.randomUUID().toString(), now)
        )
        val savedHives = hives.map { it.copy(id = hiveRepository.addHive(it).toInt()) }
        savedHives.forEachIndexed { index, hive ->
            hiveRepository.addInspection(Inspection(0, hive.id, today.minusDays((index + 1) * 3L).atStartOfMillis(), 10 - index, 5 - index.coerceAtMost(3), index != 3, "Демонстрационный осмотр", UUID.randomUUID().toString(), now))
            if (index == 0 || index == 3) {
                hiveRepository.addTreatment(Treatment(0, hive.id, today.minusDays(10).atStartOfMillis(), "Обработка от варроатоза", "По инструкции", "Демонстрационная запись", UUID.randomUUID().toString(), now))
            }
        }
        val tasks = listOf(
            CalendarTask(0, today.year, today.monthValue, apiaryUuid = apiaryUuid, title = "Проверить кормовые запасы", shortDescription = "Осмотреть семьи и оценить остаток корма", category = TaskCategory.INSPECTION, importance = Importance.HIGH, dueDateMillis = today.atStartOfMillis(), linkedHiveUuids = savedHives.take(2).map { it.uuid }, uuid = UUID.randomUUID().toString(), updatedAt = now),
            CalendarTask(0, today.year, today.monthValue, apiaryUuid = apiaryUuid, title = "Подготовить рамки", shortDescription = "Подготовить рамки к расширению гнезда", category = TaskCategory.MAINTENANCE, dueDateMillis = today.plusDays(5).atStartOfMillis(), linkedHiveUuids = savedHives.map { it.uuid }, uuid = UUID.randomUUID().toString(), updatedAt = now),
            CalendarTask(0, today.year, today.monthValue, apiaryUuid = apiaryUuid, title = "Провести контрольный осмотр", shortDescription = "Проверить матку и расплод", category = TaskCategory.INSPECTION, dueDateMillis = today.minusDays(5).atStartOfMillis(), linkedHiveUuids = listOf(savedHives[1].uuid), uuid = UUID.randomUUID().toString(), updatedAt = now),
            CalendarTask(0, today.year, today.monthValue, apiaryUuid = apiaryUuid, title = "Обработка после сезона", shortDescription = "Выполнить обработку по инструкции препарата", category = TaskCategory.TREATMENT, importance = Importance.HIGH, dueDateMillis = today.minusDays(20).atStartOfMillis(), linkedHiveUuids = listOf(savedHives[3].uuid), uuid = UUID.randomUUID().toString(), updatedAt = now),
            CalendarTask(0, today.year, today.monthValue, apiaryUuid = apiaryUuid, title = "Откачка мёда", shortDescription = "Демонстрационная выполненная работа", category = TaskCategory.HARVEST, isDone = true, dueDateMillis = today.minusDays(2).atStartOfMillis(), harvestItems = listOf(HarvestItem(HarvestProduct.HONEY, 12.5, HarvestUnit.KG), HarvestItem(HarvestProduct.POLLEN, 1.8)), linkedHiveUuids = listOf(savedHives[0].uuid), uuid = UUID.randomUUID().toString(), updatedAt = now),
            CalendarTask(0, today.year, today.monthValue, apiaryUuid = apiaryUuid, title = "Работа без точной даты", shortDescription = "Пример старой задачи, привязанной только к месяцу", category = TaskCategory.OTHER, linkedHiveUuids = listOf(savedHives[2].uuid), uuid = UUID.randomUUID().toString(), updatedAt = now)
        )
        tasks.forEach { calendarRepository.addTask(it) }
    }

    private fun LocalDate.atStartOfMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}