package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalTradeColors
import com.example.ui.theme.MonoTypography
import java.util.Locale

fun formatNumber(value: Double, decimals: Int = 2): String =
    String.format(Locale.US, "%,.${decimals}f", value)

fun formatSignedPercent(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return "$prefix${String.format(Locale.US, "%.2f", value)}%"
}

fun formatSignedAmount(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return "$prefix${String.format(Locale.US, "%,.2f", value)}"
}

@Composable
fun LtrText(
    text: String,
    style: TextStyle = MonoTypography.metricMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}

@Composable
fun PaperModeBanner(modifier: Modifier = Modifier) {
    val tradeColors = LocalTradeColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = tradeColors.warningContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, tradeColors.warning.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Science,
                contentDescription = stringResource(R.string.paper_mode_badge),
                tint = tradeColors.warning,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.paper_mode_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPulseBadge(
    isRunning: Boolean,
    isHalted: Boolean,
    modifier: Modifier = Modifier
) {
    val tradeColors = LocalTradeColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "bot_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (dotColor, bgColor, labelRes) = when {
        isHalted -> Triple(tradeColors.loss, tradeColors.lossContainer, R.string.bot_halted_drawdown)
        isRunning -> Triple(tradeColors.profit, tradeColors.profitContainer, R.string.bot_running)
        else -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant,
            R.string.bot_paused
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(if (isRunning) pulseAlpha else 1f)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = dotColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricStatTile(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            LtrText(
                text = value,
                style = MonoTypography.metricMedium,
                color = valueColor
            )
        }
    }
}
