package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun EmptyPortfolioState(title: String = "No Assets Yet", message: String = "Tap the + button to add your first investment to this category.") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.empty_portfolio_img),
            contentDescription = "Empty Portfolio",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .aspectRatio(1f)
                .padding(bottom = 24.dp)
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            0.0f to androidx.compose.ui.graphics.Color.Black,
                            0.7f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
                            1.0f to androidx.compose.ui.graphics.Color.Transparent,
                            center = center,
                            radius = size.width / 2f
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                },
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.background, androidx.compose.ui.graphics.BlendMode.Multiply)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
