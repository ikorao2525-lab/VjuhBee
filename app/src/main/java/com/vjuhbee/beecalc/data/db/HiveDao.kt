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

    @Query("SELECT * FROM hives WHERE apiaryUuid = :apiaryUuid ORDER BY name")
    fun observeHivesByApiary(apiaryUuid: String): Flow<List<HiveEntity>>

    @Query("SELECT * FROM hives WHERE id = :id")
    fun observeHive(id: Int): Flow<HiveEntity?>

    @Query("SELECT * FROM hives")
    suspend fun allHives(): List<HiveEntity>

    @Query("SELECT * FROM hives WHERE apiaryUuid = :apiaryUuid")
    suspend fun hivesByApiary(apiaryUuid: String): List<HiveEntity>

    @Query("SELECT * FROM inspections")
    suspend fun allInspections(): List<InspectionEntity>

    @Query("SELECT i.* FROM inspections i INNER JOIN hives h ON i.hiveId = h.id WHERE h.apiaryUuid = :apiaryUuid")
    suspend fun inspectionsByApiary(apiaryUuid: String): List<InspectionEntity>

    @Query("SELECT * FROM treatments")
    suspend fun allTreatments(): List<TreatmentEntity>

    @Query("SELECT t.* FROM treatments t INNER JOIN hives h ON t.hiveId = h.id WHERE h.apiaryUuid = :apiaryUuid")
    suspend fun treatmentsByApiary(apiaryUuid: String): List<TreatmentEntity>

    @Query("SELECT * FROM hives WHERE uuid = :uuid LIMIT 1")
    suspend fun findHiveByUuid(uuid: String): HiveEntity?

    @Insert
    suspend fun insertHive(hive: HiveEntity): Long

    @Update
    suspend fun updateHive(hive: HiveEntity)

    /** Осмотры и обработки улья удалятся каскадом (FK). */
    @Delete
    suspend fun deleteHive(hive: HiveEntity)

    @Query("SELECT * FROM inspections WHERE hiveId = :hiveId ORDER BY date DESC")
    fun observeInspections(hiveId: Int): Flow<List<InspectionEntity>>

    @Insert
    suspend fun insertInspection(inspection: InspectionEntity)

    @Update
    suspend fun updateInspection(inspection: InspectionEntity)

    @Delete
    suspend fun deleteInspection(inspection: InspectionEntity)

    @Query("SELECT * FROM treatments WHERE hiveId = :hiveId ORDER BY date DESC")
    fun observeTreatments(hiveId: Int): Flow<List<TreatmentEntity>>

    @Insert
    suspend fun insertTreatment(treatment: TreatmentEntity)

    @Update
    suspend fun updateTreatment(treatment: TreatmentEntity)

    @Delete
    suspend fun deleteTreatment(treatment: TreatmentEntity)
}
