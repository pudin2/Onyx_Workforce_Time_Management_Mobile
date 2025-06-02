package com.example.horas_extra_tecnipalma

import android.content.Context

import androidx.work.Worker
import androidx.work.WorkerParameters

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar

class FtpUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val filenames = listOf("clave.key", "iv.key", "Operarios_enc.json")
    private val remoteDir  = "/controlhoras/operarios"    // Ajusta al path real en tu FTP

    override fun doWork(): Result {

        FileLogger.d("FtpUploadWorker", "Sincronizando assets desde FTP si hay novedades")
        syncAssetsFromFTP(applicationContext)

        FileLogger.d("FtpUploadWorker", "Ejecutando tarea de purga de archivos antiguos")
        purgeOldUploadedFiles(applicationContext)

        FileLogger.d("FtpUploadWorker", "Ejecutando tarea de subida FTP")

        val jsonFiles = getAllJsonFiles(applicationContext)

        if (jsonFiles.isNullOrEmpty()) {
            FileLogger.e("FtpUploadWorker", "No se encontró ningún archivo JSON para subir")
            return Result.success()
        }

        var allSuccess = true

        for (file in jsonFiles) {
            val success = uploadFileToFTP(file)
            if (success) {
                FileLogger.d("FtpUploadWorker", "Archivo ${file.name} subido exitosamente")

                if (markAsUploaded(file)) {
                    FileLogger.d("FtpUploadWorker", "Archivo renombrado a ${file.nameWithoutExtension}_uploaded.json")
                } else {
                    FileLogger.e("FtpUploadWorker", "No se pudo renombrar ${file.name}")
                }
            } else {
                FileLogger.e("FtpUploadWorker", "Error al subir el archivo ${file.name}")
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

    private fun purgeOldUploadedFiles(context: Context) {
        val filesDir = context.filesDir
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, -2)  // restar 2 meses
        }
        val threshold = calendar.timeInMillis

        filesDir.listFiles { _, name ->
            name.endsWith("_uploaded.json", ignoreCase = true)
        }?.forEach { file ->
            if (file.lastModified() < threshold) {
                if (file.delete()) {
                     FileLogger.d("FtpUploadWorker", "Borrado antiguo: ${file.name}")
                } else {
                     FileLogger.e("FtpUploadWorker", "No se pudo borrar: ${file.name}")
                }
            }
        }
    }

    private fun syncAssetsFromFTP(context: Context) {
        val ftp = FTPClient()
        try {
            ftp.connect("190.65.63.135", 921)
            ftp.login("usrServicios", "pruebas123")
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)

            // Carpeta local donde “simulamos” assets escribibles
            val assetsDir = File(context.filesDir, "assets")
            if (!assetsDir.exists()) assetsDir.mkdirs()

            filenames.forEach { name ->
                val remotePath = "$remoteDir/$name"
                val remoteFiles: Array<FTPFile> = ftp.listFiles(remotePath)
                if (remoteFiles.isNotEmpty()) {
                    val remoteFile = remoteFiles[0]
                    val remoteTime = remoteFile.timestamp.timeInMillis

                    val localFile = File(assetsDir, name)
                    val needDownload = !localFile.exists() || localFile.lastModified() < remoteTime

                    if (needDownload) {
                        FileLogger.d("FtpUploadWorker", "Descargando asset: $name (remoto más nuevo)")
                        FileOutputStream(localFile, false).use { out ->
                            if (ftp.retrieveFile(remotePath, out)) {
                                localFile.setLastModified(remoteTime)
                                FileLogger.d("FtpUploadWorker", "Reemplazado asset: $name")
                            } else {
                                 FileLogger.e("FtpUploadWorker", "Error al descargar asset: $name")
                            }
                        }
                    }
                } else {
                     FileLogger.w("FtpUploadWorker", "No existe en FTP: $remotePath")
                }
            }
        } catch (e: Exception) {
             FileLogger.e("FtpUploadWorker", "FTP Sync Error: ${e.localizedMessage}")
        } finally {
            if (ftp.isConnected) {
                try { ftp.logout() } catch (_: Exception) {}
                try { ftp.disconnect() } catch (_: Exception) {}
            }
        }
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
             FileLogger.d("FtpUploadWorker", "Conectado al servidor FTP: $server:$port")
            ftpClient.login(user, pass)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val inputStream = FileInputStream(file)
            val remoteFilePath = "/controlhoras/entrada/${file.name}"
             FileLogger.d("FtpUploadWorker", "Subiendo archivo: ${file.name} a $remoteFilePath")
            val success = ftpClient.storeFile(remoteFilePath, inputStream)
            inputStream.close()

            ftpClient.logout()
            ftpClient.disconnect()

            success
        } catch (e: Exception) {
             FileLogger.e("FtpUploadWorker", "Error al subir archivo ${file.name}: ${e.localizedMessage}")
            false
        }
    }
}