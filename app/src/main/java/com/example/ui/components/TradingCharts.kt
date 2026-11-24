package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BollingerBands
import com.example.model.Candle
import com.example.model.OpenPosition
import com.example.model.SignalMarker
import com.example.ui.theme.LocalTradeColors
import com.example.ui.theme.MonoTypography
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChartCard(
    instrumentTicker: String,
    instrumentPersianName: String,
    candles: List<Candle>,
    emaFast: List<Double?>,
    emaSlow: List<Double?>,
    bollingerBands: BollingerBands,
    latestRsi: Double?,
    openPosition: OpenPosition?,
    lastSignalNote: String,
    modifier: Modifier = Modifier
) {
    val tradeColors = LocalTradeColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    val lastCandle = candles.lastOrNull()
    val firstCandle = candles.firstOrNull()
    val windowChangePct = if (lastCandle != null && firstCandle != null && firstCandle.open > 0.0) {
        ((lastCandle.close - firstCandle.open) / firstCandle.open) * 100.0
    } else 0.0
    val isUp = windowChangePct >= 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Instrument name + Live price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LtrText(
                            text = instrumentTicker,
                            style = MonoTypography.metricMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = instrumentPersianName,
                            style = MaterialTheme.typography.titleMedium,
                            color = onSurface
                        )
                    }
                    Text(
                        text = lastSignalNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    LtrText(
                        text = formatNumber(lastCandle?.close ?: 0.0),
                        style = MonoTypography.metricLarge,
                        color = onSurface
                    )
                    LtrText(
                        text = formatSignedPercent(windowChangePct),
                        style = MonoTypography.metricSmall,
                        color = if (isUp) tradeColors.profit else tradeColors.loss
                    )
                }
            }

            // Indicator Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = tradeColors.indicatorFast, label = stringResource(R.string.ema_fast_label))
                LegendChip(color = tradeColors.indicatorSlow, label = stringResource(R.string.ema_slow_label))
                LegendChip(color = tradeColors.bollingerLine, label = stringResource(R.string.bollinger_label))
            }

            // Candlestick Canvas (Time flows mathematically Left -> Right)
            if (candles.isNotEmpty()) {
                val minPrice = rememberMinPrice(candles, bollingerBands, openPosition)
                val maxPrice = rememberMaxPrice(candles, bollingerBands, openPosition)
                val priceRange = max(1e-4, maxPrice - minPrice)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(215.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val padV = 12.dp.toPx()
                        val usableH = (h - 2 * padV).coerceAtLeast(1f)

                        fun priceToY(price: Double): Float {
                            val norm = ((price - minPrice) / priceRange).toFloat().coerceIn(0f, 1f)
                            return padV + usableH * (1f - norm)
                        }

                        // Subtle horizontal gridlines
                        val gridSteps = 4
                        for (g in 0..gridSteps) {
                            val y = padV + (usableH * g / gridSteps)
                            drawLine(
                                color = tradeColors.chartGrid,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val count = candles.size
                        val slotWidth = w / max(1, count)
                        val bodyWidth = (slotWidth * 0.62f).coerceIn(3f, 14f)

                        // Draw Bollinger Band Upper & Lower dashed paths
                        val bbUpperPath = Path()
                        val bbLowerPath = Path()
                        var bbStarted = false
                        for (i in candles.indices) {
                            val u = bollingerBands.upper.getOrNull(i)
                            val l = bollingerBands.lower.getOrNull(i)
                            if (u != null && l != null) {
                                val x = i * slotWidth + slotWidth / 2f
                                if (!bbStarted) {
                                    bbUpperPath.moveTo(x, priceToY(u))
                                    bbLowerPath.moveTo(x, priceToY(l))
                                    bbStarted = true
                                } else {
                                    bbUpperPath.lineTo(x, priceToY(u))
                                    bbLowerPath.lineTo(x, priceToY(l))
                                }
                            }
                        }
                        if (bbStarted) {
                            val bbStroke = Stroke(
                                width = 1.1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                            drawPath(bbUpperPath, color = tradeColors.bollingerLine.copy(alpha = 0.55f), style = bbStroke)
                            drawPath(bbLowerPath, color = tradeColors.bollingerLine.copy(alpha = 0.55f), style = bbStroke)
                        }

                        // Draw Candlesticks
                        candles.forEachIndexed { idx, candle ->
                            val centerX = idx * slotWidth + slotWidth / 2f
                            val candleUp = candle.close >= candle.open
                            val color = if (candleUp) tradeColors.profit else tradeColors.loss

                            val yHigh = priceToY(candle.high)
                            val yLow = priceToY(candle.low)
                            val yOpen = priceToY(candle.open)
                            val yClose = priceToY(candle.close)

                            // Wick
                            drawLine(
                                color = color.copy(alpha = 0.85f),
                                start = Offset(centerX, yHigh),
                                end = Offset(centerX, yLow),
                                strokeWidth = 1.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Body
                            val top = min(yOpen, yClose)
                            val bodyHeight = max(2.dp.toPx(), kotlin.math.abs(yClose - yOpen))
                            drawRect(
                                color = color,
                                topLeft = Offset(centerX - bodyWidth / 2f, top),
                                size = Size(bodyWidth, bodyHeight)
                            )

                            // Automated Buy / Sell markers on candle
                            when (candle.signalMarker) {
                                SignalMarker.BUY -> {
                                    val triPath = Path().apply {
                                        val baseY = (yLow + 11.dp.toPx()).coerceAtMost(h - 2f)
                                        moveTo(centerX, baseY - 7.dp.toPx())
                                        lineTo(centerX - 4.5.dp.toPx(), baseY)
                                        lineTo(centerX + 4.5.dp.toPx(), baseY)
                                        close()
                                    }
                                    drawPath(triPath, color = tradeColors.profit)
                                }
                                SignalMarker.SELL_PROFIT, SignalMarker.SELL_LOSS -> {
                                    val sellColor = if (candle.signalMarker == SignalMarker.SELL_PROFIT) {
                                        tradeColors.warning
                                    } else {
                                        tradeColors.loss
                                    }
                                    val triPath = Path().apply {
                                        val tipY = (yHigh - 3.dp.toPx()).coerceAtLeast(8.dp.toPx())
                                        moveTo(centerX, tipY)
                                        lineTo(centerX - 4.5.dp.toPx(), tipY - 7.dp.toPx())
                                        lineTo(centerX + 4.5.dp.toPx(), tipY - 7.dp.toPx())
                                        close()
                                    }
                                    drawPath(triPath, color = sellColor)
                                }
                                SignalMarker.NONE -> Unit
                            }
                        }

                        // EMA Fast Line
                        drawIndicatorLine(
                            series = emaFast,
                            slotWidth = slotWidth,
                            priceToY = ::priceToY,
                            color = tradeColors.indicatorFast,
                            strokeWidth = 1.8.dp.toPx()
                        )

                        // EMA Slow Line
                        drawIndicatorLine(
                            series = emaSlow,
                            slotWidth = slotWidth,
                            priceToY = ::priceToY,
                            color = tradeColors.indicatorSlow,
                            strokeWidth = 1.8.dp.toPx()
                        )

                        // Open Position SL / TP / Entry lines
                        if (openPosition != null) {
                            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            val yTp = priceToY(openPosition.takeProfitPrice)
                            val ySl = priceToY(openPosition.stopLossPrice)
                            val yEntry = priceToY(openPosition.entryPrice)
                            drawLine(
                                color = tradeColors.profit.copy(alpha = 0.85f),
                                start = Offset(0f, yTp),
                                end = Offset(w, yTp),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dash
                            )
                            drawLine(
                                color = tradeColors.loss.copy(alpha = 0.85f),
                                start = Offset(0f, ySl),
                                end = Offset(w, ySl),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dash
                            )
                            drawLine(
                                color = onSurface.copy(alpha = 0.45f),
                                start = Offset(0f, yEntry),
                                end = Offset(w, yEntry),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash
                            )
                        }
                    }

                    // Price Scale Axis Labels
                    Column(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .height(215.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        val mid = (maxPrice + minPrice) / 2.0
                        LtrText(formatNumber(maxPrice, 1), MonoTypography.metricSmall, mutedColor)
                        LtrText(formatNumber((maxPrice + mid) / 2.0, 1), MonoTypography.metricSmall, mutedColor)
                        LtrText(formatNumber(mid, 1), MonoTypography.metricSmall, mutedColor)
                        LtrText(formatNumber((minPrice + mid) / 2.0, 1), MonoTypography.metricSmall, mutedColor)
                        LtrText(formatNumber(minPrice, 1), MonoTypography.metricSmall, mutedColor)
                    }
                }
            }

            // RSI Strip Oscillator Bar
            val rsiValue = latestRsi ?: 50.0
            val rsiColor = when {
                rsiValue <= 32.0 -> tradeColors.profit
                rsiValue >= 68.0 -> tradeColors.loss
                else -> tradeColors.indicatorFast
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.rsi_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor
                    )
                    LtrText(
                        text = formatNumber(rsiValue, 1),
                        style = MonoTypography.metricSmall,
                        color = rsiColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val fraction = (rsiValue / 100.0).toFloat().coerceIn(0.02f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(rsiColor)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIndicatorLine(
    series: List<Double?>,
    slotWidth: Float,
    priceToY: (Double) -> Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    var started = false
    for (i in series.indices) {
        val v = series[i] ?: continue
        val x = i * slotWidth + slotWidth / 2f
        val y = priceToY(v)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    if (started) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun rememberMinPrice(
    candles: List<Candle>,
    bb: BollingerBands,
    pos: OpenPosition?
): Double {
    var low = candles.minOf { it.low }
    bb.lower.filterNotNull().minOrNull()?.let { low = min(low, it) }
    pos?.let { low = min(low, it.stopLossPrice) }
    return low * 0.996
}

private fun rememberMaxPrice(
    candles: List<Candle>,
    bb: BollingerBands,
    pos: OpenPosition?
): Double {
    var high = candles.maxOf { it.high }
    bb.upper.filterNotNull().maxOrNull()?.let { high = max(high, it) }
    pos?.let { high = max(high, it.takeProfitPrice) }
    return high * 1.004
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EquityCurveChartCard(
    botEquity: List<Double>,
    benchmarkEquity: List<Double>,
    modifier: Modifier = Modifier
) {
    val tradeColors = LocalTradeColors.current
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                    text = stringResource(R.string.equity_curve_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendChip(color = tradeColors.profit, label = stringResource(R.string.legend_bot))
                LegendChip(color = tradeColors.indicatorSlow, label = stringResource(R.string.legend_benchmark))
            }

            if (botEquity.size >= 2) {
                val allValues = botEquity + benchmarkEquity
                val minVal = (allValues.minOrNull() ?: 9_000.0) * 0.99
                val maxVal = (allValues.maxOrNull() ?: 11_000.0) * 1.01
                val range = max(1.0, maxVal - minVal)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(185.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val padV = 10.dp.toPx()
                        val usableH = (h - 2 * padV).coerceAtLeast(1f)

                        fun valToY(v: Double): Float {
                            val norm = ((v - minVal) / range).toFloat().coerceIn(0f, 1f)
                            return padV + usableH * (1f - norm)
                        }

                        for (g in 0..3) {
                            val y = padV + usableH * g / 3f
                            drawLine(
                                color = tradeColors.chartGrid,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Benchmark dashed curve
                        if (benchmarkEquity.size >= 2) {
                            val benchPath = Path()
                            val stepX = w / (benchmarkEquity.size - 1)
                            benchmarkEquity.forEachIndexed { idx, v ->
                                val x = idx * stepX
                                val y = valToY(v)
                                if (idx == 0) benchPath.moveTo(x, y) else benchPath.lineTo(x, y)
                            }
                            drawPath(
                                path = benchPath,
                                color = tradeColors.indicatorSlow.copy(alpha = 0.8f),
                                style = Stroke(
                                    width = 1.8.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                )
                            )
                        }

                        // Bot Equity curve + gradient fill
                        val linePath = Path()
                        val fillPath = Path()
                        val stepX = w / (botEquity.size - 1)
                        botEquity.forEachIndexed { idx, v ->
                            val x = idx * stepX
                            val y = valToY(v)
                            if (idx == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }
                        fillPath.lineTo(w, h)
                        fillPath.close()

                        val isNetPositive = botEquity.last() >= botEquity.first()
                        val curveColor = if (isNetPositive) tradeColors.profit else tradeColors.loss

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    curveColor.copy(alpha = 0.28f),
                                    curveColor.copy(alpha = 0.0f)
                                )
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = curveColor,
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Column(
                        modifier = Modifier.height(185.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        LtrText(formatNumber(maxVal, 0), MonoTypography.metricSmall, mutedColor)
                        LtrText(formatNumber((maxVal + minVal) / 2.0, 0), MonoTypography.metricSmall, mutedColor)
                        LtrText(formatNumber(minVal, 0), MonoTypography.metricSmall, mutedColor)
                    }
                }
            }
        }
    }
}
