package com.vjuhbee.beecalc.ui.hives

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.Hive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Открытый редактор улья: новый или существующий. */
data class HiveEditor(
    val hive: Hive,
    val isNew: Boolean
)

data class HivesUiState(
    val hives: List<Hive> = emptyList(),
    val editor: HiveEditor? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HivesViewModel(app: Application) : AndroidViewModel(app) {

    private val beeCalcApp = app as BeeCalcApp
    private val repository = beeCalcApp.hiveRepository
    private val userApiaryRepository = beeCalcApp.userAndApiaryRepository

    private val editor = MutableStateFlow<HiveEditor?>(null)

    val uiState: StateFlow<HivesUiState> =
        combine(
            userApiaryRepository.observeActiveApiaryUuid().flatMapLatest { apiaryUuid ->
                if (apiaryUuid.isNullOrBlank()) {
                    repository.observeHives()
                } else {
                    repository.observeHivesForApiary(apiaryUuid)
                }
            },
            editor
        ) { hives, editorState ->
            HivesUiState(hives = hives, editor = editorState)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HivesUiState()
        )

    fun startAdd() {
        viewModelScope.launch {
            val currentApiary = userApiaryRepository.getActiveApiaryUuid() ?: ""
            editor.value = HiveEditor(hive = Hive(id = 0, name = "", apiaryUuid = currentApiary), isNew = true)
        }
    }

    fun closeEditor() {
        editor.value = null
    }

    fun saveHive(hive: Hive, isNew: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentApiary = if (hive.apiaryUuid.isBlank()) {
                userApiaryRepository.getActiveApiaryUuid() ?: ""
            } else {
                hive.apiaryUuid
            }
            val toSave = if (isNew && hive.uuid.isBlank()) {
                hive.copy(uuid = UUID.randomUUID().toString(), apiaryUuid = currentApiary, updatedAt = now)
            } else {
                hive.copy(apiaryUuid = currentApiary, updatedAt = now)
            }
            if (isNew) repository.addHive(toSave) else repository.updateHive(toSave)
            editor.value = null
        }
    }
}
