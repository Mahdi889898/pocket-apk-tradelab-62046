package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MarketInstrument
import com.example.model.OpenPosition
import com.example.ui.components.CandlestickChartCard
import com.example.ui.components.LtrText
import com.example.ui.components.MetricStatTile
import com.example.ui.components.PaperModeBanner
import com.example.ui.components.StatusPulseBadge
import com.example.ui.components.formatNumber
import com.example.ui.components.formatSignedAmount
import com.example.ui.components.formatSignedPercent
import com.example.ui.theme.LocalTradeColors
import com.example.ui.theme.MonoTypography
import com.example.viewmodel.InstrumentSnapshot
import com.example.viewmodel.SimulationSpeed
import com.example.viewmodel.TradeLabUiState

@Composable
fun DashboardScreen(
    uiState: TradeLabUiState,
    onToggleBot: () -> Unit,
    onSelectSpeed: (SimulationSpeed) -> Unit,
    onSelectInstrument: (MarketInstrument) -> Unit,
    onClosePosition: () -> Unit,
    onResetPortfolio: () -> Unit,
    onOpenStrategies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tradeColors = LocalTradeColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PaperModeBanner()
        }

        // Hero Portfolio Equity Card with atmospheric generated banner
        item {
            HeroPortfolioCard(
                uiState = uiState,
                onResetPortfolio = onResetPortfolio
            )
        }

        // Master Bot Controller Card
        item {
            BotControlCard(
                uiState = uiState,
                onToggleBot = onToggleBot,
                onSelectSpeed = onSelectSpeed,
                onOpenStrategies = onOpenStrategies
            )
        }

        // Market Instrument Horizontal Selector
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(uiState.instrumentSnapshots, key = { it.instrument.id }) { snapshot ->
                    InstrumentTickerCard(
                        snapshot = snapshot,
                        isSelected = snapshot.instrument == uiState.selectedInstrument,
                        onClick = { onSelectInstrument(snapshot.instrument) }
                    )
                }
            }
        }

        // Live Candlestick + Technical Indicators Chart
        item {
            val posForChart = uiState.openPosition?.takeIf { it.instrument == uiState.selectedInstrument }
            CandlestickChartCard(
                instrumentTicker = uiState.selectedInstrument.ticker,
                instrumentPersianName = uiState.selectedInstrument.persianName,
                candles = uiState.visibleCandles,
                emaFast = uiState.emaFastSeries,
                emaSlow = uiState.emaSlowSeries,
                bollingerBands = uiState.bollingerBands,
                latestRsi = uiState.latestRsi,
                openPosition = posForChart,
                lastSignalNote = uiState.lastSignalNote
            )
        }

        // Active Position & Risk Guard Card
        item {
            val openPos = uiState.openPosition
            val currentPosPrice = openPos?.let { p ->
                uiState.instrumentSnapshots.firstOrNull { it.instrument == p.instrument }?.lastPrice
                    ?: p.entryPrice
            }
            OpenPositionCard(
                openPosition = openPos,
                currentPrice = currentPosPrice,
                maxDrawdownLimit = uiState.activeConfig.maxDrawdownPercent,
                observedDrawdown = uiState.maxObservedDrawdownPct,
                onClosePosition = onClosePosition
            )
        }

        // Quantitative KPI Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    label = stringResource(R.string.win_rate),
                    value = "${formatNumber(uiState.winRatePercent, 1)}%",
                    valueColor = if (uiState.winRatePercent >= 50.0) tradeColors.profit else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = stringResource(R.string.profit_factor),
                    value = formatNumber(uiState.profitFactor, 2),
                    valueColor = if (uiState.profitFactor >= 1.2) tradeColors.profit else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricStatTile(
                    label = stringResource(R.string.max_drawdown),
                    value = "${formatNumber(uiState.maxObservedDrawdownPct, 2)}%",
                    valueColor = if (uiState.maxObservedDrawdownPct > 5.0) tradeColors.loss else tradeColors.warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HeroPortfolioCard(
    uiState: TradeLabUiState,
    onResetPortfolio: () -> Unit
) {
    val tradeColors = LocalTradeColors.current
    val isProfit = uiState.totalPnlAmount >= 0.0
    val pnlColor = if (isProfit) tradeColors.profit else tradeColors.loss

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_quant_header_1795554708284),
                contentDescription = stringResource(R.string.hero_banner_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9090D16),
                                Color(0xEB0B1220)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.virtual_equity),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        LtrText(
                            text = "${formatNumber(uiState.totalEquity)} VT",
                            style = MonoTypography.heroNumber,
                            color = Color(0xFFF8FAFC)
                        )
                    }
                    IconButton(
                        onClick = onResetPortfolio,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("reset_portfolio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.reset_portfolio),
                            tint = Color(0xFFCBD5E1)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isProfit) Color(0xFF053B2E) else Color(0xFF45121D)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LtrText(
                                text = formatSignedPercent(uiState.totalPnlPercent),
                                style = MonoTypography.metricMedium,
                                color = pnlColor
                            )
                            LtrText(
                                text = "(${formatSignedAmount(uiState.totalPnlAmount)})",
                                style = MonoTypography.metricSmall,
                                color = pnlColor.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.available_cash),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        LtrText(
                            text = "${formatNumber(uiState.availableCash)} VT",
                            style = MonoTypography.metricMedium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BotControlCard(
    uiState: TradeLabUiState,
    onToggleBot: () -> Unit,
    onSelectSpeed: (SimulationSpeed) -> Unit,
    onOpenStrategies: () -> Unit
) {
    val tradeColors = LocalTradeColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPulseBadge(
                    isRunning = uiState.isBotRunning,
                    isHalted = uiState.isDrawdownHalted
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenStrategies() }
                        .testTag("active_strategy_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = uiState.activeConfig.strategyType.persianName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleBot,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("toggle_bot_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isBotRunning) tradeColors.loss else tradeColors.profit,
                        contentColor = if (uiState.isBotRunning) Color.White else Color(0xFF002419)
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isBotRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (uiState.isBotRunning) R.string.bot_pause else R.string.bot_start),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Speed Selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulationSpeed.entries.forEach { speed ->
                        val selected = uiState.simulationSpeed == speed
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSpeed(speed) },
                            label = {
                                LtrText(
                                    text = speed.label,
                                    style = MonoTypography.metricSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("speed_chip_${speed.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstrumentTickerCard(
    snapshot: InstrumentSnapshot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tradeColors = LocalTradeColors.current
    val isUp = snapshot.changePercent >= 0.0
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("instrument_card_${snapshot.instrument.id.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LtrText(
                    text = snapshot.instrument.ticker,
                    style = MonoTypography.metricSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                LtrText(
                    text = formatSignedPercent(snapshot.changePercent),
                    style = MonoTypography.metricSmall,
                    color = if (isUp) tradeColors.profit else tradeColors.loss
                )
            }
            Text(
                text = snapshot.instrument.persianName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LtrText(
                text = formatNumber(snapshot.lastPrice),
                style = MonoTypography.metricMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OpenPositionCard(
    openPosition: OpenPosition?,
    currentPrice: Double?,
    maxDrawdownLimit: Double,
    observedDrawdown: Double,
    onClosePosition: () -> Unit
) {
    val tradeColors = LocalTradeColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.open_position_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tradeColors.profitContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = tradeColors.profit,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.risk_guard_active),
                            style = MaterialTheme.typography.labelSmall,
                            color = tradeColors.profit
                        )
                    }
                }
            }

            if (openPosition == null || currentPrice == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_open_position),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.no_open_position_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val pnlAmount = openPosition.unrealizedPnl(currentPrice)
                val pnlPercent = openPosition.unrealizedPnlPercent(currentPrice)
                val isPositive = pnlAmount >= 0.0
                val pnlColor = if (isPositive) tradeColors.profit else tradeColors.loss

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        LtrText(
                            text = "${openPosition.instrument.ticker} • LONG",
                            style = MonoTypography.metricMedium,
                            color = tradeColors.profit
                        )
                        Text(
                            text = openPosition.entryReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        LtrText(
                            text = formatSignedPercent(pnlPercent),
                            style = MonoTypography.metricLarge,
                            color = pnlColor
                        )
                        LtrText(
                            text = "${formatSignedAmount(pnlAmount)} VT",
                            style = MonoTypography.metricSmall,
                            color = pnlColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricStatTile(
                        label = stringResource(R.string.entry_price),
                        value = formatNumber(openPosition.entryPrice),
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = stringResource(R.string.stop_loss_short),
                        value = formatNumber(openPosition.stopLossPrice),
                        valueColor = tradeColors.loss,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatTile(
                        label = stringResource(R.string.take_profit_short),
                        value = formatNumber(openPosition.takeProfitPrice),
                        valueColor = tradeColors.profit,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Progress between SL and TP
                val span = (openPosition.takeProfitPrice - openPosition.stopLossPrice).coerceAtLeast(1e-6)
                val progress = ((currentPrice - openPosition.stopLossPrice) / span).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isPositive) tradeColors.profit else tradeColors.loss,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                OutlinedButton(
                    onClick = onClosePosition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_position_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tradeColors.loss.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = tradeColors.loss,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.close_position_now),
                        style = MaterialTheme.typography.labelLarge,
                        color = tradeColors.loss
                    )
                }
            }

            // Drawdown Safety Limit footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.risk_guard_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LtrText(
                    text = "DD ${formatNumber(observedDrawdown, 1)}% / ${formatNumber(maxDrawdownLimit, 0)}%",
                    style = MonoTypography.metricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
