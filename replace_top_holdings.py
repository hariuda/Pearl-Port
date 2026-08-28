import sys

target = """                Column(modifier = Modifier.padding(vertical = 8.dp)) {
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
                }"""

replacement = """                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    val allHoldings = mutableListOf<HoldingSummary>()
                    
                    // Group positions
                    positions.groupBy { it.symbol }.forEach { (_, lots) ->
                        val value = lots.sumOf { it.currentPrice * it.quantity }
                        val invested = lots.sumOf { it.averagePrice * it.quantity }
                        val ret = if (invested > 0) ((value - invested) / invested) * 100 else 0.0
                        allHoldings.add(HoldingSummary(lots.first().companyName, value, ret))
                    }
                    
                    // Group FDs
                    fds.groupBy { it.bankName }.forEach { (_, lots) ->
                        val value = lots.sumOf { it.currentValue }
                        val invested = lots.sumOf { it.principalAmount }
                        val ret = if (invested > 0) ((value - invested) / invested) * 100 else 0.0
                        allHoldings.add(HoldingSummary(lots.first().bankName, value, ret))
                    }
                    
                    // Group Unit Trusts
                    unitTrusts.groupBy { it.fundName }.forEach { (_, lots) ->
                        val value = lots.sumOf { (if (it.currentNav > 0) it.currentNav else it.averageNav) * it.units }
                        val invested = lots.sumOf { it.averageNav * it.units }
                        val ret = if (invested > 0) ((value - invested) / invested) * 100 else 0.0
                        allHoldings.add(HoldingSummary(lots.first().fundName, value, ret))
                    }
                    
                    // Group Crypto
                    crypto.groupBy { it.symbol }.forEach { (_, lots) ->
                        val value = lots.sumOf { (if (it.currentPrice > 0) it.currentPrice else it.averagePrice) * it.quantity }
                        val invested = lots.sumOf { it.averagePrice * it.quantity }
                        val ret = if (invested > 0) ((value - invested) / invested) * 100 else 0.0
                        allHoldings.add(HoldingSummary(lots.first().symbol, value, ret))
                    }
                    
                    // Group Other
                    otherInvestments.groupBy { it.name }.forEach { (_, lots) ->
                        val value = lots.sumOf { it.value }
                        val invested = lots.sumOf { it.quantity * it.averagePrice }
                        val ret = if (invested > 0) ((value - invested) / invested) * 100 else 0.0
                        allHoldings.add(HoldingSummary(lots.first().name, value, ret))
                    }

                    allHoldings.sortedByDescending { it.value }.take(5).forEach { h ->
                        HoldingRow(
                            name = h.name,
                            value = currencyFormatter.format(h.value),
                            weight = String.format(java.util.Locale.US, "%.1f%%", if (totalAssets > 0) (h.value / totalAssets) * 100 else 0.0),
                            change = String.format(java.util.Locale.US, "%s%.2f%%", if (h.returnPct >= 0) "+" else "", h.returnPct),
                            isProfit = h.returnPct >= 0
                        )
                    }
                }"""

with open("app/src/main/java/com/example/DashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace(target, replacement)

# Add the data class at the top
if "data class HoldingSummary" not in content:
    content = content.replace("@Composable\nfun DashboardScreen", "data class HoldingSummary(val name: String, val value: Double, val returnPct: Double)\n\n@Composable\nfun DashboardScreen")

with open("app/src/main/java/com/example/DashboardScreen.kt", "w") as f:
    f.write(content)
