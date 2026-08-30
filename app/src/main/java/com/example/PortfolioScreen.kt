package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.components.SwipeToEditDeleteContainer
import com.example.ui.components.GradientOutlinedCard
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.viewmodel.PortfolioViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class PortfolioSortOrder(val displayName: String) {
    VALUE_DESC("Highest Value"),
    VALUE_ASC("Lowest Value"),
    NAME_ASC("Name (A-Z)"),
    GAIN_DESC("Highest Return")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Any?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(PortfolioSortOrder.VALUE_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val tabs = listOf("Equities", "Fixed Deposits", "Unit Trusts", "Crypto Currency", "Gold & Other")
    val chartPaletteName by viewModel.chartColorPalette.collectAsStateWithLifecycle()
    val palette = com.example.ui.theme.ChartColors.getPalette(chartPaletteName)

    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val fds by viewModel.fixedDeposits.collectAsStateWithLifecycle()
    val unitTrusts by viewModel.unitTrusts.collectAsStateWithLifecycle()
    val crypto by viewModel.crypto.collectAsStateWithLifecycle()
    val otherInvestments by viewModel.otherInvestments.collectAsStateWithLifecycle()

    val tabCounts = listOf(
        positions.groupBy { it.symbol }.size,
        fds.size,
        unitTrusts.size,
        crypto.size,
        otherInvestments.size
    )

    val (currentInvested, currentValue) = remember(selectedTabIndex, positions, fds, unitTrusts, crypto, otherInvestments) {
        when (selectedTabIndex) {
            0 -> {
                val inv = positions.sumOf { it.averagePrice * it.quantity }
                val cur = positions.sumOf { (if (it.currentPrice > 0) it.currentPrice else it.averagePrice) * it.quantity }
                Pair(inv, cur)
            }
            1 -> {
                val inv = fds.sumOf { it.principalAmount }
                val cur = fds.sumOf { it.currentValue }
                Pair(inv, cur)
            }
            2 -> {
                val inv = unitTrusts.sumOf { it.averageNav * it.units }
                val cur = unitTrusts.sumOf { it.currentNav * it.units }
                Pair(inv, cur)
            }
            3 -> {
                val inv = crypto.sumOf { it.averagePrice * it.quantity }
                val cur = crypto.sumOf { it.currentPrice * it.quantity }
                Pair(inv, cur)
            }
            4 -> {
                val inv = otherInvestments.sumOf { if (it.quantity > 0) it.quantity * it.averagePrice else it.value }
                val cur = otherInvestments.sumOf { if (it.quantity > 0) it.quantity * it.currentPrice else it.value }
                Pair(inv, cur)
            }
            else -> Pair(0.0, 0.0)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add Investment") },
                text = { Text("Add Asset", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Portfolio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Asset Holdings & Performance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isSearchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Filled.Clear else Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            PortfolioSortOrder.values().forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = order.displayName,
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (Animated)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, ticker, or bank...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Sleek Scrollable Category Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val tabColor = palette[title] ?: MaterialTheme.colorScheme.primary
                    val count = tabCounts.getOrElse(index) { 0 }

                    val icon = when (title) {
                        "Equities" -> Icons.Filled.Domain
                        "Fixed Deposits" -> Icons.Filled.AccountBalance
                        "Unit Trusts" -> Icons.Filled.PieChart
                        "Crypto Currency" -> Icons.Filled.CurrencyBitcoin
                        "Gold & Other" -> Icons.Filled.Toll
                        else -> Icons.Filled.Info
                    }

                    Surface(
                        onClick = { selectedTabIndex = index },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) tabColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) tabColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) tabColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) tabColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) tabColor else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val currentTitle = tabs[selectedTabIndex]
            val tabColor = palette[currentTitle] ?: MaterialTheme.colorScheme.primary
            val currentCount = tabCounts.getOrElse(selectedTabIndex) { 0 }

            AssetClassSummaryHeader(
                categoryName = currentTitle,
                holdingCount = currentCount,
                tabColor = tabColor,
                invested = currentInvested,
                currentValue = currentValue
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                val onEdit = { item: Any -> editingItem = item }

                when (selectedTabIndex) {
                    0 -> EquitiesTab(viewModel, tabColor, searchQuery, sortOrder, onEdit)
                    1 -> FDsTab(viewModel, tabColor, searchQuery, sortOrder, onEdit)
                    2 -> UnitTrustsTab(viewModel, tabColor, searchQuery, sortOrder, onEdit)
                    3 -> CryptoTab(viewModel, tabColor, searchQuery, sortOrder, onEdit)
                    4 -> OtherTab(viewModel, tabColor, searchQuery, sortOrder, onEdit)
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
fun EquitiesTab(
    viewModel: PortfolioViewModel,
    tabColor: Color,
    searchQuery: String,
    sortOrder: PortfolioSortOrder,
    onEdit: (Any) -> Unit
) {
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    
    val filteredPositions = remember(positions, searchQuery, sortOrder) {
        val grouped = positions.groupBy { it.symbol }
        val filtered = if (searchQuery.isBlank()) {
            grouped.toList()
        } else {
            grouped.filter { (symbol, lots) ->
                val name = lots.firstOrNull()?.companyName ?: ""
                val sector = lots.firstOrNull()?.sector ?: ""
                symbol.contains(searchQuery, ignoreCase = true) ||
                        name.contains(searchQuery, ignoreCase = true) ||
                        sector.contains(searchQuery, ignoreCase = true)
            }.toList()
        }

        when (sortOrder) {
            PortfolioSortOrder.VALUE_DESC -> filtered.sortedByDescending { (_, lots) ->
                val qty = lots.sumOf { it.quantity }
                val price = if (lots.first().currentPrice > 0) lots.first().currentPrice else lots.first().averagePrice
                price * qty
            }
            PortfolioSortOrder.VALUE_ASC -> filtered.sortedBy { (_, lots) ->
                val qty = lots.sumOf { it.quantity }
                val price = if (lots.first().currentPrice > 0) lots.first().currentPrice else lots.first().averagePrice
                price * qty
            }
            PortfolioSortOrder.NAME_ASC -> filtered.sortedBy { (_, lots) -> lots.firstOrNull()?.companyName?.lowercase() ?: "" }
            PortfolioSortOrder.GAIN_DESC -> filtered.sortedByDescending { (_, lots) ->
                val totalCost = lots.sumOf { it.averagePrice * it.quantity }
                val totalVal = lots.sumOf { (if (it.currentPrice > 0) it.currentPrice else it.averagePrice) * it.quantity }
                if (totalCost > 0) (totalVal - totalCost) / totalCost else 0.0
            }
        }
    }

    if (positions.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Equities Yet",
            message = "Tap 'Add Asset' to log your first stock holding.",
            icon = Icons.Filled.Domain
        )
        return
    }

    if (filteredPositions.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Matching Stocks",
            message = "Try searching with a different ticker or company name.",
            icon = Icons.Filled.Search
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredPositions, key = { it.first }) { (symbol, lots) ->
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
    tabColor: Color,
    onEdit: (StockPosition) -> Unit,
    onDelete: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var lotToDelete by remember { mutableStateOf<Int?>(null) }
    
    if (lotToDelete != null) {
        AlertDialog(
            onDismissRequest = { lotToDelete = null },
            title = { Text("Delete Purchase Entry", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this purchase lot from your holdings?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        lotToDelete?.let { onDelete(it) }
                        lotToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { lotToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    val companyName = lots.first().companyName
    val sector = lots.first().sector
    val totalQuantity = lots.sumOf { it.quantity }
    val totalInvested = lots.sumOf { it.averagePrice * it.quantity }
    val avgPurchasePrice = if (totalQuantity > 0) totalInvested / totalQuantity else 0.0
    
    val currentPrice = if (lots.first().currentPrice > 0) lots.first().currentPrice else lots.first().averagePrice
    val totalCurrentValue = currentPrice * totalQuantity
    
    val isProfit = totalCurrentValue >= totalInvested
    val difference = totalCurrentValue - totalInvested
    val differencePercent = if (totalInvested > 0) (difference / totalInvested) * 100 else 0.0
    
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    GradientOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Ticker badge + Company & Sector | Current Value + P&L badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Ticker avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(tabColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol.take(3).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tabColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = companyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (sector.isNotBlank() && sector != "General") {
                                Text(
                                    text = " • $sector",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormatter.format(totalCurrentValue).replace("LKR", "LKR "),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    // Profit/loss pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isProfit) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isProfit) ProfitGreen else LossRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${if (difference > 0) "+" else ""}${String.format(Locale.US, "%.2f", differencePercent)}%",
                                color = if (isProfit) ProfitGreen else LossRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Shares", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        com.example.ui.components.FormatUtils.numberFormatter.format(totalQuantity),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg Buy Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        currencyFormatter.format(avgPurchasePrice).replace("LKR", "LKR "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        currencyFormatter.format(currentPrice).replace("LKR", "LKR "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isProfit) ProfitGreen else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Expand/Collapse Indicator row
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide Lots (${lots.size})" else "View Lots (${lots.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expanded Transaction Lots
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Purchase Lots",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                lots.sortedByDescending { it.purchaseDate }.forEachIndexed { idx, lot ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                val dateStr = if (lot.purchaseDate > 0) dateFormat.format(Date(lot.purchaseDate)) else "Initial Seed"
                                Text(dateStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${com.example.ui.components.FormatUtils.numberFormatter.format(lot.quantity)} shares @ ${currencyFormatter.format(lot.averagePrice).replace("LKR", "LKR ")}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onEdit(lot) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { lotToDelete = lot.id },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = LossRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FDsTab(
    viewModel: PortfolioViewModel,
    tabColor: Color,
    searchQuery: String,
    sortOrder: PortfolioSortOrder,
    onEdit: (Any) -> Unit
) {
    val fds by viewModel.fixedDeposits.collectAsStateWithLifecycle()
    
    val filteredFds = remember(fds, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            fds
        } else {
            fds.filter { it.bankName.contains(searchQuery, ignoreCase = true) }
        }

        when (sortOrder) {
            PortfolioSortOrder.VALUE_DESC -> filtered.sortedByDescending { it.currentValue }
            PortfolioSortOrder.VALUE_ASC -> filtered.sortedBy { it.currentValue }
            PortfolioSortOrder.NAME_ASC -> filtered.sortedBy { it.bankName.lowercase() }
            PortfolioSortOrder.GAIN_DESC -> filtered.sortedByDescending { it.interestRate }
        }
    }

    if (fds.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Fixed Deposits",
            message = "Tap 'Add Asset' to track high-yield deposits and maturity dates.",
            icon = Icons.Filled.AccountBalance
        )
        return
    }

    if (filteredFds.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Matching Fixed Deposits",
            message = "Try searching with a different bank or institution name.",
            icon = Icons.Filled.Search
        )
        return
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredFds, key = { it.id }) { fd ->
            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeFixedDeposit(fd.id) },
                onEdit = { onEdit(fd) }
            ) {
                GradientOutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tabColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AccountBalance,
                                        contentDescription = null,
                                        tint = tabColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = fd.bankName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (fd.isMonthlyInterest) "Monthly Payout" else "At Maturity",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (fd.hasAitDeduction) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = "AIT 10%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "${fd.interestRate}% p.a.",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    currencyFormatter.format(fd.principalAmount).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Current Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    currencyFormatter.format(fd.currentValue).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tabColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Maturity Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                val daysLeft = TimeUnit.MILLISECONDS.toDays(fd.maturityDate - System.currentTimeMillis()).coerceAtLeast(0)
                                Text(
                                    dateFormatter.format(Date(fd.maturityDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (daysLeft <= 30) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitTrustsTab(
    viewModel: PortfolioViewModel,
    tabColor: Color,
    searchQuery: String,
    sortOrder: PortfolioSortOrder,
    onEdit: (Any) -> Unit
) {
    val uts by viewModel.unitTrusts.collectAsStateWithLifecycle()
    
    val filteredUts = remember(uts, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            uts
        } else {
            uts.filter { it.fundName.contains(searchQuery, ignoreCase = true) }
        }

        when (sortOrder) {
            PortfolioSortOrder.VALUE_DESC -> filtered.sortedByDescending { it.currentNav * it.units }
            PortfolioSortOrder.VALUE_ASC -> filtered.sortedBy { it.currentNav * it.units }
            PortfolioSortOrder.NAME_ASC -> filtered.sortedBy { it.fundName.lowercase() }
            PortfolioSortOrder.GAIN_DESC -> filtered.sortedByDescending {
                val cost = it.averageNav * it.units
                val cur = it.currentNav * it.units
                if (cost > 0) (cur - cost) / cost else 0.0
            }
        }
    }

    if (uts.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Unit Trusts",
            message = "Tap 'Add Asset' to track mutual funds and unit trust portfolios.",
            icon = Icons.Filled.PieChart
        )
        return
    }

    if (filteredUts.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Matching Funds",
            message = "Try searching with a different fund name.",
            icon = Icons.Filled.Search
        )
        return
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredUts, key = { it.id }) { ut ->
            val totalCost = ut.averageNav * ut.units
            val totalValue = ut.currentNav * ut.units
            val diff = totalValue - totalCost
            val isProfit = diff >= 0
            val diffPercent = if (totalCost > 0) (diff / totalCost) * 100 else 0.0

            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeUnitTrust(ut.id) },
                onEdit = { onEdit(ut) }
            ) {
                GradientOutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tabColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PieChart,
                                        contentDescription = null,
                                        tint = tabColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = ut.fundName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Mutual Fund / Unit Trust",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencyFormatter.format(totalValue).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isProfit) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isProfit) ProfitGreen else LossRed,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${if (diff > 0) "+" else ""}${String.format(Locale.US, "%.2f", diffPercent)}%",
                                            color = if (isProfit) ProfitGreen else LossRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Units Held", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    com.example.ui.components.FormatUtils.numberFormatter.format(ut.units),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avg NAV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    currencyFormatter.format(ut.averageNav).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current NAV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    currencyFormatter.format(ut.currentNav).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isProfit) ProfitGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CryptoTab(
    viewModel: PortfolioViewModel,
    tabColor: Color,
    searchQuery: String,
    sortOrder: PortfolioSortOrder,
    onEdit: (Any) -> Unit
) {
    val crypto by viewModel.crypto.collectAsStateWithLifecycle()
    
    val filteredCrypto = remember(crypto, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            crypto
        } else {
            crypto.filter { it.symbol.contains(searchQuery, ignoreCase = true) }
        }

        when (sortOrder) {
            PortfolioSortOrder.VALUE_DESC -> filtered.sortedByDescending { it.currentPrice * it.quantity }
            PortfolioSortOrder.VALUE_ASC -> filtered.sortedBy { it.currentPrice * it.quantity }
            PortfolioSortOrder.NAME_ASC -> filtered.sortedBy { it.symbol.lowercase() }
            PortfolioSortOrder.GAIN_DESC -> filtered.sortedByDescending {
                val cost = it.averagePrice * it.quantity
                val cur = it.currentPrice * it.quantity
                if (cost > 0) (cur - cost) / cost else 0.0
            }
        }
    }

    if (crypto.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Crypto Assets",
            message = "Tap 'Add Asset' to track Bitcoin, Ethereum, and other digital currencies.",
            icon = Icons.Filled.CurrencyBitcoin
        )
        return
    }

    if (filteredCrypto.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Matching Crypto",
            message = "Try searching with a different token symbol.",
            icon = Icons.Filled.Search
        )
        return
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredCrypto, key = { it.id }) { c ->
            val totalCost = c.averagePrice * c.quantity
            val totalValue = c.currentPrice * c.quantity
            val diff = totalValue - totalCost
            val isProfit = diff >= 0
            val diffPercent = if (totalCost > 0) (diff / totalCost) * 100 else 0.0

            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeCrypto(c.id) },
                onEdit = { onEdit(c) }
            ) {
                GradientOutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tabColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CurrencyBitcoin,
                                        contentDescription = null,
                                        tint = tabColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = c.symbol.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Crypto Asset",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencyFormatter.format(totalValue).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isProfit) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isProfit) ProfitGreen else LossRed,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${if (diff > 0) "+" else ""}${String.format(Locale.US, "%.2f", diffPercent)}%",
                                            color = if (isProfit) ProfitGreen else LossRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Holdings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${com.example.ui.components.FormatUtils.numberFormatter.format(c.quantity)} ${c.symbol.uppercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avg Buy Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    currencyFormatter.format(c.averagePrice).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Market Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    currencyFormatter.format(c.currentPrice).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isProfit) ProfitGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OtherTab(
    viewModel: PortfolioViewModel,
    tabColor: Color,
    searchQuery: String,
    sortOrder: PortfolioSortOrder,
    onEdit: (Any) -> Unit
) {
    val other by viewModel.otherInvestments.collectAsStateWithLifecycle()
    
    val filteredOther = remember(other, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) {
            other
        } else {
            other.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.symbol.contains(searchQuery, ignoreCase = true) ||
                        it.type.contains(searchQuery, ignoreCase = true)
            }
        }

        when (sortOrder) {
            PortfolioSortOrder.VALUE_DESC -> filtered.sortedByDescending { if (it.quantity > 0) it.quantity * it.currentPrice else it.value }
            PortfolioSortOrder.VALUE_ASC -> filtered.sortedBy { if (it.quantity > 0) it.quantity * it.currentPrice else it.value }
            PortfolioSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            PortfolioSortOrder.GAIN_DESC -> filtered.sortedByDescending {
                val cost = if (it.quantity > 0) it.quantity * it.averagePrice else it.value
                val cur = if (it.quantity > 0) it.quantity * it.currentPrice else it.value
                if (cost > 0) (cur - cost) / cost else 0.0
            }
        }
    }

    if (other.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Other Assets",
            message = "Tap 'Add Asset' to track physical gold, precious metals, real estate, and private investments.",
            icon = Icons.Filled.Toll
        )
        return
    }

    if (filteredOther.isEmpty()) {
        com.example.ui.components.EmptyPortfolioState(
            title = "No Matching Assets",
            message = "Try searching with a different asset name or type.",
            icon = Icons.Filled.Search
        )
        return
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(filteredOther, key = { it.id }) { o ->
            val hasQuantity = o.quantity > 0
            val totalCost = if (hasQuantity) o.averagePrice * o.quantity else o.value
            val totalValue = if (hasQuantity) o.currentPrice * o.quantity else o.value
            val diff = totalValue - totalCost
            val isProfit = diff >= 0
            val diffPercent = if (totalCost > 0) (diff / totalCost) * 100 else 0.0

            SwipeToEditDeleteContainer(
                onDelete = { viewModel.removeOtherInvestment(o.id) },
                onEdit = { onEdit(o) }
            ) {
                GradientOutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(tabColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Toll,
                                        contentDescription = null,
                                        tint = tabColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = o.name + if (o.symbol.isNotEmpty()) " (${o.symbol})" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = o.type.ifBlank { "Alternative Asset" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currencyFormatter.format(totalValue).replace("LKR", "LKR "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (hasQuantity) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isProfit) ProfitGreen.copy(alpha = 0.12f) else LossRed.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = if (isProfit) ProfitGreen else LossRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${if (diff > 0) "+" else ""}${String.format(Locale.US, "%.2f", diffPercent)}%",
                                                color = if (isProfit) ProfitGreen else LossRed,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (hasQuantity) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val isGold = o.name.contains("Gold", ignoreCase = true) || o.type.contains("Gold", ignoreCase = true)
                                val qtyStr = com.example.ui.components.FormatUtils.numberFormatter.format(o.quantity)
                                val qtyLabel = if (isGold) "$qtyStr g" else qtyStr

                                Column {
                                    Text("Holdings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        qtyLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Avg Buy Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        currencyFormatter.format(o.averagePrice).replace("LKR", "LKR "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Market Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        currencyFormatter.format(o.currentPrice).replace("LKR", "LKR "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isProfit) ProfitGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssetClassSummaryHeader(
    categoryName: String,
    holdingCount: Int,
    tabColor: Color,
    invested: Double,
    currentValue: Double
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }
    val totalGain = currentValue - invested
    val gainPercent = if (invested > 0) (totalGain / invested) * 100 else 0.0
    val isProfit = totalGain >= 0

    val cardBgColor = Color(0xFFD3D3D3)
    val textPrimaryColor = Color(0xFF1F2937)
    val textSecondaryColor = Color(0xFF4B5563)
    val dividerColor = Color(0xFFB5B5B5)
    val profitColor = Color(0xFF15803D)
    val lossColor = Color(0xFFDC2626)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
            contentColor = textPrimaryColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category info header tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(tabColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textSecondaryColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "$holdingCount ${if (holdingCount == 1) "Holding" else "Holdings"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Metrics: Invested & Current Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Invested Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Invested",
                            modifier = Modifier.size(15.dp),
                            tint = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Invested",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = textSecondaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormatter.format(invested).replace("LKR", "LKR "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Vertical Separator
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(dividerColor)
                )

                // Current Value Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = "Current Value",
                            modifier = Modifier.size(15.dp),
                            tint = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Current Value",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = textSecondaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormatter.format(currentValue).replace("LKR", "LKR "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (invested > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Return",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = textSecondaryColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isProfit) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isProfit) profitColor else lossColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val sign = if (totalGain > 0) "+" else ""
                        Text(
                            text = "$sign${currencyFormatter.format(totalGain).replace("LKR", "LKR ")} (${String.format(Locale.US, "%.2f", gainPercent)}%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) profitColor else lossColor
                        )
                    }
                }
            }
        }
    }
}



