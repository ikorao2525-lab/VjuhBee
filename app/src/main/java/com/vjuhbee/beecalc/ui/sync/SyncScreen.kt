package com.vjuhbee.beecalc.ui.sync

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.data.sync.HiveConflict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экран «Синхронизация пасеки» (SPEC.md §9, v0.4): офлайн-экспорт/импорт
 * всего (ульи + история + календарь) через JSON-файл.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onBack: () -> Unit,
    viewModel: SyncViewModel
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.startImport(it) }
    }

    LaunchedEffect(state.message) {
        val msg = state.message
        if (msg != null) {
            when (msg) {
                is SyncMessage.Exported -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, msg.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Отправить пасеку")
                    )
                }
                is SyncMessage.Error -> {
                    snackbarHostState.showSnackbar(msg.text)
                }
            }
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_title)) },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 4.dp)
                    ) { Text(stringResource(R.string.sync_back)) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_description),
                style = MaterialTheme.typography.bodyMedium
            )

            // Экспорт
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sync_export_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.sync_export_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.export() },
                        enabled = !state.exporting && !state.importing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.exporting) stringResource(R.string.sync_working)
                            else stringResource(R.string.sync_export_button)
                        )
                    }
                }
            }

            // Импорт
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sync_import_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.sync_import_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                        },
                        enabled = !state.importing && !state.exporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.importing) stringResource(R.string.sync_working)
                            else stringResource(R.string.sync_import_button)
                        )
                    }
                }
            }

            // Мастер-шаги
            when (val step = state.step) {
                is ImportStep.Summary -> SummaryContent(
                    plan = step.plan,
                    onProceed = { viewModel.proceedToResolve() },
                    onCancel = { viewModel.reset() }
                )
                is ImportStep.Resolving -> {
                    val conflict = viewModel.currentConflict()
                    if (conflict != null) {
                        ConflictContent(
                            conflict = conflict,
                            index = step.plan.hiveConflicts.indexOf(conflict) + 1,
                            total = step.plan.hiveConflicts.size,
                            onChooseLocal = { viewModel.chooseConflict(false) },
                            onChooseRemote = { viewModel.chooseConflict(true) },
                            onSkip = { viewModel.chooseConflict(null) }
                        )
                    }
                }
                is ImportStep.Done -> DoneContent(
                    count = state.importedCount,
                    onBack = { viewModel.reset() }
                )
                else -> {}
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryContent(
    plan: com.vjuhbee.beecalc.data.sync.ImportPlan,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.sync_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.sync_summary_new, plan.totalNew))
            Text(stringResource(R.string.sync_summary_updated, plan.totalUpdated))
            Text(stringResource(R.string.sync_summary_conflicts, plan.totalConflicts))
            if (plan.totalConflicts > 0) {
                Text(
                    text = stringResource(R.string.sync_warning_lost),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sync_cancel))
                }
                Button(onClick = onProceed, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sync_proceed))
                }
            }
        }
    }
}

@Composable
private fun ConflictContent(
    conflict: HiveConflict,
    index: Int,
    total: Int,
    onChooseLocal: () -> Unit,
    onChooseRemote: () -> Unit,
    onSkip: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.sync_conflict_title, index, total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Локальная версия
            Text(stringResource(R.string.sync_conflict_local_label), fontWeight = FontWeight.Bold)
            Text(conflict.local.name.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
            if (conflict.local.note.isNotBlank()) {
                Text(conflict.local.note, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = stringResource(R.string.sync_conflict_updated, formatSyncTime(conflict.local.updatedAt)),
                style = MaterialTheme.typography.bodySmall
            )
            // Удалённая (из файла) версия
            Text(
                text = stringResource(R.string.sync_conflict_remote_label),
                fontWeight = FontWeight.Bold
            )
            Text(conflict.remote.name.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
            if (conflict.remote.note.isNotBlank()) {
                Text(conflict.remote.note, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = stringResource(R.string.sync_conflict_updated, formatSyncTime(conflict.remote.updatedAt)),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChooseLocal, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sync_conflict_keep_local))
                }
                Button(onClick = onChooseRemote, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sync_conflict_take_file))
                }
            }
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sync_conflict_skip))
            }
        }
    }
}

@Composable
private fun DoneContent(count: Int, onBack: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_done, count),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sync_done_ok))
            }
        }
    }
}

/** Читаемое время изменения для экрана конфликтов. */
private fun formatSyncTime(millis: Long): String {
    if (millis <= 0L) return "—"
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))
}
