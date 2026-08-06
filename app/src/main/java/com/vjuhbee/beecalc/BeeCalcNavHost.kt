package com.vjuhbee.beecalc

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Три вкладки нижней навигации (SPEC.md §5). */
private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

private val tabs = listOf(
    BottomTab("calculator", R.string.tab_calculator, Icons.Filled.Calculate),
    BottomTab("calendar", R.string.tab_calendar, Icons.Filled.CalendarMonth),
    BottomTab("hives", R.string.tab_hives, Icons.Filled.Hive)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeeCalcNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != "about"
    val showTopBar = currentRoute != "about" && currentRoute?.startsWith("hive/") != true

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
                        // Крупная зона нажатия: «О приложении» / донат / премиум.
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
                        val selected = currentRoute == tab.route ||
                            (tab.route == "hives" && currentRoute?.startsWith("hive/") == true)
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
                                    style = MaterialTheme.typography.bodyMedium
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
                HivesScreen(onHiveClick = { hiveId -> navController.navigate("hive/$hiveId") })
            }
            composable(
                route = "hive/{hiveId}",
                arguments = listOf(navArgument("hiveId") { type = NavType.IntType })
            ) {
                HiveDetailScreen(onBack = { navController.popBackStack() })
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
