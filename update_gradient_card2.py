import sys

with open("app/src/main/java/com/example/ui/components/GradientOutlinedCard.kt", "r") as f:
    content = f.read()

target2 = """    val isDark = isSystemInDarkTheme()
    val gradientBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A), // Dark Navy
                Color(0xFF231B4D)  // Subtle Blue/Purple Gradient
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                colors.containerColor,
                colors.containerColor
            )
        )
    }
    
    val finalBorder = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.background(gradientBrush, shape),
        shape = shape,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent, contentColor = colors.contentColor),
        border = finalBorder,
        elevation = elevation,
        content = content
    )"""

replacement2 = """    val isDark = MaterialTheme.colorScheme.background == Color(0xFF05080F) // BackgroundDark
    
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
    }"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/components/GradientOutlinedCard.kt", "w") as f:
    f.write(content)

