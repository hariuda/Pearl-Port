package com.example.data

data class BackupData(
    val positions: List<StockPosition> = emptyList(),
    val fixedDeposits: List<FixedDeposit> = emptyList(),
    val unitTrusts: List<UnitTrust> = emptyList(),
    val crypto: List<Crypto> = emptyList(),
    val otherInvestments: List<OtherInvestment> = emptyList(),
    val userName: String? = null,
    val themePreference: String? = null,
    val chartColorPalette: String? = null
)
