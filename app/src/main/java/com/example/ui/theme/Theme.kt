package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

    val SectorDefault = listOf(
        Color(0xFF4A3B8C), // PearlPurpleLight
        Color(0xFF2E8B57), // SeaGreen
        Color(0xFFD4AF37), // ChampagneGold
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6), // Violet
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // ProfitGreen
        Color(0xFFF43F5E), // Rose
        Color(0xFF6366F1), // Indigo
        Color(0xFF14B8A6)  // Teal
    )

    val SectorVibrant = listOf(
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF3F51B5), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF795548)
    )

    val SectorOcean = listOf(
        Color(0xFF03045E), Color(0xFF0077B6), Color(0xFF00B4D8), Color(0xFF90E0EF),
        Color(0xFFCAF0F8), Color(0xFF48CAE4), Color(0xFF0096C7), Color(0xFF023E8A)
    )

    fun getPalette(name: String): Map<String, Color> {
        return when (name) {
            "Vibrant" -> Vibrant
            "Ocean" -> Ocean
            else -> Default
        }
    }

    fun getSectorPalette(name: String): List<Color> {
        return when (name) {
            "Vibrant" -> SectorVibrant
            "Ocean" -> SectorOcean
            else -> SectorDefault
        }
    }
}

private val ElegantDarkColorScheme = darkColorScheme(
    primary = HighlightDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryAccentDark,
    onPrimaryContainer = Color.White,
    secondary = HighlightDark,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = LossRed,
    onError = TextPrimaryDark,
    outlineVariant = BorderDark
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
    onError = SurfacePaper,
    outlineVariant = Color.Transparent
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = ElegantLightColorScheme,
    typography = Typography,
    content = content
  )
}
