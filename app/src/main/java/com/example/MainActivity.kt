package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.network.StockUpdateWorker
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PortfolioViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

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

class MainActivity : ComponentActivity() {
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

        setContent {
            val viewModel: PortfolioViewModel = viewModel()
            var showLoadingScreen by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1500L)
                showLoadingScreen = false
            }

            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.height(72.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp,
                                windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 8.dp)
                            ) {
                                items.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = { 
                                            Icon(
                                                screen.icon, 
                                                contentDescription = screen.title,
                                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                screen.title,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            ) 
                                        },
                                        selected = selected,
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
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

                    AnimatedVisibility(
                        visible = showLoadingScreen,
                        exit = fadeOut(animationSpec = tween(durationMillis = 400))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.loading_screen),
                                contentDescription = "Loading Screen",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
