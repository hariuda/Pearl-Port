package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE stock_positions ADD COLUMN purchaseDate INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()
            database.execSQL("ALTER TABLE fixed_deposits ADD COLUMN startDate INTEGER NOT NULL DEFAULT $currentTime")
            database.execSQL("ALTER TABLE fixed_deposits ADD COLUMN periodMonths INTEGER NOT NULL DEFAULT 12")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()
            database.execSQL("ALTER TABLE unit_trusts ADD COLUMN purchaseDate INTEGER NOT NULL DEFAULT $currentTime")
            database.execSQL("ALTER TABLE crypto ADD COLUMN purchaseDate INTEGER NOT NULL DEFAULT $currentTime")
            database.execSQL("ALTER TABLE other_investments ADD COLUMN purchaseDate INTEGER NOT NULL DEFAULT $currentTime")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE other_investments ADD COLUMN symbol TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE other_investments ADD COLUMN quantity REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE other_investments ADD COLUMN averagePrice REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE other_investments ADD COLUMN currentPrice REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE fixed_deposits ADD COLUMN hasAitDeduction INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `trade_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `symbol` TEXT NOT NULL, `companyName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `buyPrice` REAL NOT NULL, `sellPrice` REAL NOT NULL, `tradeDate` INTEGER NOT NULL)")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE stock_positions ADD COLUMN totalDividends REAL NOT NULL DEFAULT 0.0")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE crypto ADD COLUMN isPrivateWallet INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE crypto ADD COLUMN exchangeName TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE fixed_deposits ADD COLUMN interestWithdrawn INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "portfolio_database"
            )
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
            INSTANCE = instance
            instance
        }
    }
}
