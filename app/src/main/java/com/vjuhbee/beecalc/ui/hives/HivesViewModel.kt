package com.vjuhbee.beecalc.ui.hives

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.Hive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Открытый редактор улья: новый или существующий. */
data class HiveEditor(
    val hive: Hive,
    val isNew: Boolean
)

data class HivesUiState(
    val hives: List<Hive> = emptyList(),
    val editor: HiveEditor? = null
)

class HivesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as BeeCalcApp).hiveRepository

    private val editor = MutableStateFlow<HiveEditor?>(null)

    val uiState: StateFlow<HivesUiState> =
        combine(repository.observeHives(), editor) { hives, editorState ->
            HivesUiState(hives = hives, editor = editorState)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HivesUiState()
        )

    fun startAdd() {
        editor.value = HiveEditor(hive = Hive(id = 0, name = ""), isNew = true)
    }

    fun closeEditor() {
        editor.value = null
    }

    fun saveHive(hive: Hive, isNew: Boolean) {
        viewModelScope.launch {
            if (isNew) repository.addHive(hive) else repository.updateHive(hive)
            editor.value = null
        }
    }
}
