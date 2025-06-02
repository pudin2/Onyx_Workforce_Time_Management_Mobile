package com.example.horas_extra_tecnipalma

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private lateinit var logFile: File
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun init(context: Context) {
        // Usa almacenamiento externo privado
        val logsDir = context.getExternalFilesDir("logs")
            ?: File(context.filesDir, "logs") // fallback
        if (!logsDir.exists()) logsDir.mkdirs()
        logFile = File(logsDir, "app.log")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeToFile(tag, message)
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
        writeToFile(tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        writeToFile(tag, "WARN: $message")
    }

    private fun writeToFile(tag: String, message: String) {
        try {
            val ts = dateFmt.format(Date())
            logFile.appendText("[$ts] $tag: $message\n")
        } catch (io: Exception) {
            Log.e("FileLogger", "No se pudo escribir log: ${io.message}")
        }
    }
}


