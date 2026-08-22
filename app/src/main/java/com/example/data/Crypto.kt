package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto")
data class Crypto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double = 0.0,
    val purchaseDate: Long = System.currentTimeMillis(),
    val sector: String = "Crypto"
)
