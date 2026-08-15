package com.vjuhbee.beecalc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.vjuhbee.beecalc.R
import com.vjuhbee.beecalc.model.ProfileType

@Composable
fun OnboardingDialog(
    onFinish: (profileName: String, profileType: ProfileType, apiaryName: String, isDemo: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var profileType by remember { mutableStateOf(ProfileType.INDIVIDUAL) }
    var profileName by remember { mutableStateOf("") }
    var apiaryName by remember { mutableStateOf("Основная пасека") }

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = when (step) {
                    1 -> stringResource(R.string.onboarding_step1_title)
                    2 -> stringResource(R.string.onboarding_step2_title)
                    else -> stringResource(R.string.onboarding_step3_title)
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    1 -> {
                        Text(stringResource(R.string.onboarding_step1_desc), style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = profileType == ProfileType.INDIVIDUAL,
                                onClick = { profileType = ProfileType.INDIVIDUAL }
                            )
                            Text(stringResource(R.string.profile_type_individual), modifier = Modifier.padding(start = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = profileType == ProfileType.COMPANY,
                                onClick = { profileType = ProfileType.COMPANY }
                            )
                            Text(stringResource(R.string.profile_type_company), modifier = Modifier.padding(start = 4.dp))
                        }
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it },
                            label = { Text(stringResource(R.string.onboarding_name_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        Text(stringResource(R.string.onboarding_step2_desc), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = apiaryName,
                            onValueChange = { apiaryName = it },
                            label = { Text(stringResource(R.string.onboarding_apiary_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    3 -> {
                        Text(stringResource(R.string.onboarding_step3_desc), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Профиль: ${profileName.ifBlank { "Мой профиль" }}\nПасека: ${apiaryName.ifBlank { "Основная пасека" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                1 -> {
                    Button(
                        onClick = { step = 2 }
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }
                2 -> {
                    Button(
                        onClick = { step = 3 }
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }
                3 -> {
                    Button(
                        onClick = {
                            val finalProfile = profileName.trim().ifBlank { "Мой профиль" }
                            val finalApiary = apiaryName.trim().ifBlank { "Основная пасека" }
                            onFinish(finalProfile, profileType, finalApiary, true)
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_start_demo))
                    }
                }
            }
        },
        dismissButton = {
            when (step) {
                1 -> { }
                2 -> {
                    TextButton(onClick = { step = 1 }) {
                        Text("← Назад")
                    }
                }
                3 -> {
                    TextButton(
                        onClick = {
                            val finalProfile = profileName.trim().ifBlank { "Мой профиль" }
                            val finalApiary = apiaryName.trim().ifBlank { "Основная пасека" }
                            onFinish(finalProfile, profileType, finalApiary, false)
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_start_empty))
                    }
                }
            }
        }
    )
}
