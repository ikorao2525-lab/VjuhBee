package com.vjuhbee.beecalc.ui.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ApiaryManagementUiState(
    val users: List<UserProfile> = emptyList(),
    val apiaries: List<Apiary> = emptyList(),
    val activeUser: UserProfile? = null,
    val activeApiary: Apiary? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ApiariesViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as BeeCalcApp).userAndApiaryRepository

    val uiState: StateFlow<ApiaryManagementUiState> =
        combine(
            repository.observeUsers(),
            repository.observeActiveUserProfile(),
            repository.observeActiveApiary()
        ) { users, activeUser, activeApiary ->
            Triple(users, activeUser, activeApiary)
        }.flatMapLatest { (users, activeUser, activeApiary) ->
            val userUuid = activeUser?.uuid ?: ""
            repository.observeApiariesForUser(userUuid).combine(
                repository.observeActiveApiary()
            ) { apiaries, currentActiveApiary ->
                ApiaryManagementUiState(
                    users = users,
                    apiaries = apiaries,
                    activeUser = activeUser,
                    activeApiary = currentActiveApiary
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApiaryManagementUiState())

    init {
        viewModelScope.launch {
            repository.ensureDefaultUserAndApiary()
        }
    }

    fun selectUser(userUuid: String) {
        viewModelScope.launch {
            repository.setActiveUser(userUuid)
        }
    }

    fun selectApiary(apiaryUuid: String) {
        viewModelScope.launch {
            repository.setActiveApiary(apiaryUuid)
        }
    }

    fun createProfile(name: String, type: ProfileType) {
        viewModelScope.launch {
            val user = repository.createUser(name, type)
            val apiary = repository.createApiary(user.uuid, "Основная пасека")
            repository.setActiveUser(user.uuid)
            repository.setActiveApiary(apiary.uuid)
        }
    }

    fun updateProfile(user: UserProfile) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    fun deleteProfile(userUuid: String) {
        viewModelScope.launch {
            repository.deleteUser(userUuid)
        }
    }

    fun createApiary(name: String, address: String = "", note: String = "") {
        viewModelScope.launch {
            val currentUser = repository.getActiveUserUuid() ?: return@launch
            val apiary = repository.createApiary(currentUser, name, note, address)
            repository.setActiveApiary(apiary.uuid)
        }
    }

    fun updateApiary(apiary: Apiary) {
        viewModelScope.launch {
            repository.updateApiary(apiary)
        }
    }

    fun deleteApiary(apiaryUuid: String) {
        viewModelScope.launch {
            repository.deleteApiary(apiaryUuid)
        }
    }
}
