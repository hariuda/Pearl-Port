package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

@Composable
fun PortfolioGrowthChart(invested: Double, current: Double, modifier: Modifier = Modifier) {
    val isProfit = current >= invested
    val lineColor = if (isProfit) ProfitGreen else LossRed
    val gradientColors = listOf(lineColor.copy(alpha = 0.5f), Color.Transparent)
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Handle edge case where both are zero
        if (invested == 0.0 && current == 0.0) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, height / 2),
                end = Offset(width, height / 2),
                strokeWidth = 2.dp.toPx()
            )
            return@Canvas
        }
        
        val minVal = minOf(invested, current) * 0.95
        val maxVal = maxOf(invested, current) * 1.05
        val range = if (maxVal == minVal) 1.0 else (maxVal - minVal)
        
        val startY = height - ((invested - minVal) / range * height).toFloat()
        val endY = height - ((current - minVal) / range * height).toFloat()
        
        val path = Path().apply {
            moveTo(0f, startY)
            cubicTo(
                width * 0.5f, startY,
                width * 0.5f, endY,
                width, endY
            )
        }
        
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = gradientColors,
                startY = minOf(startY, endY),
                endY = height
            )
        )
        
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        drawCircle(
            color = lineColor,
            radius = 5.dp.toPx(),
            center = Offset(0f, startY)
        )
        drawCircle(
            color = lineColor,
            radius = 5.dp.toPx(),
            center = Offset(width, endY)
        )
    }
}
