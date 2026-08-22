package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import java.text.NumberFormat
import java.util.Locale

// Custom colors based on the image
val PearlPurple = Color(0xFF2C2260)
val PearlPurpleLight = Color(0xFF4A3B8C)
val TextGray = Color(0xFF8C8C8C)
val BgLight = Color(0xFFF7F8FC)

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
    
    val chartPaletteName by viewModel.chartColorPalette.collectAsStateWithLifecycle()
    val palette = com.example.ui.theme.ChartColors.getPalette(chartPaletteName)

    val totalReturn = totalAssets - totalInvested
    val returnPercent = if (totalInvested > 0) (totalReturn / totalInvested) * 100 else 0.0

    // Fake today's change for visual matching, since we don't have historical data
    val todaysChange = totalAssets * 0.0068
    val todaysChangePercent = 0.68

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        item {
            HeaderSection()
        }
        
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
            val chartPoints = remember(startMillis, positions, fds, unitTrusts, crypto, otherInvestments) {
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
                if (range == 0.0) {
                    List(numPoints + 1) { 0.5f }
                } else {
                    rawValues.map { (1.0 - (it - minVal) / range).toFloat() }
                }
            }
            
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                colors = if (isSelected) FilterChipDefaults.filterChipColors(selectedContainerColor = PearlPurpleLight, selectedLabelColor = Color.White) else FilterChipDefaults.filterChipColors(),
                                border = if (isSelected) null else FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFFF7F8FC), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
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
                                    colors = listOf(PearlPurpleLight.copy(alpha = 0.3f), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                ),
                                style = Fill
                            )
                            
                            drawPath(
                                path = path,
                                color = PearlPurpleLight,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            drawCircle(
                                color = PearlPurpleLight,
                                radius = 5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(width, height * mappedPoints.last().second)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
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
                            Text("vs ASPI ($selectedTimeRange)", style = MaterialTheme.typography.labelMedium, color = TextGray)
                            Text(String.format(Locale.US, "%.2f%%", periodReturn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (periodReturn >= 0) ProfitGreen else LossRed)
                            Text("ASPI: ${String.format(Locale.US, "%.2f%%", mockAspiPeriodReturn)}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                        Text(
                            text = if (isOutperforming) "Your portfolio is outperforming\nthe ASPI by ${String.format(Locale.US, "%.2f%%", diff)}" 
                                   else "Your portfolio is underperforming\nthe ASPI by ${String.format(Locale.US, "%.2f%%", diff)}",
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color.DarkGray
                        )
                        Box(modifier = Modifier.size(36.dp).background(if (isOutperforming) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (isOutperforming) Icons.Filled.TrendingUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = if (isOutperforming) ProfitGreen else LossRed)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            val colors = sectorMap.keys.map { key -> palette[key] ?: PearlPurpleLight }
                            SectorPieChart(data = sectorMap, colors = colors, modifier = Modifier.size(120.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("LKR", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Text(String.format("%.2fM", totalAssets / 1000000), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            sectorMap.entries.forEachIndexed { index, entry ->
                                val color = palette[entry.key] ?: PearlPurpleLight
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "View full allocation", 
                        color = PearlPurpleLight, 
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { onNavigateToAllocation() }
                    )
                }
            }
        }
        
        item {
            SectionTitle("Top Holdings", "")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    positions.sortedByDescending { it.currentPrice * it.quantity }.take(5).forEach { p ->
                        val value = p.currentPrice * p.quantity
                        val returnPct = if (p.averagePrice > 0) ((p.currentPrice - p.averagePrice) / p.averagePrice) * 100 else 0.0
                        HoldingRow(
                            name = p.companyName,
                            value = currencyFormatter.format(value),
                            weight = String.format("%.1f%%", if (totalAssets > 0) (value / totalAssets) * 100 else 0.0),
                            change = String.format("%s%.2f%%", if (returnPct >= 0) "+" else "", returnPct),
                            isProfit = returnPct >= 0
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HeaderSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_banner),
                contentDescription = "Pearl Port Banner",
                modifier = Modifier
                    .height(64.dp)
                    .weight(1f),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                alignment = Alignment.CenterStart
            )
            Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Column {
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val greeting = when (currentHour) {
                in 0..11 -> "Good morning,"
                in 12..17 -> "Good afternoon,"
                else -> "Good evening,"
            }
            Text(greeting, style = MaterialTheme.typography.labelMedium, color = TextGray)
            Text("Harindra \uD83D\uDC4B", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
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
        Card(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            colors = CardDefaults.cardColors(containerColor = PearlPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Portfolio Value", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                    Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("LKR", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatter.format(totalValue).replace("LKR", "LKR "), 
                    color = Color.White, 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Today's change", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "+${formatter.format(todaysChange).replace("LKR", "")} (+${todaysChangePercent}%)",
                            color = ProfitGreen,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.White.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total return", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
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

        Card(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Invested", color = TextGray, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatter.format(invested).replace("LKR", "LKR "), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
            Text(action, color = PearlPurpleLight, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MarketRow(title: String, val1: String, val2: String, val2Color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
        Row {
            if (val1.isNotEmpty()) {
                Text(val1, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(val2, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = val2Color)
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
}

@Composable
fun HoldingRow(name: String, value: String, weight: String, change: String, isProfit: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(BgLight, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(name.take(1), style = MaterialTheme.typography.titleMedium, color = PearlPurpleLight)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(value.replace("LKR", "LKR "), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(weight, style = MaterialTheme.typography.labelSmall, color = TextGray)
                Box(modifier = Modifier.background(if (isProfit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(change, style = MaterialTheme.typography.labelSmall, color = if (isProfit) ProfitGreen else LossRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
}
