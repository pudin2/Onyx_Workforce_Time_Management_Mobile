package com.example.horas_extra_tecnipalma

import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

var loggedUserId: Int? = null
var loggedUserName: String? = null
var loggedUserCargo: String? = null
var loggedUserLocation: String? = null
var loggedUserNumeroOT: String? = null
var loggedUserTipoIngreso: String? = null

class MainActivity : ComponentActivity() {

    private val requestAllPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val notifGranted = results[Manifest.permission.POST_NOTIFICATIONS] == true
            val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!notifGranted) {
                Toast.makeText(this, "Sin permiso de notificaciones, no recibirá alertas del servidor.", Toast.LENGTH_SHORT).show()
            }
            if (!fineGranted && !coarseGranted) {
                Toast.makeText(this, "Sin permiso de ubicación, no podrá obtener coordenadas.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.init(applicationContext)

        val toRequest = mutableListOf<String>()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest += Manifest.permission.POST_NOTIFICATIONS
        }

        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED &&
            coarse != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest += Manifest.permission.ACCESS_FINE_LOCATION
            toRequest += Manifest.permission.ACCESS_COARSE_LOCATION
        }

        if (toRequest.isNotEmpty()) {
            requestAllPermissions.launch(toRequest.toTypedArray())
        }

        setContent {
            Horas_Extra_TecnipalmaTheme {
                LoginScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var numeroOT by remember { mutableStateOf("") }
    var tipoIngreso by remember { mutableStateOf("Planta") }
    var expanded by remember { mutableStateOf(false) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateVersionName by remember { mutableStateOf("") }
    var updateApkPath by remember { mutableStateOf("") }

    val opcionesIngreso = listOf("Planta", "Montaje")

    fun revisarActualizacionPendiente() {
        val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
        val updateAvailable = prefs.getBoolean("update_available", false)
        val apkPath = prefs.getString("update_apk_path", "") ?: ""
        val versionName = prefs.getString("update_remote_version_name", "") ?: ""
        val remoteVersionCode = prefs.getLong("update_remote_version_code", -1L)

        val localVersionCode = getInstalledVersionCode(context)

        if (updateAvailable && apkPath.isNotBlank() && remoteVersionCode > localVersionCode) {
            val apkFile = File(apkPath)
            if (apkFile.exists()) {
                updateApkPath = apkPath
                updateVersionName = versionName
                showUpdateDialog = true
            } else {
                prefs.edit()
                    .putBoolean("update_available", false)
                    .remove("update_apk_path")
                    .remove("update_remote_version_code")
                    .remove("update_remote_version_name")
                    .apply()
                showUpdateDialog = false
            }
        } else {
            showUpdateDialog = false
            prefs.edit()
                .putBoolean("update_available", false)
                .remove("update_apk_path")
                .remove("update_remote_version_code")
                .remove("update_remote_version_name")
                .apply()
        }
    }

    LaunchedEffect(Unit) {
        revisarActualizacionPendiente()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                revisarActualizacionPendiente()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Actualización disponible") },
            text = {
                Text(
                    text = if (updateVersionName.isNotBlank()) {
                        "Hay una nueva versión disponible ($updateVersionName). ¿Desea instalarla ahora?"
                    } else {
                        "Hay una nueva versión disponible. ¿Desea instalarla ahora?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val apkFile = File(updateApkPath)
                        if (!apkFile.exists()) {
                            Toast.makeText(context, "No se encontró el archivo de actualización.", Toast.LENGTH_SHORT).show()
                            showUpdateDialog = false
                            return@TextButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            !context.packageManager.canRequestPackageInstalls()
                        ) {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            Toast.makeText(
                                context,
                                "Habilite la instalación desde esta fuente y vuelva a intentarlo.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@TextButton
                        }

                        try {
                            val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("update_available", false)
                                .remove("update_apk_path")
                                .remove("update_remote_version_code")
                                .remove("update_remote_version_name")
                                .apply()

                            installDownloadedApk(context, apkFile)
                            showUpdateDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "No fue posible iniciar la instalación: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Actualizar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateDialog = false }
                ) {
                    Text("Después")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logor1),
                contentDescription = "Login Image",
                modifier = Modifier
                    .size(250.dp)
                    .padding(bottom = 16.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = tipoIngreso,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de ingreso") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    opcionesIngreso.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                tipoIngreso = opcion
                                expanded = false

                                if (opcion == "Planta") {
                                    numeroOT = ""
                                }
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Identificación") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (tipoIngreso == "Montaje") {
                OutlinedTextField(
                    value = numeroOT,
                    onValueChange = { numeroOT = it },
                    label = { Text("Número de OT") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(
                            context,
                            "Debe ingresar identificación y contraseña",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    if (tipoIngreso == "Montaje" && numeroOT.isBlank()) {
                        Toast.makeText(
                            context,
                            "Debe ingresar un Número de OT",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val user = validateUser(context, username, password)

                    if (user != null) {
                        loggedUserId = user.identificacion
                        loggedUserName = user.nombre
                        loggedUserLocation = ""
                        loggedUserCargo = user.cargo ?: ""
                        loggedUserTipoIngreso = tipoIngreso
                        loggedUserNumeroOT = if (tipoIngreso == "Montaje") numeroOT else ""

                        FileLogger.d(
                            "VALIDACION",
                            "Usuario Logueado: ID=$loggedUserId, Nombre=$loggedUserName, Cargo=$loggedUserCargo, Tipo=$loggedUserTipoIngreso, OT=$loggedUserNumeroOT"
                        )

                        val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("ultimoEstado", "")
                            .apply()

                        val intent = Intent(context, HomeActivity::class.java).apply {
                            putExtra("Identificacion", loggedUserId)
                            putExtra("Nombre", loggedUserName)
                            putExtra("NumeroOT", loggedUserNumeroOT)
                            putExtra("TipoIngreso", loggedUserTipoIngreso)
                        }
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(
                            context,
                            "Identificación o contraseña inválida",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ingresar")
            }
        }
    }
}

private fun installDownloadedApk(context: Context, apkFile: File) {
    val authority = "${context.packageName}.fileprovider"
    val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(intent)
}

fun getInstalledVersionCode(context: Context): Long {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
}

data class User(
    @SerializedName("Identificacion") val identificacion: Int,
    @SerializedName("Contraseña") val contrasena: String,
    @SerializedName("Nombre") val nombre: String,
    @SerializedName("Cargo") val cargo: String? = null
)

fun readFilePriority(context: Context, fileName: String): ByteArray {
    val local = File(context.filesDir, "assets/$fileName")

    if (local.exists()) {
        return try {
            local.readBytes()
        } catch (e: Exception) {
            FileLogger.e("VALIDACION", "ERROR leyendo local $fileName: ${e.message}")
            ByteArray(0)
        }
    }

    return try {
        context.assets.open(fileName).use { it.readBytes() }
    } catch (e: Exception) {
        FileLogger.e("VALIDACION", "ERROR leyendo APK asset $fileName: ${e.message}")
        ByteArray(0)
    }
}

fun validateUser(context: Context, username: String, password: String): User? {
    val claveFileName = "clave.key"
    val ivFileName = "iv.key"
    val jsonEncFileName = "Operarios_enc.json"

    try {
        val claveBytes = readFilePriority(context, claveFileName)
        val ivBytes = readFilePriority(context, ivFileName)
        val encryptedData = readFilePriority(context, jsonEncFileName)
        val decryptedJson = decryptAES(encryptedData, claveBytes, ivBytes)

        if (decryptedJson.isEmpty()) {
            FileLogger.e("VALIDACION", "ERROR: No se pudo desencriptar el JSON")
            return null
        }

        FileLogger.d("VALIDACION", "JSON Desencriptado: $decryptedJson")

        val gson = Gson()
        val userType = object : TypeToken<List<User>>() {}.type
        val users: List<User> = gson.fromJson(decryptedJson, userType)

        val enteredUsername = username.toIntOrNull()

        if (enteredUsername == null) {
            FileLogger.e("VALIDACION", "ERROR: Username no es un número válido")
            return null
        }

        return users.find { it.identificacion == enteredUsername && it.contrasena == password }

    } catch (e: Exception) {
        FileLogger.e("VALIDACION", "ERROR GENERAL: ${e.message}")
        return null
    }
}

fun decryptAES(data: ByteArray, key: ByteArray, iv: ByteArray): String {
    return try {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(data)

        String(decryptedBytes, StandardCharsets.UTF_8)
    } catch (e: Exception) {
        FileLogger.e("VALIDACION", "ERROR AL DESENCRIPTAR: ${e.message}")
        ""
    }
}