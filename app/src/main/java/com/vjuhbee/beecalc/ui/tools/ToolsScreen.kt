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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vjuhbee.beecalc.R

/**
 * Вкладка «Инструменты» (SPEC.md §5): быстрый доступ к скан�рованию QR
 * и синхронизации пасеки. Держит нижнюю панель чистой, без перегрузки.
 */
@Composable
fun ToolsScreen(
    onScanClick: () -> Unit,
    onSyncClick: () -> Unit,
    onReportsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.tools_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        ToolCard(
            title = stringResource(R.string.tools_qr_title),
            description = stringResource(R.string.tools_qr_desc),
            icon = {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onClick = onScanClick
        )
        ToolCard(
            title = stringResource(R.string.tools_reports_title),
            description = stringResource(R.string.tools_reports_desc),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onClick = onReportsClick
        )
        ToolCard(
            title = stringResource(R.string.tools_sync_title),
            description = stringResource(R.string.tools_sync_desc),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            onClick = onSyncClick
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
