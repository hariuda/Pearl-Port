package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun GradientOutlinedCard(
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = CardDefaults.outlinedCardColors(),
    shape: Shape = RoundedCornerShape(12.dp),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF05080F) // BackgroundDark
    
    if (isDark) {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A), // Dark Navy
                Color(0xFF231B4D)  // Subtle Blue/Purple Gradient
            )
        )
        val finalBorder = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        
        OutlinedCard(
            modifier = modifier.background(gradientBrush, shape),
            shape = shape,
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent, contentColor = colors.contentColor),
            border = finalBorder,
            elevation = elevation,
            content = content
        )
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border ?: CardDefaults.outlinedCardBorder(),
            elevation = elevation,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientOutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = CardDefaults.outlinedCardColors(),
    shape: Shape = RoundedCornerShape(12.dp),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF05080F) // BackgroundDark
    
    if (isDark) {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A), // Dark Navy
                Color(0xFF231B4D)  // Subtle Blue/Purple Gradient
            )
        )
        val finalBorder = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        
        OutlinedCard(
            onClick = onClick,
            modifier = modifier.background(gradientBrush, shape),
            shape = shape,
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent, contentColor = colors.contentColor),
            border = finalBorder,
            elevation = elevation,
            content = content
        )
    } else {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border ?: CardDefaults.outlinedCardBorder(),
            elevation = elevation,
            content = content
        )
    }
}
