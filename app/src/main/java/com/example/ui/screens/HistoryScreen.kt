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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ClosedTradeSummary
import com.example.ui.components.LtrText
import com.example.ui.components.MetricStatTile
import com.example.ui.components.formatNumber
import com.example.ui.components.formatSignedAmount
import com.example.ui.components.formatSignedPercent
import com.example.ui.theme.LocalTradeColors
import com.example.ui.theme.MonoTypography
import com.example.viewmodel.TradeHistoryFilter
import com.example.viewmodel.TradeLabUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    uiState: TradeLabUiState,
    onSelectFilter: (TradeHistoryFilter) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToDashboard)

    val tradeColors = LocalTradeColors.current
    val trades = uiState.filteredTrades
    val netClosedPnl = remember(uiState.closedTrades) {
        uiState.closedTrades.sumOf { it.pnlAmount }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (uiState.closedTrades.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.clear_history),
                            tint = tradeColors.loss
                        )
                    }
                }
            }
        }

        // Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    label = stringResource(R.string.metric_total_trades),
                    value = "${uiState.closedTrades.size}",
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = stringResource(R.string.win_rate),
                    value = "${formatNumber(uiState.winRatePercent, 1)}%",
                    valueColor = if (uiState.winRatePercent >= 50.0) tradeColors.profit else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = stringResource(R.string.total_pnl),
                    value = "${formatSignedAmount(netClosedPnl)} VT",
                    valueColor = if (netClosedPnl >= 0.0) tradeColors.profit else tradeColors.loss,
                    modifier = Modifier.weight(1.2f)
                )
            }
        }

        // Filter Chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.historyFilter == TradeHistoryFilter.ALL,
                    onClick = { onSelectFilter(TradeHistoryFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    modifier = Modifier.testTag("filter_all")
                )
                FilterChip(
                    selected = uiState.historyFilter == TradeHistoryFilter.WINS,
                    onClick = { onSelectFilter(TradeHistoryFilter.WINS) },
                    label = { Text(stringResource(R.string.filter_wins)) },
                    modifier = Modifier.testTag("filter_wins")
                )
                FilterChip(
                    selected = uiState.historyFilter == TradeHistoryFilter.LOSSES,
                    onClick = { onSelectFilter(TradeHistoryFilter.LOSSES) },
                    label = { Text(stringResource(R.string.filter_losses)) },
                    modifier = Modifier.testTag("filter_losses")
                )
            }
        }

        if (trades.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HistoryToggleOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.empty_history_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(trades, key = { it.id }) { trade ->
                TradeHistoryItemCard(trade = trade)
            }
        }

        // Honest Risk & Simulation Transparency Notice
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = tradeColors.warning,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.disclaimer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.disclaimer_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun TradeHistoryItemCard(trade: ClosedTradeSummary) {
    val tradeColors = LocalTradeColors.current
    val isProfit = trade.pnlAmount >= 0.0
    val pnlColor = if (isProfit) tradeColors.profit else tradeColors.loss
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trade_item_${trade.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LtrText(
                        text = trade.instrument.ticker,
                        style = MonoTypography.metricMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = trade.exitReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                LtrText(
                    text = formatSignedPercent(trade.pnlPercent),
                    style = MonoTypography.metricMedium,
                    color = pnlColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trade.strategyType.persianName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LtrText(
                    text = "${formatSignedAmount(trade.pnlAmount)} VT",
                    style = MonoTypography.metricSmall,
                    color = pnlColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LtrText(
                    text = "${formatNumber(trade.entryPrice)} → ${formatNumber(trade.exitPrice)}",
                    style = MonoTypography.metricSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LtrText(
                    text = timeFormat.format(Date(trade.timestampMillis)),
                    style = MonoTypography.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
