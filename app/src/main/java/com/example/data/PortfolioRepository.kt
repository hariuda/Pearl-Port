package com.example.data

import com.example.network.NetworkProvider
import kotlinx.coroutines.flow.Flow

class PortfolioRepository(private val portfolioDao: PortfolioDao) {

    val allPositions: Flow<List<StockPosition>> = portfolioDao.getAllPositions()
    val allFixedDeposits: Flow<List<FixedDeposit>> = portfolioDao.getAllFixedDeposits()
    val allAlerts: Flow<List<StockAlert>> = portfolioDao.getAllAlerts()
    val allUnitTrusts: Flow<List<UnitTrust>> = portfolioDao.getAllUnitTrusts()
    val allCrypto: Flow<List<Crypto>> = portfolioDao.getAllCrypto()
    val allOtherInvestments: Flow<List<OtherInvestment>> = portfolioDao.getAllOtherInvestments()

    suspend fun insertPosition(position: StockPosition) = portfolioDao.insertPosition(position)
    suspend fun updatePosition(position: StockPosition) = portfolioDao.updatePosition(position)
    suspend fun deletePosition(id: Int) = portfolioDao.deletePositionById(id)

    suspend fun insertFixedDeposit(fd: FixedDeposit) = portfolioDao.insertFixedDeposit(fd)
    suspend fun deleteFixedDeposit(id: Int) = portfolioDao.deleteFixedDepositById(id)

    suspend fun insertAlert(alert: StockAlert) = portfolioDao.insertAlert(alert)
    suspend fun updateAlert(alert: StockAlert) = portfolioDao.updateAlert(alert)
    suspend fun deleteAlert(id: Int) = portfolioDao.deleteAlertById(id)

    suspend fun insertUnitTrust(ut: UnitTrust) = portfolioDao.insertUnitTrust(ut)
    suspend fun deleteUnitTrust(id: Int) = portfolioDao.deleteUnitTrustById(id)

    suspend fun insertCrypto(crypto: Crypto) = portfolioDao.insertCrypto(crypto)
    suspend fun deleteCrypto(id: Int) = portfolioDao.deleteCryptoById(id)

    suspend fun insertOtherInvestment(other: OtherInvestment) = portfolioDao.insertOtherInvestment(other)
    suspend fun deleteOtherInvestment(id: Int) = portfolioDao.deleteOtherInvestmentById(id)
    
    suspend fun fetchCompanyProfile(symbol: String): com.example.network.CompanySummaryInfo? {
        return try {
            val response = NetworkProvider.cseApi.getCompanyProfile(symbol)
            response.reqComSumInfo?.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchLatestPrices() {
        try {
            val response = NetworkProvider.cseApi.getTradeSummary()
            val tradeList = response.reqTradeSummery ?: emptyList()
            val priceMap = tradeList.associate { it.symbol to it.price }
            
            val currentPositions = portfolioDao.getAllPositionsSync()
            currentPositions.forEach { position ->
                val newPrice = priceMap[position.symbol]
                if (newPrice != null && newPrice != position.currentPrice) {
                    portfolioDao.updatePosition(position.copy(currentPrice = newPrice))
                }
            }
            
            val currentCrypto = portfolioDao.getAllCryptoSync()
            currentCrypto.forEach { crypto ->
                val newPrice = NetworkProvider.getGoogleCryptoPrice(crypto.symbol)
                if (newPrice != null && newPrice != crypto.currentPrice) {
                    portfolioDao.updateCrypto(crypto.copy(currentPrice = newPrice))
                }
            }
            
            val currentOther = portfolioDao.getAllOtherInvestmentsSync()
            currentOther.forEach { other ->
                if (other.symbol.isNotEmpty() && other.quantity > 0) {
                    val newPrice = NetworkProvider.getGoogleCryptoPrice(other.symbol)
                    if (newPrice != null && newPrice != other.currentPrice) {
                        portfolioDao.updateOtherInvestment(other.copy(currentPrice = newPrice))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun replaceWithBackup(backupData: BackupData) {
        portfolioDao.deleteAllPositions()
        portfolioDao.deleteAllFixedDeposits()
        portfolioDao.deleteAllUnitTrusts()
        portfolioDao.deleteAllCrypto()
        portfolioDao.deleteAllOtherInvestments()

        backupData.positions.forEach { portfolioDao.insertPosition(it) }
        backupData.fixedDeposits.forEach { portfolioDao.insertFixedDeposit(it) }
        backupData.unitTrusts.forEach { portfolioDao.insertUnitTrust(it) }
        backupData.crypto.forEach { portfolioDao.insertCrypto(it) }
        backupData.otherInvestments.forEach { portfolioDao.insertOtherInvestment(it) }
    }
}
