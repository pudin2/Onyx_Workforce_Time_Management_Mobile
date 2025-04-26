package com.example.horas_extra_tecnipalma

import android.content.Context
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient

import java.io.File
import java.io.FileInputStream

class FtpUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("FtpUploadWorker", "Ejecutando tarea de subida FTP")
        val jsonFiles = getAllJsonFiles(applicationContext)
        if (jsonFiles.isNullOrEmpty()) {
            Log.e("FtpUploadWorker", "No se encontró ningún archivo JSON para subir")
            return Result.success() // o Result.failure() según la lógica que desees
        }

        var allSuccess = true
        for (file in jsonFiles) {
            val success = uploadFileToFTP(file)
            if (success) {
                Log.d("FtpUploadWorker", "Archivo ${file.name} subido exitosamente")
                // Opcional: podrías eliminar el archivo tras una subida exitosa:
                // file.delete()
                if (markAsUploaded(file)) {
                    Log.d("FtpUploadWorker", "Archivo renombrado a ${file.nameWithoutExtension}_uploaded.json")
                } else {
                    Log.e("FtpUploadWorker", "No se pudo renombrar ${file.name}")
                }
            } else {
                Log.e("FtpUploadWorker", "Error al subir el archivo ${file.name}")
                allSuccess = false
            }
        }

        return if (allSuccess) {
            Result.success()
        } else {
            Result.retry()  // se reintenta la tarea más tarde si algún archivo no se subió correctamente
        }
    }

    private fun getAllJsonFiles(context: Context): Array<File>? {
        val filesDir = context.filesDir
        return filesDir.listFiles { _, name ->
            name.endsWith(".json")
                    && !name.contains("_uploaded", ignoreCase = true)
        }
    }

    private fun markAsUploaded(file: File): Boolean {
        val parent = file.parentFile ?: return false
        val newName = file.nameWithoutExtension + "_uploaded.json"
        val renamed = File(parent, newName)
        return file.renameTo(renamed)
    }


    private fun uploadFileToFTP(file: File): Boolean {
        val ftpClient = FTPClient()
        return try {
            // Configuración del servidor FTP: reemplaza con tus datos
            val server = "190.65.63.135"
            val port = 921
            val user = "usrServicios"
            val pass = "pruebas123"

            ftpClient.connect(server, port)
            Log.d("FtpUploadWorker", "Conectado al servidor FTP: $server:$port")
            ftpClient.login(user, pass)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val inputStream = FileInputStream(file)
            val remoteFilePath = "/controlhoras/entrada/${file.name}"
            Log.d("FtpUploadWorker", "Subiendo archivo: ${file.name} a $remoteFilePath")
            val success = ftpClient.storeFile(remoteFilePath, inputStream)
            inputStream.close()

            ftpClient.logout()
            ftpClient.disconnect()

            success
        } catch (e: Exception) {
            Log.e("FtpUploadWorker", "Error al subir archivo ${file.name}: ${e.localizedMessage}", e)
            false
        }
    }
}