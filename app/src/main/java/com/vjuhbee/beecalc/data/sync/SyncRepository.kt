package com.vjuhbee.beecalc.data.sync

import com.vjuhbee.beecalc.BuildConfig
import androidx.room.withTransaction
import com.vjuhbee.beecalc.data.CalendarRepository
import com.vjuhbee.beecalc.data.HiveRepository
import com.vjuhbee.beecalc.data.UserAndApiaryRepository
import com.vjuhbee.beecalc.data.db.BeeCalcDatabase
import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.Treatment
import com.vjuhbee.beecalc.model.UserProfile

/**
 * Экспорт и импорт (слияние) данных пасеки (SPEC.md §9, v0.4, v0.9.0).
 *
 * Поддерживает:
 * - Экспорт всей базы (все пользователи, пасеки, ульи, задачи)
 * - Экспорт только активной пасеки
 * - Импорт и объединение данных по UUID с разрешением конфликтов
 * - Точное восстановление (Full Restore) из резервной копии
 */
class SyncRepository(
    private val hiveRepository: HiveRepository,
    private val calendarRepository: CalendarRepository,
    private val userAndApiaryRepository: UserAndApiaryRepository? = null,
    private val database: BeeCalcDatabase? = null
) {

    // ---------- Экспорт ----------

    suspend fun export(scope: SyncScope = SyncScope.ALL): SyncFile {
        val activeApiaryUuid = userAndApiaryRepository?.getActiveApiaryUuid() ?: ""

        val users = if (scope == SyncScope.ALL) {
            userAndApiaryRepository?.allUsers() ?: emptyList()
        } else {
            val activeProfile = userAndApiaryRepository?.allUsers()?.firstOrNull { it.uuid == userAndApiaryRepository.getActiveUserUuid() }
            if (activeProfile != null) listOf(activeProfile) else emptyList()
        }

        val apiaries = if (scope == SyncScope.ALL) {
            userAndApiaryRepository?.allApiaries() ?: emptyList()
        } else {
            val activeApiary = userAndApiaryRepository?.allApiaries()?.firstOrNull { it.uuid == activeApiaryUuid }
            if (activeApiary != null) listOf(activeApiary) else emptyList()
        }

        val hives = if (scope == SyncScope.ALL || activeApiaryUuid.isBlank()) {
            hiveRepository.allHives()
        } else {
            hiveRepository.hivesForApiary(activeApiaryUuid)
        }

        val hiveIds = hives.map { it.id }.toSet()
        val hiveUuidById = hives.associate { it.id to it.uuid }

        val inspections = hiveRepository.allInspections().filter { it.hiveId in hiveIds }
        val treatments = hiveRepository.allTreatments().filter { it.hiveId in hiveIds }

        val tasks = if (scope == SyncScope.ALL || activeApiaryUuid.isBlank()) {
            calendarRepository.allTasks()
        } else {
            calendarRepository.allTasksForApiary(activeApiaryUuid)
        }

        return SyncFile(
            version = 2,
            scope = scope,
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            source = "BeeCalc",
            users = users.map { it.toSync() },
            apiaries = apiaries.map { it.toSync() },
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
                if (!local.equalsFields(remote)) {
                    HiveConflict(
                        local = local,
                        remote = SyncHive(remote.uuid, remote.name, remote.note, remote.apiaryUuid, remote.updatedAt)
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
                (local.isDeleted && !remote.isDeleted) || local.updatedAt < remote.updatedAt
            }
        )
    }

    suspend fun apply(file: SyncFile, plan: ImportPlan, resolveConflict: (HiveConflict) -> Boolean?) {
        val execute: suspend () -> Unit = { applyUnsafe(file, plan, resolveConflict) }
        database?.withTransaction { execute() } ?: execute()
    }

    private suspend fun applyUnsafe(file: SyncFile, plan: ImportPlan, resolveConflict: (HiveConflict) -> Boolean?) {
        val targetApiaryUuid = userAndApiaryRepository?.getActiveApiaryUuid() ?: ""

        // 1) Импорт пользователей и пасек (если есть в файле)
        if (file.users.isNotEmpty() && userAndApiaryRepository != null) {
            val localUsers = userAndApiaryRepository.allUsers().associateBy { it.uuid }
            for (u in file.users) {
                if (u.uuid !in localUsers) {
                    val profileType = if (u.type == ProfileType.COMPANY.code) ProfileType.COMPANY else ProfileType.INDIVIDUAL
                    userAndApiaryRepository.insertUserDirect(
                        UserProfile(
                            uuid = u.uuid,
                            name = u.name,
                            type = profileType,
                            createdAt = u.createdAt,
                            updatedAt = u.updatedAt
                        )
                    )
                }
            }
        }

        if (file.apiaries.isNotEmpty() && userAndApiaryRepository != null) {
            val localApiaries = userAndApiaryRepository.allApiaries().associateBy { it.uuid }
            for (a in file.apiaries) {
                if (a.uuid !in localApiaries) {
                    userAndApiaryRepository.insertApiaryDirect(
                        Apiary(
                            uuid = a.uuid,
                            userUuid = a.userUuid,
                            name = a.name,
                            note = a.note,
                            address = a.address,
                            createdAt = a.createdAt,
                            updatedAt = a.updatedAt
                        )
                    )
                }
            }
        }

        // 2) Ульи
        for (remote in plan.newHives) {
            val hiveApiary = remote.apiaryUuid.ifBlank { targetApiaryUuid }
            hiveRepository.addHive(
                Hive(
                    id = 0,
                    name = remote.name,
                    note = remote.note,
                    apiaryUuid = hiveApiary,
                    uuid = remote.uuid,
                    updatedAt = remote.updatedAt
                )
            )
        }

        for (conflict in plan.hiveConflicts) {
            val choice = resolveConflict(conflict) ?: continue
            if (choice) {
                updateHiveByUuid(conflict.remote.uuid) {
                    val hiveApiary = conflict.remote.apiaryUuid.ifBlank { it.apiaryUuid }.ifBlank { targetApiaryUuid }
                    Hive(
                        id = it.id,
                        name = conflict.remote.name,
                        note = conflict.remote.note,
                        apiaryUuid = hiveApiary,
                        uuid = conflict.remote.uuid,
                        updatedAt = conflict.remote.updatedAt
                    )
                }
            }
        }

        // 3) Осмотры и обработки
        val allHivesAfter = hiveRepository.allHives()
        val uuidToId = allHivesAfter.associate { it.uuid to it.id }

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
                local.copy(
                    hiveId = hiveId, date = remote.date, frames = remote.frames,
                    brood = remote.brood, queenSeen = remote.queenSeen, note = remote.note,
                    updatedAt = remote.updatedAt
                )
            )
        }

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
                local.copy(
                    hiveId = hiveId, date = remote.date, medicine = remote.medicine,
                    dose = remote.dose, note = remote.note, updatedAt = remote.updatedAt
                )
            )
        }

        // 4) Календарь
        val localTasks = calendarRepository.allTasks().associateBy { it.uuid }
        for (remote in plan.newTasks) {
            val taskApiary = remote.apiaryUuid.ifBlank { targetApiaryUuid }
            calendarRepository.addTask(remote.toModel().copy(apiaryUuid = taskApiary))
        }
        for (remote in plan.updatedTasks) {
            val local = localTasks[remote.uuid] ?: continue
            val taskApiary = remote.apiaryUuid.ifBlank { local.apiaryUuid }.ifBlank { targetApiaryUuid }
            calendarRepository.updateTask(local.copyFromSync(remote).copy(apiaryUuid = taskApiary))
        }
    }

    /** Полное точное восстановление базы из бэкапа в одной транзакции. */
    suspend fun restoreExactly(file: SyncFile) = requireNotNull(database) { "Восстановление требует Room базы" }.withTransaction {
        val db = requireNotNull(database)
        db.harvestItemDao().deleteAll()
        db.calendarTaskDao().deleteAll()
        db.hiveDao().deleteAllInspections()
        db.hiveDao().deleteAllTreatments()
        db.hiveDao().deleteAllHives()
        db.apiaryDao().deleteAll()
        db.userDao().deleteAll()

        val plan = ImportPlan(
            newHives = file.hives, hiveConflicts = emptyList(),
            newInspections = file.inspections, updatedInspections = emptyList(),
            newTreatments = file.treatments, updatedTreatments = emptyList(),
            newTasks = file.tasks, updatedTasks = emptyList()
        )
        applyUnsafe(file, plan) { false }

        // Если файл восстановил пользователей и пасеки, выставляем активные
        userAndApiaryRepository?.let { repo ->
            val firstUser = repo.allUsers().firstOrNull()
            if (firstUser != null) {
                repo.setActiveUser(firstUser.uuid)
                val firstApiary = repo.allApiaries().firstOrNull { it.userUuid == firstUser.uuid }
                if (firstApiary != null) {
                    repo.setActiveApiary(firstApiary.uuid)
                }
            } else {
                repo.ensureDefaultUserAndApiary()
            }
        }
    }

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

/** Конфликт по улью: локальная и удалённая версии. */
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
    id = 0, year = year, month = month, apiaryUuid = apiaryUuid, dueDateMillis = dueDateMillis, title = title,
    shortDescription = shortDescription, fullDescription = fullDescription,
    category = com.vjuhbee.beecalc.model.TaskCategory.valueOf(category),
    importance = com.vjuhbee.beecalc.model.Importance.valueOf(importance),
    tags = if (tags.isBlank()) emptyList() else tags.split(","),
    isDone = isDone, isDeleted = isDeleted,
    harvestItems = if (harvestItems.isBlank()) legacyHarvestItems(honeyKg, honeyLiters, pollenKg, beeBreadKg, propolisGrams, waxKg, royalJellyGrams) else harvestItems.parseHarvestItems(),
    honeyKg = honeyKg, honeyLiters = honeyLiters,
    pollenKg = pollenKg, beeBreadKg = beeBreadKg,
    propolisGrams = propolisGrams, waxKg = waxKg, royalJellyGrams = royalJellyGrams,
    linkedHiveUuids = if (linkedHiveUuids.isBlank()) emptyList() else linkedHiveUuids.split(",").filter { it.isNotBlank() },
    uuid = uuid, updatedAt = updatedAt
)

private fun CalendarTask.copyFromSync(s: SyncTask) = copy(
    year = s.year, month = s.month, apiaryUuid = s.apiaryUuid.ifBlank { apiaryUuid }, dueDateMillis = s.dueDateMillis, title = s.title,
    shortDescription = s.shortDescription, fullDescription = s.fullDescription,
    category = com.vjuhbee.beecalc.model.TaskCategory.valueOf(s.category),
    importance = com.vjuhbee.beecalc.model.Importance.valueOf(s.importance),
    tags = if (s.tags.isBlank()) emptyList() else s.tags.split(","),
    isDone = s.isDone, isDeleted = s.isDeleted,
    harvestItems = if (s.harvestItems.isBlank()) legacyHarvestItems(s.honeyKg, s.honeyLiters, s.pollenKg, s.beeBreadKg, s.propolisGrams, s.waxKg, s.royalJellyGrams) else s.harvestItems.parseHarvestItems(),
    honeyKg = s.honeyKg, honeyLiters = s.honeyLiters,
    pollenKg = s.pollenKg, beeBreadKg = s.beeBreadKg,
    propolisGrams = s.propolisGrams, waxKg = s.waxKg, royalJellyGrams = s.royalJellyGrams,
    linkedHiveUuids = if (s.linkedHiveUuids.isBlank()) emptyList() else s.linkedHiveUuids.split(",").filter { it.isNotBlank() },
    updatedAt = s.updatedAt
)
