package com.example.horas_extra_tecnipalma

import android.content.Context
import android.util.Log

import java.text.ParseException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object FileLogger {
    private lateinit var logFile: File
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private const val RETENTION_DAYS = 7L

    fun init(context: Context) {

        val logsDir = context.getExternalFilesDir("logs")
            ?: File(context.filesDir, "logs")
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
            val ts = Date()
            val tsText = dateFmt.format(ts)
            val newEntry = "[$tsText] $tag: $message"
            val existing = if (::logFile.isInitialized && logFile.exists()) {
                logFile.readText()
            } else {
                ""
            }
            val cutoff = Date(ts.time - RETENTION_DAYS * 24 * 60 * 60 * 1000)
            val filteredLines = existing
                .lines()
                .mapNotNull { line ->
                    if (line.startsWith("[")) {
                        val endIdx = line.indexOf(']')
                        if (endIdx > 1) {
                            val datePart = line.substring(1, endIdx)
                            try {
                                val lineDate = dateFmt.parse(datePart)
                                if (lineDate != null && lineDate.after(cutoff)) {
                                    return@mapNotNull line
                                }
                            } catch (pe: ParseException) {
                            }
                        }
                    }
                    null
                }
            val newContent = buildString {
                append(newEntry)
                append("\n")
                filteredLines.forEach { append(it).append("\n") }
            }

            logFile.writeText(newContent)
        } catch (io: Exception) {
            Log.e("FileLogger", "No se pudo escribir log: ${io.message}")
        }
    }
}


