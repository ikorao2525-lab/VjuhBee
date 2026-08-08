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

/** Вид продукции, стабильный код хранится в базе и файле синхронизации. */
enum class HarvestProduct(val code: String, val defaultUnit: HarvestUnit) {
    HONEY("honey", HarvestUnit.KG),
    POLLEN("pollen", HarvestUnit.KG),
    BEE_BREAD("bee_bread", HarvestUnit.KG),
    PROPOLIS("propolis", HarvestUnit.GRAM),
    WAX("wax", HarvestUnit.KG),
    ROYAL_JELLY("royal_jelly", HarvestUnit.GRAM),
    CAPPINGS("cappings", HarvestUnit.KG),
    WAX_MERVA("wax_merva", HarvestUnit.KG),
    BEE_VENOM("bee_venom", HarvestUnit.GRAM),
    WINTER_BEES("winter_bees", HarvestUnit.KG),
    QUEENS("queens", HarvestUnit.PIECE),
    NUCLEUS_COLONIES("nucleus_colonies", HarvestUnit.PIECE),
    PACKAGE_BEES("package_bees", HarvestUnit.PIECE)
}

enum class HarvestUnit(val code: String, val label: String) { KG("kg", "кг"), LITER("l", "л"), GRAM("g", "г"), PIECE("piece", "шт") }

data class HarvestItem(
    val product: HarvestProduct,
    val amount: Double,
    val unit: HarvestUnit = product.defaultUnit
)
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
    /** Список фактически собранной продукции. */
    val harvestItems: List<HarvestItem> = emptyList(),
    @Deprecated("Use harvestItems") val honeyKg: Double? = null,
    @Deprecated("Use harvestItems") val honeyLiters: Double? = null,
    @Deprecated("Use harvestItems") val pollenKg: Double? = null,
    @Deprecated("Use harvestItems") val beeBreadKg: Double? = null,
    @Deprecated("Use harvestItems") val propolisGrams: Double? = null,
    @Deprecated("Use harvestItems") val waxKg: Double? = null,
    @Deprecated("Use harvestItems") val royalJellyGrams: Double? = null,
    /**
     * Привязка работы к ульям (SPEC.md §5.2, v0.5).
     * Список **uuid** ульев (не внутренних id — они различаются между
     * телефонами и меняются при восстановлении). Хранится строкой через
     * запятую в базе, как теги; переносится при синхронизации.
     */
    val linkedHiveUuids: List<String> = emptyList(),
    /** Стабильный ключ для синхронизации. */
    val uuid: String = "",
    /** Время последнего изменения (мс). */
    val updatedAt: Long = 0L
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
    val note: String = "",
    /**
     * Постоянный публичный идентификатор улья (SPEC.md §9, v0.4).
     * Не зависит от внутреннего id базы — переживает перенос базы на
     * другой телефон и используется в QR-кодах улья.
     */
    val uuid: String = "",
    /** Время последнего изменения (мс). Нужно для синхронизации. */
    val updatedAt: Long = 0L
)

/** Осмотр улья (SPEC.md §7). */
data class Inspection(
    val id: Int,
    val hiveId: Int,
    val date: Long,       // millis
    val frames: Int,      // всего рамок
    val brood: Int,       // рамок с расплодом
    val queenSeen: Boolean,
    val note: String = "",
    /** Стабильный ключ для синхронизации. */
    val uuid: String = "",
    /** Время последнего изменения (мс). */
    val updatedAt: Long = 0L
)

/** Обработка/лечение улья (SPEC.md §7). */
data class Treatment(
    val id: Int,
    val hiveId: Int,
    val date: Long,       // millis
    val medicine: String,
    val dose: String = "",
    val note: String = "",
    /** Стабильный ключ для синхронизации. */
    val uuid: String = "",
    /** Время последнего изменения (мс). */
    val updatedAt: Long = 0L
)
