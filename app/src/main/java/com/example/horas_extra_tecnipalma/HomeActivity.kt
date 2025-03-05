package com.example.horas_extra_tecnipalma

import android.content.Context
import android.os.Bundle
import android.util.Log

import kotlinx.coroutines.launch

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

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
data class StateChangeRecord(val estado: String, val hora: String, val fecha: String)
//agregado

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

@Composable
fun DrawerContent(onItemSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1BA1BF)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center, // ✅ Centra verticalmente en la pantalla
            horizontalAlignment = Alignment.CenterHorizontally // ✅ Centra horizontalmente el contenido

        ) {
            DrawerItem("Página Principal", onClick = { onItemSelected("home") })
            DrawerItem("Reporte", onClick = { onItemSelected("reporte") })
            DrawerItem("Configuración", onClick = { onItemSelected("config") })
        }
    }
}

@Composable
fun DrawerItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(context: Context) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val stateChanges = remember{mutableStateListOf<StateChangeRecord>()}

    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent { route ->
                scope.launch {
                    drawerState.close()
                }
                navController.navigate(route)
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Menu") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {  MenuContent(context, stateChanges) }
                composable("reporte") { ReporteScreen(stateChanges) }
                composable("config") { ConfigScreen() }
            }
        }
    }
}

@Composable
fun MenuContent(context: Context, stateChanges: MutableList<StateChangeRecord>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by rememberSaveable { mutableStateOf("") }
    var selectedTime by rememberSaveable { mutableStateOf("") }

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

        // 📌 Se envuelve el DropdownMenu en un Column separado
        Column {
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
                                val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                                stateChanges.add(StateChangeRecord(selectedOption, selectedTime, fechaHoy))

                                saveStateChanges(context, selectedOption, selectedTime)

                                Log.d("VALIDACION", "✅ Estado guardado en JSON: $selectedOption a las $selectedTime")

                                expanded = false
                            }
                        )
                    }
                }
            }

            // 📌 Aquí agregamos un Spacer mayor para asegurarnos de que el texto baje
            Spacer(modifier = Modifier.height(80.dp))

            // 📌 Ahora el texto no se solapa con el DropdownMenu
            if (selectedOption.isNotEmpty()) {
                Text(
                    text = "$selectedOption - $selectedTime",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

// 📌 Función para guardar un nuevo estado en el JSON sin modificar los datos del usuario
fun saveStateChanges(context: Context, estado: String, hora: String) {
    try {
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 📌 Verificar si el último archivo contiene "Fuera de Turno"
        val lastFileHadExit = lastTurnHadExit(context)

        // 📌 Si el estado es "En Turno" Y el último turno ya finalizó, iniciar un nuevo archivo
        val fileName = if ((estado == "En Turno" && lastFileHadExit) || lastFileHadExit) {
            "estado_${fechaHoy}_${System.currentTimeMillis()}.json"
        } else {
            getLatestJsonFileName(context) ?: "estado_${fechaHoy}.json"
        }

        val file = File(context.filesDir, fileName)

        // 📌 Cargar datos existentes si el archivo ya existe, sino crear nueva estructura
        val jsonData: MutableMap<String, Any> = if (file.exists()) {
            val existingJson = FileReader(file).use { it.readText() }
            try {
                Gson().fromJson(existingJson, object : TypeToken<MutableMap<String, Any>>() {}.type) ?: mutableMapOf()
            } catch (e: Exception) {
                Log.e("JSON", "❌ ERROR al parsear el JSON existente: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf(
                "usuario" to mapOf(
                    "Identificacion" to (loggedUserId ?: 0),
                    "Nombre" to (loggedUserName ?: "No disponible"),
                    "Ubicacion" to (loggedUserLocation ?: "No seleccionada"),
                    "NumeroOT" to (loggedUserNumeroOT ?: "")
                ),
                fechaHoy to mutableListOf<Map<String, String>>() // 📌 Lista de estados bajo la fecha
            )
        }

        // 📌 Obtener lista de estados en la fecha actual y agregar el nuevo estado
        val estadosDelDia = (jsonData[fechaHoy] as? MutableList<Map<String, String>>)?.toMutableList() ?: mutableListOf()
        estadosDelDia.add(mapOf(estado to hora))

        // 📌 Si el estado es "Fuera de Turno", agregar la fecha exacta de salida
        if (estado == "Fuera de Turno") {
            val fechaSalida = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            jsonData["FechaSalida"] = fechaSalida
        }

        // 📌 Guardar de nuevo los estados en la fecha actual
        jsonData[fechaHoy] = estadosDelDia

        // 📌 Guardar en JSON
        val json = Gson().toJson(jsonData)
        FileWriter(file).use { it.write(json) }

        // 📌 🔥 Mostrar el JSON COMPLETO en el log
        Log.d("JSON", "📂 JSON COMPLETO en archivo $fileName:\n$json")

    } catch (e: Exception) {
        Log.e("JSON", "❌ ERROR al guardar el estado en JSON: ${e.message}")
    }
}




fun lastTurnHadExit(context: Context): Boolean {
    val latestFile = getLatestJsonFileName(context) ?: return false
    val file = File(context.filesDir, latestFile)

    if (!file.exists()) return false

    return try {
        val json = FileReader(file).use { it.readText() }
        val jsonData: MutableMap<String, Any> = Gson().fromJson(json, object : TypeToken<MutableMap<String, Any>>() {}.type) ?: mutableMapOf()

        // 📌 Obtener la fecha más reciente
        val lastDate = jsonData.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.maxOrNull() ?: return false
        val estados = jsonData[lastDate] as? List<Map<String, String>> ?: return false

        // 📌 Verificar si el último estado fue "Fuera de Turno"
        estados.any { it.containsKey("Fuera de Turno") }

    } catch (e: Exception) {
        Log.e("JSON", "❌ ERROR al verificar 'Fuera de Turno': ${e.message}")
        false
    }
}

fun getLatestJsonFileName(context: Context): String? {
    val files = context.filesDir.listFiles { _, name -> name.startsWith("estado_") && name.endsWith(".json") }
    return files?.maxByOrNull { it.lastModified() }?.name
}





