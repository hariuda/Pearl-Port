package com.example
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.network.StockUpdateWorker

import android.content.Context
import android.os.Bundle
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PortfolioViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object Portfolio : Screen("portfolio", "Portfolio", Icons.Filled.ShowChart)
    object Reports : Screen("reports", "Reports", Icons.Filled.Description)
    
}

val items = listOf(
    Screen.Dashboard,
    Screen.Portfolio,
    Screen.Reports
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val updateWorkRequest = PeriodicWorkRequestBuilder<StockUpdateWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "stock_update_work",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWorkRequest
        )
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

        setContent {
            val viewModel: PortfolioViewModel = viewModel()
            MyApplicationTheme {

                val navController = rememberNavController()
                var showMenu by remember { mutableStateOf(false) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Scaffold(
                    topBar = {
                    if (currentDestination?.route != Screen.Dashboard.route) {
                        TopAppBar(
                            title = { Text("Pearl Port", fontWeight = FontWeight.SemiBold) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                actionIconContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            actions = {
                                if (currentDestination?.route == Screen.Dashboard.route) {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                    androidx.compose.material3.Text("  Chart Colors", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                                    DropdownMenuItem(
                                        text = { Text("Default") },
                                        onClick = {
                                            viewModel.setChartColorPalette("Default")
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Vibrant") },
                                        onClick = {
                                            viewModel.setChartColorPalette("Vibrant")
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ocean") },
                                        onClick = {
                                            viewModel.setChartColorPalette("Ocean")
                                            showMenu = false
                                        }
                                    )
                                }
                                }
                            }
                        )
                    }
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.height(72.dp),
                            windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp, 0.dp, 0.dp, 8.dp)
                        ) {
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.title) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = (screen.route != Screen.Dashboard.route)
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) { 
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAllocation = { navController.navigate("allocation") }
                            ) 
                        }
                        composable(Screen.Portfolio.route) { PortfolioScreen(viewModel) }
                        composable(Screen.Reports.route) { ReportsScreen(viewModel) }
                        composable("allocation") { 
                            AllocationScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            ) 
                        }
                    }
                }
            }
        }
    }
}
