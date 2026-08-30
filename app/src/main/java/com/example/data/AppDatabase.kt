package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StockPosition::class, 
        FixedDeposit::class, 
        StockAlert::class,
        UnitTrust::class,
        Crypto::class,
        OtherInvestment::class
    ], 
    version = 8, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portfolioDao(): PortfolioDao
}
