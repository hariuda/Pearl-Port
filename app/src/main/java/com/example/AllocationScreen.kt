package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GradientOutlinedCard
import com.example.ui.components.SectorPieChart
import com.example.viewmodel.PortfolioViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllocationScreen(viewModel: PortfolioViewModel, onNavigateBack: () -> Unit) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val fds by viewModel.fixedDeposits.collectAsStateWithLifecycle()
    val unitTrusts by viewModel.unitTrusts.collectAsStateWithLifecycle()
    val crypto by viewModel.crypto.collectAsStateWithLifecycle()
    val otherInvestments by viewModel.otherInvestments.collectAsStateWithLifecycle()

    var totalStocksValue = 0.0
    val sectorMap = mutableMapOf<String, Double>()
    
    positions.forEach { p ->
        val currentPrice = if (p.currentPrice > 0) p.currentPrice else p.averagePrice
        val value = currentPrice * p.quantity
        totalStocksValue += value
        sectorMap[p.sector] = (sectorMap[p.sector] ?: 0.0) + value
    }

    val totalFdValue = fds.sumOf { it.currentValue }
    val totalUTValue = unitTrusts.sumOf { it.currentNav * it.units }
    val totalCryptoValue = crypto.sumOf { 
        val currentPrice = if (it.currentPrice > 0) it.currentPrice else it.averagePrice
        currentPrice * it.quantity 
    }
    val totalOtherValue = otherInvestments.sumOf { if (it.quantity > 0) it.quantity * it.currentPrice else it.value }

    val totalAssets = totalStocksValue + totalFdValue + totalUTValue + totalCryptoValue + totalOtherValue

    val assetClassMap = mutableMapOf<String, Double>()
    if (totalStocksValue > 0) assetClassMap["Equities"] = totalStocksValue
    if (totalFdValue > 0) assetClassMap["Fixed Deposits"] = totalFdValue
    if (totalUTValue > 0) assetClassMap["Unit Trusts"] = totalUTValue
    if (totalCryptoValue > 0) assetClassMap["Crypto Currency"] = totalCryptoValue
    if (totalOtherValue > 0) assetClassMap["Gold & Other"] = totalOtherValue

    val chartPaletteName by viewModel.chartColorPalette.collectAsStateWithLifecycle()
    val palette = com.example.ui.theme.ChartColors.getPalette(chartPaletteName)
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Asset Allocation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Portfolio Diversification Breakdown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Donut Hero Section
            if (assetClassMap.isNotEmpty()) {
                item {
                    GradientOutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Allocation Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val colors = assetClassMap.keys.map { key -> palette[key] ?: MaterialTheme.colorScheme.primaryContainer }
                                    SectorPieChart(data = assetClassMap, colors = colors, modifier = Modifier.size(130.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            String.format(Locale.US, "%.1fM", totalAssets / 1_000_000),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    assetClassMap.entries.sortedByDescending { it.value }.take(4).forEach { entry ->
                                        val color = palette[entry.key] ?: MaterialTheme.colorScheme.primaryContainer
                                        val pct = if (totalAssets > 0) (entry.value / totalAssets) * 100 else 0.0
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(entry.key, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(90.dp), maxLines = 1)
                                            Text(String.format(Locale.US, "%.1f%%", pct), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionTitle("Asset Classes", "")
                AllocationSection(data = assetClassMap, total = totalAssets, palette = palette, currencyFormatter = currencyFormatter)
            }

            if (totalStocksValue > 0) {
                item {
                    SectionTitle("Equities by Sector", "")
                    AllocationSection(data = sectorMap, total = totalStocksValue, palette = palette, currencyFormatter = currencyFormatter)
                }
            }

            // Fixed Deposits by institution
            if (fds.isNotEmpty()) {
                val fdMap = mutableMapOf<String, Double>()
                fds.forEach { fdMap[it.bankName] = (fdMap[it.bankName] ?: 0.0) + it.currentValue }
                item {
                    SectionTitle("Fixed Deposits by Institution", "")
                    AllocationSection(data = fdMap, total = totalFdValue, palette = palette, currencyFormatter = currencyFormatter)
                }
            }

            // Unit Trusts by category
            if (unitTrusts.isNotEmpty()) {
                val utMap = mutableMapOf<String, Double>()
                unitTrusts.forEach { 
                    val value = it.currentNav * it.units
                    utMap[it.fundName] = (utMap[it.fundName] ?: 0.0) + value 
                }
                item {
                    SectionTitle("Unit Trusts by Fund", "")
                    AllocationSection(data = utMap, total = totalUTValue, palette = palette, currencyFormatter = currencyFormatter)
                }
            }

            // Crypto by symbol
            if (crypto.isNotEmpty()) {
                val cryptoMap = mutableMapOf<String, Double>()
                crypto.forEach {
                    val currentPrice = if (it.currentPrice > 0) it.currentPrice else it.averagePrice
                    val value = currentPrice * it.quantity
                    cryptoMap[it.symbol] = (cryptoMap[it.symbol] ?: 0.0) + value
                }
                item {
                    SectionTitle("Crypto by Asset", "")
                    AllocationSection(data = cryptoMap, total = totalCryptoValue, palette = palette, currencyFormatter = currencyFormatter)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AllocationSection(
    data: Map<String, Double>,
    total: Double,
    palette: Map<String, Color>,
    currencyFormatter: NumberFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }
) {
    GradientOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val sortedData = data.entries.sortedByDescending { it.value }
            val paletteValues = palette.values.toList()
            val fallbackColor = MaterialTheme.colorScheme.primaryContainer
            
            sortedData.forEachIndexed { index, entry ->
                val percentage = if (total > 0) (entry.value / total) * 100 else 0.0
                val color = palette[entry.key] ?: (if (paletteValues.isNotEmpty()) paletteValues[index % paletteValues.size] else fallbackColor)
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(entry.key, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currencyFormatter.format(entry.value).replace("LKR", "LKR "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", percentage),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (percentage / 100f).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                if (index < sortedData.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

