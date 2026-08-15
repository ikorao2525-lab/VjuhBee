package com.vjuhbee.beecalc.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    suspend fun getAll(): List<UserEntity>

    @Query("SELECT * FROM users WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM users WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}

@Dao
interface ApiaryDao {
    @Query("SELECT * FROM apiaries ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ApiaryEntity>>

    @Query("SELECT * FROM apiaries WHERE userUuid = :userUuid ORDER BY createdAt ASC")
    fun observeByUser(userUuid: String): Flow<List<ApiaryEntity>>

    @Query("SELECT * FROM apiaries ORDER BY createdAt ASC")
    suspend fun getAll(): List<ApiaryEntity>

    @Query("SELECT * FROM apiaries WHERE userUuid = :userUuid ORDER BY createdAt ASC")
    suspend fun getByUser(userUuid: String): List<ApiaryEntity>

    @Query("SELECT * FROM apiaries WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): ApiaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiary: ApiaryEntity): Long

    @Update
    suspend fun update(apiary: ApiaryEntity)

    @Query("DELETE FROM apiaries WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}
