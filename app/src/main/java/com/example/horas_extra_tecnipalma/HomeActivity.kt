package com.example.horas_extra_tecnipalma

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 📌 Data Class para el JSON
data class StateChangeRecord(
    val Identificacion: Int,
    val Nombre: String,
    val Ubicacion: String,
    val NumeroOT: String?,
    val estado: String?,
    val hora: String?
)

val stateChanges = mutableStateListOf<StateChangeRecord>()

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Horas_Extra_TecnipalmaTheme {
                MenuScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(context: Context) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Menú") },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MenuContent(context)
        }
    }
}

@Composable
fun MenuContent(context: Context) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    // 📌 Guardar la información del usuario en el JSON desde el inicio
    LaunchedEffect(Unit) {
        initializeJsonWithUserInfo(context)
    }

    fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logor1),
            contentDescription = "Login Image",
            modifier = Modifier
                .size(250.dp)
                .padding(bottom = 32.dp)
        )

        Box {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedOption.isNotEmpty()) {
                        "Estado: $selectedOption "
                    } else {
                        "Selecciona el Estado"
                    }
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("En Turno", "Pausa Activa", "Almuerzo", "Descanso", "Fuera de Turno").forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state) },
                        onClick = {
                            selectedOption = state
                            selectedTime = getCurrentTime()

                            // 📌 Ahora también guardamos "Fuera de Turno"
                            saveStateChanges(context, selectedOption, selectedTime)

                            Log.d("VALIDACION", "✅ Estado guardado en JSON: $selectedOption a las $selectedTime")

                            expanded = false
                        }
                    )
                }
            }

        }
    }
}

// 📌 Función para inicializar el JSON con la información del usuario
fun initializeJsonWithUserInfo(context: Context) {
    try {
        val file = File(context.filesDir, "estado.json")

        // 📌 Si el archivo ya existe, leer su contenido
        val jsonMap: MutableMap<String, Any> = if (file.exists()) {
            val existingJson = FileReader(file).use { it.readText() }
            if (existingJson.isNotEmpty()) {
                try {
                    Gson().fromJson(existingJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
                        ?: mutableMapOf()
                } catch (e: Exception) {
                    Log.e("JSON", "❌ ERROR al parsear el JSON existente: ${e.message}")
                    mutableMapOf()
                }
            } else {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }

        // 📌 Agregar la información del usuario SOLO si aún no está en el JSON
        if (!jsonMap.containsKey("usuario")) {
            val userInfo = mapOf(
                "Identificacion" to (loggedUserId ?: 0),
                "Nombre" to (loggedUserName ?: "No disponible"),
                "Ubicacion" to (loggedUserLocation ?: "No seleccionada"),
                "NumeroOT" to (loggedUserNumeroOT ?: "")
            )
            jsonMap["usuario"] = userInfo
        }

        // 📌 Guardar el JSON corregido
        val json = Gson().toJson(jsonMap)
        FileWriter(file).use { it.write(json) }

        Log.d("JSON", "✅ Información del usuario guardada en JSON correctamente:\n$json")

    } catch (e: Exception) {
        Log.e("JSON", "❌ ERROR al inicializar el JSON con la información del usuario: ${e.message}")
    }
}


// 📌 Función para guardar un nuevo estado en el JSON sin modificar los datos del usuario
fun saveStateChanges(context: Context, estado: String, hora: String) {
    try {
        val file = File(context.filesDir, "estado.json")

        // 📌 Obtener la fecha actual como clave (yyyy-MM-dd)
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 📌 Cargar el JSON existente
        val jsonMap = if (file.exists()) {
            val existingJson = FileReader(file).use { it.readText() }
            Gson().fromJson(existingJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        } else {
            mutableMapOf<String, Any>()
        }

        // 📌 Obtener la lista de estados del día actual (o crearla si no existe)
        val estadosDelDia = jsonMap.getOrDefault(fechaHoy, mutableListOf<Map<String, String>>()) as MutableList<Map<String, String>>

        // 📌 Agregar el nuevo estado con su hora
        estadosDelDia.add(mapOf("estado" to estado, "hora" to hora))

        // 📌 Guardar la lista de estados en la fecha correspondiente
        jsonMap[fechaHoy] = estadosDelDia

        // 📌 Guardar el JSON actualizado
        val json = Gson().toJson(jsonMap)
        FileWriter(file).use { it.write(json) }

        // 📌 🔥 Log completo para verificar el JSON
        Log.d("JSON", "📂 JSON COMPLETO:\n$json")

    } catch (e: Exception) {
        Log.e("JSON", "❌ ERROR al guardar el estado en JSON: ${e.message}")
    }
}




