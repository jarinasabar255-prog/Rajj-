package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.EngineSettingsScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.OrbTerminalScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextMuted
import kotlinx.serialization.Serializable

@Serializable
data object OrbTerminalRoute

@Serializable
data object LedgerRoute

@Serializable
data object EngineSettingsRoute

private data class NavDestinationItem(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun FinorixApp(
    viewModel: FinorixViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val terminalState by viewModel.terminalState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allSignals by viewModel.allSignals.collectAsStateWithLifecycle()
    val filteredSignals by viewModel.filteredSignals.collectAsStateWithLifecycle()
    val ledgerStats by viewModel.ledgerStats.collectAsStateWithLifecycle()
    val ledgerFilter by viewModel.ledgerFilter.collectAsStateWithLifecycle()

    val pairSignals = allSignals.filter { it.pairSymbol == terminalState.activePair.symbol }

    val navItems = listOf(
        NavDestinationItem(
            route = OrbTerminalRoute,
            labelRes = R.string.nav_orb,
            selectedIcon = Icons.Filled.AutoAwesome,
            unselectedIcon = Icons.Outlined.AutoAwesome,
            testTag = "nav_orb"
        ),
        NavDestinationItem(
            route = LedgerRoute,
            labelRes = R.string.nav_ledger,
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History,
            testTag = "nav_ledger"
        ),
        NavDestinationItem(
            route = EngineSettingsRoute,
            labelRes = R.string.nav_engine,
            selectedIcon = Icons.Filled.Tune,
            unselectedIcon = Icons.Outlined.Tune,
            testTag = "nav_engine"
        )
    )

    fun navigateTo(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(id = item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = SlateElevated,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(id = item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = item.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = SlateElevated,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = OrbTerminalRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<OrbTerminalRoute> {
                        OrbTerminalScreen(
                            uiState = terminalState,
                            settings = settings,
                            pairSignals = pairSignals,
                            formatPrice = viewModel::formatPrice,
                            onSelectPair = viewModel::selectPair,
                            onScanOrbClick = { viewModel.scanWithGemini38(isAutoTriggered = false) },
                            onAskGeminiLive = viewModel::askGeminiLive,
                            onAnalyzeChartBitmap = viewModel::analyzeChartScreenshot,
                            onToggleVoice = viewModel::updateVoiceCallout
                        )
                    }
                    composable<LedgerRoute> {
                        LedgerScreen(
                            signals = filteredSignals,
                            stats = ledgerStats,
                            activeFilter = ledgerFilter,
                            formatPrice = viewModel::formatPrice,
                            onSelectFilter = viewModel::setLedgerFilter,
                            onClearHistory = viewModel::clearLedgerHistory
                        )
                    }
                    composable<EngineSettingsRoute> {
                        EngineSettingsScreen(
                            settings = settings,
                            isTestingConnection = terminalState.isTestingConnection,
                            diagnosticMessage = terminalState.diagnosticMessage,
                            onMinConfidenceChange = viewModel::updateMinConfidence,
                            onAutoLockChange = viewModel::updateAutoLock,
                            onVoiceCalloutChange = viewModel::updateVoiceCallout,
                            onTestConnectionClick = viewModel::runDiagnosticCheck
                        )
                    }
                }
            }
        }
    }
}
