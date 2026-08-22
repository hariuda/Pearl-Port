package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_alerts")
data class StockAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val targetPrice: Double,
    val isBuy: Boolean, // True if we alert when price drops to/below targetPrice, false if selling (price goes above)
    val isActive: Boolean = true
)
