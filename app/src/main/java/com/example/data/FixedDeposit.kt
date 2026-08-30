package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "fixed_deposits")
data class FixedDeposit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val principalAmount: Double,
    val interestRate: Double, // percentage
    val maturityDate: Long, // timestamp
    val isMonthlyInterest: Boolean = false,
    val sector: String = "Fixed Deposits",
    val startDate: Long = System.currentTimeMillis(),
    val periodMonths: Int = 12,
    val hasAitDeduction: Boolean = false
) {
    fun calculateAccruedInterest(): Double {
        val now = System.currentTimeMillis()
        val calendarStart = Calendar.getInstance().apply { timeInMillis = startDate }
        val calendarNow = Calendar.getInstance().apply { timeInMillis = now }
        
        val diffYear = calendarNow.get(Calendar.YEAR) - calendarStart.get(Calendar.YEAR)
        val diffMonth = diffYear * 12 + calendarNow.get(Calendar.MONTH) - calendarStart.get(Calendar.MONTH)
        
        // Coerce the months passed so it doesn't exceed the period
        val monthsPassed = diffMonth.coerceIn(0, periodMonths)
        
        val grossInterest = if (isMonthlyInterest) {
            principalAmount * (interestRate / 100) * (monthsPassed / 12.0)
        } else {
            if (monthsPassed >= periodMonths || now >= maturityDate) {
                principalAmount * (interestRate / 100) * (periodMonths / 12.0)
            } else {
                0.0
            }
        }

        return if (hasAitDeduction) {
            grossInterest * 0.90 // 10% tax deducted from monthly/accrued interest
        } else {
            grossInterest
        }
    }
    
    val currentValue: Double
        get() = principalAmount + calculateAccruedInterest()
}
