package com.example.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StockUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = DatabaseProvider.getDatabase(applicationContext)
            val dao = database.portfolioDao()
            
            // Get all current stock positions
            val currentPositions = dao.getAllPositionsSync()
            if (currentPositions.isNotEmpty()) {
                val response = NetworkProvider.cseApi.getTradeSummary()
                val tradeList = response.reqTradeSummery ?: emptyList()
                val priceMap = tradeList.associate { it.symbol to it.price }
                
                // Update all positions in DB
                currentPositions.forEach { position ->
                    val newPrice = priceMap[position.symbol]
                    if (newPrice != null && newPrice != position.currentPrice) {
                        dao.updatePosition(position.copy(currentPrice = newPrice))
                    }
                }
            }

            // Get all crypto positions
            val currentCrypto = dao.getAllCryptoSync()
            if (currentCrypto.isNotEmpty()) {
                currentCrypto.forEach { crypto ->
                    val newPrice = NetworkProvider.getGoogleCryptoPrice(crypto.symbol)
                    if (newPrice != null && newPrice != crypto.currentPrice) {
                        dao.updateCrypto(crypto.copy(currentPrice = newPrice))
                    }
                }
            }

            // Get all other positions
            val currentOther = dao.getAllOtherInvestmentsSync()
            if (currentOther.isNotEmpty()) {
                currentOther.forEach { other ->
                    if (other.symbol.isNotEmpty() && other.quantity > 0) {
                        val newPrice = NetworkProvider.getGoogleCryptoPrice(other.symbol)
                        if (newPrice != null && newPrice != other.currentPrice) {
                            dao.updateOtherInvestment(other.copy(currentPrice = newPrice))
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
