package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_history")
data class TradeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val companyName: String,
    val quantity: Int,
    val buyPrice: Double,
    val sellPrice: Double,
    val tradeDate: Long = System.currentTimeMillis()
)
