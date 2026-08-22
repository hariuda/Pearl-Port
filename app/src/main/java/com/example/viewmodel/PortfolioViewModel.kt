package com.example.viewmodel

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Crypto
import com.example.data.DatabaseProvider
import com.example.data.FixedDeposit
import com.example.data.OtherInvestment
import com.example.data.PortfolioRepository
import com.example.data.StockAlert
import com.example.data.StockPosition
import com.example.data.UnitTrust
import com.example.network.NetworkProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PortfolioRepository

    val positions: StateFlow<List<StockPosition>>
    val fixedDeposits: StateFlow<List<FixedDeposit>>
    val unitTrusts: StateFlow<List<UnitTrust>>
    val crypto: StateFlow<List<Crypto>>
    val otherInvestments: StateFlow<List<OtherInvestment>>
    val alerts: StateFlow<List<StockAlert>>
    

    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    private val _chartColorPalette = MutableStateFlow(prefs.getString("chart_palette", "Default") ?: "Default")
    val chartColorPalette: StateFlow<String> = _chartColorPalette

    private val _aspiData = MutableStateFlow<com.example.network.AspiDataResponse?>(null)
    val aspiData: StateFlow<com.example.network.AspiDataResponse?> = _aspiData

    fun setChartColorPalette(paletteName: String) {
        prefs.edit().putString("chart_palette", paletteName).apply()
        _chartColorPalette.value = paletteName
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

        // Seed some initial data if empty
        viewModelScope.launch {
            repository.allPositions.collect { list ->
                if (list.isEmpty()) {
                    repository.insertPosition(StockPosition(symbol = "COMB.N0000", companyName = "Commercial Bank", quantity = 1000, averagePrice = 90.0, sector = "Banks"))
                    repository.insertPosition(StockPosition(symbol = "JKH.N0000", companyName = "John Keells", quantity = 500, averagePrice = 180.0, sector = "Capital Goods"))
                    repository.insertPosition(StockPosition(symbol = "SAMP.N0000", companyName = "Sampath Bank", quantity = 2000, averagePrice = 75.0, sector = "Banks"))
                    repository.insertPosition(StockPosition(symbol = "DIAL.N0000", companyName = "Dialog Axiata", quantity = 10000, averagePrice = 10.5, sector = "Telecommunication"))
                }
            }
        }
        
        viewModelScope.launch {
            repository.allFixedDeposits.collect { list ->
                if (list.isEmpty()) {
                    repository.insertFixedDeposit(FixedDeposit(bankName = "Bank of Ceylon", principalAmount = 1000000.0, interestRate = 8.5, maturityDate = System.currentTimeMillis() + 31536000000L))
                }
            }
        }
        
        viewModelScope.launch {
            repository.allUnitTrusts.collect { list ->
                if (list.isEmpty()) {
                    repository.insertUnitTrust(UnitTrust(fundName = "CAL Money Market Fund", units = 10000.0, averageNav = 15.0, currentNav = 15.5))
                }
            }
        }
        
        viewModelScope.launch {
            repository.allCrypto.collect { list ->
                if (list.isEmpty()) {
                    repository.insertCrypto(Crypto(symbol = "BTC", quantity = 0.5, averagePrice = 60000.0, currentPrice = 62000.0))
                }
            }
        }
        
        viewModelScope.launch {
            repository.allOtherInvestments.collect { list ->
                if (list.isEmpty()) {
                    repository.insertOtherInvestment(OtherInvestment(name = "Gold Sovereigns", type = "Gold", value = 500000.0))
                }
            }
        }

        // Poll for real-time price updates
        viewModelScope.launch {
            while (isActive) {
                repository.fetchLatestPrices()
                delay(30000) // update every 30 seconds
            }
        }
    }

    fun addPosition(position: StockPosition) {
        viewModelScope.launch { repository.insertPosition(position) }
    }

    fun removePosition(id: Int) {
        viewModelScope.launch { repository.deletePosition(id) }
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
        // Return CSV string content
        val currentPositions = positions.value
        val currentFds = fixedDeposits.value
        val currentUTs = unitTrusts.value
        val currentCrypto = crypto.value
        val currentOther = otherInvestments.value
        
        val sb = StringBuilder()
        sb.append("Type,Asset,Quantity/Principal,Average Price/Interest,Current Value\n")
        
        currentPositions.forEach { p ->
            val cp = if (p.currentPrice > 0) p.currentPrice else p.averagePrice
            val totalValue = cp * p.quantity
            sb.append("Stock,${p.symbol},${p.quantity},${p.averagePrice},$totalValue\n")
        }
        
        currentFds.forEach { fd ->
            sb.append("Fixed Deposit,${fd.bankName},${fd.principalAmount},${fd.interestRate}%,${fd.principalAmount}\n")
        }
        
        currentUTs.forEach { ut ->
            val totalValue = ut.currentNav * ut.units
            sb.append("Unit Trust,${ut.fundName},${ut.units},${ut.averageNav},$totalValue\n")
        }
        
        currentCrypto.forEach { c ->
            val totalValue = c.currentPrice * c.quantity
            sb.append("Crypto,${c.symbol},${c.quantity},${c.averagePrice},$totalValue\n")
        }
        
        currentOther.forEach { o ->
            sb.append("Other (${o.type}),${o.name},N/A,N/A,${o.value}\n")
        }
        
        return sb.toString()
    }
}
