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
import kotlinx.coroutines.Dispatchers
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

        // Removed auto-seeding of mock data when empty so EmptyPortfolioState can be shown

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
