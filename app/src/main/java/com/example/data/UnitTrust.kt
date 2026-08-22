package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unit_trusts")
data class UnitTrust(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fundName: String,
    val units: Double,
    val averageNav: Double,
    val currentNav: Double = 0.0,
    val purchaseDate: Long = System.currentTimeMillis(),
    val sector: String = "Unit Trusts"
)
