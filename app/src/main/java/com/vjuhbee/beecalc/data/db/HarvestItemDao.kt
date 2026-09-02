package com.vjuhbee.beecalc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HarvestItemDao {
    @Query("SELECT * FROM harvest_items WHERE taskUuid = :taskUuid ORDER BY id")
    suspend fun forTask(taskUuid: String): List<HarvestItemEntity>

    @Query("SELECT * FROM harvest_items")
    fun observeAll(): Flow<List<HarvestItemEntity>>

    @Query("SELECT * FROM harvest_items")
    suspend fun all(): List<HarvestItemEntity>

    @Insert
    suspend fun insertAll(items: List<HarvestItemEntity>)

    @Query("DELETE FROM harvest_items WHERE taskUuid = :taskUuid")
    suspend fun deleteForTask(taskUuid: String)

    @Query("DELETE FROM harvest_items")
    suspend fun deleteAll()
}