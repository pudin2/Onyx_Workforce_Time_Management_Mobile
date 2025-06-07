package com.example.horas_extra_tecnipalma

import android.content.Context
import android.content.Intent//
import android.os.Bundle

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

data class StateChangeRecord(val estado: String, val hora: String, val fecha: String, val latitud: Double?, val longitud: Double?)

fun necesitaLogin(context: Context): Boolean {
    val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
    val ultimoEstado = prefs.getString("ultimoEstado", "")
    return ultimoEstado == "Fuera de Turno"
}//nueva

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (necesitaLogin(this)) {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }//nuevo

        setContent {
            Horas_Extra_TecnipalmaTheme {
                MenuScreen(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (necesitaLogin(this)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }//nuevo
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

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
    val selectedOption = rememberSaveable { mutableStateOf("") }
    val selectedTime   = rememberSaveable { mutableStateOf("") }

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
                composable("home") {
                    MenuContent(
                        context = context,
                        stateChanges = stateChanges,
                        selectedOption = selectedOption.value,
                        selectedTime   = selectedTime.value,
                        onOptionChosen = { opción, hora ->
                            selectedOption.value = opción
                            selectedTime.value   = hora
                        }
                    )
                }
                composable("reporte") { ReporteScreen(stateChanges) }
                composable("config") { ConfigScreen() }
            }
        }
    }
}

@Composable
fun MenuContent(
    context: Context,
    stateChanges: MutableList<StateChangeRecord>,
    selectedOption: String,
    selectedTime: String,
    onOptionChosen: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()//nuevo

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
            contentDescription = "Logo",
            modifier = Modifier
                .size(250.dp)
                .padding(bottom = 32.dp)
        )

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
                    listOf("En Turno", "Pausa Activa", "Almuerzo", "Descanso", "Fuera de Turno")
                        .forEach { state ->
                            DropdownMenuItem(
                                text = { Text(text = state) },
                                onClick = {
                                    val horaActual = getCurrentTime()
                                    val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        .format(Date())

                                    scope.launch {
                                        var lat: Double? = null
                                        var lon: Double? = null
                                        try {
                                            val ubicacion = context.getLastKnownLocation()
                                            if (ubicacion != null) {
                                                lat = ubicacion.latitude
                                                lon = ubicacion.longitude
                                            }
                                        } catch (e: Exception) {
                                            FileLogger.e("LOCATION", "❌ Error al obtener ubicación: ${e.message}")
                                        }

                                        onOptionChosen(state, horaActual)
                                        stateChanges.add(
                                            StateChangeRecord(
                                                estado = state,
                                                hora = horaActual,
                                                fecha = fechaHoy,
                                                latitud = lat,
                                                longitud = lon
                                            )
                                        )

                                        saveStateChanges(
                                            context = context,
                                            estado = state,
                                            hora = horaActual,
                                            latitud = lat,
                                            longitud = lon
                                        )
                                        FileLogger.d(
                                            "VALIDACION",
                                            "✅ Estado guardado en JSON: $state a las $horaActual (lat=$lat, lon=$lon)"
                                        )

                                        expanded = false
                                    }
                                }
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            if (selectedOption.isNotEmpty()) {
                Text(
                    text = "$selectedOption - $selectedTime",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

fun saveStateChanges(
    context: Context,
    estado: String,
    hora: String,
    latitud: Double?,
    longitud: Double?
) {
    try {
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastFileHadExit = lastTurnHadExit(context)

        val fileName = if ((estado == "En Turno" && lastFileHadExit) || lastFileHadExit) {
            "estado_${fechaHoy}_${System.currentTimeMillis()}.json"
        } else {
            getLatestJsonFileName(context) ?: "estado_${fechaHoy}.json"
        }

        val file = File(context.filesDir, fileName)

        val jsonData: MutableMap<String, Any> = if (file.exists()) {
            val existingJson = FileReader(file).use { it.readText() }
            try {
                Gson().fromJson(existingJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
                    ?: mutableMapOf()
            } catch (e: Exception) {
                FileLogger.e("JSON", "❌ ERROR al parsear el JSON existente: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf(
                "usuario" to mapOf(
                    "Identificacion" to (loggedUserId?.toString() ?: ""),
                    "Nombre" to (loggedUserName ?: "No disponible"),
                    "Ubicacion" to (loggedUserLocation ?: "No seleccionada"),
                    "NumeroOT" to (loggedUserNumeroOT ?: "")
                ),
                fechaHoy to mutableListOf<Map<String, Any>>()
            )
        }

        val estadosDelDia = (jsonData[fechaHoy] as? MutableList<Map<String, Any>>)?.toMutableList()
            ?: mutableListOf()

        val registro: MutableMap<String, Any> = mutableMapOf(
            "estado" to estado,
            "hora" to hora,
            "latitud" to (latitud ?: ""),
            "longitud" to (longitud ?: "")
        )//nuevo

        estadosDelDia.add(registro)//nuevo

        if (estado == "Fuera de Turno") {
            val fechaSalida = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            jsonData["FechaSalida"] = fechaSalida
        }

        jsonData[fechaHoy] = estadosDelDia
        val json = Gson().toJson(jsonData)
        FileWriter(file).use { it.write(json) }

        FileLogger.d("JSON", "📂 JSON COMPLETO en archivo $fileName:\n$json")

        // ─── GUARDAMOS EN SharedPreferences EL ULTIMO ESTADO ───
        val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("ultimoEstado", estado)
            .apply()//nuevo

    } catch (e: Exception) {
        FileLogger.e("JSON", "❌ ERROR al guardar el estado en JSON: ${e.message}")
    }
}

fun lastTurnHadExit(context: Context): Boolean {
    val latestFile = getLatestJsonFileName(context) ?: return false
    val file = File(context.filesDir, latestFile)

    if (!file.exists()) return false

    return try {
        val json = FileReader(file).use { it.readText() }
        val jsonData: MutableMap<String, Any> = Gson().fromJson(json, object : TypeToken<MutableMap<String, Any>>() {}.type) ?: mutableMapOf()

        val lastDate = jsonData.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.maxOrNull() ?: return false
        val estados = jsonData[lastDate] as? List<Map<String, String>> ?: return false

        estados.any { registro ->
            (registro["estado"] as? String) == "Fuera de Turno"
        }

    } catch (e: Exception) {
        FileLogger.e("JSON", "❌ ERROR al verificar 'Fuera de Turno': ${e.message}")
        false
    }
}

fun getLatestJsonFileName(context: Context): String? {
    val files = context.filesDir.listFiles { _, name -> name.startsWith("estado_") && name.endsWith(".json") }
    return files?.maxByOrNull { it.lastModified() }?.name
}





