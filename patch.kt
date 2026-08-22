import androidx.compose.material.icons.filled.Edit

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
                    Text("$totalQuantity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                            Text("${lot.quantity} shares", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
