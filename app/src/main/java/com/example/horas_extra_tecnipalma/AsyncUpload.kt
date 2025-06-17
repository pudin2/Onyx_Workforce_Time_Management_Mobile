package com.example.horas_extra_tecnipalma

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager

import android.os.Build

import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar

data class FtpConfig(
    val server: String,
    val port: Int,
    val user: String,
    val pass: String,
    val remoteDirAssets: String = "/controlhoras/operarios",
    val remoteDirUpload: String = "/controlhoras/entrada"
)

class FtpUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val filenames = listOf("clave.key", "iv.key", "Operarios_enc.json")
    private val ftpConfigs = listOf(
        FtpConfig(
            server = "190.65.63.135",
            port   = 921,
            user   = "usrServicios",
            pass   = "pruebas123"
        ),
        FtpConfig(
            server = "192.168.10.17",
            port   = 921,
            user   = "usrServiciosLocal",
            pass   = "pruebas123"
        )
    )

    override fun doWork(): Result {
        FileLogger.d("FtpUploadWorker", "Iniciando Worker con fallback FTP")

        val primary   = ftpConfigs[0]
        val secondary = ftpConfigs.getOrNull(1)

        // 3. Sincronizar assets
        syncAssetsFromFTP(applicationContext)
        // 4. Purgar locales antiguos
        purgeOldUploadedFiles(applicationContext)

        // 5. Obtiene todos los JSON pendientes
        val jsonFiles = getAllJsonFiles(applicationContext)
        if (jsonFiles.isNullOrEmpty()) {
            FileLogger.w("FtpUploadWorker", "No hay archivos JSON para subir")
            return Result.success()
        }

        var allSuccess = true

        for (file in jsonFiles) {
            FileLogger.d("FtpUploadWorker", "Procesando ${file.name}")

            // Intento con FTP primario
            if (uploadFileToFTP(file, primary)) {
                FileLogger.d("FtpUploadWorker", "[${primary.server}] ${file.name} subido (primario)")
                markAsUploaded(file)
                continue
            }

            // Si falla y hay servidor secundario, lo intento ahí
            if (secondary != null) {
                FileLogger.w("FtpUploadWorker", "Fallo primario, probando en secundario ${secondary.server}")
                if (uploadFileToFTP(file, secondary)) {
                    FileLogger.d("FtpUploadWorker", "[${secondary.server}] ${file.name} subido (secundario)")
                    markAsUploaded(file)
                    continue
                }
            }

            // Si ambos fallan
            FileLogger.e("FtpUploadWorker", "ERROR: ${file.name} no pudo subirse en ningún servidor")
            allSuccess = false
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    private fun getAllJsonFiles(context: Context): List<File> {
        // Sólo ficheros .json pendientes
        val candidates = context.filesDir.listFiles { _, name ->
            name.endsWith(".json", ignoreCase = true)
                    && !name.contains("_uploaded", ignoreCase = true)
        } ?: return emptyList()

        // Filtra sólo los que tengan "FechaSalida" en su JSON
        return candidates.filter { file ->
            try {
                val text = file.readText()
                // Busca literalmente la propiedad FechaSalida
                text.contains("\"FechaSalida\"")
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun markAsUploaded(file: File): Boolean {
        val renamed = File(file.parentFile, "${file.nameWithoutExtension}_uploaded.json")
        return file.renameTo(renamed)
    }

    private fun purgeOldUploadedFiles(context: Context) {
        val threshold = Calendar.getInstance().apply { add(Calendar.MONTH, -2) }.timeInMillis
        context.filesDir.listFiles { _, name ->
            name.endsWith("_uploaded.json", ignoreCase = true)
        }?.forEach { file ->
            if (file.lastModified() < threshold && file.delete()) {
                FileLogger.d("FtpUploadWorker", "Borrado antiguo: ${file.name}")
            }
        }
    }

    private fun syncAssetsFromFTP(context: Context) {
        val assetsDir = File(context.filesDir, "assets").apply { if (!exists()) mkdirs() }
        filenames.forEach { name ->
            var downloaded = false
            ftpConfigs.forEach { cfg ->
                if (downloaded) return@forEach

                val ftp = FTPClient().apply {
                    // timeouts en milisegundos
                    setConnectTimeout(5_000)
                    setDefaultTimeout(5_000)
                    setDataTimeout(5_000)
                }

                try {
                    ftp.connect(cfg.server, cfg.port)
                    ftp.login(cfg.user, cfg.pass)
                    ftp.enterLocalPassiveMode()
                    ftp.setFileType(FTP.BINARY_FILE_TYPE)

                    val remotePath = "${cfg.remoteDirAssets}/$name"
                    val files = ftp.listFiles(remotePath)
                    if (files.isNotEmpty()) {
                        val remoteFile = files[0]
                        val remoteTime = remoteFile.timestamp.timeInMillis
                        val localFile = File(assetsDir, name)
                        if (!localFile.exists() || localFile.lastModified() < remoteTime) {
                            FileOutputStream(localFile).use { out ->
                                if (ftp.retrieveFile(remotePath, out)) {
                                    localFile.setLastModified(remoteTime)
                                    FileLogger.d("FtpUploadWorker", "[${cfg.server}] Descargado $name exitosamente")
                                    downloaded = true
                                }
                            }
                        } else {
                            downloaded = true // ya actualizado
                        }
                    }
                } catch (e: Exception) {
                    FileLogger.w("FtpUploadWorker", "Fallo descarga $name de ${cfg.server}: ${e.localizedMessage}")
                } finally {
                    if (ftp.isConnected) {
                        ftp.logout()
                        ftp.disconnect()
                    }
                }
            }
        }
    }

    private fun uploadFileToFTP(file: File, cfg: FtpConfig): Boolean {
        // 1) Configurar timeouts para no bloquear demasiado tiempo
        val ftpClient = FTPClient().apply {
            // Timeout para establecer la conexión TCP (milisegundos)
            setConnectTimeout(5_000)
            // Timeout por defecto para otras operaciones de socket
            setDefaultTimeout(5_000)
            // Timeout para las transferencias de datos (read/write)
            setDataTimeout(5_000)
        }

        return try {
            // 2) Intento de conexión y login
            ftpClient.connect(cfg.server, cfg.port)
            ftpClient.login(cfg.user, cfg.pass)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            // 3) Subida del archivo
            val remotePath = "${cfg.remoteDirUpload}/${file.name}"
            val success = FileInputStream(file).use { input ->
                ftpClient.storeFile(remotePath, input)
            }

            // 4) Notificación y log
            if (success) {
                FileLogger.d(
                    "FtpUploadWorker",
                    "✅ Archivo subido exitosamente a ${cfg.server}: ${file.name}"
                )
                // Mostrar notificación al usuario
                val channelId = "ftp_upload_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Subidas FTP"
                    val description = "Notificaciones de archivos subidos al FTP"
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    NotificationChannel(channelId, name, importance).apply {
                        this.description = description
                    }.also { channel ->
                        applicationContext
                            .getSystemService(NotificationManager::class.java)
                            .createNotificationChannel(channel)
                    }
                }
                val notification = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                    .setContentTitle("Subida completada")
                    .setContentText("Archivo ${file.name} cargado a ${cfg.server}")
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat
                    .from(applicationContext)
                    .notify(file.name.hashCode(), notification)
            } else {
                FileLogger.e(
                    "FtpUploadWorker",
                    "❌ La subida falló en ${cfg.server}: ${file.name}"
                )
            }
            success
        } catch (e: Exception) {
            FileLogger.e(
                "FtpUploadWorker",
                "Error upload ${cfg.server}: ${e.localizedMessage}"
            )
            false
        } finally {
            // 5) Limpieza de recursos
            if (ftpClient.isConnected) {
                ftpClient.logout()
                ftpClient.disconnect()
            }
        }
    }

}