package com.vjuhbee.beecalc.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory

/**
 * Строка таблицы календаря. Enum-ы храним текстом (имя константы),
 * теги — одной строкой через запятую: без конвертеров и лишней магии.
 */
@Entity(
    tableName = "calendar_tasks",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class CalendarTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
    val linkedHiveUuids: String,
    val uuid: String = "",
    val updatedAt: Long = 0L
)

fun CalendarTaskEntity.toModel() = CalendarTask(
    id = id,
    year = year,
    month = month,
    title = title,
    shortDescription = shortDescription,
    fullDescription = fullDescription,
    category = TaskCategory.valueOf(category),
    importance = Importance.valueOf(importance),
    tags = if (tags.isBlank()) emptyList() else tags.split(","),
    isDone = isDone,
    isDeleted = isDeleted,
    honeyKg = honeyKg,
    honeyLiters = honeyLiters,
    pollenKg = pollenKg,
    beeBreadKg = beeBreadKg,
    propolisGrams = propolisGrams,
    waxKg = waxKg,
    royalJellyGrams = royalJellyGrams,
    linkedHiveUuids = if (linkedHiveUuids.isBlank()) emptyList() else linkedHiveUuids.split(",").filter { it.isNotBlank() },
    uuid = uuid,
    updatedAt = updatedAt
)

fun CalendarTask.toEntity() = CalendarTaskEntity(
    id = id,
    year = year,
    month = month,
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
    linkedHiveUuids = linkedHiveUuids.joinToString(","),
    uuid = uuid,
    updatedAt = updatedAt
)
