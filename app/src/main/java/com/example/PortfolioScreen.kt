package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.SwipeToEditDeleteContainer
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.viewmodel.PortfolioViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Any?>(null) }
    val tabs = listOf("Equities", "Fixed Deposits", "Unit Trusts", "Crypto Currency", "Gold & Other")
    val chartPaletteName by viewModel.chartColorPalette.collectAsStateWithLifecycle()
    val palette = com.example.ui.theme.ChartColors.getPalette(chartPaletteName)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    val tabColor = palette[title] ?: MaterialTheme.colorScheme.primary
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                text = title,
                                color = if (selectedTabIndex == index) tabColor else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                val currentTitle = tabs[selectedTabIndex]
                val tabColor = palette[currentTitle] ?: MaterialTheme.colorScheme.primary
                
                val onEdit = { item: Any -> editingItem = item }

                when (selectedTabIndex) {
                    0 -> EquitiesTab(viewModel, tabColor, onEdit)
                    1 -> FDsTab(viewModel, tabColor, onEdit)
                    2 -> UnitTrustsTab(viewModel, tabColor, onEdit)
                    3 -> CryptoTab(viewModel, tabColor, onEdit)
                    4 -> OtherTab(viewModel, tabColor, onEdit)
                }
            }
        }
        
        if (showAddDialog) {
            AddInvestmentDialog(
                tabIndex = selectedTabIndex,
                viewModel = viewModel,
                onDismiss = { showAddDialog = false }
            )
        }

        if (editingItem != null) {
            val tabIdx = when (editingItem) {
                is StockPosition -> 0
                is FixedDeposit -> 1
                is UnitTrust -> 2
                is Crypto -> 3
                is OtherInvestment -> 4
                else -> 0
            }
            AddInvestmentDialog(
                tabIndex = tabIdx,
                viewModel = viewModel,
                itemToEdit = editingItem,
                onDismiss = { editingItem = null }
            )
        }
    }
}

@Composable
fun EquitiesTab(viewModel: PortfolioViewModel, tabColor: androidx.compose.ui.graphics.Color, onEdit: (Any) -> Unit) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val groupedPositions = remember(positions) { positions.groupBy { it.symbol } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(groupedPositions.entries.toList(), key = { it.key }) { (symbol, lots) ->
            SwipeToEditDeleteContainer(
                onDelete = { lots.forEach { viewModel.removePosition(it.id) } },
                onEdit = { onEdit(lots.first()) }
            ) {
                GroupedPortfolioCard(
                    symbol = symbol,
                    lots = lots,
                    tabColor = tabColor,
                    onEdit = onEdit,
                    onDelete = { viewModel.removePosition(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedPortfolioCard(
    symbol: String,
    lots: List<StockPosition>,
    tabColor: androidx.compose.ui.graphics.Color,
    onEdit: (StockPosition) -> Unit,
    onDelete: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    val companyName = lots.first().companyName
    val totalQuantity = lots.sumOf { it.quantity }
    val totalInvested = lots.sumOf { it.averagePrice * it.quantity }
    val avgPurchasePrice = if (totalQuantity > 0) totalInvested / totalQuantity else 0.0
    
    val currentPrice = if (lots.first().currentPrice > 0) lots.first().currentPrice else lots.first().averagePrice
    val totalCurrentValue = currentPrice * totalQuantity
    
    val isProfit = totalCurrentValue >= totalInvested
    val difference = totalCurrentValue - totalInvested
    val differencePercent = if (totalInvested > 0) (difference / totalInvested) * 100 else 0.0
    
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = symbol,
                        color = tabColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = companyName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(totalCurrentValue),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isProfit) ProfitGreen else LossRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (difference > 0) "+" else ""}${currencyFormatter.format(difference)} (${String.format("%.2f", differencePercent)}%)",
                            color = if (isProfit) ProfitGreen else LossRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Shares", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${com.example.ui.components.FormatUtils.numberFormatter.format(totalQuantity)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg Buy Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currencyFormatter.format(avgPurchasePrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currencyFormatter.format(currentPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Purchase History",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                lots.sortedByDescending { it.purchaseDate }.forEach { lot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val dateStr = if (lot.purchaseDate > 0) dateFormat.format(Date(lot.purchaseDate)) else "Initial Seed"
                            Text(dateStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("${com.example.ui.components.FormatUtils.numberFormatter.format(lot.quantity)} shares", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(currencyFormatter.format(lot.averagePrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { onEdit(lot) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDelete(lot.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun FDsTab(viewModel: PortfolioViewModel, tabColor: androidx.compose.ui.graphics.Color, onEdit: (Any) -> Unit) {
    val fds by viewModel.fixedDeposits.collectAsStateWithLifecycle()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(fds, key = { it.id }) { fd ->
            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeFixedDeposit(fd.id) },
                onEdit = { onEdit(fd) }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = fd.bankName,
                                    color = tabColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (fd.isMonthlyInterest) "Interest paid Monthly" else "Interest paid at Maturity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${fd.interestRate}% p.a.",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Principal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormatter.format(fd.principalAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormatter.format(fd.currentValue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Maturity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormatter.format(Date(fd.maturityDate)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun UnitTrustsTab(viewModel: PortfolioViewModel, tabColor: androidx.compose.ui.graphics.Color, onEdit: (Any) -> Unit) {
    val uts by viewModel.unitTrusts.collectAsStateWithLifecycle()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uts, key = { it.id }) { ut ->
            val totalValue = ut.currentNav * ut.units
            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeUnitTrust(ut.id) },
                onEdit = { onEdit(ut) }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ut.fundName, color = tabColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFormatter.format(totalValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("${com.example.ui.components.FormatUtils.numberFormatter.format(ut.units)} units @ ${currencyFormatter.format(ut.averageNav)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoTab(viewModel: PortfolioViewModel, tabColor: androidx.compose.ui.graphics.Color, onEdit: (Any) -> Unit) {
    val crypto by viewModel.crypto.collectAsStateWithLifecycle()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK")) // LKR for crypto

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(crypto, key = { it.id }) { c ->
            val isProfit = c.currentPrice >= c.averagePrice
            val totalValue = c.currentPrice * c.quantity
            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeCrypto(c.id) },
                onEdit = { onEdit(c) }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(c.symbol, color = tabColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFormatter.format(totalValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${com.example.ui.components.FormatUtils.numberFormatter.format(c.quantity)} @ ${currencyFormatter.format(c.averagePrice)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = currencyFormatter.format(c.currentPrice),
                                color = if (isProfit) ProfitGreen else LossRed,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Purchased: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(c.purchaseDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OtherTab(viewModel: PortfolioViewModel, tabColor: androidx.compose.ui.graphics.Color, onEdit: (Any) -> Unit) {
    val other by viewModel.otherInvestments.collectAsStateWithLifecycle()
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(other, key = { it.id }) { o ->
            val hasQuantity = o.quantity > 0
            val isProfit = if (hasQuantity) o.currentPrice >= o.averagePrice else true
            val totalValue = if (hasQuantity) o.currentPrice * o.quantity else o.value

            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeOtherInvestment(o.id) },
                onEdit = { onEdit(o) }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(o.name + if (o.symbol.isNotEmpty()) " (${o.symbol})" else "", color = tabColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(o.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currencyFormatter.format(totalValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        if (hasQuantity) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${com.example.ui.components.FormatUtils.numberFormatter.format(o.quantity)} @ ${currencyFormatter.format(o.averagePrice)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = currencyFormatter.format(o.currentPrice),
                                    color = if (isProfit) ProfitGreen else LossRed,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Purchased: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(o.purchaseDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
