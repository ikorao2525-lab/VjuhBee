package com.vjuhbee.beecalc.data

import com.vjuhbee.beecalc.model.CalendarTask
import com.vjuhbee.beecalc.model.Importance
import com.vjuhbee.beecalc.model.TaskCategory

/**
 * Источник данных календаря (SPEC.md §5.2).
 * UI работает только с интерфейсом — реализацию позже можно
 * заменить на Room без переписывания экранов.
 */
interface CalendarRepository {
    fun getTasks(): List<CalendarTask>
}

/**
 * Статичная реализация для v0.1: данные зашиты в код.
 * Сроки ориентированы на среднюю полосу России — в тёплых/холодных
 * регионах работы сдвигаются на 2–4 недели.
 */
object StaticCalendarRepository : CalendarRepository {

    override fun getTasks(): List<CalendarTask> = tasks

    private val tasks = listOf(
        // Январь
        CalendarTask(
            id = 1, month = 1,
            title = "Контроль зимовки",
            shortDescription = "Раз в 2–3 недели прослушать семьи, не открывая ульев.",
            fullDescription = "Ровный тихий гул — всё в порядке. Громкое беспокойное гудение — " +
                "возможны проблемы: мыши, сырость, кончился корм. Тишина — постучать и послушать отклик.",
            category = TaskCategory.INSPECTION
        ),
        CalendarTask(
            id = 2, month = 1,
            title = "Проверка вентиляции и летков",
            shortDescription = "Летки не забиты подмором и льдом, вентиляция работает.",
            category = TaskCategory.MAINTENANCE
        ),
        CalendarTask(
            id = 3, month = 1,
            title = "Защита от птиц и мышей",
            shortDescription = "Проверить заградители, отпугнуть синиц, стучащих по улью.",
            category = TaskCategory.OTHER
        ),

        // Февраль
        CalendarTask(
            id = 4, month = 2,
            title = "Прослушивание семей",
            shortDescription = "В конце зимовки семьи беспокойнее: контроль каждые 1–2 недели.",
            category = TaskCategory.INSPECTION
        ),
        CalendarTask(
            id = 5, month = 2,
            title = "Подкормка канди при нехватке корма",
            shortDescription = "Если корма мало — положить канди на рамки под холстик.",
            fullDescription = "Сироп зимой не дают. Канди кладут лепёшкой 0.5–1 кг прямо над клубом. " +
                "Проверять расход каждые 2–3 недели.",
            category = TaskCategory.FEEDING,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 6, month = 2,
            title = "Подготовка инвентаря",
            shortDescription = "Ремонт ульев и рамок, наващивание, закупка вощины.",
            category = TaskCategory.SEASONAL
        ),

        // Март
        CalendarTask(
            id = 7, month = 3,
            title = "Первый весенний облёт",
            shortDescription = "В тёплый день (от +8…+10 °C) проследить за облётом семей.",
            fullDescription = "Дружный облёт — семья в порядке. Семьи, которые не облетелись " +
                "или облетываются вяло, осмотреть в первую очередь.",
            category = TaskCategory.INSPECTION,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 8, month = 3,
            title = "Беглый осмотр после облёта",
            shortDescription = "Проверить корм, наличие матки и расплода, убрать сырые рамки.",
            category = TaskCategory.INSPECTION
        ),
        CalendarTask(
            id = 9, month = 3,
            title = "Чистка доньев",
            shortDescription = "Убрать подмор и мусор, заменить или почистить донья.",
            category = TaskCategory.MAINTENANCE
        ),

        // Апрель
        CalendarTask(
            id = 10, month = 4,
            title = "Главная весенняя ревизия",
            shortDescription = "Полный осмотр всех семей в тёплый день (от +14…+15 °C).",
            fullDescription = "Оценить силу семьи, количество корма (не меньше 8–10 кг), качество матки " +
                "по расплоду, состояние гнезда. Слабые семьи — сократить и утеплить, безматочные — исправить.",
            category = TaskCategory.INSPECTION,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 11, month = 4,
            title = "Сокращение и утепление гнёзд",
            shortDescription = "Убрать лишние рамки, поставить утепление: расплоду нужно тепло.",
            category = TaskCategory.MAINTENANCE
        ),
        CalendarTask(
            id = 12, month = 4,
            title = "Весенняя обработка от варроатоза",
            shortDescription = "Обработать семьи до начала главного взятка (по инструкции препарата).",
            category = TaskCategory.TREATMENT,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 13, month = 4,
            title = "Стимулирующая подкормка",
            shortDescription = "При слабом взятке — жидкий сироп 1:1 малыми порциями.",
            category = TaskCategory.FEEDING
        ),

        // Май
        CalendarTask(
            id = 14, month = 5,
            title = "Расширение гнёзд",
            shortDescription = "Подставлять рамки с вощиной по мере роста семей.",
            fullDescription = "Не опаздывать: теснота — главная причина ранних роёв. " +
                "Сильным семьям ставить вощину между крайней рамкой расплода и кормовой.",
            category = TaskCategory.MAINTENANCE,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 15, month = 5,
            title = "Контроль роевого настроения",
            shortDescription = "Раз в 7–9 дней проверять наличие роевых маточников.",
            category = TaskCategory.INSPECTION
        ),
        CalendarTask(
            id = 16, month = 5,
            title = "Вывод и замена маток",
            shortDescription = "Заложить вывод маток, наметить семьи на замену старых.",
            category = TaskCategory.OTHER
        ),

        // Июнь
        CalendarTask(
            id = 17, month = 6,
            title = "Противороевые меры",
            shortDescription = "Отводки, расширение, вентиляция — не дать семьям зароиться.",
            fullDescription = "При роевых маточниках: сформировать отводок на старую матку " +
                "или использовать другой противороевой приём. Простое срывание маточников проблему не решает.",
            category = TaskCategory.MAINTENANCE,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 18, month = 6,
            title = "Постановка магазинов и корпусов",
            shortDescription = "К началу взятка дать семьям место под нектар.",
            category = TaskCategory.HARVEST
        ),
        CalendarTask(
            id = 19, month = 6,
            title = "Контроль засева и качества маток",
            shortDescription = "Проверить сплошность засева у молодых маток.",
            category = TaskCategory.INSPECTION
        ),

        // Июль
        CalendarTask(
            id = 20, month = 7,
            title = "Главный медосбор",
            shortDescription = "Следить за заполнением магазинов, вовремя подставлять пустые.",
            category = TaskCategory.HARVEST,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 21, month = 7,
            title = "Откачка зрелого мёда",
            shortDescription = "Откачивать запечатанные не меньше чем на треть рамки.",
            category = TaskCategory.HARVEST
        ),
        CalendarTask(
            id = 22, month = 7,
            title = "Контроль кормов в гнезде",
            shortDescription = "Не выкачивать гнездовой мёд — оставлять запас семье.",
            category = TaskCategory.INSPECTION
        ),

        // Август
        CalendarTask(
            id = 23, month = 8,
            title = "Снятие магазинов, последняя откачка",
            shortDescription = "После окончания взятка снять магазины и откачать товарный мёд.",
            category = TaskCategory.HARVEST
        ),
        CalendarTask(
            id = 24, month = 8,
            title = "Обработка от варроатоза после откачки",
            shortDescription = "Главная противоклещевая обработка сезона — сразу после снятия магазинов.",
            fullDescription = "Именно августовская обработка решает, какими пойдут в зиму «зимние» пчёлы. " +
                "Опоздание на 2–3 недели заметно ослабляет зимовку.",
            category = TaskCategory.TREATMENT,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 25, month = 8,
            title = "Начало осенней закормки",
            shortDescription = "Начать давать густой сироп 2:1 на зимние корма.",
            category = TaskCategory.FEEDING,
            importance = Importance.HIGH
        ),

        // Сентябрь
        CalendarTask(
            id = 26, month = 9,
            title = "Осенняя ревизия",
            shortDescription = "Оценить силу семей, количество и качество зимних кормов.",
            fullDescription = "На зиму средней семье нужно 18–25 кг корма. Падевый мёд для зимовки " +
                "непригоден — заменить сиропом.",
            category = TaskCategory.INSPECTION,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 27, month = 9,
            title = "Завершение закормки в зиму",
            shortDescription = "Закончить давать сироп до устойчивого похолодания.",
            category = TaskCategory.FEEDING,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 28, month = 9,
            title = "Сборка гнёзд на зиму",
            shortDescription = "Собрать гнёзда по силе семей, убрать лишние рамки.",
            category = TaskCategory.SEASONAL
        ),

        // Октябрь
        CalendarTask(
            id = 29, month = 10,
            title = "Окончательная сборка гнёзд",
            shortDescription = "Проверить сборку, поставить утепление по сезону.",
            category = TaskCategory.SEASONAL
        ),
        CalendarTask(
            id = 30, month = 10,
            title = "Заградители от мышей",
            shortDescription = "Поставить летковые заградители до первых холодов.",
            category = TaskCategory.MAINTENANCE,
            importance = Importance.HIGH
        ),
        CalendarTask(
            id = 31, month = 10,
            title = "Осенняя обработка от клеща",
            shortDescription = "Контрольная обработка в безрасплодный период (по погоде).",
            category = TaskCategory.TREATMENT
        ),

        // Ноябрь
        CalendarTask(
            id = 32, month = 11,
            title = "Постановка в зимовник",
            shortDescription = "При зимовке в помещении — занести ульи после устойчивых холодов.",
            category = TaskCategory.SEASONAL
        ),
        CalendarTask(
            id = 33, month = 11,
            title = "Подготовка зимовки на воле",
            shortDescription = "Ветрозащита, снегозадержание, контроль летков.",
            category = TaskCategory.SEASONAL
        ),

        // Декабрь
        CalendarTask(
            id = 34, month = 12,
            title = "Покой на пасеке",
            shortDescription = "Не беспокоить семьи. Контроль снаружи раз в 3–4 недели.",
            category = TaskCategory.INSPECTION
        ),
        CalendarTask(
            id = 35, month = 12,
            title = "Ремонт и заготовка инвентаря",
            shortDescription = "Чинить ульи и рамки, планировать закупки к сезону.",
            category = TaskCategory.SEASONAL
        ),
        CalendarTask(
            id = 36, month = 12,
            title = "Планирование сезона",
            shortDescription = "Итоги года: мёд, зимовка, план расширения пасеки.",
            category = TaskCategory.OTHER
        )
    )
}
