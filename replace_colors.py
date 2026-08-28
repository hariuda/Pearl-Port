import sys

with open("app/src/main/java/com/example/ui/theme/Color.kt", "r") as f:
    content = f.read()

# Replace Dark Theme Colors
target_colors = """// Dark Theme Colors
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF8A8F9E)
val BackgroundDark = Color(0xFF0A0D14)
val SurfaceDark = Color(0xFF131620)
val SurfaceVariantDark = Color(0xFF1A1D29)
val BorderDark = Color(0xFF24283B)
val PearlPurpleDark = Color(0xFF5B43D6)"""

new_colors = """// Dark Theme Colors
val TextPrimaryDark = Color(0xFFFFFFFF) // white
val TextSecondaryDark = Color(0xFF94A3B8) // cool grey
val BackgroundDark = Color(0xFF05080F) // very dark navy/black background
val SurfaceDark = Color(0xFF0F172A) // dark navy
val SurfaceVariantDark = Color(0xFF151D36) // slightly lighter dark navy
val BorderDark = Color(0xFF211D45) // extremely subtle blue/purple border
val PrimaryAccentDark = Color(0xFF4C1D95) // deep violet/purple
val HighlightDark = Color(0xFFB026FF) // electric purple"""

content = content.replace(target_colors, new_colors)

with open("app/src/main/java/com/example/ui/theme/Color.kt", "w") as f:
    f.write(content)
