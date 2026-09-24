package com.optiontracker.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.optiontracker.app.domain.money.Money
import com.optiontracker.app.domain.pnl.MonthReportRow
import com.optiontracker.app.ui.format.formatMonthShort
import com.optiontracker.app.ui.format.pnlColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyRealizedChart(
    rows: List<MonthReportRow>,
    year: Int,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    val description = rows.joinToString(separator = ", ") { row ->
        "${formatMonthShort(row.yearMonth)} ${Money.formatSigned(row.totalCents)}"
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Monthly realized by open month. $description" },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Monthly realized", style = MaterialTheme.typography.titleMedium)
                Text(
                    chartSubtitle(rows, year),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BarPlot(rows)
            ValueLabels(rows)
        }
    }
}

@Composable
private fun BarPlot(rows: List<MonthReportRow>) {
    val top = rows.maxOf { it.totalCents }.coerceAtLeast(0L)
    val bottom = rows.minOf { it.totalCents }.coerceAtMost(0L)
    val profit = MaterialTheme.colorScheme.primary
    val loss = MaterialTheme.colorScheme.error
    val axis = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant
    val zeroMark = MaterialTheme.colorScheme.onSurfaceVariant
    val flat = top == 0L && bottom == 0L
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                AxisLabel(if (flat) "" else Money.formatChart(top), TextAlign.End)
                AxisLabel(if (flat || (top > 0L && bottom < 0L)) "$0" else "", TextAlign.End)
                AxisLabel(if (flat) "" else Money.formatChart(bottom), TextAlign.End)
            }
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val plotHeight = size.height
                val plotWidth = size.width
                val span = (top - bottom).takeIf { it > 0L } ?: 1L
                fun yOf(cents: Long): Float {
                    if (flat) return plotHeight / 2f
                    val fraction = (top - cents).toFloat() / span.toFloat()
                    return fraction * plotHeight
                }
                val zeroY = yOf(0L)
                drawLine(grid, Offset(0f, 1f), Offset(plotWidth, 1f), strokeWidth = 1f)
                drawLine(
                    grid,
                    Offset(0f, plotHeight - 1f),
                    Offset(plotWidth, plotHeight - 1f),
                    strokeWidth = 1f,
                )
                drawLine(axis, Offset(0f, zeroY), Offset(plotWidth, zeroY), strokeWidth = 2f)
                val slot = plotWidth / rows.size
                val barWidth = slot * 0.62f
                val minBar = 2.dp.toPx()
                rows.forEachIndexed { index, row ->
                    val center = slot * index + slot / 2f
                    val valueY = yOf(row.totalCents)
                    val barTop = min(valueY, zeroY)
                    val barHeight = max(abs(valueY - zeroY), if (row.totalCents == 0L) 0f else minBar)
                    val color = barColor(row.totalCents, profit, loss, zeroMark)
                    if (row.totalCents == 0L) {
                        drawLine(
                            color = color.copy(alpha = 0.45f),
                            start = Offset(center - barWidth / 2f, zeroY),
                            end = Offset(center + barWidth / 2f, zeroY),
                            strokeWidth = 3.dp.toPx(),
                        )
                    } else {
                        val radius = min(4.dp.toPx(), min(barWidth / 2f, barHeight / 2f))
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(center - barWidth / 2f, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(radius, radius),
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 56.dp)) {
            rows.forEach { row ->
                Text(
                    text = formatMonthShort(row.yearMonth),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String, align: TextAlign) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = align,
        maxLines = 1,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ValueLabels(rows: List<MonthReportRow>) {
    val labeled = rows.filter { it.tradeCount > 0 }
    if (labeled.isEmpty()) {
        Text(
            "No closed trades opened in this period.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labeled.forEach { row ->
            Text(
                text = "${formatMonthShort(row.yearMonth)} ${Money.formatSigned(row.totalCents)}",
                style = MaterialTheme.typography.labelLarge,
                color = pnlColor(row.totalCents),
            )
        }
    }
}

private fun chartSubtitle(rows: List<MonthReportRow>, year: Int): String {
    val first = formatMonthShort(rows.first().yearMonth)
    val last = formatMonthShort(rows.last().yearMonth)
    return if (rows.size == 12) {
        "By open month · $year"
    } else {
        "By open month · $first–$last $year"
    }
}

private fun barColor(cents: Long, profit: Color, loss: Color, zero: Color): Color = when {
    cents > 0L -> profit
    cents < 0L -> loss
    else -> zero
}
