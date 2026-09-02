package com.vjuhbee.beecalc.data.sync

import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.HarvestItem
import com.vjuhbee.beecalc.model.HarvestProduct
import com.vjuhbee.beecalc.model.HarvestUnit
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.Treatment
import com.vjuhbee.beecalc.model.UserProfile

/** Область экспорта данных: вся база или конкретная активная пасека. */
enum class SyncScope(val code: String) {
    ALL("all"),
    ACTIVE_APIARY("active_apiary")
}

/**
 * Переносимые представления данных пасеки (SPEC.md §9, v0.4, v0.9.0).
 * Идентификация — по стабильному uuid.
 * `version = 2` поддерживает пользователей (users), пасеки (apiaries) и привязку apiaryUuid.
 */
data class SyncFile(
    val version: Int = 2,
    val scope: SyncScope = SyncScope.ALL,
    val exportedAt: Long = 0L,
    val appVersion: String = "unknown",
    val source: String = "BeeCalc",
    val users: List<SyncUser> = emptyList(),
    val apiaries: List<SyncApiary> = emptyList(),
    val hives: List<SyncHive> = emptyList(),
    val inspections: List<SyncInspection> = emptyList(),
    val treatments: List<SyncTreatment> = emptyList(),
    val tasks: List<SyncTask> = emptyList()
)

data class SyncUser(
    val uuid: String,
    val name: String,
    val type: String = ProfileType.INDIVIDUAL.code,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class SyncApiary(
    val uuid: String,
    val userUuid: String,
    val name: String,
    val note: String = "",
    val address: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class SyncHive(
    val uuid: String,
    val name: String,
    val note: String,
    val apiaryUuid: String = "",
    val updatedAt: Long
)

data class SyncInspection(
    val uuid: String,
    val hiveUuid: String,
    val date: Long,
    val frames: Int,
    val brood: Int,
    val queenSeen: Boolean,
    val note: String,
    val updatedAt: Long
)

data class SyncTreatment(
    val uuid: String,
    val hiveUuid: String,
    val date: Long,
    val medicine: String,
    val dose: String,
    val note: String,
    val updatedAt: Long
)

data class SyncTask(
    val uuid: String,
    val year: Int,
    val month: Int,
    val apiaryUuid: String = "",
    val dueDateMillis: Long? = null,
    val title: String,
    val shortDescription: String,
    val fullDescription: String?,
    val category: String,
    val importance: String,
    val tags: String,
    val isDone: Boolean,
    val isDeleted: Boolean,
    val honeyKg: Double?,
    val honeyLiters: Double?,
    val pollenKg: Double?,
    val beeBreadKg: Double?,
    val propolisGrams: Double?,
    val waxKg: Double?,
    val royalJellyGrams: Double?,
    val harvestItems: String = "",
    val linkedHiveUuids: String,
    val updatedAt: Long
)

fun UserProfile.toSync() = SyncUser(
    uuid = uuid,
    name = name,
    type = type.code,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Apiary.toSync() = SyncApiary(
    uuid = uuid,
    userUuid = userUuid,
    name = name,
    note = note,
    address = address,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Hive.toSync() = SyncHive(
    uuid = uuid,
    name = name,
    note = note,
    apiaryUuid = apiaryUuid,
    updatedAt = updatedAt
)

fun Inspection.toSync() = SyncInspection(
    uuid = uuid,
    hiveUuid = "",
    date = date,
    frames = frames,
    brood = brood,
    queenSeen = queenSeen,
    note = note,
    updatedAt = updatedAt
)

fun Treatment.toSync() = SyncTreatment(
    uuid = uuid,
    hiveUuid = "",
    date = date,
    medicine = medicine,
    dose = dose,
    note = note,
    updatedAt = updatedAt
)

fun CalendarTask.toSync() = SyncTask(
    uuid = uuid,
    year = year,
    month = month,
    apiaryUuid = apiaryUuid,
    dueDateMillis = dueDateMillis,
    title = title,
    shortDescription = shortDescription,
    fullDescription = fullDescription,
    category = category.name,
    importance = importance.name,
    tags = tags.joinToString(","),
    isDone = isDone,
    isDeleted = isDeleted,
    honeyKg = honeyKg,
    honeyLiters = honeyLiters,
    pollenKg = pollenKg,
    beeBreadKg = beeBreadKg,
    propolisGrams = propolisGrams,
    waxKg = waxKg,
    royalJellyGrams = royalJellyGrams,
    harvestItems = harvestItems.toSyncValue(),
    linkedHiveUuids = linkedHiveUuids.joinToString(","),
    updatedAt = updatedAt
)

fun List<HarvestItem>.toSyncValue(): String = joinToString(";") { "${it.product.code},${it.amount},${it.unit.code}" }

fun String.parseHarvestItems(): List<HarvestItem> {
    if (isBlank()) return emptyList()
    return split(';').mapIndexed { index, row ->
        val parts = row.split(',')
        require(parts.size == 3) { "Некорректная запись урожая #${index + 1}" }
        val product = requireNotNull(HarvestProduct.entries.firstOrNull { it.code == parts[0] }) {
            "Неизвестный продукт урожая: ${parts[0]}"
        }
        val amount = requireNotNull(parts[1].toDoubleOrNull()) { "Некорректное количество урожая: ${parts[1]}" }
        val unit = requireNotNull(HarvestUnit.entries.firstOrNull { it.code == parts[2] }) {
            "Неизвестная единица урожая: ${parts[2]}"
        }
        HarvestItem(product = product, amount = amount, unit = unit)
    }
}
