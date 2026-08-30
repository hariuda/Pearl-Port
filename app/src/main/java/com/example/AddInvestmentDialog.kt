package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Crypto
import com.example.data.FixedDeposit
import com.example.data.OtherInvestment
import com.example.data.StockPosition
import com.example.data.UnitTrust
import com.example.viewmodel.PortfolioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddInvestmentDialog(
    tabIndex: Int,
    viewModel: PortfolioViewModel,
    itemToEdit: Any? = null,
    onDismiss: () -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var typeSector by remember { mutableStateOf("") }
    var dateStr by remember { 
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var periodStr by remember { mutableStateOf("12") }
    var isMonthlyInterest by remember { mutableStateOf(false) }
    var hasAitDeduction by remember { mutableStateOf(false) }

    LaunchedEffect(itemToEdit) {
        if (itemToEdit != null) {
            when (itemToEdit) {
                is StockPosition -> {
                    symbol = itemToEdit.symbol
                    name = itemToEdit.companyName
                    quantity = itemToEdit.quantity.toString()
                    price = itemToEdit.averagePrice.toString()
                    typeSector = itemToEdit.sector
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateStr = format.format(Date(itemToEdit.purchaseDate))
                }
                is FixedDeposit -> {
                    name = itemToEdit.bankName
                    quantity = itemToEdit.principalAmount.toString()
                    price = itemToEdit.interestRate.toString()
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateStr = format.format(Date(itemToEdit.startDate))
                    periodStr = itemToEdit.periodMonths.toString()
                    isMonthlyInterest = itemToEdit.isMonthlyInterest
                    hasAitDeduction = itemToEdit.hasAitDeduction
                }
                is UnitTrust -> {
                    name = itemToEdit.fundName
                    quantity = itemToEdit.units.toString()
                    price = itemToEdit.averageNav.toString()
                }
                is Crypto -> {
                    symbol = itemToEdit.symbol
                    quantity = itemToEdit.quantity.toString()
                    price = itemToEdit.averagePrice.toString()
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateStr = format.format(Date(itemToEdit.purchaseDate))
                }
                is OtherInvestment -> {
                    name = itemToEdit.name
                    symbol = itemToEdit.symbol
                    quantity = itemToEdit.quantity.toString()
                    typeSector = itemToEdit.type
                    price = if (itemToEdit.quantity > 0) itemToEdit.averagePrice.toString() else itemToEdit.value.toString()
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateStr = format.format(Date(itemToEdit.purchaseDate))
                }
            }
        }
    }

    LaunchedEffect(symbol) {
        if (tabIndex == 0 && symbol.length >= 4 && itemToEdit == null) {
            kotlinx.coroutines.delay(500) // Debounce before fetching
            // Try to extract the base symbol (e.g. COMB from COMB.N0000)
            val baseSymbol = symbol.split(".")[0].uppercase()
            val profile = viewModel.fetchCompanyProfile(baseSymbol)
            if (profile != null) {
                if (!profile.name.isNullOrEmpty()) name = profile.name
                if (!profile.sector.isNullOrEmpty()) typeSector = profile.sector
            }
        }
    }

    val actionText = if (itemToEdit != null) "Edit" else "Add"
    val title = when (tabIndex) {
        0 -> "$actionText Equity"
        1 -> "$actionText Fixed Deposit"
        2 -> "$actionText Unit Trust"
        3 -> "$actionText Crypto"
        else -> "$actionText Other Investment"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (tabIndex) {
                    0 -> {
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = { symbol = it.uppercase() },
                            label = { Text("Symbol (e.g. COMB.N0000)") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = price, onValueChange = { price = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Average Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Purchase Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = typeSector, onValueChange = { typeSector = it }, label = { Text("Sector") }, modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Principal Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = price, onValueChange = { price = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Interest Rate (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Start Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = periodStr, onValueChange = { periodStr = it.replace(Regex("[^0-9]"), "") }, label = { Text("Time Period (Months)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Monthly Interest?")
                            Switch(
                                checked = isMonthlyInterest,
                                onCheckedChange = { isMonthlyInterest = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AIT Deduction", style = MaterialTheme.typography.bodyMedium)
                                Text("Deduct withholding tax 10%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = hasAitDeduction,
                                onCheckedChange = { hasAitDeduction = it }
                            )
                        }
                    }
                    2 -> {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Fund Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Units") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = price, onValueChange = { price = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Average NAV") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    }
                    3 -> {
                        OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol (e.g. BTC)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = price, onValueChange = { price = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Rate (LKR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Purchase Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                    }
                    4 -> {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Gold)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol (e.g. PAXG) for tracking") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = typeSector, onValueChange = { typeSector = it }, label = { Text("Type") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = price, onValueChange = { price = it.replace(",", "") }, visualTransformation = com.example.ui.components.NumberCommaVisualTransformation(), label = { Text("Rate / Value (LKR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Purchase Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val editId = when (itemToEdit) {
                            is StockPosition -> itemToEdit.id
                            is FixedDeposit -> itemToEdit.id
                            is UnitTrust -> itemToEdit.id
                            is Crypto -> itemToEdit.id
                            is OtherInvestment -> itemToEdit.id
                            else -> 0
                        }

                        when (tabIndex) {
                            0 -> {
                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = format.parse(dateStr)?.time ?: System.currentTimeMillis()
                                viewModel.addPosition(StockPosition(id = editId, symbol = symbol.trim().uppercase(), companyName = name.trim(), quantity = quantity.toIntOrNull() ?: 0, averagePrice = price.toDoubleOrNull() ?: 0.0, sector = typeSector, purchaseDate = date))
                            }
                            1 -> {
                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val startDate = format.parse(dateStr)?.time ?: System.currentTimeMillis()
                                val periodMonths = periodStr.toIntOrNull() ?: 12
                                val maturityCalendar = java.util.Calendar.getInstance().apply {
                                    timeInMillis = startDate
                                    add(java.util.Calendar.MONTH, periodMonths)
                                }
                                val maturityDate = maturityCalendar.timeInMillis
                                viewModel.addFixedDeposit(
                                    FixedDeposit(
                                        id = editId,
                                        bankName = name,
                                        principalAmount = quantity.toDoubleOrNull() ?: 0.0,
                                        interestRate = price.toDoubleOrNull() ?: 0.0,
                                        maturityDate = maturityDate,
                                        isMonthlyInterest = isMonthlyInterest,
                                        startDate = startDate,
                                        periodMonths = periodMonths,
                                        hasAitDeduction = hasAitDeduction
                                    )
                                )
                            }
                            2 -> {
                                viewModel.addUnitTrust(UnitTrust(id = editId, fundName = name, units = quantity.toDoubleOrNull() ?: 0.0, averageNav = price.toDoubleOrNull() ?: 0.0, currentNav = (itemToEdit as? UnitTrust)?.currentNav ?: (price.toDoubleOrNull() ?: 0.0)))
                            }
                            3 -> {
                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = format.parse(dateStr)?.time ?: System.currentTimeMillis()
                                viewModel.addCrypto(Crypto(id = editId, symbol = symbol, quantity = quantity.toDoubleOrNull() ?: 0.0, averagePrice = price.toDoubleOrNull() ?: 0.0, currentPrice = (itemToEdit as? Crypto)?.currentPrice ?: (price.toDoubleOrNull() ?: 0.0), purchaseDate = date))
                            }
                            4 -> {
                                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = format.parse(dateStr)?.time ?: System.currentTimeMillis()
                                val parsedQty = quantity.toDoubleOrNull() ?: 0.0
                                val parsedPrice = price.toDoubleOrNull() ?: 0.0
                                val storedValue = if (parsedQty == 0.0) parsedPrice else parsedQty * parsedPrice
                                viewModel.addOtherInvestment(OtherInvestment(id = editId, name = name, symbol = symbol, type = typeSector, quantity = parsedQty, averagePrice = parsedPrice, currentPrice = (itemToEdit as? OtherInvestment)?.currentPrice ?: parsedPrice, value = storedValue, purchaseDate = date))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
