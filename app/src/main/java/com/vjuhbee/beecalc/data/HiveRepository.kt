package com.vjuhbee.beecalc.data

import com.vjuhbee.beecalc.data.db.HiveDao
import com.vjuhbee.beecalc.data.db.toEntity
import com.vjuhbee.beecalc.data.db.toModel
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Учёт ульев (SPEC.md §7): CRUD ульев, осмотры, обработки, история. */
interface HiveRepository {
    fun observeHives(): Flow<List<Hive>>
    fun observeHive(id: Int): Flow<Hive?>
    /** Найти улей по постоянному uuid (для открытия по QR, SPEC.md §9 v0.4). */
    suspend fun findHiveByUuid(uuid: String): Hive?
    suspend fun addHive(hive: Hive)
    suspend fun updateHive(hive: Hive)

    /** Удаляет улей вместе со всей историей (каскад в базе). */
    suspend fun deleteHive(hive: Hive)

    fun observeInspections(hiveId: Int): Flow<List<Inspection>>
    suspend fun addInspection(inspection: Inspection)
    suspend fun deleteInspection(inspection: Inspection)

    fun observeTreatments(hiveId: Int): Flow<List<Treatment>>
    suspend fun addTreatment(treatment: Treatment)
    suspend fun deleteTreatment(treatment: Treatment)
}

class RoomHiveRepository(private val dao: HiveDao) : HiveRepository {

    override fun observeHives(): Flow<List<Hive>> =
        dao.observeHives().map { entities -> entities.map { it.toModel() } }

    override fun observeHive(id: Int): Flow<Hive?> =
        dao.observeHive(id).map { it?.toModel() }

    override suspend fun findHiveByUuid(uuid: String): Hive? =
        dao.findHiveByUuid(uuid)?.toModel()

    override suspend fun addHive(hive: Hive) = dao.insertHive(hive.toEntity())

    override suspend fun updateHive(hive: Hive) = dao.updateHive(hive.toEntity())

    override suspend fun deleteHive(hive: Hive) = dao.deleteHive(hive.toEntity())

    override fun observeInspections(hiveId: Int): Flow<List<Inspection>> =
        dao.observeInspections(hiveId).map { entities -> entities.map { it.toModel() } }

    override suspend fun addInspection(inspection: Inspection) =
        dao.insertInspection(inspection.toEntity())

    override suspend fun deleteInspection(inspection: Inspection) =
        dao.deleteInspection(inspection.toEntity())

    override fun observeTreatments(hiveId: Int): Flow<List<Treatment>> =
        dao.observeTreatments(hiveId).map { entities -> entities.map { it.toModel() } }

    override suspend fun addTreatment(treatment: Treatment) =
        dao.insertTreatment(treatment.toEntity())

    override suspend fun deleteTreatment(treatment: Treatment) =
        dao.deleteTreatment(treatment.toEntity())
}
