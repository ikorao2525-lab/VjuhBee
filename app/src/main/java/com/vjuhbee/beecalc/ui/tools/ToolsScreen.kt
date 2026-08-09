package com.vjuhbee.beecalc.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.R

@Composable
fun ToolsScreen(
    onScanClick: () -> Unit,
    onSyncClick: () -> Unit,
    onReportsClick: () -> Unit,
    onReportScanClick: () -> Unit,
    onDiagnosticsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.tools_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ToolCard(stringResource(R.string.tools_qr_title), stringResource(R.string.tools_qr_desc), onScanClick) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Card(onClick = onReportsClick, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tools_reports_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.tools_reports_desc), style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onReportScanClick) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.report_qr_scan))
                }
            }
        }
        ToolCard(stringResource(R.string.tools_sync_title), stringResource(R.string.tools_sync_desc), onSyncClick) {
            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ToolCard(title: String, description: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
