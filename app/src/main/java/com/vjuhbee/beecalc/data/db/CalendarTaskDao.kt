package com.vjuhbee.beecalc.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarTaskDao {

    /** Flow: календарь сам обновится при любом изменении данных. */
    @Query("SELECT * FROM calendar_tasks ORDER BY month, id")
    fun observeAll(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT COUNT(*) FROM calendar_tasks")
    suspend fun count(): Int

    @Insert
    suspend fun insert(task: CalendarTaskEntity)

    @Insert
    suspend fun insertAll(tasks: List<CalendarTaskEntity>)

    @Update
    suspend fun update(task: CalendarTaskEntity)

    @Delete
    suspend fun delete(task: CalendarTaskEntity)
}
