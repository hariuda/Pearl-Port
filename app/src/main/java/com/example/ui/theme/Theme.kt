package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ChartColors {
    val Default = mapOf(
        "Equities" to PearlPurple,
        "Fixed Deposits" to ProfitGreen,
        "Unit Trusts" to Color(0xFF3B82F6),
        "Crypto Currency" to Color(0xFF8B5CF6),
        "Gold & Other" to ChampagneGold
    )
    
    val Vibrant = mapOf(
        "Equities" to Color(0xFFFF5722),
        "Fixed Deposits" to Color(0xFF9C27B0),
        "Unit Trusts" to Color(0xFF2196F3),
        "Crypto Currency" to Color(0xFF4CAF50),
        "Gold & Other" to Color(0xFFFFC107)
    )
    
    val Ocean = mapOf(
        "Equities" to Color(0xFF0077B6),
        "Fixed Deposits" to Color(0xFF00B4D8),
        "Unit Trusts" to Color(0xFF90E0EF),
        "Crypto Currency" to Color(0xFF03045E),
        "Gold & Other" to Color(0xFFCAF0F8)
    )
    
    fun getPalette(name: String): Map<String, Color> {
        return when (name) {
            "Vibrant" -> Vibrant
            "Ocean" -> Ocean
            else -> Default
        }
    }
}

private val ElegantDarkColorScheme = darkColorScheme(
    primary = PearlPurple,
    onPrimary = SurfacePaper,
    primaryContainer = PearlPurpleLight,
    onPrimaryContainer = SurfacePaper,
    secondary = PearlPurpleLight,
    onSecondary = SurfacePaper,
    background = DeepCharcoal,
    onBackground = TextPrimary,
    surface = DeepCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = DeepCharcoal,
    onSurfaceVariant = TextSecondary,
    error = LossRed,
    onError = TextPrimary
)

private val ElegantLightColorScheme = lightColorScheme(
    primary = PearlPurple,
    onPrimary = SurfacePaper,
    primaryContainer = PearlPurpleLight,
    onPrimaryContainer = SurfacePaper,
    secondary = PearlPurpleLight,
    onSecondary = SurfacePaper,
    background = BgLight,
    onBackground = DarkText,
    surface = SurfacePaper,
    onSurface = DarkText,
    surfaceVariant = BgLight,
    onSurfaceVariant = MutedText,
    error = LossRed,
    onError = SurfacePaper
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Force light theme for consistency with design
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) ElegantDarkColorScheme else ElegantLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
