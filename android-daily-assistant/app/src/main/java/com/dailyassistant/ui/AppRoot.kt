@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dailyassistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailyassistant.vm.MainViewModel

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("history", "History", Icons.Default.History),
    Tab("reports", "Reports", Icons.Default.BarChart),
    Tab("bills", "Bills", Icons.Default.Receipt),
    Tab("more", "More", Icons.Default.MoreHoriz)
)

@Composable
fun AppRoot(vm: MainViewModel, deepLinkRoute: String?, onDeepLinkHandled: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val unlocked by vm.unlocked.collectAsState()

    if (settings.pinHash != null && !unlocked) {
        LockScreen(onSubmit = { vm.tryUnlock(it) })
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by vm.message.collectAsState()
    val context = LocalContext.current

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(deepLinkRoute) {
        if (deepLinkRoute != null) {
            navController.navigate(deepLinkRoute)
            onDeepLinkHandled()
        }
    }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            vm.messageShown()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute in tabs.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo("home")
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "home" || currentRoute == "history") {
                FloatingActionButton(onClick = { navController.navigate("add") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add entry")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                DashboardScreen(
                    vm = vm,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("history") { HistoryScreen(vm) }
            composable("reports") { ReportsScreen(vm) }
            composable("bills") { BillsScreen(vm) }
            composable("more") {
                MoreScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable("add") {
                AddEntryScreen(vm = vm, onDone = { navController.popBackStack() })
            }
            composable("goals") { GoalsScreen(vm, onBack = { navController.popBackStack() }) }
            composable("reminders") { RemindersScreen(vm, onBack = { navController.popBackStack() }) }
            composable("debts") { DebtsScreen(vm, onBack = { navController.popBackStack() }) }
            composable("categories") { CategoriesScreen(vm, onBack = { navController.popBackStack() }) }
            composable("settings") { SettingsScreen(vm, onBack = { navController.popBackStack() }) }
        }
    }
}
