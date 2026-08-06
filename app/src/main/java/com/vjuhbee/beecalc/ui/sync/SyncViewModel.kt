package com.vjuhbee.beecalc.ui.sync

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.data.sync.HiveConflict
import com.vjuhbee.beecalc.data.sync.ImportPlan
import com.vjuhbee.beecalc.data.sync.SyncFile
import com.vjuhbee.beecalc.data.sync.SyncFileUtils
import com.vjuhbee.beecalc.data.sync.SyncSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Этап импорт-мастера (SPEC.md §9). */
sealed interface ImportStep {
    data object Pending : ImportStep          // файл ещё не выбран
    data class Summary(val plan: ImportPlan) : ImportStep  // сводка
    data class Resolving(val plan: ImportPlan) : ImportStep // разбор конфликтов
    data object Done : ImportStep             // применено
}

sealed interface SyncMessage {
    data class Error(val text: String) : SyncMessage
    data class Exported(val uri: Uri) : SyncMessage
}

data class SyncUiState(
    val step: ImportStep = ImportStep.Pending,
    val exporting: Boolean = false,
    val importing: Boolean = false,
    val message: SyncMessage? = null,
    val importedCount: Int = 0
)

class SyncViewModel(app: Application) : AndroidViewModel(app) {

    private val syncRepository = (app as BeeCalcApp).syncRepository
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState

    private var file: SyncFile? = null
    private var plan: ImportPlan? = null
    private var conflictIndex = 0
    private val conflictChoices = mutableMapOf<String, Boolean>()

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun export() {
        _uiState.value = _uiState.value.copy(exporting = true)
        viewModelScope.launch {
            val sf = syncRepository.export()
            val json = SyncSerializer.encode(sf)
            val uri = SyncFileUtils.writeExport(getApplication(), json)
            _uiState.value = _uiState.value.copy(
                exporting = false,
                message = SyncMessage.Exported(uri)
            )
        }
    }

    /**
     * Сохраняет экспорт прямо в выбранное пользователем место
     * (системный диалог «Сохранить», обычно Downloads) через SAF.
     */
    fun save(uri: Uri) {
        _uiState.value = _uiState.value.copy(exporting = true)
        viewModelScope.launch {
            try {
                val sf = syncRepository.export()
                val json = SyncSerializer.encode(sf)
                val ok = try {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } != null
                } catch (_: Exception) {
                    false
                }
                if (ok) {
                    _uiState.value = _uiState.value.copy(
                        exporting = false,
                        message = SyncMessage.Error("Пасека сохранена на устройство.")
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        exporting = false,
                        message = SyncMessage.Error("Не удалось сохранить файл.")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    exporting = false,
                    message = SyncMessage.Error("Не удалось сохранить файл: ${e.message}")
                )
            }
        }
    }

    fun startImport(uri: Uri) {
        _uiState.value = _uiState.value.copy(importing = true)
        viewModelScope.launch {
            val text = SyncFileUtils.readUri(getApplication(), uri)
            if (text == null) {
                _uiState.value = _uiState.value.copy(
                    importing = false,
                    message = SyncMessage.Error("Не удалось прочитать файл пасеки.")
                )
                return@launch
            }
            try {
                val sf = SyncSerializer.decode(text)
                file = sf
                val p = syncRepository.buildPlan(sf)
                plan = p
                conflictIndex = 0
                conflictChoices.clear()
                _uiState.value = _uiState.value.copy(
                    importing = false,
                    step = ImportStep.Summary(p)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    importing = false,
                    message = SyncMessage.Error("Похоже, это не файл пасеки BeeCalc: ${e.message}")
                )
            }
        }
    }

    /** Переход к разбору конфликтов (или сразу применение, если их нет). */
    fun proceedToResolve() {
        val p = plan ?: return
        if (p.hiveConflicts.isEmpty()) {
            applyPlan()
        } else {
            conflictIndex = 0
            _uiState.value = _uiState.value.copy(step = ImportStep.Resolving(p))
        }
    }

    fun currentConflict(): HiveConflict? {
        val p = plan ?: return null
        return p.hiveConflicts.getOrNull(conflictIndex)
    }

    /** Пользователь выбрал версию. null = пропустить, false = оставить свою, true = взять из файла. */
    fun chooseConflict(choice: Boolean?) {
        val conflict = currentConflict() ?: return
        if (choice != null) {  // null — пропустить (не выбираем, ничего не пишем)
            conflictChoices[conflict.remote.uuid] = choice
            conflictIndex++
        } else {
            conflictIndex++
        }
        val p = plan ?: return
        if (conflictIndex >= p.hiveConflicts.size) {
            applyPlan()
        } else {
            _uiState.value = _uiState.value.copy(step = ImportStep.Resolving(p))
        }
    }

    private fun applyPlan() {
        val sf = file ?: return
        val p = plan ?: return
        _uiState.value = _uiState.value.copy(importing = true)
        viewModelScope.launch {
            syncRepository.apply(sf, p) { conflict ->
                conflictChoices[conflict.remote.uuid]
            }
            _uiState.value = _uiState.value.copy(
                importing = false,
                step = ImportStep.Done,
                importedCount = p.totalNew + p.totalUpdated
            )
        }
    }

    fun reset() {
        file = null
        plan = null
        conflictIndex = 0
        conflictChoices.clear()
        _uiState.value = SyncUiState()
    }

    fun backToSummary() {
        val p = plan ?: return
        _uiState.value = _uiState.value.copy(step = ImportStep.Summary(p))
    }
}
