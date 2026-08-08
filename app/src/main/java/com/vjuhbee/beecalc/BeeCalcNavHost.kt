package com.vjuhbee.beecalc

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vjuhbee.beecalc.ui.about.AboutScreen
import com.vjuhbee.beecalc.ui.calculator.CalculatorScreen
import com.vjuhbee.beecalc.ui.calendar.CalendarScreen
import com.vjuhbee.beecalc.ui.hives.HiveDetailScreen
import com.vjuhbee.beecalc.ui.hives.HivesScreen
import com.vjuhbee.beecalc.ui.hives.QrScannerScreen
import com.vjuhbee.beecalc.ui.sync.SyncScreen
import com.vjuhbee.beecalc.ui.tools.ToolsScreen
import com.vjuhbee.beecalc.model.Hive
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.utils.parseHiveQr
import com.vjuhbee.beecalc.utils.QrHiveData
import kotlinx.coroutines.launch

/** Три вкладки нижней навигации (SPEC.md §5). */
private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val tabs = listOf(
    BottomTab("calculator", R.string.tab_calculator, Icons.Filled.Calculate),
    BottomTab("calendar", R.string.tab_calendar, Icons.Filled.CalendarMonth),
    BottomTab("hives", R.string.tab_hives, Icons.Filled.Hive),
    BottomTab("tools", R.string.tab_tools, Icons.Filled.Build)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeeCalcNavHost() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hiveRepository = (context.applicationContext as BeeCalcApp).hiveRepository
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var pendingImport by remember { mutableStateOf<QrHiveData?>(null) }
    val showBottomBar = currentRoute != "about" && currentRoute != "qr-scan" && currentRoute != "sync"
    val showTopBar = currentRoute != "about" &&
        currentRoute?.startsWith("hive/") != true &&
        currentRoute != "qr-scan" &&
        currentRoute != "sync"

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (BuildConfig.IS_BETA) {
                                stringResource(R.string.app_name_beta)
                            } else {
                                stringResource(R.string.app_name)
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        // «О приложении» / чаевые (SPEC.md §4).
                        IconButton(
                            onClick = { navController.navigate("about") },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = stringResource(R.string.about_title)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        // Экран улья (hive/{id}) относится к вкладке «Ульи».
                        // Маршрут calendar/{year}/{month} (переход из улья) — к «Календарю».
                        val selected = currentRoute == tab.route ||
                            (tab.route == "hives" && currentRoute?.startsWith("hive/") == true) ||
                            (tab.route == "calendar" && currentRoute?.startsWith("calendar/") == true)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "calculator",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("calculator") { CalculatorScreen() }
            composable("calendar") { CalendarScreen() }
            composable("hives") {
                HivesScreen(
                    onHiveClick = { hiveId -> navController.navigate("hive/$hiveId") },
                    onScanClick = { navController.navigate("qr-scan") }
                )
            }
            composable("tools") {
                ToolsScreen(
                    onScanClick = { navController.navigate("qr-scan") },
                    onSyncClick = { navController.navigate("sync") }
                )
            }
            composable("sync") {
                SyncScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel()
                )
            }
            composable("qr-scan") {
                QrScannerScreen(
                    onBack = { navController.popBackStack() },
                    onQrScanned = { raw ->
                        scope.launch {
                            // QR содержит beecalc://hive/<uuid>[?name=|note=].
                            val data = parseHiveQr(raw)
                            val hive = data?.let { hiveRepository.findHiveByUuid(data.uuid) }
                            if (hive != null) {
                                // Свой улей — просто открываем его.
                                navController.popBackStack()
                                navController.navigate("hive/${hive.id}")
                            } else if (data != null) {
                                // Чужой/неизвестный улей — предлагаем импортировать.
                                pendingImport = data
                            } else {
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
            composable(
                route = "hive/{hiveId}",
                arguments = listOf(navArgument("hiveId") { type = NavType.IntType })
            ) {
                HiveDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    pendingImport?.let { data ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.qr_import_title)) },
            text = {
                Text(
                    text = if (data.name.isNotBlank()) {
                        stringResource(R.string.qr_import_text, data.name)
                    } else {
                        stringResource(R.string.qr_import_text_no_name)
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // Импортируем улей, сохраняя uuid из QR (чтобы связь не терялась).
                            val id = hiveRepository.addHive(
                                Hive(
                                    id = 0,
                                    name = data.name,
                                    note = data.note,
                                    uuid = data.uuid
                                )
                            ).toInt()
                            pendingImport = null
                            navController.popBackStack()
                            navController.navigate("hive/$id")
                        }
                    }
                ) {
                    Text(stringResource(R.string.qr_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(stringResource(R.string.qr_import_cancel))
                }
            }
        )
    }
}
