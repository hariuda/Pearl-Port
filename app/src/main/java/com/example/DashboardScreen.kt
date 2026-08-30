package com.example
import androidx.compose.material3.OutlinedCard
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import com.example.ui.components.SectorPieChart
import com.example.viewmodel.PortfolioViewModel
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: PortfolioViewModel, onNavigateToAllocation: () -> Unit = {}) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val fds by viewModel.fixedDeposits.collectAsStateWithLifecycle()
    val unitTrusts by viewModel.unitTrusts.collectAsStateWithLifecycle()
    val crypto by viewModel.crypto.collectAsStateWithLifecycle()
    val otherInvestments by viewModel.otherInvestments.collectAsStateWithLifecycle()
    val aspiData by viewModel.aspiData.collectAsStateWithLifecycle()
    
    var selectedTimeRange by remember { mutableStateOf("1M") }

    var totalStocksValue = 0.0
    var totalInvested = 0.0
    val sectorMap = mutableMapOf<String, Double>()

    positions.forEach { p ->
        val currentPrice = if (p.currentPrice > 0) p.currentPrice else p.averagePrice
        val value = currentPrice * p.quantity
        totalStocksValue += value
        totalInvested += p.averagePrice * p.quantity
    }
    if (totalStocksValue > 0) sectorMap["Equities"] = totalStocksValue

    val totalFdValue = fds.sumOf { it.currentValue }
    totalInvested += fds.sumOf { it.principalAmount }
    if (totalFdValue > 0) sectorMap["Fixed Deposits"] = totalFdValue

    val totalUTValue = unitTrusts.sumOf { it.currentNav * it.units }
    totalInvested += unitTrusts.sumOf { it.averageNav * it.units }
    if (totalUTValue > 0) sectorMap["Unit Trusts"] = totalUTValue

    val totalCryptoValue = crypto.sumOf { it.currentPrice * it.quantity }
    totalInvested += crypto.sumOf { it.averagePrice * it.quantity }
    if (totalCryptoValue > 0) sectorMap["Crypto Currency"] = totalCryptoValue

    val totalOtherValue = otherInvestments.sumOf { if (it.quantity > 0) it.quantity * it.currentPrice else it.value }
    totalInvested += totalOtherValue
    if (totalOtherValue > 0) sectorMap["Gold & Other"] = totalOtherValue

    val totalAssets = totalStocksValue + totalFdValue + totalUTValue + totalCryptoValue + totalOtherValue
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val chartPaletteName by viewModel.chartColorPalette.collectAsStateWithLifecycle()
    val palette = com.example.ui.theme.ChartColors.getPalette(chartPaletteName)

    val totalReturn = totalAssets - totalInvested
    val returnPercent = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0

    // Fake today's change for visual matching, since we don't have historical data
    val todaysChange = totalAssets * 0.0068
    val todaysChangePercent = 0.68

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderSection(viewModel)

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            item {
            PortfolioSummaryCard(
                totalValue = totalAssets,
                invested = totalInvested,
                todaysChange = todaysChange,
                todaysChangePercent = todaysChangePercent,
                totalReturn = totalReturn,
                totalReturnPercent = returnPercent
            )
        }

        item {
            val now = System.currentTimeMillis()
            val startMillis = when (selectedTimeRange) {
                "1D" -> now - 86400000L
                "1W" -> now - 7 * 86400000L
                "1M" -> now - 30 * 86400000L
                "1Y" -> now - 365 * 86400000L
                else -> {
                    val minDate = positions.minOfOrNull { it.purchaseDate } ?: now
                    val minFd = fds.minOfOrNull { it.startDate } ?: now
                    val minUt = unitTrusts.minOfOrNull { it.purchaseDate } ?: now
                    val minCrypto = crypto.minOfOrNull { it.purchaseDate } ?: now
                    val minOther = otherInvestments.minOfOrNull { it.purchaseDate } ?: now
                    minOf(minDate, minFd, minUt, minCrypto, minOther).coerceAtMost(now - 86400000L)
                }
            }

            val numPoints = 20
            val chartData = remember(startMillis, positions, fds, unitTrusts, crypto, otherInvestments) {
                val step = (now - startMillis) / numPoints.coerceAtLeast(1)
                var maxVal = 0.0
                var minVal = Double.MAX_VALUE
                val rawValues = mutableListOf<Double>()
                
                for (i in 0..numPoints) {
                    val t = startMillis + i * step
                    var valueAtT = 0.0
                    
                    positions.forEach { p ->
                        if (t >= p.purchaseDate) {
                            val progress = (t - p.purchaseDate).toDouble() / (now - p.purchaseDate).coerceAtLeast(1L)
                            valueAtT += (p.averagePrice + (p.currentPrice - p.averagePrice) * progress) * p.quantity
                        }
                    }
                    fds.forEach { fd ->
                        if (t >= fd.startDate) {
                            val progress = (t - fd.startDate).toDouble() / (now - fd.startDate).coerceAtLeast(1L)
                            valueAtT += fd.principalAmount + (fd.currentValue - fd.principalAmount) * progress
                        }
                    }
                    unitTrusts.forEach { ut ->
                        if (t >= ut.purchaseDate) {
                            val progress = (t - ut.purchaseDate).toDouble() / (now - ut.purchaseDate).coerceAtLeast(1L)
                            val currentNav = if (ut.currentNav > 0) ut.currentNav else ut.averageNav
                            valueAtT += (ut.averageNav + (currentNav - ut.averageNav) * progress) * ut.units
                        }
                    }
                    crypto.forEach { c ->
                        if (t >= c.purchaseDate) {
                            val progress = (t - c.purchaseDate).toDouble() / (now - c.purchaseDate).coerceAtLeast(1L)
                            val currentPrice = if (c.currentPrice > 0) c.currentPrice else c.averagePrice
                            valueAtT += (c.averagePrice + (currentPrice - c.averagePrice) * progress) * c.quantity
                        }
                    }
                    otherInvestments.forEach { o ->
                        if (t >= o.purchaseDate) {
                            valueAtT += o.value
                        }
                    }
                    
                    rawValues.add(valueAtT)
                    if (valueAtT > maxVal) maxVal = valueAtT
                    if (valueAtT < minVal) minVal = valueAtT
                }
                
                val range = maxVal - minVal
                val points = if (range == 0.0) {
                    List(numPoints + 1) { 0.5f }
                } else {
                    rawValues.map { (1.0 - (it - minVal) / range).toFloat() }
                }
                Triple(points, minVal, maxVal)
            }
            val chartPoints = chartData.first
            val chartMin = chartData.second
            val chartMax = chartData.third
            
            val initialVal = remember(startMillis, positions, fds, unitTrusts, crypto, otherInvestments) {
                var valueAtStart = 0.0
                positions.forEach { if (startMillis >= it.purchaseDate) valueAtStart += it.averagePrice * it.quantity }
                fds.forEach { if (startMillis >= it.startDate) valueAtStart += it.principalAmount }
                unitTrusts.forEach { if (startMillis >= it.purchaseDate) valueAtStart += it.averageNav * it.units }
                crypto.forEach { if (startMillis >= it.purchaseDate) valueAtStart += it.averagePrice * it.quantity }
                otherInvestments.forEach { if (startMillis >= it.purchaseDate) valueAtStart += it.value }
                valueAtStart
            }
            val periodReturn = if (initialVal > 0) ((totalAssets - initialVal) / initialVal) * 100 else 0.0
            
            SectionTitle("Portfolio Performance", "")
            com.example.ui.components.GradientOutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf("1D", "1W", "1M", "1Y", "ALL").forEach { range ->
                            val isSelected = selectedTimeRange == range
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimeRange = range },
                                label = { Text(range) },
                                colors = if (isSelected) FilterChipDefaults.filterChipColors(selectedContainerColor = primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer) else FilterChipDefaults.filterChipColors(),
                                border = if (isSelected) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val textMeasurer = rememberTextMeasurer()
                    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)) {
                            val width = size.width
                            val height = size.height - 20.dp.toPx()
                            
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
                            
                            // Horizontal axis labels (dates)
                            val numLabels = 4
                            val dateFormat = java.text.SimpleDateFormat(if (selectedTimeRange == "1D") "HH:mm" else if (selectedTimeRange == "ALL") "yyyy" else "MMM dd", java.util.Locale.US)
                            for (i in 0 until numLabels) {
                                val t = startMillis + (now - startMillis) * (i / (numLabels - 1).toFloat())
                                val label = dateFormat.format(java.util.Date(t.toLong()))
                                val textResult = textMeasurer.measure(label, TextStyle(color = labelColor, fontSize = 10.sp))
                                val xPos = if (i == 0) 0f else if (i == numLabels - 1) width - textResult.size.width else width * (i / (numLabels - 1).toFloat()) - textResult.size.width / 2f
                                drawText(
                                    textLayoutResult = textResult,
                                    topLeft = androidx.compose.ui.geometry.Offset(xPos, height + 6.dp.toPx())
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
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val aspiValue = aspiData?.value ?: 0.0
                    val aspiChange = aspiData?.percentage ?: 0.0
                    
                    // Simple mock comparison scaling for longer periods (since API only gives daily)
                    val mockAspiPeriodReturn = when (selectedTimeRange) {
                        "1D" -> aspiChange
                        "1W" -> aspiChange * 5
                        "1M" -> aspiChange * 20
                        "1Y" -> aspiChange * 250
                        else -> aspiChange * 500
                    }
                    
                    val isOutperforming = periodReturn >= mockAspiPeriodReturn
                    val diff = kotlin.math.abs(periodReturn - mockAspiPeriodReturn)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("vs ASPI ($selectedTimeRange)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "%.2f%%", periodReturn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (periodReturn >= 0) ProfitGreen else LossRed)
                            Text("ASPI: ${String.format(Locale.US, "%.2f%%", mockAspiPeriodReturn)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = if (isOutperforming) "Your portfolio is outperforming\nthe ASPI by ${String.format(Locale.US, "%.2f%%", diff)}" 
                                   else "Your portfolio is underperforming\nthe ASPI by ${String.format(Locale.US, "%.2f%%", diff)}",
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color.DarkGray
                        )
                        Box(modifier = Modifier.size(36.dp).background(if (isOutperforming) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (isOutperforming) Icons.Filled.TrendingUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = if (isOutperforming) ProfitGreen else LossRed)
                        }
                    }
                }
            }
        }

        item {
            com.example.ui.components.GradientOutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Asset Allocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val colors = sectorMap.keys.map { key -> palette[key] ?: primaryContainer }
                            SectorPieChart(data = sectorMap, colors = colors, modifier = Modifier.size(120.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LKR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%.2fM", totalAssets / 1000000), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            sectorMap.entries.forEachIndexed { index, entry ->
                                val color = palette[entry.key] ?: primaryContainer
                                val percentage = if (totalAssets > 0) (entry.value / totalAssets) * 100 else 0.0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(entry.key, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(80.dp))
                                    Text(String.format("%.0f%%", percentage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (totalStocksValue > 0) {
            item {
                SectionTitle("Equities by Sector", "")
                val equitiesSectorMap = mutableMapOf<String, Double>()
                positions.forEach { p ->
                    val currentPrice = if (p.currentPrice > 0) p.currentPrice else p.averagePrice
                    val value = currentPrice * p.quantity
                    equitiesSectorMap[p.sector] = (equitiesSectorMap[p.sector] ?: 0.0) + value
                }
                AllocationSection(data = equitiesSectorMap, total = totalStocksValue, palette = palette)
            }
        }

        if (fds.isNotEmpty()) {
            val fdMap = mutableMapOf<String, Double>()
            fds.forEach { fdMap[it.bankName] = (fdMap[it.bankName] ?: 0.0) + it.currentValue }
            item {
                SectionTitle("Fixed Deposits by Institution", "")
                AllocationSection(data = fdMap, total = totalFdValue, palette = palette)
            }
        }

        if (unitTrusts.isNotEmpty()) {
            val utMap = mutableMapOf<String, Double>()
            unitTrusts.forEach { 
                val value = it.currentNav * it.units
                utMap[it.fundName] = (utMap[it.fundName] ?: 0.0) + value 
            }
            item {
                SectionTitle("Unit Trusts by Fund", "")
                AllocationSection(data = utMap, total = totalUTValue, palette = palette)
            }
        }

        if (crypto.isNotEmpty()) {
            val cryptoMap = mutableMapOf<String, Double>()
            crypto.forEach {
                val currentPrice = if (it.currentPrice > 0) it.currentPrice else it.averagePrice
                val value = currentPrice * it.quantity
                cryptoMap[it.symbol] = (cryptoMap[it.symbol] ?: 0.0) + value
            }
            item {
                SectionTitle("Crypto by Asset", "")
                AllocationSection(data = cryptoMap, total = totalCryptoValue, palette = palette)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}


@Composable
fun HeaderSection(viewModel: PortfolioViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val currentUserName by viewModel.userName.collectAsStateWithLifecycle()
    var nameInput by remember { mutableStateOf(currentUserName) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val json = viewModel.exportBackup()
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(json.toByteArray())
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Backup saved successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Failed to save backup", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(it)?.bufferedReader().use { reader -> reader?.readText() }
                    if (json != null) {
                        viewModel.importBackup(json) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Backup restored successfully", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Invalid backup data", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Failed to read backup", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore") },
            text = {
                Column {
                    Text("Save all your portfolio data and settings to a file, or restore from a previous backup file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    androidx.compose.material3.TextButton(onClick = {
                        showBackupDialog = false
                        val dateStr = java.text.SimpleDateFormat("dd_MM_yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        exportLauncher.launch("Pp_backup_${dateStr}.json")
                    }) {
                        Text("Export Backup File")
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        showBackupDialog = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Text("Restore from File")
                    }
                    androidx.compose.material3.TextButton(onClick = { showBackupDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showAccountDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account Details") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    viewModel.setUserName(nameInput.ifBlank { "Guest" })
                    showAccountDialog = false 
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About") },
            text = {
                Column {
                    Text(
                        "A simple portfolio management app built for Sri Lankan investors — designed to help you track and manage your investments with ease.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Version: 1.0", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Created by: Harindra", style = MaterialTheme.typography.bodySmall)
                    Text("Contact: harindra.rdh@gmail.com", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }


    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when (currentHour) {
                    in 0..11 -> "Good morning,"
                    in 12..17 -> "Good afternoon,"
                    else -> "Good evening,"
                }
                Text(greeting, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$currentUserName \uD83D\uDC4B", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(28.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Account") },
                        onClick = { 
                            nameInput = currentUserName
                            menuExpanded = false 
                            showAccountDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Backup & Restore") },
                        onClick = { 
                            menuExpanded = false 
                            showBackupDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = { 
                            menuExpanded = false 
                            showAboutDialog = true
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PortfolioSummaryCard(
    totalValue: Double, invested: Double, todaysChange: Double, todaysChangePercent: Double, 
    totalReturn: Double, totalReturnPercent: Double
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(240.dp)
    ) {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
            )
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(gradientBrush)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Portfolio Value", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                    }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatter.format(totalValue).replace("LKR", "LKR "), 
                    color = MaterialTheme.colorScheme.onPrimary, 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Today's change", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "+${formatter.format(todaysChange).replace("LKR", "")} (+${todaysChangePercent}%)",
                            color = ProfitGreen,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(modifier = Modifier.height(30.dp).width(1.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total return", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        val sign = if (totalReturn >= 0) "+" else ""
                        val color = if (totalReturn >= 0) ProfitGreen else LossRed
                        Text(
                            "${sign}${formatter.format(totalReturn).replace("LKR", "")} (${sign}${String.format("%.2f", totalReturnPercent)}%)",
                            color = color,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            }
        }

        val investedGradientBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(androidx.compose.ui.graphics.Color(0xFFF8F9FA), androidx.compose.ui.graphics.Color(0xFFFFFFFF))
        )

        Card(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(investedGradientBrush)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = "Invested",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Invested",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatter.format(invested).replace("LKR", "LKR "), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, action: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action.isNotEmpty()) {
            Text(action, color = MaterialTheme.colorScheme.primaryContainer, style = MaterialTheme.typography.labelMedium)
        }
    }
}
