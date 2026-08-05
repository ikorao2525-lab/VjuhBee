package com.vjuhbee.beecalc.ui.hives

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.model.Hive
import com.vjuhbee.beecalc.model.Inspection
import com.vjuhbee.beecalc.model.Treatment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Какой диалог открыт на экране улья. */
sealed interface HiveDialog {
    data object EditHive : HiveDialog
    data object AddInspection : HiveDialog
    data object AddTreatment : HiveDialog
}

data class HiveDetailUiState(
    val hive: Hive? = null,
    val inspections: List<Inspection> = emptyList(),
    val treatments: List<Treatment> = emptyList(),
    val dialog: HiveDialog? = null,
    val isDeleted: Boolean = false
)

class HiveDetailViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val repository = (app as BeeCalcApp).hiveRepository

    /** id улья приходит из маршрута hive/{hiveId}. */
    private val hiveId: Int = checkNotNull(savedStateHandle["hiveId"])

    private val dialog = MutableStateFlow<HiveDialog?>(null)
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<HiveDetailUiState> =
        combine(
            repository.observeHive(hiveId),
            repository.observeInspections(hiveId),
            repository.observeTreatments(hiveId),
            dialog,
            deleted
        ) { hive, inspections, treatments, dialogState, isDeleted ->
            HiveDetailUiState(
                hive = hive,
                inspections = inspections,
                treatments = treatments,
                dialog = dialogState,
                isDeleted = isDeleted
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HiveDetailUiState()
        )

    fun openDialog(newDialog: HiveDialog) {
        dialog.value = newDialog
    }

    fun closeDialog() {
        dialog.value = null
    }

    fun saveHive(hive: Hive) {
        viewModelScope.launch {
            repository.updateHive(hive)
            dialog.value = null
        }
    }

    fun deleteHive(hive: Hive) {
        viewModelScope.launch {
            repository.deleteHive(hive)
            deleted.value = true   // сигнал экрану вернуться к списку
        }
    }

    fun addInspection(inspection: Inspection) {
        viewModelScope.launch {
            repository.addInspection(inspection.copy(hiveId = hiveId))
            dialog.value = null
        }
    }

    fun deleteInspection(inspection: Inspection) {
        viewModelScope.launch { repository.deleteInspection(inspection) }
    }

    fun addTreatment(treatment: Treatment) {
        viewModelScope.launch {
            repository.addTreatment(treatment.copy(hiveId = hiveId))
            dialog.value = null
        }
    }

    fun deleteTreatment(treatment: Treatment) {
        viewModelScope.launch { repository.deleteTreatment(treatment) }
    }
}
