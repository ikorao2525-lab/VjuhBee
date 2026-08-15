package com.vjuhbee.beecalc.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.UserProfile

@Entity(
    tableName = "users",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String,
    val name: String,
    val type: String = ProfileType.INDIVIDUAL.code,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Entity(
    tableName = "apiaries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["userUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index("userUuid")
    ]
)
data class ApiaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String,
    val userUuid: String,
    val name: String,
    val note: String = "",
    val address: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun UserEntity.toModel(): UserProfile {
    val profileType = if (type == ProfileType.COMPANY.code) ProfileType.COMPANY else ProfileType.INDIVIDUAL
    return UserProfile(
        id = id,
        uuid = uuid,
        name = name,
        type = profileType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun UserProfile.toEntity(): UserEntity = UserEntity(
    id = id,
    uuid = uuid,
    name = name,
    type = type.code,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ApiaryEntity.toModel(): Apiary = Apiary(
    id = id,
    uuid = uuid,
    userUuid = userUuid,
    name = name,
    note = note,
    address = address,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Apiary.toEntity(): ApiaryEntity = ApiaryEntity(
    id = id,
    uuid = uuid,
    userUuid = userUuid,
    name = name,
    note = note,
    address = address,
    createdAt = createdAt,
    updatedAt = updatedAt
)
