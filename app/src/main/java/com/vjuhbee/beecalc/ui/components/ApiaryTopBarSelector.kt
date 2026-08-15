package com.vjuhbee.beecalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.Apiary
import com.vjuhbee.beecalc.model.ProfileType
import com.vjuhbee.beecalc.model.UserProfile
import kotlinx.coroutines.launch

@Composable
fun ApiaryTopBarSelector(
    onManageClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as BeeCalcApp).userAndApiaryRepository
    val scope = rememberCoroutineScope()

    val activeUser: UserProfile? by repository.observeActiveUserProfile().collectAsStateWithLifecycle(initialValue = null)
    val activeApiary: Apiary? by repository.observeActiveApiary().collectAsStateWithLifecycle(initialValue = null)
    val allUsers: List<UserProfile> by repository.observeUsers().collectAsStateWithLifecycle(initialValue = emptyList())
    val allApiaries: List<Apiary> by repository.observeApiaries().collectAsStateWithLifecycle(initialValue = emptyList())

    var showDialog by remember { mutableStateOf(false) }

    val userName = activeUser?.name ?: "Мой профиль"
    val apiaryName = activeApiary?.name ?: "Основная пасека"
    val isCompany = activeUser?.type == ProfileType.COMPANY

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDialog = true },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isCompany) Icons.Filled.Business else Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$userName · $apiaryName",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.switch_apiary_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Пасеки текущего профиля
                    Text(
                        text = "Пасеки («$userName»):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val userApiaries = allApiaries.filter { it.userUuid == activeUser?.uuid }
                    userApiaries.forEach { apiary ->
                        val isSelected = apiary.uuid == activeApiary?.uuid
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        repository.setActiveApiary(apiary.uuid)
                                        showDialog = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = apiary.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (allUsers.size > 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = DividerDefaults.color)
                        Text(
                            text = "Сменить профиль:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        allUsers.forEach { user ->
                            val isUserSelected = user.uuid == activeUser?.uuid
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            repository.setActiveUser(user.uuid)
                                            showDialog = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (user.type == ProfileType.COMPANY) Icons.Filled.Business else Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isUserSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isUserSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isUserSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onManageClick()
                    }
                ) {
                    Text(stringResource(R.string.manage_apiaries_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.editor_cancel))
                }
            }
        )
    }
}
