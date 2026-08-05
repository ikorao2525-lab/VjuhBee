package com.vjuhbee.beecalc.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory

/**
 * Строка таблицы календаря. Enum-ы храним текстом (имя константы),
 * теги — одной строкой через запятую: без конвертеров и лишней магии.
 */
@Entity(tableName = "calendar_tasks")
data class CalendarTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: Int,
    val title: String,
    val shortDescription: String,
    val fullDescription: String?,
    val category: String,
    val importance: String,
    val tags: String,
    val isDone: Boolean
)

fun CalendarTaskEntity.toModel() = CalendarTask(
    id = id,
    month = month,
    title = title,
    shortDescription = shortDescription,
    fullDescription = fullDescription,
    category = TaskCategory.valueOf(category),
    importance = Importance.valueOf(importance),
    tags = if (tags.isBlank()) emptyList() else tags.split(","),
    isDone = isDone
)

fun CalendarTask.toEntity() = CalendarTaskEntity(
    id = id,
    month = month,
    title = title,
    shortDescription = shortDescription,
    fullDescription = fullDescription,
    category = category.name,
    importance = importance.name,
    tags = tags.joinToString(","),
    isDone = isDone
)
