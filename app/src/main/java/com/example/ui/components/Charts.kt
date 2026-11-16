package com.example.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChemistryColor
import com.example.ui.theme.MathsColor
import com.example.ui.theme.PhysicsColor

@Composable
fun RetentionLineChart(
    points: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Complete your first revision session to plot retention trend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height

            // Horizontal reference lines (25%, 50%, 75%, 100%)
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = h * (1f - frac)
                drawLine(
                    color = gridColor.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val coords = points.mapIndexed { index, (_, value) ->
                val x = if (points.size == 1) w / 2f else (index.toFloat() / (points.size - 1)) * w
                val y = h - ((value.toFloat().coerceIn(0f, 100f) / 100f) * h)
                Offset(x, y)
            }

            if (coords.size > 1) {
                val areaPath = Path().apply {
                    moveTo(coords.first().x, h)
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, h)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = pt)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BarDistributionChart(
    bars: List<Triple<String, Float, Color>>,
    modifier: Modifier = Modifier,
    emptyText: String = "No data recorded yet."
) {
    if (bars.isEmpty() || bars.all { it.second <= 0f }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = (bars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val slotW = size.width / bars.size
            val barW = (slotW * 0.52f).coerceAtMost(36.dp.toPx())
            bars.forEachIndexed { idx, (_, value, color) ->
                val barH = (value / maxVal) * size.height
                val left = idx * slotW + (slotW - barW) / 2f
                val top = size.height - barH
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barW, barH.coerceAtLeast(2.dp.toPx())),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEach { (label, value, _) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

data class MultiSeriesTestPoint(
    val label: String,
    val physicsPct: Float,
    val chemistryPct: Float,
    val mathsPct: Float,
    val totalPct: Float
)

@Composable
fun TestTrendChart(
    points: List<MultiSeriesTestPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log 1+ mock or coaching tests to visualize Physics, Chemistry & Maths trends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val totalColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            val w = size.width
            val h = size.height
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = h * (1f - frac)
                drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }

            fun drawSeries(values: List<Float>, color: Color) {
                val coords = values.mapIndexed { idx, v ->
                    val x = if (values.size == 1) w / 2f else (idx.toFloat() / (values.size - 1)) * w
                    val y = h - (v.coerceIn(0f, 100f) / 100f) * h
                    Offset(x, y)
                }
                if (coords.size > 1) {
                    val path = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) lineTo(coords[i].x, coords[i].y)
                    }
                    drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                coords.forEach { drawCircle(color, radius = 3.5.dp.toPx(), center = it) }
            }

            drawSeries(points.map { it.physicsPct }, PhysicsColor)
            drawSeries(points.map { it.chemistryPct }, ChemistryColor)
            drawSeries(points.map { it.mathsPct }, MathsColor)
            drawSeries(points.map { it.totalPct }, totalColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot("Phy", PhysicsColor)
            LegendDot("Chem", ChemistryColor)
            LegendDot("Math", MathsColor)
            LegendDot("Total", totalColor)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
