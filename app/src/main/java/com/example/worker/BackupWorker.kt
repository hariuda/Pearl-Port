package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.BackupData
import com.example.data.DatabaseProvider
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dao = DatabaseProvider.getDatabase(applicationContext).portfolioDao()
            
            val positions = dao.getAllPositions().first()
            val fixedDeposits = dao.getAllFixedDeposits().first()
            val unitTrusts = dao.getAllUnitTrusts().first()
            val crypto = dao.getAllCrypto().first()
            val otherInvestments = dao.getAllOtherInvestments().first()
            
            val prefs = applicationContext.getSharedPreferences("portfolio_prefs", Context.MODE_PRIVATE)
            val userName = prefs.getString("user_name", "Guest")
            val chartColorPalette = prefs.getString("chart_color_palette", "Default")

            val backup = BackupData(
                positions = positions,
                fixedDeposits = fixedDeposits,
                unitTrusts = unitTrusts,
                crypto = crypto,
                otherInvestments = otherInvestments,
                userName = userName,
                chartColorPalette = chartColorPalette
            )

            val json = Gson().toJson(backup)
            
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            // Save to standard internal app data folder
            val backupsDir = File(applicationContext.getExternalFilesDir(null), "backups")
            if (!backupsDir.exists()) {
                backupsDir.mkdirs()
            }
            
            val backupFile = File(backupsDir, "Pp_backup_${dateStr}.json")
            backupFile.writeText(json)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
