package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_positions")
data class StockPosition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String, // e.g., "COMB.N0000"
    val companyName: String,
    val quantity: Int,
    val averagePrice: Double,
    val sector: String, // e.g., "Banks"
    val currentPrice: Double = 0.0, // To be updated via our mock API or input
    val purchaseDate: Long = System.currentTimeMillis()
)
