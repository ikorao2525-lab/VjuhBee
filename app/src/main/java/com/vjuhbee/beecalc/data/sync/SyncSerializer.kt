package com.vjuhbee.beecalc.data.sync

import org.json.JSONArray
import org.json.JSONObject

/**
 * Сериализация/десериализация SyncFile в JSON (org.json, без доп. библиотек).
 * Схема: top-level объект с полями version/exportedAt и массивами сущностей.
 */
object SyncSerializer {

    fun encode(file: SyncFile): String {
        val root = JSONObject()
        root.put("version", file.version)
        root.put("exportedAt", file.exportedAt)
        root.put("appVersion", file.appVersion)
        root.put("source", file.source)
        root.put("hives", JSONArray().apply {
            file.hives.forEach {
                put(JSONObject().apply {
                    put("uuid", it.uuid)
                    put("name", it.name)
                    put("note", it.note)
                    put("updatedAt", it.updatedAt)
                })
            }
        })
        root.put("inspections", JSONArray().apply {
            file.inspections.forEach {
                put(JSONObject().apply {
                    put("uuid", it.uuid)
                    put("hiveUuid", it.hiveUuid)
                    put("date", it.date)
                    put("frames", it.frames)
                    put("brood", it.brood)
                    put("queenSeen", it.queenSeen)
                    put("note", it.note)
                    put("updatedAt", it.updatedAt)
                })
            }
        })
        root.put("treatments", JSONArray().apply {
            file.treatments.forEach {
                put(JSONObject().apply {
                    put("uuid", it.uuid)
                    put("hiveUuid", it.hiveUuid)
                    put("date", it.date)
                    put("medicine", it.medicine)
                    put("dose", it.dose)
                    put("note", it.note)
                    put("updatedAt", it.updatedAt)
                })
            }
        })
        root.put("tasks", JSONArray().apply {
            file.tasks.forEach {
                put(JSONObject().apply {
                    put("uuid", it.uuid)
                    put("year", it.year)
                    put("month", it.month)
                    put("dueDateMillis", it.dueDateMillis ?: JSONObject.NULL)
                    put("title", it.title)
                    put("shortDescription", it.shortDescription)
                    put("fullDescription", it.fullDescription ?: JSONObject.NULL)
                    put("category", it.category)
                    put("importance", it.importance)
                    put("tags", it.tags)
                    put("isDone", it.isDone)
                    put("isDeleted", it.isDeleted)
                    put("honeyKg", it.honeyKg ?: JSONObject.NULL)
                    put("honeyLiters", it.honeyLiters ?: JSONObject.NULL)
                    put("pollenKg", it.pollenKg ?: JSONObject.NULL)
                    put("beeBreadKg", it.beeBreadKg ?: JSONObject.NULL)
                    put("propolisGrams", it.propolisGrams ?: JSONObject.NULL)
                    put("waxKg", it.waxKg ?: JSONObject.NULL)
                    put("royalJellyGrams", it.royalJellyGrams ?: JSONObject.NULL)
                    put("harvestItems", it.harvestItems)
                    put("linkedHiveUuids", it.linkedHiveUuids)
                    put("updatedAt", it.updatedAt)
                })
            }
        })
        return root.toString()
    }

    fun decode(json: String): SyncFile {
        val root = JSONObject(json)
        validateRoot(root)
        val hives = root.optJSONArray("hives") ?: JSONArray()
        val inspections = root.optJSONArray("inspections") ?: JSONArray()
        val treatments = root.optJSONArray("treatments") ?: JSONArray()
        val tasks = root.optJSONArray("tasks") ?: JSONArray()

        val syncHives = (0 until hives.length()).map { i ->
            val o = hives.getJSONObject(i)
            SyncHive(
                uuid = o.getString("uuid"),
                name = o.optString("name", ""),
                note = o.optString("note", ""),
                updatedAt = o.optLong("updatedAt", 0L)
            )
        }
        val syncInspections = (0 until inspections.length()).map { i ->
            val o = inspections.getJSONObject(i)
            SyncInspection(
                uuid = o.getString("uuid"),
                hiveUuid = o.optString("hiveUuid", ""),
                date = o.optLong("date", 0L),
                frames = o.optInt("frames", 0),
                brood = o.optInt("brood", 0),
                queenSeen = o.optBoolean("queenSeen", false),
                note = o.optString("note", ""),
                updatedAt = o.optLong("updatedAt", 0L)
            )
        }
        val syncTreatments = (0 until treatments.length()).map { i ->
            val o = treatments.getJSONObject(i)
            SyncTreatment(
                uuid = o.getString("uuid"),
                hiveUuid = o.optString("hiveUuid", ""),
                date = o.optLong("date", 0L),
                medicine = o.optString("medicine", ""),
                dose = o.optString("dose", ""),
                note = o.optString("note", ""),
                updatedAt = o.optLong("updatedAt", 0L)
            )
        }
        val syncTasks = (0 until tasks.length()).map { i ->
            val o = tasks.getJSONObject(i)
            SyncTask(
                uuid = o.getString("uuid"),
                year = o.optInt("year", 0),
                month = o.optInt("month", 0),
                dueDateMillis = if (o.isNull("dueDateMillis")) null else o.optLong("dueDateMillis"),
                title = o.optString("title", ""),
                shortDescription = o.optString("shortDescription", ""),
                fullDescription = optNullableString(o, "fullDescription"),
                category = o.optString("category", ""),
                importance = o.optString("importance", ""),
                tags = o.optString("tags", ""),
                isDone = o.optBoolean("isDone", false),
                isDeleted = o.optBoolean("isDeleted", false),
                honeyKg = optNullableDouble(o, "honeyKg"),
                honeyLiters = optNullableDouble(o, "honeyLiters"),
                pollenKg = optNullableDouble(o, "pollenKg"),
                beeBreadKg = optNullableDouble(o, "beeBreadKg"),
                propolisGrams = optNullableDouble(o, "propolisGrams"),
                waxKg = optNullableDouble(o, "waxKg"),
                royalJellyGrams = optNullableDouble(o, "royalJellyGrams"),
                harvestItems = o.optString("harvestItems", ""),
                linkedHiveUuids = o.optString("linkedHiveUuids", ""),
                updatedAt = o.optLong("updatedAt", 0L)
            )
        }

        return SyncFile(
            version = root.optInt("version", 1),
            exportedAt = root.optLong("exportedAt", 0L),
            appVersion = root.optString("appVersion", "unknown"),
            source = root.optString("source", "BeeCalc"),
            hives = syncHives,
            inspections = syncInspections,
            treatments = syncTreatments,
            tasks = syncTasks
        )
    }

private fun validateRoot(root: JSONObject) {
        val version = root.optInt("version", 1)
        require(version == 1) { "Неподдерживаемая версия файла: $version" }
        listOf("hives", "inspections", "treatments", "tasks").forEach { key ->
            require(root.optJSONArray(key) != null) { "В файле отсутствует раздел: $key" }
        }
        listOf("hives", "inspections", "treatments", "tasks").forEach { key ->
            val array = root.getJSONArray(key)
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                require(item != null) { "Некорректная запись в разделе $key" }
                require(item.optString("uuid").isNotBlank()) { "У записи $key нет uuid" }
            }
        }
    }
    private fun validateTaskEnums(root: JSONObject) {
        val categories = com.vjuhbee.beecalc.model.TaskCategory.entries.map { it.name }.toSet()
        val importance = com.vjuhbee.beecalc.model.Importance.entries.map { it.name }.toSet()
        val products = com.vjuhbee.beecalc.model.HarvestProduct.entries.map { it.code }.toSet()
        val units = com.vjuhbee.beecalc.model.HarvestUnit.entries.map { it.code }.toSet()
        val tasks = root.getJSONArray("tasks")
        for (index in 0 until tasks.length()) {
            val item = tasks.getJSONObject(index)
            require(item.optString("category") in categories) { "Неизвестная категория работы #$index" }
            require(item.optString("importance") in importance) { "Неизвестная важность работы #$index" }
            item.optString("harvestItems", "").takeIf { it.isNotBlank() }?.split(';')?.forEach { row ->
                val parts = row.split(',')
                require(parts.size == 3 && parts[0] in products && parts[2] in units && parts[1].toDoubleOrNull()?.let { it > 0 } == true) {
                    "Некорректный урожай в работе #$index"
                }
            }
        }
    }
    private fun optNullableString(o: JSONObject, key: String): String? =
        if (o.isNull(key)) null else o.optString(key, "")

    private fun optNullableDouble(o: JSONObject, key: String): Double? =
        if (o.isNull(key)) null else o.optDouble(key, 0.0)
}
