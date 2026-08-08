package com.vjuhbee.beecalc.data.sync

import com.vjuhbee.beecalc.BuildConfig

import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.data.HiveRepository
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import org.json.JSONArray
import org.json.JSONObject

/**
 * Экспорт и импорт (слияние) всей пасеки (SPEC.md §9, v0.4).
 *
 * Экспорт сериализует все сущности в JSON (org.json, без доп. библиотек).
 * Импорт разбирает файл и строит [ImportPlan] — сводку новых/изменённых
 * конфликтов, а затем применяет слияние по стабильному uuid.
 *
 * Идентификация — по uuid; локальные int-id и внутренние связи hiveId
 * восстанавливаются по uuid во время импорта (на разных телефонах id разные).
 */
class SyncRepository(
    private val hiveRepository: HiveRepository,
    private val calendarRepository: CalendarRepository
) {

    // ---------- Экспорт ----------

    suspend fun export(): SyncFile {
        val hives = hiveRepository.allHives()
        val inspections = hiveRepository.allInspections()
        val treatments = hiveRepository.allTreatments()
        val tasks = calendarRepository.allTasks()

        val hiveUuidById = hives.associate { it.id to it.uuid }

        return SyncFile(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            source = "BeeCalc",
            hives = hives.map { it.toSync() },
            inspections = inspections.map {
                it.toSync().copy(hiveUuid = hiveUuidById[it.hiveId] ?: "")
            },
            treatments = treatments.map {
                it.toSync().copy(hiveUuid = hiveUuidById[it.hiveId] ?: "")
            },
            tasks = tasks.map { it.toSync() }
        )
    }

    // ---------- Импорт (план + применение) ----------

    /**
     * Строит план слияния [file] с текущей базой.
     * Конфликтуют сущности одного uuid, изменённые на обоих телефонах
     * (сравнение по updatedAt требует выбора пользователем — "ask_each").
     */
    suspend fun buildPlan(file: SyncFile): ImportPlan {
        val localHives = hiveRepository.allHives()
        val localInspections = hiveRepository.allInspections()
        val localTreatments = hiveRepository.allTreatments()
        val localTasks = calendarRepository.allTasks()

        val localHivesByUuid = localHives.associateBy { it.uuid }
        val localInspByUuid = localInspections.associateBy { it.uuid }
        val localTreatByUuid = localTreatments.associateBy { it.uuid }
        val localTasksByUuid = localTasks.associateBy { it.uuid }

        return ImportPlan(
            newHives = file.hives.filter { it.uuid !in localHivesByUuid },
            hiveConflicts = file.hives.mapNotNull { remote ->
                val local = localHivesByUuid[remote.uuid] ?: return@mapNotNull null
                // Улей есть на обоих телефонах и отличается — пользователь решает.
                if (!local.equalsFields(remote)) {
                    HiveConflict(
                        local = local,
                        remote = SyncHive(remote.uuid, remote.name, remote.note, remote.updatedAt)
                    )
                } else null
            },
            newInspections = file.inspections.filter { it.uuid !in localInspByUuid },
            updatedInspections = file.inspections.filter { remote ->
                val local = localInspByUuid[remote.uuid] ?: return@filter false
                local.updatedAt < remote.updatedAt
            },
            newTreatments = file.treatments.filter { it.uuid !in localTreatByUuid },
            updatedTreatments = file.treatments.filter { remote ->
                val local = localTreatByUuid[remote.uuid] ?: return@filter false
                local.updatedAt < remote.updatedAt
            },
            newTasks = file.tasks.filter { it.uuid !in localTasksByUuid },
            updatedTasks = file.tasks.filter { remote ->
                val local = localTasksByUuid[remote.uuid] ?: return@filter false
                // Активная задача из файла восстанавливает локальную задачу из корзины.
                // Вместе с ней возвращаются linkedHiveUuids и остальные данные.
                (local.isDeleted && !remote.isDeleted) || local.updatedAt < remote.updatedAt
            }
        )
    }

    /**
     * Применяет слияние. [resolveConflicts] вызывается для каждого конфликта
     * улья и должен вернуть версию, которую оставить: null = пропустить улей,
     * false = локальную (текущую), true = удалённую (из файла).
     */
    suspend fun apply(file: SyncFile, plan: ImportPlan, resolveConflict: (HiveConflict) -> Boolean?) {
        // Новые ульи.
        for (remote in plan.newHives) {
            hiveRepository.addHive(
                Hive(id = 0, name = remote.name, note = remote.note, uuid = remote.uuid, updatedAt = remote.updatedAt)
            )
        }

        // Конфликтующие ульи — по выбору пользователя.
        for (conflict in plan.hiveConflicts) {
            val choice = resolveConflict(conflict) ?: continue
            if (choice) {
                updateHiveByUuid(conflict.remote.uuid) {
                    Hive(id = it.id, name = conflict.remote.name, note = conflict.remote.note,
                        uuid = conflict.remote.uuid, updatedAt = conflict.remote.updatedAt)
                }
            }
        }

        // 2) Связь uuid улья -> локальный id для вставки осмотров/обработок.
        val allHivesAfter = hiveRepository.allHives()
        val uuidToId = allHivesAfter.associate { it.uuid to it.id }

        // Осмотры: новые + обновлённые (по новому времени).
        val localInsp = hiveRepository.allInspections().associateBy { it.uuid }
        for (remote in plan.newInspections) {
            val hiveId = uuidToId[remote.hiveUuid] ?: continue
            hiveRepository.addInspection(
                Inspection(
                    id = 0, hiveId = hiveId, date = remote.date, frames = remote.frames,
                    brood = remote.brood, queenSeen = remote.queenSeen, note = remote.note,
                    uuid = remote.uuid, updatedAt = remote.updatedAt
                )
            )
        }
        for (remote in plan.updatedInspections) {
            val local = localInsp[remote.uuid] ?: continue
            val hiveId = uuidToId[remote.hiveUuid] ?: local.hiveId
            hiveRepository.updateInspection(
                local.copy(hiveId = hiveId, date = remote.date, frames = remote.frames,
                    brood = remote.brood, queenSeen = remote.queenSeen, note = remote.note,
                    updatedAt = remote.updatedAt)
            )
        }

        // Обработки.
        val localTreat = hiveRepository.allTreatments().associateBy { it.uuid }
        for (remote in plan.newTreatments) {
            val hiveId = uuidToId[remote.hiveUuid] ?: continue
            hiveRepository.addTreatment(
                Treatment(
                    id = 0, hiveId = hiveId, date = remote.date, medicine = remote.medicine,
                    dose = remote.dose, note = remote.note, uuid = remote.uuid, updatedAt = remote.updatedAt
                )
            )
        }
        for (remote in plan.updatedTreatments) {
            val local = localTreat[remote.uuid] ?: continue
            val hiveId = uuidToId[remote.hiveUuid] ?: local.hiveId
            hiveRepository.updateTreatment(
                local.copy(hiveId = hiveId, date = remote.date, medicine = remote.medicine,
                    dose = remote.dose, note = remote.note, updatedAt = remote.updatedAt)
            )
        }

        // 3) Календарь: новые + обновлённые.
        val localTasks = calendarRepository.allTasks().associateBy { it.uuid }
        for (remote in plan.newTasks) {
            calendarRepository.addTask(remote.toModel())
        }
        for (remote in plan.updatedTasks) {
            val local = localTasks[remote.uuid] ?: continue
            calendarRepository.updateTask(local.copyFromSync(remote))
        }
    }

    // ---------- helpers ----------

    private suspend fun updateHiveByUuid(uuid: String, mutate: (Hive) -> Hive) {
        val hive = hiveRepository.findHiveByUuid(uuid) ?: return
        hiveRepository.updateHive(mutate(hive))
    }
}

/** Сводка слияния (SPEC.md §9): что будет добавлено/изменено/конфликтует. */
data class ImportPlan(
    val newHives: List<SyncHive>,
    val hiveConflicts: List<HiveConflict>,
    val newInspections: List<SyncInspection>,
    val updatedInspections: List<SyncInspection>,
    val newTreatments: List<SyncTreatment>,
    val updatedTreatments: List<SyncTreatment>,
    val newTasks: List<SyncTask>,
    val updatedTasks: List<SyncTask>
) {
    val totalNew: Int get() = newHives.size + newInspections.size + newTreatments.size + newTasks.size
    val totalUpdated: Int get() = updatedInspections.size + updatedTreatments.size + updatedTasks.size
    val totalConflicts: Int get() = hiveConflicts.size
}

/** Конфликт по улью: локальная и удалённая версии (пользователь выбирает). */
data class HiveConflict(
    val local: Hive,
    val remote: SyncHive
)

private fun Hive.equalsFields(other: SyncHive): Boolean =
    name == other.name && note == other.note

private fun legacyHarvestItems(honeyKg: Double?, honeyLiters: Double?, pollenKg: Double?, beeBreadKg: Double?, propolisGrams: Double?, waxKg: Double?, royalJellyGrams: Double?): List<com.vjuhbee.beecalc.model.HarvestItem> = buildList {
    honeyKg?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.HONEY, it, com.vjuhbee.beecalc.model.HarvestUnit.KG)) }
    honeyLiters?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.HONEY, it, com.vjuhbee.beecalc.model.HarvestUnit.LITER)) }
    pollenKg?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.POLLEN, it)) }
    beeBreadKg?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.BEE_BREAD, it)) }
    propolisGrams?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.PROPOLIS, it, com.vjuhbee.beecalc.model.HarvestUnit.GRAM)) }
    waxKg?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.WAX, it)) }
    royalJellyGrams?.takeIf { it > 0 }?.let { add(com.vjuhbee.beecalc.model.HarvestItem(com.vjuhbee.beecalc.model.HarvestProduct.ROYAL_JELLY, it, com.vjuhbee.beecalc.model.HarvestUnit.GRAM)) }
}

private fun SyncTask.toModel() = CalendarTask(
    id = 0, year = year, month = month, title = title,
    shortDescription = shortDescription, fullDescription = fullDescription,
    category = com.vjuhbee.beecalc.model.TaskCategory.valueOf(category),
    importance = com.vjuhbee.beecalc.model.Importance.valueOf(importance),
    tags = if (tags.isBlank()) emptyList() else tags.split(","),
    isDone = isDone, isDeleted = isDeleted,
    harvestItems = harvestItems.toHarvestItems().ifEmpty { legacyHarvestItems(honeyKg, honeyLiters, pollenKg, beeBreadKg, propolisGrams, waxKg, royalJellyGrams) },
    honeyKg = honeyKg, honeyLiters = honeyLiters,
    pollenKg = pollenKg, beeBreadKg = beeBreadKg,
    propolisGrams = propolisGrams, waxKg = waxKg, royalJellyGrams = royalJellyGrams,
    linkedHiveUuids = if (linkedHiveUuids.isBlank()) emptyList() else linkedHiveUuids.split(",").filter { it.isNotBlank() },
    uuid = uuid, updatedAt = updatedAt
)

private fun CalendarTask.copyFromSync(s: SyncTask) = copy(
    year = s.year, month = s.month, title = s.title,
    shortDescription = s.shortDescription, fullDescription = s.fullDescription,
    category = com.vjuhbee.beecalc.model.TaskCategory.valueOf(s.category),
    importance = com.vjuhbee.beecalc.model.Importance.valueOf(s.importance),
    tags = if (s.tags.isBlank()) emptyList() else s.tags.split(","),
    isDone = s.isDone, isDeleted = s.isDeleted,
    harvestItems = s.harvestItems.toHarvestItems().ifEmpty { legacyHarvestItems(s.honeyKg, s.honeyLiters, s.pollenKg, s.beeBreadKg, s.propolisGrams, s.waxKg, s.royalJellyGrams) },
    honeyKg = s.honeyKg, honeyLiters = s.honeyLiters,
    pollenKg = s.pollenKg, beeBreadKg = s.beeBreadKg,
    propolisGrams = s.propolisGrams, waxKg = s.waxKg, royalJellyGrams = s.royalJellyGrams,
    linkedHiveUuids = if (s.linkedHiveUuids.isBlank()) emptyList() else s.linkedHiveUuids.split(",").filter { it.isNotBlank() },
    updatedAt = s.updatedAt
)
