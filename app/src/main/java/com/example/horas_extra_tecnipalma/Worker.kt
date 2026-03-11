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

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicWork = PeriodicWorkRequestBuilder<FtpUploadWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                5, TimeUnit.MINUTES
            )
            .build()
        val immediateWork = OneTimeWorkRequestBuilder<FtpUploadWorker>()
            .setConstraints(constraints)
            .build()
        val wm = WorkManager.getInstance(this)

        wm.enqueueUniquePeriodicWork(
            "ftp_upload_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        wm.enqueue(immediateWork)
    }
}
