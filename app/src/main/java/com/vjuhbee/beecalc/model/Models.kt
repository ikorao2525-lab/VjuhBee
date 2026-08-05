package com.vjuhbee.beecalc.model

/**
 * Модели данных приложения (SPEC.md §5).
 * Здесь нет никакой логики — только описание структур.
 */

/** Пресет концентрации сиропа. Пресеты — данные, а не зашитые константы (SPEC.md §5.1). */
data class SyrupPreset(
    val id: String,
    val ratioLabel: String,        // «1:1», «1.5:1», «2:1»
    val name: String,              // «жидкий», «средний», «густой»
    val sugarPerLiterWater: Double // k: кг сахара на 1 л воды (SPEC.md §6)
)

/** Результат расчёта сиропа. */
data class SyrupResult(
    val waterLiters: Double,
    val sugarKg: Double
)

/** Категории работ в календаре (SPEC.md §5.2). Русские названия — в strings.xml. */
enum class TaskCategory {
    INSPECTION,  // Осмотр
    FEEDING,     // Кормление
    TREATMENT,   // Лечение / обработки
    MAINTENANCE, // Уход
    HARVEST,     // Медосбор
    SEASONAL,    // Подготовка к сезону
    OTHER        // Прочее
}

/** Важность работы (опциональное поле, SPEC.md §5.2). */
enum class Importance {
    NORMAL,
    HIGH
}

/**
 * Работа в сезонном календаре (SPEC.md §5.2).
 * У каждого года — свой набор работ: в новом году список копируется
 * из прошлого со сброшенными отметками, а прошлый год остаётся архивом.
 * Удалённые работы не пропадают, а попадают в корзину (isDeleted).
 */
data class CalendarTask(
    val id: Int,                         // 0 = новая, id выдаст база
    val year: Int = 0,                   // год выставляется при сохранении
    val month: Int,                      // 1..12
    val title: String,
    val shortDescription: String,
    val fullDescription: String? = null,
    val category: TaskCategory,
    val importance: Importance = Importance.NORMAL,
    val tags: List<String> = emptyList(),
    val isDone: Boolean = false,
    val isDeleted: Boolean = false
)

/** Статус премиума (SPEC.md §4): модель заложена, UI в v0.1 нет. */
enum class PremiumState {
    FREE,
    PREMIUM
}
