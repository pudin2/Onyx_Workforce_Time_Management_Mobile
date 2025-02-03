package com.example.horas_extra_tecnipalma

import android.util.Log
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import android.util.Base64
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var selectedLocation by remember { mutableStateOf("") }
    var showTextField by remember { mutableStateOf(false) }
    var additionalInfo by remember { mutableStateOf("") }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                DropdownMenuExample(
                    selectedLocation = selectedLocation,
                    onLocationSelected = {
                        selectedLocation = it
                        showTextField = (it == "Montaje")
                    }
                )

                if (showTextField) {
                    TextField(
                        value = additionalInfo,
                        onValueChange = { additionalInfo = it },
                        label = { Text("Número de OT") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        if (validateUser(context, username, password)) {
                            val intent = Intent(context, HomeActivity::class.java)
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Identificación o contraseña inválida", Toast.LENGTH_SHORT).show()
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

// 📌 Modelo de usuario con identificación y contraseña en Base64
// 📌 Modelo de usuario
data class User(
    @SerializedName("Identificacion") val identificacion: Int,
    @SerializedName("ContraseñaHash") val contraseñaHash: String
)

fun validateUser(context: Context, username: String, password: String): Boolean {
    val claveFileName = "clave.key"
    val ivFileName = "iv.key"
    val jsonEncFileName = "Operarios_enc.json"

    try {
        // 1️⃣ Leer la clave AES de 32 bytes desde assets/
        val claveBytes = readFileFromAssets(context, claveFileName)
        if (claveBytes.isEmpty()) {
            Log.e("VALIDACION", "❌ ERROR: No se pudo leer la clave AES")
            return false
        }

        // 2️⃣ Leer el IV de 16 bytes desde assets/
        val ivBytes = readFileFromAssets(context, ivFileName)
        if (ivBytes.isEmpty()) {
            Log.e("VALIDACION", "❌ ERROR: No se pudo leer el IV")
            return false
        }

        // 3️⃣ Leer el archivo JSON encriptado desde assets/
        val encryptedData = readFileFromAssets(context, jsonEncFileName)
        if (encryptedData.isEmpty()) {
            Log.e("VALIDACION", "❌ ERROR: No se pudo leer el archivo encriptado")
            return false
        }

        // 4️⃣ Desencriptar el JSON
        val decryptedJson = decryptAES(encryptedData, claveBytes, ivBytes)
        if (decryptedJson.isEmpty()) {
            Log.e("VALIDACION", "❌ ERROR: No se pudo desencriptar el JSON")
            return false
        }

        Log.d("VALIDACION", "✅ JSON Desencriptado: $decryptedJson")

        // 5️⃣ Convertir JSON a lista de usuarios
        val gson = Gson()
        val userType = object : TypeToken<List<User>>() {}.type
        val users: List<User> = gson.fromJson(decryptedJson, userType)

        val enteredUsername = username.toIntOrNull()
        if (enteredUsername == null) {
            Log.e("VALIDACION", "❌ ERROR: Username no es un número válido")
            return false
        }

        val user = users.find { it.identificacion == enteredUsername }
        if (user != null) {
            val enteredPasswordHash = hashPassword(password)
            val storedPasswordHashBase64 = user.contraseñaHash

            // 📌 Convertir el hash almacenado de Base64 a HEX
            val storedPasswordHashBytes = Base64.decode(storedPasswordHashBase64, Base64.DEFAULT)
            val storedPasswordHashHex = storedPasswordHashBytes.joinToString("") { "%02x".format(it) }.uppercase()

            // 📌 Agregamos logs para verificar qué se está comparando
            Log.d("VALIDACION", "🔑 Hash ingresado en HEX: $enteredPasswordHash")
            Log.d("VALIDACION", "📂 Hash almacenado convertido de Base64 a HEX: $storedPasswordHashHex")

// Comparar los hashes en formato HEX
            return enteredPasswordHash == storedPasswordHashHex
        } else {
            Log.e("VALIDACION", "❌ ERROR: Usuario no encontrado en JSON")
        }

        return false

    } catch (e: Exception) {
        Log.e("VALIDACION", "❌ ERROR GENERAL: ${e.message}")
        return false
    }
}

// 📌 Función para desencriptar con AES-256 CBC
fun decryptAES(data: ByteArray, key: ByteArray, iv: ByteArray): String {
    return try {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipher.doFinal(data)

        val decryptedText = String(decryptedBytes, StandardCharsets.UTF_8)
        Log.d("VALIDACION", "✅ Archivo desencriptado correctamente")

        decryptedText
    } catch (e: Exception) {
        Log.e("VALIDACION", "❌ ERROR AL DESENCRIPTAR: ${e.message}")
        ""
    }
}

// 📌 Función para generar hash de contraseña con SHA-256
fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")

    // 📌 Convertir la contraseña a UTF-16LE (igual que SQL Server usa NVARCHAR)
    val passwordBytes = password.toByteArray(StandardCharsets.UTF_16LE)

    // 📌 Generar hash SHA-256
    val hashedBytes = digest.digest(passwordBytes)

    // 📌 Convertir a Base64 (si JSON usa Base64)
    return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
}

// 📌 Función para leer archivos desde `assets/`
fun readFileFromAssets(context: Context, fileName: String): ByteArray {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val bytes = inputStream.readBytes()
        inputStream.close()

        Log.d("VALIDACION", "📂 Archivo leído: $fileName, Tamaño: ${bytes.size} bytes")

        bytes
    } catch (e: Exception) {
        Log.e("VALIDACION", "❌ ERROR LEYENDO ARCHIVO: $fileName - ${e.message}")
        ByteArray(0)
    }
}
