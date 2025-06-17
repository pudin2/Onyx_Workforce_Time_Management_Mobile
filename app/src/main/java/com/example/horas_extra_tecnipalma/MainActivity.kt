package com.example.horas_extra_tecnipalma

import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import java.io.File

// Variables globales
var loggedUserId: Int? = null
var loggedUserName: String? = null
var loggedUserLocation: String? = null    // Queda en blanco
var loggedUserNumeroOT: String? = null

class MainActivity : ComponentActivity() {

    private val requestAllPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val notifGranted = results[Manifest.permission.POST_NOTIFICATIONS] == true
            val fineGranted  = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted= results[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!notifGranted) {
                Toast.makeText(this, "Sin permiso de notificaciones, no recibirá alertas FTP.", Toast.LENGTH_SHORT).show()
            }
            if (!fineGranted && !coarseGranted) {
                Toast.makeText(this, "Sin permiso de ubicación, no podrá obtener coordenadas.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.init(applicationContext)

        // Solicitar permisos si faltan
        val toRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            toRequest += Manifest.permission.POST_NOTIFICATIONS
        }
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED &&
            coarse != PackageManager.PERMISSION_GRANTED) {
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

@Composable
fun LoginScreen(context: Context) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var numeroOT by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            Column(
                modifier = Modifier
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

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Identificación") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Campo OT siempre visible
                TextField(
                    value = numeroOT,
                    onValueChange = { numeroOT = it },
                    label = { Text("Número de OT") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        // Ahora sólo validamos OT
                        if (numeroOT.isEmpty()) {
                            Toast.makeText(context, "Debe ingresar un Número de OT", Toast.LENGTH_SHORT).show()
                        } else {
                            val user = validateUser(context, username, password)
                            if (user != null) {
                                // Guardamos globals
                                loggedUserId = user.identificacion
                                loggedUserName = user.nombre
                                loggedUserLocation = ""                 // Sin ubicación
                                loggedUserNumeroOT = numeroOT

                                FileLogger.d(
                                    "VALIDACION",
                                    "✅ Usuario Logueado: ID=$loggedUserId, Nombre=$loggedUserName, OT=$loggedUserNumeroOT"
                                )

                                // Limpiar último estado
                                val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putString("ultimoEstado", "")
                                    .apply()

                                // Navegar a Home
                                val intent = Intent(context, HomeActivity::class.java).apply {
                                    putExtra("Identificacion", loggedUserId)
                                    putExtra("Nombre", loggedUserName)
                                    putExtra("NumeroOT", loggedUserNumeroOT)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Identificación o contraseña inválida", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar")
                }
            }
        }
    )
}

@Composable
fun DropdownMenuExample(selectedLocation: String, onLocationSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Button(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1BA1BF),
            contentColor = Color.White
        )
    ) {
        Text(text = "Ubicación: $selectedLocation")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth()
    ) {
        DropdownMenuItem(
            text = { Text("Planta") },
            onClick = {
                onLocationSelected("Planta")
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Montaje") },
            onClick = {
                onLocationSelected("Montaje")
                expanded = false
            }
        )
    }
}

data class User(
    @SerializedName("Identificacion") val identificacion: Int,
    @SerializedName("Contraseña") val contrasena: String,
    @SerializedName("Nombre") val nombre: String,
)

fun readFilePriority(context: Context, fileName: String): ByteArray {

    val local = File(context.filesDir, "assets/$fileName")
    if (local.exists()) {
        return try {
            local.readBytes()
        } catch (e: Exception) {
            FileLogger.e("VALIDACION", "❌ ERROR leyendo local $fileName: ${e.message}")
            ByteArray(0)
        }
    }

    return try {
        context.assets.open(fileName).use { it.readBytes() }
    } catch (e: Exception) {
        FileLogger.e("VALIDACION", "❌ ERROR leyendo APK asset $fileName: ${e.message}")
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
            FileLogger.e("VALIDACION", "❌ ERROR: No se pudo desencriptar el JSON")
            return null
        }

        FileLogger.d("VALIDACION", "✅ JSON Desencriptado: $decryptedJson")

        val gson = Gson()
        val userType = object : TypeToken<List<User>>() {}.type
        val users: List<User> = gson.fromJson(decryptedJson, userType)

        val enteredUsername = username.toIntOrNull()
        if (enteredUsername == null) {
            FileLogger.e("VALIDACION", "❌ ERROR: Username no es un número válido")
            return null
        }

        return users.find { it.identificacion == enteredUsername && it.contrasena == password }

    } catch (e: Exception) {
        FileLogger.e("VALIDACION", "❌ ERROR GENERAL: ${e.message}")
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
        FileLogger.e("VALIDACION", "❌ ERROR AL DESENCRIPTAR: ${e.message}")
        ""
    }
}


