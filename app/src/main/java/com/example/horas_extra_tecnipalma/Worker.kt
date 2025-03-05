package com.example.horas_extra_tecnipalma

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configurar las restricciones: solo se ejecuta si hay conexión a Internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Crear y encolar el Worker
        val ftpUploadWorkRequest = OneTimeWorkRequestBuilder<FtpUploadWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(ftpUploadWorkRequest)
    }
}
