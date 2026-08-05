package com.vjuhbee.beecalc.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HiveDao {

    @Query("SELECT * FROM hives ORDER BY name")
    fun observeHives(): Flow<List<HiveEntity>>

    @Query("SELECT * FROM hives WHERE id = :id")
    fun observeHive(id: Int): Flow<HiveEntity?>

    @Insert
    suspend fun insertHive(hive: HiveEntity)

    @Update
    suspend fun updateHive(hive: HiveEntity)

    /** Осмотры и обработки улья удалятся каскадом (FK). */
    @Delete
    suspend fun deleteHive(hive: HiveEntity)

    @Query("SELECT * FROM inspections WHERE hiveId = :hiveId ORDER BY date DESC")
    fun observeInspections(hiveId: Int): Flow<List<InspectionEntity>>

    @Insert
    suspend fun insertInspection(inspection: InspectionEntity)

    @Delete
    suspend fun deleteInspection(inspection: InspectionEntity)

    @Query("SELECT * FROM treatments WHERE hiveId = :hiveId ORDER BY date DESC")
    fun observeTreatments(hiveId: Int): Flow<List<TreatmentEntity>>

    @Insert
    suspend fun insertTreatment(treatment: TreatmentEntity)

    @Delete
    suspend fun deleteTreatment(treatment: TreatmentEntity)
}
