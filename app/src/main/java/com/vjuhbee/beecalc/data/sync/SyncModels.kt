package com.vjuhbee.beecalc.data.sync

import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import com.vjuhbee.beecalc.model.HarvestItem
import com.vjuhbee.beecalc.model.HarvestProduct
import com.vjuhbee.beecalc.model.HarvestUnit

/**
 * Переносимые представления данных пасеки (SPEC.md §9, v0.4).
 * Идентификация — по стабильному uuid, локальные int-id не сохраняются
 * (они различаются между устройствами). Внутренние связи (hiveId)
 * храним тоже по uuid улья.
 *
 * Эти DTO сериализуются в JSON (экспорт) и разбираются обратно (импорт).
 * `version` — формат файла для возможных будущих миграций схемы.
 */
data class SyncFile(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val appVersion: String = "unknown",
    val source: String = "BeeCalc",
    val hives: List<SyncHive> = emptyList(),
    val inspections: List<SyncInspection> = emptyList(),
    val treatments: List<SyncTreatment> = emptyList(),
    val tasks: List<SyncTask> = emptyList()
)

data class SyncHive(
    val uuid: String,
    val name: String,
    val note: String,
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

fun Hive.toSync() = SyncHive(
    uuid = uuid, name = name, note = note, updatedAt = updatedAt
)

fun Inspection.toSync() = SyncInspection(
    uuid = uuid, hiveUuid = "", date = date, frames = frames, brood = brood,
    queenSeen = queenSeen, note = note, updatedAt = updatedAt
)

fun Treatment.toSync() = SyncTreatment(
    uuid = uuid, hiveUuid = "", date = date, medicine = medicine,
    dose = dose, note = note, updatedAt = updatedAt
)

fun CalendarTask.toSync() = SyncTask(
    uuid = uuid, year = year, month = month, title = title,
    shortDescription = shortDescription, fullDescription = fullDescription,
    category = category.name, importance = importance.name, tags = tags.joinToString(","),
    isDone = isDone, isDeleted = isDeleted,
    honeyKg = honeyKg, honeyLiters = honeyLiters,
    pollenKg = pollenKg, beeBreadKg = beeBreadKg,
    propolisGrams = propolisGrams, waxKg = waxKg, royalJellyGrams = royalJellyGrams,
    harvestItems = harvestItems.toSyncValue(),
    linkedHiveUuids = linkedHiveUuids.joinToString(","),
    updatedAt = updatedAt
)

fun List<HarvestItem>.toSyncValue(): String = joinToString(";") { "${it.product.code},${it.amount},${it.unit.code}" }

fun String.toHarvestItems(): List<HarvestItem> = split(';').mapNotNull { row ->
    val parts = row.split(',')
    if (parts.size != 3) return@mapNotNull null
    val product = HarvestProduct.entries.firstOrNull { it.code == parts[0] } ?: return@mapNotNull null
    val unit = HarvestUnit.entries.firstOrNull { it.code == parts[2] } ?: return@mapNotNull null
    parts[1].toDoubleOrNull()?.takeIf { it > 0 }?.let { HarvestItem(product, it, unit) }
}