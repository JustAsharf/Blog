package com.dailyassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Simple vertical bars; [bars] is label to value, oldest first. */
@Composable
fun BarChart(
    bars: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxHeight: Int = 96,
    highlightLast: Boolean = true
) {
    if (bars.isEmpty()) return
    val max = bars.maxOf { it.second }.coerceAtLeast(1L)
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bars.forEachIndexed { index, (label, value) ->
            val fraction = (value.toDouble() / max).toFloat().coerceIn(0f, 1f)
            val barHeight = (fraction * maxHeight).toInt().coerceAtLeast(if (value > 0) 4 else 2)
            val isLast = highlightLast && index == bars.lastIndex
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                        .background(
                            if (value == 0L) MaterialTheme.colorScheme.surfaceVariant
                            else if (isLast) barColor
                            else barColor.copy(alpha = 0.55f),
                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                        )
                )
                Text(
                    label,
                    fontSize = 9.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Donut for the category split. [slices] is colour to value. */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Long>>,
    modifier: Modifier = Modifier,
    centerLabel: String = "",
    centerCaption: String = ""
) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1L)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val stroke = Stroke(width = 30f)
            val inset = 20f
            val diameter = minOf(size.width, size.height) - inset * 2
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            var start = -90f
            slices.forEach { (color, value) ->
                val sweep = (value.toDouble() / total * 360.0).toFloat()
                if (sweep > 0.4f) {
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep - 1.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = MaterialTheme.typography.titleMedium)
            if (centerCaption.isNotEmpty()) {
                Text(
                    centerCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Horizontal breakdown row used under the donut. */
@Composable
fun BreakdownRow(
    color: Color,
    label: String,
    value: String,
    fraction: Float
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
    }
}
