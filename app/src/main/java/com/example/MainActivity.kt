package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BacktestScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.StrategiesScreen
import com.example.ui.theme.TradeLabTheme
import com.example.viewmodel.TradeLabViewModel

enum class TradeLabTab(
    val routeId: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Rounded.CandlestickChart, Icons.Outlined.CandlestickChart),
    STRATEGIES("strategies", R.string.nav_strategies, Icons.Rounded.AutoGraph, Icons.Outlined.AutoGraph),
    BACKTEST("backtest", R.string.nav_backtest, Icons.Rounded.Science, Icons.Outlined.Science),
    HISTORY("history", R.string.nav_history, Icons.Rounded.History, Icons.Outlined.History)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TradeLabViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TradeLabTheme(darkTheme = uiState.isDarkTheme) {
                    TradeLabApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeLabApp(viewModel: TradeLabViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(TradeLabTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeSnackbar()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme_desc)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        TradeLabTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("nav_${tab.routeId}")
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
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        TradeLabTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.testTag("rail_${tab.routeId}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)

                    when (currentTab) {
                        TradeLabTab.DASHBOARD -> DashboardScreen(
                            uiState = uiState,
                            onToggleBot = { viewModel.toggleBotRunning() },
                            onSelectSpeed = { viewModel.setSimulationSpeed(it) },
                            onSelectInstrument = { viewModel.selectInstrument(it) },
                            onClosePosition = { viewModel.closeOpenPositionManually() },
                            onResetPortfolio = { viewModel.resetVirtualPortfolio() },
                            onOpenStrategies = { currentTab = TradeLabTab.STRATEGIES },
                            modifier = contentModifier
                        )
                        TradeLabTab.STRATEGIES -> StrategiesScreen(
                            activeConfig = uiState.activeConfig,
                            onSaveConfig = { viewModel.updateStrategyConfig(it) },
                            onNavigateBackToDashboard = { currentTab = TradeLabTab.DASHBOARD },
                            modifier = contentModifier
                        )
                        TradeLabTab.BACKTEST -> BacktestScreen(
                            uiState = uiState,
                            onSelectInstrument = { viewModel.setBacktestInstrument(it) },
                            onSelectStrategy = { viewModel.setBacktestStrategy(it) },
                            onSelectRegime = { viewModel.setBacktestRegime(it) },
                            onSelectCandleCount = { viewModel.setBacktestCandleCount(it) },
                            onRunBacktest = { viewModel.runBacktest() },
                            onApplyToLiveBot = {
                                viewModel.applyBacktestToLiveBot()
                                currentTab = TradeLabTab.DASHBOARD
                            },
                            onNavigateBackToDashboard = { currentTab = TradeLabTab.DASHBOARD },
                            modifier = contentModifier
                        )
                        TradeLabTab.HISTORY -> HistoryScreen(
                            uiState = uiState,
                            onSelectFilter = { viewModel.setHistoryFilter(it) },
                            onClearHistory = { viewModel.clearTradeHistory() },
                            onNavigateBackToDashboard = { currentTab = TradeLabTab.DASHBOARD },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
