package com.vjuhbee.beecalc.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment

/**
 * Таблицы учёта ульев (SPEC.md §7).
 * Осмотры и обработки привязаны к улью; при удалении улья
 * его история удаляется каскадом.
 */

@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val note: String
)

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = HiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["hiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hiveId")]
)
data class InspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hiveId: Int,
    val date: Long,
    val frames: Int,
    val brood: Int,
    val queenSeen: Boolean,
    val note: String
)

@Entity(
    tableName = "treatments",
    foreignKeys = [
        ForeignKey(
            entity = HiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["hiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hiveId")]
)
data class TreatmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hiveId: Int,
    val date: Long,
    val medicine: String,
    val dose: String,
    val note: String
)

fun HiveEntity.toModel() = Hive(id = id, name = name, note = note)
fun Hive.toEntity() = HiveEntity(id = id, name = name, note = note)

fun InspectionEntity.toModel() = Inspection(
    id = id, hiveId = hiveId, date = date,
    frames = frames, brood = brood, queenSeen = queenSeen, note = note
)

fun Inspection.toEntity() = InspectionEntity(
    id = id, hiveId = hiveId, date = date,
    frames = frames, brood = brood, queenSeen = queenSeen, note = note
)

fun TreatmentEntity.toModel() = Treatment(
    id = id, hiveId = hiveId, date = date,
    medicine = medicine, dose = dose, note = note
)

fun Treatment.toEntity() = TreatmentEntity(
    id = id, hiveId = hiveId, date = date,
    medicine = medicine, dose = dose, note = note
)
