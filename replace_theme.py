import sys

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    content = f.read()

target_theme = """private val ElegantDarkColorScheme = darkColorScheme(
    primary = PearlPurpleDark,
    onPrimary = BackgroundDark,
    primaryContainer = PearlPurple,
    onPrimaryContainer = SurfacePaper,
    secondary = PearlPurpleLight,
    onSecondary = SurfacePaper,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = LossRed,
    onError = TextPrimaryDark,
    outlineVariant = BorderDark
)"""

new_theme = """private val ElegantDarkColorScheme = darkColorScheme(
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
)"""

content = content.replace(target_theme, new_theme)

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(content)
