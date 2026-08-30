package com.example

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GradientOutlinedCard
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.viewmodel.PortfolioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: PortfolioViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val fds by viewModel.fixedDeposits.collectAsStateWithLifecycle()
    val unitTrusts by viewModel.unitTrusts.collectAsStateWithLifecycle()
    val crypto by viewModel.crypto.collectAsStateWithLifecycle()
    val otherInvestments by viewModel.otherInvestments.collectAsStateWithLifecycle()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "LK")) }

    // Computations
    val totalStockVal = positions.sumOf { (if (it.currentPrice > 0) it.currentPrice else it.averagePrice) * it.quantity }
    val totalStockCost = positions.sumOf { it.averagePrice * it.quantity }
    val stockGain = totalStockVal - totalStockCost

    val totalFdPrincipal = fds.sumOf { it.principalAmount }
    val totalFdVal = fds.sumOf { it.currentValue }
    
    var totalGrossInterest = 0.0
    var totalAitTax = 0.0
    var totalNetInterest = 0.0

    fds.forEach { fd ->
        val accrued = fd.calculateAccruedInterest()
        val gross = if (fd.hasAitDeduction) accrued / 0.90 else accrued
        val tax = if (fd.hasAitDeduction) gross * 0.10 else 0.0
        totalGrossInterest += gross
        totalAitTax += tax
        totalNetInterest += accrued
    }

    val totalUTVal = unitTrusts.sumOf { it.currentNav * it.units }
    val totalUTCost = unitTrusts.sumOf { it.averageNav * it.units }
    val totalCryptoVal = crypto.sumOf { (if (it.currentPrice > 0) it.currentPrice else it.averagePrice) * it.quantity }
    val totalOtherVal = otherInvestments.sumOf { if (it.quantity > 0) it.quantity * it.currentPrice else it.value }

    val totalNetWorth = totalStockVal + totalFdVal + totalUTVal + totalCryptoVal + totalOtherVal
    val totalInvested = totalStockCost + totalFdPrincipal + totalUTCost + crypto.sumOf { it.averagePrice * it.quantity } + otherInvestments.sumOf { if (it.quantity > 0) it.quantity * it.averagePrice else it.value }
    val overallGain = totalNetWorth - totalInvested

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val csvData = viewModel.exportTaxReport()
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(csvData.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Tax & Valuation Statement saved successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Tax & Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Tax statements, interest withholdings & portfolio audits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Metric Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Total Portfolio Value",
                    value = currencyFormatter.format(totalNetWorth).replace("LKR", "LKR "),
                    subtitle = if (overallGain >= 0) "+${currencyFormatter.format(overallGain).replace("LKR", "")} profit" else "${currencyFormatter.format(overallGain).replace("LKR", "")} loss",
                    subtitleColor = if (overallGain >= 0) ProfitGreen else LossRed,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Total AIT Withholding",
                    value = currencyFormatter.format(totalAitTax).replace("LKR", "LKR "),
                    subtitle = "${fds.count { it.hasAitDeduction }} FD(s) with 10% AIT",
                    subtitleColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Outlined.ReceiptLong,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Accrued FD Interest",
                    value = currencyFormatter.format(totalNetInterest).replace("LKR", "LKR "),
                    subtitle = "Gross: ${currencyFormatter.format(totalGrossInterest).replace("LKR", "")}",
                    subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = Icons.Outlined.Savings,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Unrealized Equities P&L",
                    value = currencyFormatter.format(stockGain).replace("LKR", "LKR "),
                    subtitle = if (totalStockCost > 0) String.format(Locale.US, "%.2f%% return", (stockGain / totalStockCost) * 100) else "0.00%",
                    subtitleColor = if (stockGain >= 0) ProfitGreen else LossRed,
                    icon = Icons.Outlined.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // AIT / Withholding Tax Breakdown Card
        item {
            GradientOutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AIT & Withholding Tax Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Advance Income Tax (AIT) / Withholding Tax is deducted at source on eligible fixed deposits at a statutory rate of 10% from monthly interest accruals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    TaxRowItem(
                        label = "Gross Interest Earned",
                        amount = currencyFormatter.format(totalGrossInterest).replace("LKR", "LKR ")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TaxRowItem(
                        label = "AIT Tax Deducted (10%)",
                        amount = "- ${currencyFormatter.format(totalAitTax).replace("LKR", "LKR ")}",
                        valueColor = LossRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TaxRowItem(
                        label = "Net Accrued Interest",
                        amount = currencyFormatter.format(totalNetInterest).replace("LKR", "LKR "),
                        isBold = true,
                        valueColor = ProfitGreen
                    )
                }
            }
        }

        // Export Actions Section
        item {
            GradientOutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Export Documents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate structured audit reports compatible with Excel, Apple Numbers, and tax filing software.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val sdf = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
                            exportLauncher.launch("PearlPort_Tax_Report_${sdf.format(Date())}.csv")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Tax & Valuation Report (CSV)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val summary = """
                                PEARL PORT - PORTFOLIO SUMMARY
                                Net Worth: ${currencyFormatter.format(totalNetWorth)}
                                Total Invested: ${currencyFormatter.format(totalInvested)}
                                Overall Return: ${currencyFormatter.format(overallGain)}
                                Accrued FD Interest: ${currencyFormatter.format(totalNetInterest)}
                                AIT Withholding Tax (10%): ${currencyFormatter.format(totalAitTax)}
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(summary))
                            Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Summary to Clipboard")
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GradientOutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TaxRowItem(
    label: String,
    amount: String,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.bodySmall,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = valueColor
        )
    }
}

