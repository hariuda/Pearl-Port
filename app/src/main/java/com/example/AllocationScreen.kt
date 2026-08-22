package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PortfolioViewModel

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
    if (totalCryptoValue > 0) assetClassMap["Crypto"] = totalCryptoValue
    if (totalOtherValue > 0) assetClassMap["Other"] = totalOtherValue

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asset Allocation", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                .background(Color(0xFFF7F8FC))
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle("Asset Classes", "")
                AllocationSection(data = assetClassMap, total = totalAssets)
            }

            if (totalStocksValue > 0) {
                item {
                    SectionTitle("Equities by Sector", "")
                    AllocationSection(data = sectorMap, total = totalStocksValue)
                }
            }

            // Fixed Deposits by institution
            if (fds.isNotEmpty()) {
                val fdMap = mutableMapOf<String, Double>()
                fds.forEach { fdMap[it.bankName] = (fdMap[it.bankName] ?: 0.0) + it.currentValue }
                item {
                    SectionTitle("Fixed Deposits by Institution", "")
                    AllocationSection(data = fdMap, total = totalFdValue)
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
                    AllocationSection(data = utMap, total = totalUTValue)
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
                    AllocationSection(data = cryptoMap, total = totalCryptoValue)
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AllocationSection(data: Map<String, Double>, total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val sortedData = data.entries.sortedByDescending { it.value }
            val paletteValues = com.example.ui.theme.ChartColors.getPalette("Default").values.toList()
            val fallbackColor = Color(0xFF4A3B8C)
            
            sortedData.forEachIndexed { index, entry ->
                val percentage = if (total > 0) (entry.value / total) * 100 else 0.0
                val color = if (paletteValues.isNotEmpty()) paletteValues[index % paletteValues.size] else fallbackColor
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(entry.key, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(String.format("%.1f%%", percentage), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                
                if (index < sortedData.size - 1) {
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}
