package com.example.horas_extra_tecnipalma

import android.app.Application
import androidx.work.Constraints
import androidx.work.*
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1) Definimos las restricciones: solo cuando haya conexión
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2) Creamos un PeriodicWorkRequest que se disparará cada hora
        val periodicWork = PeriodicWorkRequestBuilder<FtpUploadWorker>(
            1, TimeUnit.HOURS              // intervalo de ejecución
        )
            .setConstraints(constraints)  // aplicamos las restricciones
            .setBackoffCriteria(          // política de reintento exponencial
                BackoffPolicy.EXPONENTIAL,
                5, TimeUnit.MINUTES
            )
            .build()

        val immediateWork = OneTimeWorkRequestBuilder<FtpUploadWorker>()
            .setConstraints(constraints)
            .build()

        val wm = WorkManager.getInstance(this)

        // 4) Encolar único periódico
        wm.enqueueUniquePeriodicWork(
            "ftp_upload_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        // 5) Encolar envío inmediato para procesar JSON pendientes
        wm.enqueue(immediateWork)
    }
}
