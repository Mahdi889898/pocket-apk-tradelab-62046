package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MarketInstrument
import com.example.model.MarketRegime
import com.example.model.StrategyType
import com.example.ui.components.EquityCurveChartCard
import com.example.ui.components.LtrText
import com.example.ui.components.MetricStatTile
import com.example.ui.components.formatNumber
import com.example.ui.components.formatSignedPercent
import com.example.ui.theme.LocalTradeColors
import com.example.ui.theme.MonoTypography
import com.example.viewmodel.TradeLabUiState

@Composable
fun BacktestScreen(
    uiState: TradeLabUiState,
    onSelectInstrument: (MarketInstrument) -> Unit,
    onSelectStrategy: (StrategyType) -> Unit,
    onSelectRegime: (MarketRegime) -> Unit,
    onSelectCandleCount: (Int) -> Unit,
    onRunBacktest: () -> Unit,
    onApplyToLiveBot: () -> Unit,
    onNavigateBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToDashboard)

    val tradeColors = LocalTradeColors.current
    val candleOptions = listOf(120, 250, 500)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.backtest_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.backtest_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Configuration Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.select_instrument),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MarketInstrument.entries) { inst ->
                            FilterChip(
                                selected = uiState.backtestInstrument == inst,
                                onClick = { onSelectInstrument(inst) },
                                label = { Text(inst.persianName, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("bt_inst_${inst.id.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.nav_strategies),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StrategyType.entries) { strat ->
                            FilterChip(
                                selected = uiState.backtestStrategyType == strat,
                                onClick = { onSelectStrategy(strat) },
                                label = { Text(strat.persianName, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("bt_strat_${strat.id.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.select_regime),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MarketRegime.entries) { regime ->
                            FilterChip(
                                selected = uiState.backtestRegime == regime,
                                onClick = { onSelectRegime(regime) },
                                label = { Text(regime.persianName, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("bt_regime_${regime.id.lowercase()}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.candle_count),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            candleOptions.forEach { count ->
                                FilterChip(
                                    selected = uiState.backtestCandleCount == count,
                                    onClick = { onSelectCandleCount(count) },
                                    label = {
                                        LtrText(
                                            text = "$count",
                                            style = MonoTypography.metricSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("bt_candles_$count")
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onRunBacktest,
                        enabled = !uiState.isBacktestRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("run_backtest_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isBacktestRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.run_backtest),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }

        val result = uiState.backtestResult
        if (result == null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource(R.string.backtest_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            item {
                EquityCurveChartCard(
                    botEquity = result.equityCurve,
                    benchmarkEquity = result.benchmarkCurve
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatTile(
                            label = stringResource(R.string.metric_net_return),
                            value = formatSignedPercent(result.netReturnPercent),
                            valueColor = if (result.netReturnPercent >= 0.0) tradeColors.profit else tradeColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatTile(
                            label = stringResource(R.string.legend_benchmark),
                            value = formatSignedPercent(result.benchmarkReturnPercent),
                            valueColor = if (result.benchmarkReturnPercent >= 0.0) tradeColors.indicatorSlow else tradeColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatTile(
                            label = stringResource(R.string.win_rate),
                            value = "${formatNumber(result.winRatePercent, 1)}%",
                            valueColor = if (result.winRatePercent >= 50.0) tradeColors.profit else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatTile(
                            label = stringResource(R.string.metric_total_trades),
                            value = "${result.totalTrades} (${result.winningTrades}W/${result.losingTrades}L)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatTile(
                            label = stringResource(R.string.max_drawdown),
                            value = "${formatNumber(result.maxDrawdownPercent, 2)}%",
                            valueColor = tradeColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatTile(
                            label = stringResource(R.string.metric_sharpe),
                            value = formatNumber(result.sharpeRatio, 2),
                            valueColor = if (result.sharpeRatio >= 1.0) tradeColors.profit else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatTile(
                            label = stringResource(R.string.metric_best_trade),
                            value = formatSignedPercent(result.bestTradePercent),
                            valueColor = tradeColors.profit,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatTile(
                            label = stringResource(R.string.metric_worst_trade),
                            value = formatSignedPercent(result.worstTradePercent),
                            valueColor = tradeColors.loss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onApplyToLiveBot,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_backtest_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.apply_to_live),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
