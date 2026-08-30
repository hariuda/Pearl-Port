package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SectorPieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        Color(0xFF00C853),
        Color(0xFFFFD600)
    )
) {
    if (data.isEmpty()) return
    val total = data.values.sum()
    if (total == 0.0) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val sweepAngles = data.values.map { (it / total) * 360f }
        var startAngle = -90f
        val strokeWidth = 24.dp.toPx()
        
        // Add a small gap between arcs for the Material You look
        val gapAngle = 4f
        
        sweepAngles.forEachIndexed { index, sweepAngle ->
            val actualSweep = if (sweepAngle > gapAngle * 1.5f && sweepAngles.size > 1) sweepAngle - gapAngle else sweepAngle
            val actualStart = if (sweepAngle > gapAngle * 1.5f && sweepAngles.size > 1) startAngle + gapAngle / 2f else startAngle
            
            drawArc(
                color = colors[index % colors.size],
                startAngle = actualStart.toFloat(),
                sweepAngle = actualSweep.toFloat(),
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle.toFloat()
        }
    }
}

@Composable
fun PerformanceLineChart(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (dataPoints.isEmpty()) return
    val maxPoint = dataPoints.maxOrNull() ?: 0.0
    val minPoint = dataPoints.minOrNull() ?: 0.0
    val range = maxPoint - minPoint

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val pointSpacing = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

        val path = Path()
        dataPoints.forEachIndexed { index, point ->
            val x = index * pointSpacing
            // if range is 0, draw line in middle
            val y = if (range == 0.0) height / 2f else height - ((point - minPoint) / range * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
