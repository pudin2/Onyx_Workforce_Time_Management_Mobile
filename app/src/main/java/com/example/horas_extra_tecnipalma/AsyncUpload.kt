package com.example.horas_extra_tecnipalma

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.json.JSONObject

import java.io.ByteArrayOutputStream
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

data class RemoteAppVersion(
    val versionCode: Long,
    val versionName: String,
    val apkFile: String
)

class FtpUploadWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private val filenames = listOf("clave.key", "iv.key", "Operarios_enc.json")
    private val appVersionFileName = "app-version.json"

    private val ftpConfigs = listOf(
        FtpConfig(
            server = "190.65.63.135",
            port = 921,
            user = "usrServicios",
            pass = "pruebas123"
        ),
        FtpConfig(
            server = "192.168.10.17",
            port = 921,
            user = "usrServiciosLocal",
            pass = "pruebas123"
        )
    )

    override fun doWork(): Result {
        FileLogger.d("FtpUploadWorker", "Iniciando Worker con fallback FTP")

        val primary = ftpConfigs[0]
        val secondary = ftpConfigs.getOrNull(1)

        syncAssetsFromFTP(applicationContext)
        checkAndDownloadAppUpdate(applicationContext)
        purgeOldUploadedFiles(applicationContext)

        val jsonFiles = getAllJsonFiles(applicationContext)

        if (jsonFiles.isNullOrEmpty()) {
            FileLogger.w("FtpUploadWorker", "No hay archivos JSON para subir")
            return Result.success()
        }

        var allSuccess = true

        for (file in jsonFiles) {
            FileLogger.d("FtpUploadWorker", "Procesando ${file.name}")

            if (uploadFileToFTP(file, primary)) {
                FileLogger.d("FtpUploadWorker", "[${primary.server}] ${file.name} subido (primario)")
                markAsUploaded(file)
                continue
            }

            if (secondary != null) {
                FileLogger.w("FtpUploadWorker", "Fallo primario, probando en secundario ${secondary.server}")
                if (uploadFileToFTP(file, secondary)) {
                    FileLogger.d("FtpUploadWorker", "[${secondary.server}] ${file.name} subido (secundario)")
                    markAsUploaded(file)
                    continue
                }
            }

            FileLogger.e("FtpUploadWorker", "ERROR: ${file.name} no pudo subirse en ningún servidor")
            allSuccess = false
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    private fun getAllJsonFiles(context: Context): List<File> {
        val candidates = context.filesDir.listFiles { _, name ->
            name.endsWith(".json", ignoreCase = true) &&
                    !name.contains("_uploaded", ignoreCase = true)
        } ?: return emptyList()

        return candidates.filter { file ->
            try {
                val root = JSONObject(file.readText())
                val fechaSalida = root.optString("FechaSalida", "").trim()

                if (fechaSalida.isBlank()) return@filter false

                val modified = fillMissingLatLonFromLocation(context, root)

                if (modified) {
                    file.writeText(root.toString())
                }

                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun fillMissingLatLonFromLocation(context: Context, root: JSONObject): Boolean {
        var needsLocation = false
        val it1 = root.keys()

        while (it1.hasNext()) {
            val key = it1.next()

            if (key == "usuario" || key == "FechaSalida") continue

            val arr = root.optJSONArray(key) ?: continue

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue

                if (isEmptyField(obj, "latitud") || isEmptyField(obj, "longitud")) {
                    needsLocation = true
                    break
                }
            }
            if (needsLocation) break
        }

        if (!needsLocation) return false

        val location = runBlocking {
            withTimeoutOrNull(2500L) {
                context.getLastKnownLocation()
            }
        } ?: run {
            FileLogger.w("FtpUploadWorker", "No hay ubicación disponible para completar lat/lon")
            return false
        }

        val lat = location.latitude
        val lon = location.longitude
        var modified = false
        val it2 = root.keys()

        while (it2.hasNext()) {
            val key = it2.next()
            if (key == "usuario" || key == "FechaSalida") continue

            val arr = root.optJSONArray(key) ?: continue
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue

                if (isEmptyField(obj, "latitud")) {
                    obj.put("latitud", lat)
                    modified = true
                }
                if (isEmptyField(obj, "longitud")) {
                    obj.put("longitud", lon)
                    modified = true
                }
            }
        }

        return modified
    }

    private fun isEmptyField(obj: JSONObject, key: String): Boolean {
        if (!obj.has(key) || obj.isNull(key)) return true
        val v = obj.get(key)
        return (v is String && v.trim().isEmpty())
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
                            downloaded = true
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

    private fun checkAndDownloadAppUpdate(context: Context) {
        val assetsDir = File(context.filesDir, "assets").apply { if (!exists()) mkdirs() }
        val localVersionCode = getLocalVersionCode(context)

        ftpConfigs.forEach { cfg ->
            val ftp = FTPClient().apply {
                setConnectTimeout(5_000)
                setDefaultTimeout(5_000)
                setDataTimeout(5_000)
            }

            try {
                ftp.connect(cfg.server, cfg.port)
                ftp.login(cfg.user, cfg.pass)
                ftp.enterLocalPassiveMode()
                ftp.setFileType(FTP.BINARY_FILE_TYPE)

                val versionRemotePath = "${cfg.remoteDirAssets}/$appVersionFileName"
                val versionFiles = ftp.listFiles(versionRemotePath)

                if (versionFiles.isEmpty()) {
                    FileLogger.w("FtpUploadWorker", "No existe $appVersionFileName en ${cfg.server}")
                    return@forEach
                }

                val versionJson = ByteArrayOutputStream().use { out ->
                    val ok = ftp.retrieveFile(versionRemotePath, out)
                    if (!ok) {
                        FileLogger.w("FtpUploadWorker", "No se pudo leer $appVersionFileName desde ${cfg.server}")
                        return@forEach
                    }
                    out.toString(Charsets.UTF_8.name())
                }

                val remoteVersion = parseRemoteAppVersion(versionJson) ?: run {
                    FileLogger.w("FtpUploadWorker", "JSON remoto de versión inválido en ${cfg.server}")
                    return@forEach
                }

                if (remoteVersion.versionCode <= localVersionCode) {
                    FileLogger.d(
                        "FtpUploadWorker",
                        "No hay actualización. Local=$localVersionCode Remota=${remoteVersion.versionCode}"
                    )
                    return@forEach
                }

                val localApkFile = File(assetsDir, remoteVersion.apkFile)
                val remoteApkPath = "${cfg.remoteDirAssets}/${remoteVersion.apkFile}"

                FileOutputStream(localApkFile).use { out ->
                    val ok = ftp.retrieveFile(remoteApkPath, out)
                    if (!ok) {
                        FileLogger.e("FtpUploadWorker", "No se pudo descargar APK ${remoteVersion.apkFile} desde ${cfg.server}")
                        return@forEach
                    }
                }

                val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("update_available", true)
                    .putString("update_apk_path", localApkFile.absolutePath)
                    .putLong("update_remote_version_code", remoteVersion.versionCode)
                    .putString("update_remote_version_name", remoteVersion.versionName)
                    .apply()

                FileLogger.d(
                    "FtpUploadWorker",
                    "APK descargada para actualización: ${localApkFile.absolutePath}, versión remota=${remoteVersion.versionCode}"
                )

                notifyUpdateAvailable(remoteVersion.versionName)
                return
            } catch (e: Exception) {
                FileLogger.w("FtpUploadWorker", "Fallo revisión de actualización en ${cfg.server}: ${e.localizedMessage}")
            } finally {
                if (ftp.isConnected) {
                    ftp.logout()
                    ftp.disconnect()
                }
            }
        }
    }

    private fun parseRemoteAppVersion(json: String): RemoteAppVersion? {
        return try {
            val obj = JSONObject(json)
            RemoteAppVersion(
                versionCode = obj.getLong("versionCode"),
                versionName = obj.optString("versionName", ""),
                apkFile = obj.getString("apkFile")
            )
        } catch (e: Exception) {
            FileLogger.e("FtpUploadWorker", "Error parseando app-version.json: ${e.localizedMessage}")
            null
        }
    }

    private fun getLocalVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun notifyUpdateAvailable(versionName: String) {
        val channelId = "ftp_update_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Actualizaciones de app"
            val description = "Notificaciones de actualización de la aplicación"
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
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Actualización disponible")
            .setContentText("Hay una nueva versión disponible ($versionName). Abra la app para actualizar.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat
            .from(applicationContext)
            .notify(900001, notification)
    }

    private fun uploadFileToFTP(file: File, cfg: FtpConfig): Boolean {
        val ftpClient = FTPClient().apply {
            setConnectTimeout(5_000)
            setDefaultTimeout(5_000)
            setDataTimeout(5_000)
        }

        return try {
            ftpClient.connect(cfg.server, cfg.port)
            ftpClient.login(cfg.user, cfg.pass)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val remotePath = "${cfg.remoteDirUpload}/${file.name}"
            val success = FileInputStream(file).use { input ->
                ftpClient.storeFile(remotePath, input)
            }

            if (success) {
                FileLogger.d(
                    "FtpUploadWorker",
                    "Archivo subido exitosamente a ${cfg.server}: ${file.name}"
                )

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
                    "La subida falló en ${cfg.server}: ${file.name}"
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
            if (ftpClient.isConnected) {
                ftpClient.logout()
                ftpClient.disconnect()
            }
        }
    }
}