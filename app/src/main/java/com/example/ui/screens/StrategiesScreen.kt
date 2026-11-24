package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.StrategyConfig
import com.example.model.StrategyType
import com.example.ui.components.LtrText
import com.example.ui.components.formatNumber
import com.example.ui.theme.LocalTradeColors
import com.example.ui.theme.MonoTypography
import kotlin.math.roundToInt

@Composable
fun StrategiesScreen(
    activeConfig: StrategyConfig,
    onSaveConfig: (StrategyConfig) -> Unit,
    onNavigateBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBackToDashboard)

    var draft by remember(activeConfig) { mutableStateOf(activeConfig) }
    val tradeColors = LocalTradeColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.strategies_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.strategies_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(StrategyType.entries, key = { it.id }) { strategy ->
            val isSelected = draft.strategyType == strategy
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { draft = draft.copy(strategyType = strategy) }
                    .testTag("strategy_card_${strategy.id.lowercase()}"),
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strategy.persianName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(R.string.active_badge),
                                    tint = tradeColors.profit,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.active_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tradeColors.profit
                                )
                            }
                        }
                    }

                    Text(
                        text = strategy.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        LtrText(
                            text = strategy.formulaHint,
                            style = MonoTypography.metricSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Risk Management Sliders
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.risk_params_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ParameterSliderRow(
                        label = stringResource(R.string.stop_loss_percent),
                        valueText = "${formatNumber(draft.stopLossPercent, 1)}%",
                        value = draft.stopLossPercent.toFloat(),
                        valueRange = 0.5f..10f,
                        onValueChange = { draft = draft.copy(stopLossPercent = (it * 2f).roundToInt() / 2.0) },
                        testTag = "slider_stop_loss"
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.take_profit_percent),
                        valueText = "${formatNumber(draft.takeProfitPercent, 1)}%",
                        value = draft.takeProfitPercent.toFloat(),
                        valueRange = 1f..20f,
                        onValueChange = { draft = draft.copy(takeProfitPercent = (it * 2f).roundToInt() / 2.0) },
                        testTag = "slider_take_profit"
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.allocation_percent),
                        valueText = "${draft.allocationPercent.roundToInt()}%",
                        value = draft.allocationPercent.toFloat(),
                        valueRange = 5f..50f,
                        onValueChange = { draft = draft.copy(allocationPercent = it.roundToInt().toDouble()) },
                        testTag = "slider_allocation"
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.max_drawdown_limit),
                        valueText = "${draft.maxDrawdownPercent.roundToInt()}%",
                        value = draft.maxDrawdownPercent.toFloat(),
                        valueRange = 3f..25f,
                        onValueChange = { draft = draft.copy(maxDrawdownPercent = it.roundToInt().toDouble()) },
                        testTag = "slider_max_drawdown"
                    )
                }
            }
        }

        // Indicator Parameters
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
                        text = stringResource(R.string.indicator_params_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.ema_fast_period),
                        valueText = "${draft.emaFastPeriod}",
                        value = draft.emaFastPeriod.toFloat(),
                        valueRange = 4f..18f,
                        onValueChange = {
                            val f = it.roundToInt()
                            draft = draft.copy(
                                emaFastPeriod = f,
                                emaSlowPeriod = maxOf(draft.emaSlowPeriod, f + 4)
                            )
                        },
                        testTag = "slider_ema_fast"
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.ema_slow_period),
                        valueText = "${draft.emaSlowPeriod}",
                        value = draft.emaSlowPeriod.toFloat(),
                        valueRange = 12f..45f,
                        onValueChange = {
                            val s = it.roundToInt().coerceAtLeast(draft.emaFastPeriod + 4)
                            draft = draft.copy(emaSlowPeriod = s)
                        },
                        testTag = "slider_ema_slow"
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.rsi_oversold),
                        valueText = "${draft.rsiOversold}",
                        value = draft.rsiOversold.toFloat(),
                        valueRange = 18f..42f,
                        onValueChange = { draft = draft.copy(rsiOversold = it.roundToInt()) },
                        testTag = "slider_rsi_oversold"
                    )

                    ParameterSliderRow(
                        label = stringResource(R.string.rsi_overbought),
                        valueText = "${draft.rsiOverbought}",
                        value = draft.rsiOverbought.toFloat(),
                        valueRange = 58f..82f,
                        onValueChange = { draft = draft.copy(rsiOverbought = it.roundToInt()) },
                        testTag = "slider_rsi_overbought"
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onSaveConfig(draft) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_strategy_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.save_settings),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ParameterSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            LtrText(
                text = valueText,
                style = MonoTypography.metricMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
