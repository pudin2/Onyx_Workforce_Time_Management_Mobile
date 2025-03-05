package com.example.horas_extra_tecnipalma
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme

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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName

import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

var loggedUserId: Int? = null
var loggedUserName: String? = null
var loggedUserLocation: String? = null
var loggedUserNumeroOT: String? = null

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

                DropdownMenuExample(
                    selectedLocation = selectedLocation,
                    onLocationSelected = {
                        selectedLocation = it
                        showTextField = (it == "Montaje")
                    }
                )

                if (showTextField) {
                    TextField(
                        value = numeroOT,
                        onValueChange = { numeroOT = it }, // 📌 Guardar el valor en numeroOT
                        label = { Text("Número de OT") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        if (selectedLocation.isEmpty()) {
                            Toast.makeText(context, "Debe seleccionar una ubicación", Toast.LENGTH_SHORT).show()
                        } else if (selectedLocation == "Montaje" && numeroOT.isEmpty()) {
                            Toast.makeText(context, "Debe ingresar un Número de OT", Toast.LENGTH_SHORT).show()
                        } else {
                            val user = validateUser(context, username, password)
                            if (user != null) {
                                // 📌 Guardamos los datos globalmente
                                loggedUserId = user.identificacion
                                loggedUserName = user.nombre
                                loggedUserLocation = selectedLocation
                                loggedUserNumeroOT = if (selectedLocation == "Montaje") numeroOT else ""

                                Log.d("VALIDACION", "✅ Usuario Logueado: ID=$loggedUserId, Nombre=$loggedUserName, Ubicación=$loggedUserLocation, OT=$loggedUserNumeroOT")

                                // 📌 Enviar datos a la siguiente pantalla
                                val intent = Intent(context, HomeActivity::class.java)
                                intent.putExtra("Identificacion", loggedUserId)
                                intent.putExtra("Nombre", loggedUserName)
                                intent.putExtra("Ubicacion", loggedUserLocation)
                                intent.putExtra("NumeroOT", loggedUserNumeroOT)
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

fun validateUser(context: Context, username: String, password: String): User? {
    val claveFileName = "clave.key"
    val ivFileName = "iv.key"
    val jsonEncFileName = "Operarios_enc.json"

    try {
        val claveBytes = readFileFromAssets(context, claveFileName)
        val ivBytes = readFileFromAssets(context, ivFileName)
        val encryptedData = readFileFromAssets(context, jsonEncFileName)

        val decryptedJson = decryptAES(encryptedData, claveBytes, ivBytes)
        if (decryptedJson.isEmpty()) {
            Log.e("VALIDACION", "❌ ERROR: No se pudo desencriptar el JSON")
            return null
        }

        Log.d("VALIDACION", "✅ JSON Desencriptado: $decryptedJson")

        val gson = Gson()
        val userType = object : TypeToken<List<User>>() {}.type
        val users: List<User> = gson.fromJson(decryptedJson, userType)

        val enteredUsername = username.toIntOrNull()
        if (enteredUsername == null) {
            Log.e("VALIDACION", "❌ ERROR: Username no es un número válido")
            return null
        }

        // 📌 Buscar usuario por Identificación
        return users.find { it.identificacion == enteredUsername && it.contrasena == password }

    } catch (e: Exception) {
        Log.e("VALIDACION", "❌ ERROR GENERAL: ${e.message}")
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
        Log.e("VALIDACION", "❌ ERROR AL DESENCRIPTAR: ${e.message}")
        ""
    }
}

fun readFileFromAssets(context: Context, fileName: String): ByteArray {
    return try {
        val inputStream: InputStream = context.assets.open(fileName)
        val bytes = inputStream.readBytes()
        inputStream.close()
        bytes
    } catch (e: Exception) {
        Log.e("VALIDACION", "❌ ERROR LEYENDO ARCHIVO: $fileName - ${e.message}")
        ByteArray(0)
    }
}

