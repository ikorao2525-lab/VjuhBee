package com.vjuhbee.beecalc.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vjuhbee.beecalc.data.db.ApiaryDao
import com.vjuhbee.beecalc.data.db.UserDao
import com.vjuhbee.beecalc.data.db.toEntity
import com.vjuhbee.beecalc.data.db.toModel
import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "active_user_apiary_prefs")

interface UserAndApiaryRepository {
    fun observeUsers(): Flow<List<UserProfile>>
    fun observeApiaries(): Flow<List<Apiary>>
    fun observeApiariesForUser(userUuid: String): Flow<List<Apiary>>

    fun observeActiveUserUuid(): Flow<String?>
    fun observeActiveApiaryUuid(): Flow<String?>

    fun observeActiveUserProfile(): Flow<UserProfile?>
    fun observeActiveApiary(): Flow<Apiary?>

    suspend fun getActiveUserUuid(): String?
    suspend fun getActiveApiaryUuid(): String?

    suspend fun setActiveUser(userUuid: String)
    suspend fun setActiveApiary(apiaryUuid: String)

    suspend fun createUser(name: String, type: ProfileType): UserProfile
    suspend fun updateUser(user: UserProfile)
    suspend fun deleteUser(userUuid: String)

    suspend fun createApiary(userUuid: String, name: String, note: String = "", address: String = ""): Apiary
    suspend fun updateApiary(apiary: Apiary)
    suspend fun deleteApiary(apiaryUuid: String)

    /** Инициализация: если в БД нет ни одного пользователя, создаём дефолтного и дефолтную пасеку. */
    suspend fun ensureDefaultUserAndApiary(): Pair<UserProfile, Apiary>
}

class RoomUserAndApiaryRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val apiaryDao: ApiaryDao
) : UserAndApiaryRepository {

    private val KEY_ACTIVE_USER = stringPreferencesKey("active_user_uuid")
    private val KEY_ACTIVE_APIARY = stringPreferencesKey("active_apiary_uuid")

    override fun observeUsers(): Flow<List<UserProfile>> =
        userDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeApiaries(): Flow<List<Apiary>> =
        apiaryDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeApiariesForUser(userUuid: String): Flow<List<Apiary>> =
        apiaryDao.observeByUser(userUuid).map { list -> list.map { it.toModel() } }

    override fun observeActiveUserUuid(): Flow<String?> =
        context.userPrefsDataStore.data.map { prefs -> prefs[KEY_ACTIVE_USER] }

    override fun observeActiveApiaryUuid(): Flow<String?> =
        context.userPrefsDataStore.data.map { prefs -> prefs[KEY_ACTIVE_APIARY] }

    override fun observeActiveUserProfile(): Flow<UserProfile?> =
        combine(observeUsers(), observeActiveUserUuid()) { users, activeUuid ->
            users.firstOrNull { it.uuid == activeUuid } ?: users.firstOrNull()
        }

    override fun observeActiveApiary(): Flow<Apiary?> =
        combine(observeApiaries(), observeActiveApiaryUuid()) { apiaries, activeUuid ->
            apiaries.firstOrNull { it.uuid == activeUuid } ?: apiaries.firstOrNull()
        }

    override suspend fun getActiveUserUuid(): String? =
        observeActiveUserUuid().firstOrNull()

    override suspend fun getActiveApiaryUuid(): String? =
        observeActiveApiaryUuid().firstOrNull()

    override suspend fun setActiveUser(userUuid: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_USER] = userUuid
        }
        // При смене пользователя переключаем активную пасеку на первую пасеку этого пользователя
        val apiaries = apiaryDao.getByUser(userUuid)
        if (apiaries.isNotEmpty()) {
            setActiveApiary(apiaries.first().uuid)
        }
    }

    override suspend fun setActiveApiary(apiaryUuid: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_APIARY] = apiaryUuid
        }
    }

    override suspend fun createUser(name: String, type: ProfileType): UserProfile {
        val now = System.currentTimeMillis()
        val user = UserProfile(
            uuid = UUID.randomUUID().toString(),
            name = name,
            type = type,
            createdAt = now,
            updatedAt = now
        )
        userDao.insert(user.toEntity())
        return user
    }

    override suspend fun updateUser(user: UserProfile) {
        val updated = user.copy(updatedAt = System.currentTimeMillis())
        userDao.update(updated.toEntity())
    }

    override suspend fun deleteUser(userUuid: String) {
        userDao.deleteByUuid(userUuid)
        // Если удалили активного пользователя, переключаемся на любого оставшегося
        val remaining = userDao.getAll()
        if (remaining.isNotEmpty()) {
            setActiveUser(remaining.first().uuid)
        }
    }

    override suspend fun createApiary(userUuid: String, name: String, note: String, address: String): Apiary {
        val now = System.currentTimeMillis()
        val apiary = Apiary(
            uuid = UUID.randomUUID().toString(),
            userUuid = userUuid,
            name = name,
            note = note,
            address = address,
            createdAt = now,
            updatedAt = now
        )
        apiaryDao.insert(apiary.toEntity())
        return apiary
    }

    override suspend fun updateApiary(apiary: Apiary) {
        val updated = apiary.copy(updatedAt = System.currentTimeMillis())
        apiaryDao.update(updated.toEntity())
    }

    override suspend fun deleteApiary(apiaryUuid: String) {
        apiaryDao.deleteByUuid(apiaryUuid)
        // Если удалили активную пасеку, переключаемся на оставшуюся
        val activeUser = getActiveUserUuid()
        if (activeUser != null) {
            val remaining = apiaryDao.getByUser(activeUser)
            if (remaining.isNotEmpty()) {
                setActiveApiary(remaining.first().uuid)
            }
        }
    }

    override suspend fun ensureDefaultUserAndApiary(): Pair<UserProfile, Apiary> {
        val users = userDao.getAll()
        if (users.isNotEmpty()) {
            val user = users.first().toModel()
            val apiaries = apiaryDao.getByUser(user.uuid)
            val apiary = if (apiaries.isNotEmpty()) {
                apiaries.first().toModel()
            } else {
                createApiary(user.uuid, "Основная пасека")
            }
            if (getActiveUserUuid() == null) {
                setActiveUser(user.uuid)
            }
            if (getActiveApiaryUuid() == null) {
                setActiveApiary(apiary.uuid)
            }
            return Pair(user, apiary)
        }

        val now = System.currentTimeMillis()
        val user = createUser("Мой профиль", ProfileType.INDIVIDUAL)
        val apiary = createApiary(user.uuid, "Основная пасека")
        setActiveUser(user.uuid)
        setActiveApiary(apiary.uuid)
        return Pair(user, apiary)
    }
}
