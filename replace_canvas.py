import sys

target = """                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path()
                            val width = size.width
                            val height = size.height
                            
                            val mappedPoints = chartPoints.mapIndexed { index, y ->
                                (index.toFloat() / numPoints) to y
                            }
                            
                            path.moveTo(0f, height * mappedPoints.first().second)
                            for (i in 1 until mappedPoints.size) {
                                path.lineTo(width * mappedPoints[i].first, height * mappedPoints[i].second)
                            }
                            
                            val areaPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryContainer.copy(alpha = 0.3f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                ),
                                style = Fill
                            )
                            
                            drawPath(
                                path = path,
                                color = primaryContainer,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            drawCircle(
                                color = primaryContainer,
                                radius = 5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(width, height * mappedPoints.last().second)
                            )
                        }
                    }"""

replacement = """                    val textMeasurer = rememberTextMeasurer()
                    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)) {
                            val width = size.width
                            val height = size.height
                            
                            val gridLines = 4
                            for (i in 0 until gridLines) {
                                val y = height * (i / (gridLines - 1).toFloat())
                                drawLine(
                                    color = gridColor,
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                
                                val value = chartMax - (chartMax - chartMin) * (i / (gridLines - 1).toFloat())
                                val formattedValue = if (value >= 1_000_000) {
                                    String.format(java.util.Locale.US, "%.1fM", value / 1_000_000)
                                } else if (value >= 1_000) {
                                    String.format(java.util.Locale.US, "%.1fK", value / 1_000)
                                } else {
                                    String.format(java.util.Locale.US, "%.0f", value)
                                }
                                
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = formattedValue,
                                    style = TextStyle(color = labelColor, fontSize = 10.sp),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, y - 14.dp.toPx())
                                )
                            }
                            
                            val path = Path()
                            
                            val mappedPoints = chartPoints.mapIndexed { index, y ->
                                (index.toFloat() / numPoints) to y
                            }
                            
                            path.moveTo(0f, height * mappedPoints.first().second)
                            for (i in 1 until mappedPoints.size) {
                                path.lineTo(width * mappedPoints[i].first, height * mappedPoints[i].second)
                            }
                            
                            val areaPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryContainer.copy(alpha = 0.3f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                ),
                                style = Fill
                            )
                            
                            drawPath(
                                path = path,
                                color = primaryContainer,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            drawCircle(
                                color = primaryContainer,
                                radius = 5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(width, height * mappedPoints.last().second)
                            )
                        }
                    }"""

with open("app/src/main/java/com/example/DashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/DashboardScreen.kt", "w") as f:
    f.write(content)
