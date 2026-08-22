package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "other_investments")
data class OtherInvestment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // e.g., "Gold", "Real Estate"
    val value: Double,
    val purchaseDate: Long = System.currentTimeMillis(),
    val sector: String = "Gold & Other",
    val symbol: String = "",
    val quantity: Double = 0.0,
    val averagePrice: Double = 0.0,
    val currentPrice: Double = 0.0
)
