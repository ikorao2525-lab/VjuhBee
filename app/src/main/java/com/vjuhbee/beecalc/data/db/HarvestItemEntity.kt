package com.vjuhbee.beecalc.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjuhbee.beecalc.model.HarvestItem
import com.vjuhbee.beecalc.model.HarvestProduct
import com.vjuhbee.beecalc.model.HarvestUnit

@Entity(tableName = "harvest_items", indices = [Index("taskUuid")])
data class HarvestItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskUuid: String,
    val product: String,
    val amount: Double,
    val unit: String
)

fun HarvestItemEntity.toModel() = HarvestItem(
    product = HarvestProduct.entries.firstOrNull { it.code == product } ?: HarvestProduct.HONEY,
    amount = amount,
    unit = HarvestUnit.entries.firstOrNull { it.code == unit } ?: HarvestUnit.KG
)

fun HarvestItem.toEntity(taskUuid: String) = HarvestItemEntity(
    taskUuid = taskUuid,
    product = product.code,
    amount = amount,
    unit = unit.code
)