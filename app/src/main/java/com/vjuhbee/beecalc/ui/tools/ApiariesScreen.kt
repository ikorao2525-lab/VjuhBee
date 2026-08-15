package com.vjuhbee.beecalc.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.UserProfile

@Composable
fun ApiariesScreen(
    onBack: () -> Unit,
    viewModel: ApiariesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showProfileDialog by remember { mutableStateOf<UserProfile?>(null) }
    var isNewProfile by remember { mutableStateOf(false) }

    var showApiaryDialog by remember { mutableStateOf<Apiary?>(null) }
    var isNewApiary by remember { mutableStateOf(false) }

    var deleteProfileConfirm by remember { mutableStateOf<UserProfile?>(null) }
    var deleteApiaryConfirm by remember { mutableStateOf<Apiary?>(null) }
    var errorDialogText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.hive_back))
        }

        Text(
            text = stringResource(R.string.apiaries_management_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 1. Секция профилей (пользователи)
        Text(
            text = stringResource(R.string.profiles_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        state.users.forEach { user ->
            val isActive = user.uuid == state.activeUser?.uuid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectUser(user.uuid) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (user.type == ProfileType.COMPANY) Icons.Filled.Business else Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = if (user.type == ProfileType.COMPANY) stringResource(R.string.profile_type_company) else stringResource(R.string.profile_type_individual),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isActive) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        showProfileDialog = user
                        isNewProfile = false
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    }
                    IconButton(onClick = {
                        if (state.users.size <= 1) {
                            errorDialogText = "Нельзя удалить единственный профиль"
                        } else {
                            deleteProfileConfirm = user
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                showProfileDialog = UserProfile(name = "", type = ProfileType.INDIVIDUAL)
                isNewProfile = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.add_profile), modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Секция пасек текущего пользователя
        Text(
            text = stringResource(R.string.apiaries_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        state.apiaries.forEach { apiary ->
            val isActive = apiary.uuid == state.activeApiary?.uuid
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectApiary(apiary.uuid) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = apiary.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                        if (apiary.address.isNotBlank()) {
                            Text(
                                text = apiary.address,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (isActive) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = {
                        showApiaryDialog = apiary
                        isNewApiary = false
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                    }
                    IconButton(onClick = {
                        if (state.apiaries.size <= 1) {
                            errorDialogText = "Нельзя удалить единственную пасеку"
                        } else {
                            deleteApiaryConfirm = apiary
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }
        }

        Button(
            onClick = {
                showApiaryDialog = Apiary(name = "")
                isNewApiary = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.add_apiary), modifier = Modifier.padding(start = 8.dp))
        }
    }

    // Диалог создания/редактирования профиля
    showProfileDialog?.let { profile ->
        var name by remember { mutableStateOf(profile.name) }
        var type by remember { mutableStateOf(profile.type) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = null },
            title = { Text(if (isNewProfile) stringResource(R.string.new_profile_title) else stringResource(R.string.edit_profile_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.profile_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = type == ProfileType.INDIVIDUAL,
                            onClick = { type = ProfileType.INDIVIDUAL }
                        )
                        Text(stringResource(R.string.profile_type_individual), modifier = Modifier.padding(start = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = type == ProfileType.COMPANY,
                            onClick = { type = ProfileType.COMPANY }
                        )
                        Text(stringResource(R.string.profile_type_company), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            if (isNewProfile) {
                                viewModel.createProfile(name.trim(), type)
                            } else {
                                viewModel.updateProfile(profile.copy(name = name.trim(), type = type))
                            }
                            showProfileDialog = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.editor_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = null }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        )
    }

    // Диалог создания/редактирования пасеки
    showApiaryDialog?.let { apiary ->
        var name by remember { mutableStateOf(apiary.name) }
        var address by remember { mutableStateOf(apiary.address) }
        var note by remember { mutableStateOf(apiary.note) }

        AlertDialog(
            onDismissRequest = { showApiaryDialog = null },
            title = { Text(if (isNewApiary) stringResource(R.string.new_apiary_title) else stringResource(R.string.edit_apiary_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.apiary_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(stringResource(R.string.apiary_address_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.apiary_note_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            if (isNewApiary) {
                                viewModel.createApiary(name.trim(), address.trim(), note.trim())
                            } else {
                                viewModel.updateApiary(apiary.copy(name = name.trim(), address = address.trim(), note = note.trim()))
                            }
                            showApiaryDialog = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.editor_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiaryDialog = null }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        )
    }

    // Подтверждение удаления профиля
    deleteProfileConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteProfileConfirm = null },
            title = { Text(stringResource(R.string.hive_delete_confirm)) },
            text = { Text(stringResource(R.string.delete_profile_confirm, user.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfile(user.uuid)
                        deleteProfileConfirm = null
                    }
                ) {
                    Text(stringResource(R.string.editor_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteProfileConfirm = null }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        )
    }

    // Подтверждение удаления пасеки
    deleteApiaryConfirm?.let { apiary ->
        AlertDialog(
            onDismissRequest = { deleteApiaryConfirm = null },
            title = { Text(stringResource(R.string.hive_delete_confirm)) },
            text = { Text(stringResource(R.string.delete_apiary_confirm, apiary.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteApiary(apiary.uuid)
                        deleteApiaryConfirm = null
                    }
                ) {
                    Text(stringResource(R.string.editor_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteApiaryConfirm = null }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        )
    }

    // Диалог ошибки
    errorDialogText?.let { errorText ->
        AlertDialog(
            onDismissRequest = { errorDialogText = null },
            title = { Text("Внимание") },
            text = { Text(errorText) },
            confirmButton = {
                Button(onClick = { errorDialogText = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }
}
