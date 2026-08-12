package com.vjuhbee.beecalc.ui.tools

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.BeeCalcApp
import com.vjuhbee.beecalc.R

@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val logger = (context.applicationContext as BeeCalcApp).diagnosticLogger
    val shareSubject = stringResource(R.string.diagnostics_share_subject)
    val shareTitle = stringResource(R.string.diagnostics_share)
    var text by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { text = logger.read() }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.diagnostics_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.diagnostics_privacy), style = MaterialTheme.typography.bodyMedium)
        Text(text.ifBlank { stringResource(R.string.diagnostics_empty) }, modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()))
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, shareTitle))
        }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.diagnostics_share)) }
        OutlinedButton(onClick = { logger.clear(); text = "" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.diagnostics_clear)) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.calculator_back)) }
    }
}
