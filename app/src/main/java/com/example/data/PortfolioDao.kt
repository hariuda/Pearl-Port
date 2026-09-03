package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM stock_positions")
    fun getAllPositions(): Flow<List<StockPosition>>
    
    @Query("SELECT * FROM stock_positions")
    suspend fun getAllPositionsSync(): List<StockPosition>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: StockPosition)

    @Update
    suspend fun updatePosition(position: StockPosition)

    @Query("DELETE FROM stock_positions WHERE id = :id")
    suspend fun deletePositionById(id: Int)

    @Query("SELECT * FROM fixed_deposits")
    fun getAllFixedDeposits(): Flow<List<FixedDeposit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedDeposit(fd: FixedDeposit)

    @Query("DELETE FROM fixed_deposits WHERE id = :id")
    suspend fun deleteFixedDepositById(id: Int)

    @Query("SELECT * FROM stock_alerts")
    fun getAllAlerts(): Flow<List<StockAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: StockAlert)

    @Update
    suspend fun updateAlert(alert: StockAlert)

    @Query("DELETE FROM stock_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)

    // Unit Trusts
    @Query("SELECT * FROM unit_trusts")
    fun getAllUnitTrusts(): Flow<List<UnitTrust>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitTrust(ut: UnitTrust)

    @Query("DELETE FROM unit_trusts WHERE id = :id")
    suspend fun deleteUnitTrustById(id: Int)

    // Crypto
    @Query("SELECT * FROM crypto")
    fun getAllCrypto(): Flow<List<Crypto>>

    @Query("SELECT * FROM crypto")
    suspend fun getAllCryptoSync(): List<Crypto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrypto(crypto: Crypto)

    @Update
    suspend fun updateCrypto(crypto: Crypto)

    @Query("DELETE FROM crypto WHERE id = :id")
    suspend fun deleteCryptoById(id: Int)

    // Other Investments
    @Query("SELECT * FROM other_investments")
    fun getAllOtherInvestments(): Flow<List<OtherInvestment>>

    @Query("SELECT * FROM other_investments")
    suspend fun getAllOtherInvestmentsSync(): List<OtherInvestment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtherInvestment(other: OtherInvestment)

    @Update
    suspend fun updateOtherInvestment(other: OtherInvestment)

    @Query("DELETE FROM other_investments WHERE id = :id")
    suspend fun deleteOtherInvestmentById(id: Int)

    // Trade History
    @Query("SELECT * FROM trade_history ORDER BY tradeDate DESC")
    fun getAllTradeRecords(): Flow<List<TradeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeRecord(trade: TradeRecord)

    @Query("DELETE FROM trade_history")
    suspend fun deleteAllTradeRecords()

    @Query("DELETE FROM stock_positions")
    suspend fun deleteAllPositions()

    @Query("DELETE FROM fixed_deposits")
    suspend fun deleteAllFixedDeposits()

    @Query("DELETE FROM unit_trusts")
    suspend fun deleteAllUnitTrusts()

    @Query("DELETE FROM crypto")
    suspend fun deleteAllCrypto()

    @Query("DELETE FROM other_investments")
    suspend fun deleteAllOtherInvestments()
}
