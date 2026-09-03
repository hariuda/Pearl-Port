package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Crypto
import com.example.data.DatabaseProvider
import com.example.data.FixedDeposit
import com.example.data.OtherInvestment
import com.example.data.PortfolioRepository
import com.example.data.StockAlert
import com.example.data.StockPosition
import com.example.data.TradeRecord
import com.example.data.UnitTrust
import com.example.network.NetworkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PortfolioRepository

    val positions: StateFlow<List<StockPosition>>
    val fixedDeposits: StateFlow<List<FixedDeposit>>
    val unitTrusts: StateFlow<List<UnitTrust>>
    val crypto: StateFlow<List<Crypto>>
    val otherInvestments: StateFlow<List<OtherInvestment>>
    val alerts: StateFlow<List<StockAlert>>
    val tradeRecords: StateFlow<List<TradeRecord>>
    
    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights: StateFlow<String?> = _aiInsights

    private val _isFetchingInsights = MutableStateFlow(false)
    val isFetchingInsights: StateFlow<Boolean> = _isFetchingInsights
    

    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    private val _chartColorPalette = MutableStateFlow(prefs.getString("chart_palette", "Default") ?: "Default")
    val chartColorPalette: StateFlow<String> = _chartColorPalette


    private val _userName = MutableStateFlow(prefs.getString("user_name", "Guest") ?: "Guest")
    val userName: StateFlow<String> = _userName

    private val _aspiData = MutableStateFlow<com.example.network.AspiDataResponse?>(null)
    val aspiData: StateFlow<com.example.network.AspiDataResponse?> = _aspiData

    fun setChartColorPalette(paletteName: String) {
        prefs.edit().putString("chart_palette", paletteName).apply()
        _chartColorPalette.value = paletteName
    }


    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _userName.value = name
    }

    fun exportBackup(): String {
        val backup = com.example.data.BackupData(
            positions = positions.value,
            fixedDeposits = fixedDeposits.value,
            unitTrusts = unitTrusts.value,
            crypto = crypto.value,
            otherInvestments = otherInvestments.value,
            userName = userName.value,
            chartColorPalette = chartColorPalette.value
        )
        return com.google.gson.Gson().toJson(backup)
    }

    fun importBackup(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backup = com.google.gson.Gson().fromJson(json, com.example.data.BackupData::class.java)
                if (backup != null) {
                    repository.replaceWithBackup(backup)
                    backup.userName?.let { setUserName(it) }
                    backup.chartColorPalette?.let { setChartColorPalette(it) }
                    kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    init {
        val database = DatabaseProvider.getDatabase(application)
        repository = PortfolioRepository(database.portfolioDao())

        viewModelScope.launch {
            try {
                _aspiData.value = NetworkProvider.cseApi.getAspiData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                repository.fetchLatestPrices()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        positions = repository.allPositions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        fixedDeposits = repository.allFixedDeposits.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        unitTrusts = repository.allUnitTrusts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        crypto = repository.allCrypto.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        otherInvestments = repository.allOtherInvestments.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        alerts = repository.allAlerts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        tradeRecords = repository.allTradeRecords.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Removed auto-seeding of mock data when empty so EmptyPortfolioState can be shown

        // Poll for real-time price updates
        viewModelScope.launch {
            while (isActive) {
                repository.fetchLatestPrices()
                delay(30000) // update every 30 seconds
            }
        }
    }

    fun refreshPrices(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.fetchLatestPrices()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }

    fun generateAIInsights() {
        if (_isFetchingInsights.value) return
        _isFetchingInsights.value = true
        
        viewModelScope.launch {
            try {
                val dataStr = exportDetailedTaxReport()
                val insights = com.example.network.GeminiService.generatePortfolioInsights(dataStr)
                _aiInsights.value = insights
            } catch (e: Exception) {
                _aiInsights.value = "Failed to load insights: ${e.message}"
            } finally {
                _isFetchingInsights.value = false
            }
        }
    }

    fun clearAIInsights() {
        _aiInsights.value = null
    }

    fun addPosition(position: StockPosition) {
        viewModelScope.launch { repository.insertPosition(position) }
    }

    fun removePosition(id: Int) {
        viewModelScope.launch { repository.deletePosition(id) }
    }

    fun sellPosition(position: StockPosition, sellPrice: Double, sellQuantity: Int) {
        viewModelScope.launch {
            val tradeRecord = TradeRecord(
                symbol = position.symbol,
                companyName = position.companyName,
                quantity = sellQuantity,
                buyPrice = position.averagePrice,
                sellPrice = sellPrice
            )
            repository.insertTradeRecord(tradeRecord)

            if (sellQuantity >= position.quantity) {
                repository.deletePosition(position.id)
            } else {
                repository.updatePosition(position.copy(quantity = position.quantity - sellQuantity))
            }
        }
    }

    fun logDividend(position: StockPosition, amount: Double) {
        viewModelScope.launch {
            repository.updatePosition(position.copy(totalDividends = position.totalDividends + amount))
        }
    }

    suspend fun fetchCompanyProfile(symbol: String): com.example.network.CompanySummaryInfo? {
        return repository.fetchCompanyProfile(symbol)
    }

    fun addFixedDeposit(fd: FixedDeposit) {
        viewModelScope.launch { repository.insertFixedDeposit(fd) }
    }

    fun removeFixedDeposit(id: Int) {
        viewModelScope.launch { repository.deleteFixedDeposit(id) }
    }

    fun addUnitTrust(ut: UnitTrust) {
        viewModelScope.launch { repository.insertUnitTrust(ut) }
    }

    fun removeUnitTrust(id: Int) {
        viewModelScope.launch { repository.deleteUnitTrust(id) }
    }

    fun addCrypto(crypto: Crypto) {
        viewModelScope.launch { repository.insertCrypto(crypto) }
    }

    fun removeCrypto(id: Int) {
        viewModelScope.launch { repository.deleteCrypto(id) }
    }

    fun addOtherInvestment(other: OtherInvestment) {
        viewModelScope.launch { repository.insertOtherInvestment(other) }
    }

    fun removeOtherInvestment(id: Int) {
        viewModelScope.launch { repository.deleteOtherInvestment(id) }
    }

    fun addAlert(alert: StockAlert) {
        viewModelScope.launch { repository.insertAlert(alert) }
    }

    fun removeAlert(id: Int) {
        viewModelScope.launch { repository.deleteAlert(id) }
    }

    fun toggleAlert(alert: StockAlert) {
        viewModelScope.launch { repository.updateAlert(alert.copy(isActive = !alert.isActive)) }
    }
    
    fun exportTaxReport(): String {
        return exportDetailedTaxReport()
    }

    fun exportDetailedTaxReport(): String {
        val currentPositions = positions.value
        val currentFds = fixedDeposits.value
        val currentUTs = unitTrusts.value
        val currentCrypto = crypto.value
        val currentOther = otherInvestments.value
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        val sb = StringBuilder()
        sb.append("=================================================================================\n")
        sb.append("PEARL PORT - PORTFOLIO TAX & VALUATION STATEMENT\n")
        sb.append("Generated On: ${sdf.format(Date())}\n")
        sb.append("=================================================================================\n\n")

        sb.append("--- EQUITIES & STOCKS ---\n")
        sb.append("Symbol,Company,Quantity,Avg Buy Price (LKR),Total Cost (LKR),Current Price (LKR),Current Value (LKR),Unrealized P&L (LKR),Gain %,Dividends (LKR)\n")
        var totalStockCost = 0.0
        var totalStockVal = 0.0
        var totalDividends = 0.0
        currentPositions.forEach { p ->
            val cp = if (p.currentPrice > 0) p.currentPrice else p.averagePrice
            val cost = p.averagePrice * p.quantity
            val value = cp * p.quantity
            val gain = value - cost
            val gainPct = if (cost > 0) (gain / cost) * 100 else 0.0
            totalStockCost += cost
            totalStockVal += value
            totalDividends += p.totalDividends
            sb.append("\"${p.symbol}\",\"${p.companyName}\",${p.quantity},${String.format(Locale.US, "%.2f", p.averagePrice)},${String.format(Locale.US, "%.2f", cost)},${String.format(Locale.US, "%.2f", cp)},${String.format(Locale.US, "%.2f", value)},${String.format(Locale.US, "%.2f", gain)},${String.format(Locale.US, "%.2f", gainPct)}%,${String.format(Locale.US, "%.2f", p.totalDividends)}\n")
        }
        sb.append("Subtotal Stocks Cost: LKR ${String.format(Locale.US, "%.2f", totalStockCost)}, Subtotal Stocks Value: LKR ${String.format(Locale.US, "%.2f", totalStockVal)}, Total Dividends: LKR ${String.format(Locale.US, "%.2f", totalDividends)}\n\n")

        sb.append("--- FIXED DEPOSITS & AIT WITHHOLDING TAX ---\n")
        sb.append("Bank/Institution,Principal (LKR),Interest Rate %,Payout Type,AIT 10% Deducted,Gross Interest (LKR),Tax Deducted (LKR),Net Accrued Interest (LKR),Total Value (LKR)\n")
        var totalFdPrincipal = 0.0
        var totalGrossInterest = 0.0
        var totalTaxDeducted = 0.0
        var totalFdVal = 0.0
        currentFds.forEach { fd ->
            val accrued = fd.calculateAccruedInterest()
            val gross = if (fd.hasAitDeduction) accrued / 0.90 else accrued
            val tax = if (fd.hasAitDeduction) gross * 0.10 else 0.0
            totalFdPrincipal += fd.principalAmount
            totalGrossInterest += gross
            totalTaxDeducted += tax
            totalFdVal += fd.currentValue
            val payoutType = if (fd.isMonthlyInterest) "Monthly" else "At Maturity"
            val aitStatus = if (fd.hasAitDeduction) "YES (10%)" else "NO"
            sb.append("\"${fd.bankName}\",${String.format(Locale.US, "%.2f", fd.principalAmount)},${String.format(Locale.US, "%.2f", fd.interestRate)}%,\"$payoutType\",$aitStatus,${String.format(Locale.US, "%.2f", gross)},${String.format(Locale.US, "%.2f", tax)},${String.format(Locale.US, "%.2f", accrued)},${String.format(Locale.US, "%.2f", fd.currentValue)}\n")
        }
        sb.append("Subtotal FD Principal: LKR ${String.format(Locale.US, "%.2f", totalFdPrincipal)}, Total Tax Withheld: LKR ${String.format(Locale.US, "%.2f", totalTaxDeducted)}, Total FD Value: LKR ${String.format(Locale.US, "%.2f", totalFdVal)}\n\n")

        sb.append("--- UNIT TRUSTS ---\n")
        sb.append("Fund Name,Units,Avg NAV (LKR),Current NAV (LKR),Total Cost (LKR),Current Value (LKR),Unrealized Gain (LKR)\n")
        currentUTs.forEach { ut ->
            val cost = ut.averageNav * ut.units
            val value = ut.currentNav * ut.units
            sb.append("\"${ut.fundName}\",${ut.units},${String.format(Locale.US, "%.2f", ut.averageNav)},${String.format(Locale.US, "%.2f", ut.currentNav)},${String.format(Locale.US, "%.2f", cost)},${String.format(Locale.US, "%.2f", value)},${String.format(Locale.US, "%.2f", value - cost)}\n")
        }
        sb.append("\n")

        sb.append("--- CRYPTOCURRENCY & OTHER ASSETS ---\n")
        sb.append("Asset Type,Name/Symbol,Quantity,Avg Price / Value (LKR),Current Value (LKR)\n")
        currentCrypto.forEach { c ->
            val cp = if (c.currentPrice > 0) c.currentPrice else c.averagePrice
            sb.append("Crypto,\"${c.symbol}\",${c.quantity},${String.format(Locale.US, "%.2f", c.averagePrice)},${String.format(Locale.US, "%.2f", cp * c.quantity)}\n")
        }
        currentOther.forEach { o ->
            val valStr = if (o.quantity > 0) String.format(Locale.US, "%.2f", o.quantity * o.currentPrice) else String.format(Locale.US, "%.2f", o.value)
            sb.append("\"Other (${o.type})\",\"${o.name}\",${o.quantity},${String.format(Locale.US, "%.2f", o.averagePrice)},$valStr\n")
        }
        sb.append("\n")

        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val trades = tradeRecords.value
        val tradesThisYear = trades.filter {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = it.tradeDate
            cal.get(java.util.Calendar.YEAR) == currentYear
        }

        sb.append("--- TRADE HISTORY & REALIZED CAPITAL GAINS (FY $currentYear) ---\n")
        sb.append("Date,Symbol,Company,Quantity,Avg Buy Price (LKR),Sell Price (LKR),Realized P&L (LKR)\n")
        var totalRealizedGains = 0.0
        tradesThisYear.forEach { t ->
            val gain = (t.sellPrice - t.buyPrice) * t.quantity
            totalRealizedGains += gain
            val tradeDateStr = sdf.format(Date(t.tradeDate))
            sb.append("$tradeDateStr,\"${t.symbol}\",\"${t.companyName}\",${t.quantity},${String.format(Locale.US, "%.2f", t.buyPrice)},${String.format(Locale.US, "%.2f", t.sellPrice)},${String.format(Locale.US, "%.2f", gain)}\n")
        }
        sb.append("Total Realized Capital Gains (FY $currentYear): LKR ${String.format(Locale.US, "%.2f", totalRealizedGains)}\n")

        return sb.toString()
    }
}
