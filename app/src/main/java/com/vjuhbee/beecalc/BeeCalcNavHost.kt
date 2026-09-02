package com.vjuhbee.beecalc

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.foundation.layout.size
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import android.net.Uri
import com.vjuhbee.beecalc.data.UserAndApiaryRepository
import com.vjuhbee.beecalc.ui.about.AboutScreen
import com.vjuhbee.beecalc.ui.calculator.CalculatorScreen
import com.vjuhbee.beecalc.ui.calculator.CalculatorCatalogScreen
import com.vjuhbee.beecalc.ui.calculator.CandiScreen
import com.vjuhbee.beecalc.ui.calculator.TreatmentScreen
import com.vjuhbee.beecalc.ui.calculator.ApiaryExpansionScreen
import com.vjuhbee.beecalc.ui.calculator.WinterFeedScreen
import com.vjuhbee.beecalc.ui.calculator.HoneyJarsScreen
import com.vjuhbee.beecalc.ui.calendar.CalendarScreen
import com.vjuhbee.beecalc.ui.hives.HiveDetailScreen
import com.vjuhbee.beecalc.ui.hives.HivesScreen
import com.vjuhbee.beecalc.ui.hives.QrScannerScreen
import com.vjuhbee.beecalc.ui.sync.SyncScreen
import com.vjuhbee.beecalc.ui.tools.ToolsScreen
import com.vjuhbee.beecalc.ui.tools.ApiariesScreen
import com.vjuhbee.beecalc.ui.tools.DiagnosticsScreen
import com.vjuhbee.beecalc.ui.components.ApiaryTopBarSelector
import com.vjuhbee.beecalc.ui.components.OnboardingDialog
import com.vjuhbee.beecalc.ui.home.HomeScreen
import com.vjuhbee.beecalc.data.DemoApiaryData
import com.vjuhbee.beecalc.ui.reports.ReportsScreen
import com.vjuhbee.beecalc.ui.reports.ReportQrScannerScreen
import com.vjuhbee.beecalc.model.Hive
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vjuhbee.beecalc.utils.parseHiveQr
import com.vjuhbee.beecalc.utils.QrHiveData
import kotlinx.coroutines.launch

/** Основные вкладки нижней навигации (SPEC.md §5). */
private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val tabs = listOf(
    BottomTab("home", R.string.tab_home, Icons.Filled.Home),
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
    var startupChoiceVisible by remember { mutableStateOf(!context.getSharedPreferences("beecalc", 0).getBoolean("startup_choice_done", false)) }
    val showBottomBar = currentRoute != "about" && currentRoute != "qr-scan" && currentRoute != "sync" && currentRoute != "reports"
    val showTopBar = currentRoute != "about" &&
        currentRoute?.startsWith("hive/") != true &&
        currentRoute != "qr-scan" &&
        currentRoute != "sync" && currentRoute != "reports"

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        ApiaryTopBarSelector(
                            onManageClick = { navController.navigate("apiaries") }
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
                NavigationBar(modifier = Modifier.padding(horizontal = 0.dp)) {
                    tabs.forEach { tab ->
                        // Экран улья (hive/{id}) относится к вкладке «Ульи».
                        // Маршрут calendar/{year}/{month} (переход из улья) — к «Календарю».
                        val selected = currentRoute == tab.route ||
                            (tab.route == "hives" && currentRoute?.startsWith("hive/") == true) ||
                            (tab.route == "calendar" && currentRoute?.startsWith("calendar/") == true)
                        NavigationBarItem(
                            modifier = Modifier.weight(if (tab.route == "tools") 1.2f else 0.95f),
                            selected = selected,
                            onClick = {
                                if (tab.route == "home") {
                                    navController.popBackStack("home", inclusive = false)
                                } else if (tab.route == "calculator" && currentRoute?.startsWith("calculator/") == true) {
                                    navController.popBackStack("calculator", inclusive = false)
                                } else {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes), modifier = Modifier.size(21.dp)) },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
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
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(onCalendarClick = { navController.navigate("calendar") }, onHivesClick = { navController.navigate("hives") }, onQrClick = { navController.navigate("qr-scan") }, onReportsClick = { navController.navigate("reports") }) }
            composable("calculator") { CalculatorCatalogScreen(onSyrupClick = { navController.navigate("calculator/syrup") }, onCandiClick = { navController.navigate("calculator/candi") }, onTreatmentClick = { navController.navigate("calculator/treatment") }, onExpansionClick = { navController.navigate("calculator/expansion") }, onWinterClick = { navController.navigate("calculator/winter") }, onHoneyJarsClick = { navController.navigate("calculator/honey-jars") }) }
            composable("calculator/syrup") { CalculatorScreen() }
            composable("calculator/candi") { CandiScreen(onBack = { navController.popBackStack() }) }
            composable("calculator/treatment") { TreatmentScreen(onBack = { navController.popBackStack() }) }
            composable("calculator/expansion") { ApiaryExpansionScreen(onBack = { navController.popBackStack() }) }
            composable("calculator/winter") { WinterFeedScreen(onBack = { navController.popBackStack() }) }
            composable("calculator/honey-jars") { HoneyJarsScreen(onBack = { navController.popBackStack() }) }
            composable("calendar") { CalendarScreen() }
            composable("calendar/{year}/{month}/{taskUuid}", arguments = listOf(navArgument("year") { type = NavType.IntType }, navArgument("month") { type = NavType.IntType }, navArgument("taskUuid") { type = NavType.StringType })) { entry ->
                CalendarScreen(year = entry.arguments?.getInt("year"), month = entry.arguments?.getInt("month"), taskUuid = entry.arguments?.getString("taskUuid"))
            }
            composable("hives") {
                HivesScreen(
                    onHiveClick = { hiveId -> navController.navigate("hive/$hiveId") },
                    onScanClick = { navController.navigate("qr-scan") }
                )
            }
            composable("tools") {
                ToolsScreen(
                    onScanClick = { navController.navigate("qr-scan") },
                    onApiariesClick = { navController.navigate("apiaries") },
                    onSyncClick = { navController.navigate("sync") },
                    onReportsClick = { navController.navigate("reports") }, onReportScanClick = { navController.navigate("report-qr-scan") }, onDiagnosticsClick = { navController.navigate("diagnostics") }
                )
            }
            composable("apiaries") { ApiariesScreen(onBack = { navController.popBackStack() }) }
            composable("diagnostics") { DiagnosticsScreen(onBack = { navController.popBackStack() }) }
            composable("reports") { ReportsScreen(onBack = { navController.popBackStack() }) }
            composable("report-qr-scan") { ReportQrScannerScreen(onBack = { navController.popBackStack() }) }
            composable(
                route = "reports/{hiveUuid}",
                arguments = listOf(navArgument("hiveUuid") { type = NavType.StringType })
            ) { entry ->
                ReportsScreen(
                    initialHiveUuid = entry.arguments?.getString("hiveUuid"),
                    onBack = { navController.popBackStack() }
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
                    onQuickReport = { hiveUuid -> navController.navigate("reports/${Uri.encode(hiveUuid)}") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    if (startupChoiceVisible) {
        OnboardingDialog(
            onFinish = { profileName, profileType, apiaryName, isDemo ->
                scope.launch {
                    val userApiaryRepo = (context.applicationContext as BeeCalcApp).userAndApiaryRepository
                    val user = userApiaryRepo.createUser(profileName, profileType)
                    val apiary = userApiaryRepo.createApiary(user.uuid, apiaryName)
                    userApiaryRepo.setActiveUser(user.uuid)
                    userApiaryRepo.setActiveApiary(apiary.uuid)

                    if (isDemo) {
                        DemoApiaryData.seed(hiveRepository, (context.applicationContext as BeeCalcApp).calendarRepository, apiary.uuid)
                    }

                    context.getSharedPreferences("beecalc", 0).edit().putBoolean("startup_choice_done", true).apply()
                    startupChoiceVisible = false
                }
            }
        )
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
