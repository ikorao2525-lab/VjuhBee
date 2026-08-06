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
    val isDeleted: Boolean = false,
    /** Сбор продукции с пасеки (все поля необязательные). */
    val honeyKg: Double? = null,
    val honeyLiters: Double? = null,
    val pollenKg: Double? = null,          // цветочная пыльца (обножка)
    val beeBreadKg: Double? = null,        // перга
    val propolisGrams: Double? = null,     // прополис — обычно граммы
    val waxKg: Double? = null,             // воск
    val royalJellyGrams: Double? = null    // маточное молочко — обычно граммы
)

/**
 * Итоги сбора продукции за год (суммы по всем работам).
 * Нули не показываются в UI.
 */
data class YearHarvestTotals(
    val honeyKg: Double = 0.0,
    val honeyLiters: Double = 0.0,
    val pollenKg: Double = 0.0,
    val beeBreadKg: Double = 0.0,
    val propolisGrams: Double = 0.0,
    val waxKg: Double = 0.0,
    val royalJellyGrams: Double = 0.0
) {
    val hasAny: Boolean
        get() = honeyKg > 0 || honeyLiters > 0 || pollenKg > 0 ||
            beeBreadKg > 0 || propolisGrams > 0 || waxKg > 0 || royalJellyGrams > 0
}

/** Улей (SPEC.md §7). */
data class Hive(
    val id: Int,          // 0 = новый, id выдаст база
    val name: String,
    val note: String = ""
)

/** Осмотр улья (SPEC.md §7). */
data class Inspection(
    val id: Int,
    val hiveId: Int,
    val date: Long,       // millis
    val frames: Int,      // всего рамок
    val brood: Int,       // рамок с расплодом
    val queenSeen: Boolean,
    val note: String = ""
)

/** Обработка/лечение улья (SPEC.md §7). */
data class Treatment(
    val id: Int,
    val hiveId: Int,
    val date: Long,       // millis
    val medicine: String,
    val dose: String = "",
    val note: String = ""
)
