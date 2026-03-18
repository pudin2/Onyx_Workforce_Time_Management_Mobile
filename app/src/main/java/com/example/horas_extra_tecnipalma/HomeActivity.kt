package com.example.horas_extra_tecnipalma

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.launch

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StateChangeRecord(
    val estado: String,
    val hora: String,
    val fecha: String,
    val latitud: Double?,
    val longitud: Double?,
    val operarios: List<String> = emptyList()
)

fun necesitaLogin(context: Context): Boolean {
    val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
    val ultimoEstado = prefs.getString("ultimoEstado", "")
    return ultimoEstado == "Fuera de Turno"
}

fun mostrarNotificacionMarcacion(context: Context, estado: String) {
    Toast.makeText(
        context,
        "Ubicación reportada en la marcación: $estado",
        Toast.LENGTH_SHORT
    ).show()
}

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (necesitaLogin(this)) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

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
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DrawerContent(onItemSelected: (String) -> Unit) {
    BoxWithConstraints {
        val fontSize = when {
            maxWidth < 360.dp -> 16.sp
            maxWidth < 600.dp -> 18.sp
            else -> 24.sp
        }

        val padding = when {
            maxWidth < 360.dp -> 8.dp
            maxWidth < 600.dp -> 16.dp
            else -> 32.dp
        }

        val verticalArrangement = when {
            maxWidth < 600.dp -> Arrangement.Center
            else -> Arrangement.Top
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DrawerItem("Página Principal", onClick = { onItemSelected("home") }, fontSize, padding)
                DrawerItem("Reporte", onClick = { onItemSelected("reporte") }, fontSize, padding)
                DrawerItem("Configuración", onClick = { onItemSelected("config") }, fontSize, padding)
            }
        }
    }
}

@Composable
fun DrawerItem(text: String, onClick: () -> Unit, fontSize: TextUnit, padding: Dp) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(all = padding),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(context: Context) {
    val selectedOption = rememberSaveable { mutableStateOf("") }
    val selectedTime = rememberSaveable { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val stateChanges = remember { mutableStateListOf<StateChangeRecord>() }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(200.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                DrawerContent { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route)
                    }
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Menu",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
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
                        selectedTime = selectedTime.value,
                        onOptionChosen = { opcion, hora ->
                            selectedOption.value = opcion
                            selectedTime.value = hora
                        }
                    )
                }
                composable("reporte") { ReporteScreen() }
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

    fun requiereOperariosPorCargo(cargo: String?): Boolean {
        val cargoNorm = normalizaNombre(cargo ?: "")
        return Regex("\\bSUPERVISOR\\b").containsMatchIn(cargoNorm)
    }

    val requiereOperarios = requiereOperariosPorCargo(loggedUserCargo)
    var pendingEstado by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingHora by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var pendingLon by rememberSaveable { mutableStateOf<Double?>(null) }
    val operariosPendientes = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (requiereOperarios && pendingEstado != null && pendingHora != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            val estado = pendingEstado!!
                            val hora = pendingHora!!

                            stateChanges.add(
                                StateChangeRecord(
                                    estado = estado,
                                    hora = hora,
                                    fecha = fechaHoy,
                                    latitud = pendingLat,
                                    longitud = pendingLon,
                                    operarios = operariosPendientes.toList()
                                )
                            )

                            saveStateChanges(
                                context = context,
                                estado = estado,
                                hora = hora,
                                latitud = pendingLat,
                                longitud = pendingLon,
                                operarios = operariosPendientes.toList()
                            )

                            mostrarNotificacionMarcacion(context, estado)

                            pendingEstado = null
                            pendingHora = null
                            pendingLat = null
                            pendingLon = null
                            operariosPendientes.clear()
                        }
                    ) {
                        Text("Guardar estado")
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(scrollState)
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

            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedOption.isNotEmpty()) {
                            "Estado: $selectedOption"
                        } else {
                            "Selecciona el Estado"
                        }
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    listOf("En Turno", "Alimentacion", "Fuera de Turno")
                        .forEach { state ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = state,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    val horaActual = getCurrentTime()
                                    scope.launch {
                                        var lat: Double? = null
                                        var lon: Double? = null

                                        try {
                                            val ubicacion = context.getLastKnownLocation()
                                            if (ubicacion != null) {
                                                lat = ubicacion.latitude
                                                lon = ubicacion.longitude
                                            }
                                        } catch (_: Exception) {
                                        }

                                        pendingEstado = state
                                        pendingHora = horaActual
                                        pendingLat = lat
                                        pendingLon = lon

                                        onOptionChosen(state, horaActual)

                                        operariosPendientes.clear()

                                        if (requiereOperarios) {
                                            operariosPendientes.addAll(getUltimosOperariosDesdeUltimoJson(context))
                                        } else {
                                            val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

                                            stateChanges.add(
                                                StateChangeRecord(
                                                    estado = state,
                                                    hora = horaActual,
                                                    fecha = fechaHoy,
                                                    latitud = lat,
                                                    longitud = lon,
                                                    operarios = emptyList()
                                                )
                                            )

                                            saveStateChanges(
                                                context = context,
                                                estado = state,
                                                hora = horaActual,
                                                latitud = lat,
                                                longitud = lon,
                                                operarios = emptyList()
                                            )

                                            mostrarNotificacionMarcacion(context, state)

                                            pendingEstado = null
                                            pendingHora = null
                                            pendingLat = null
                                            pendingLon = null
                                        }

                                        expanded = false
                                    }
                                }
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedOption.isNotEmpty()) {
                Text(
                    text = "$selectedOption - $selectedTime",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (requiereOperarios && pendingEstado != null && pendingHora != null) {
                OperariosSection(
                    operarios = operariosPendientes,
                    enabled = true
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun OperariosSection(
    operarios: MutableList<String>,
    enabled: Boolean,
) {
    var nuevoOperario by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Operarios (opcional)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = nuevoOperario,
                onValueChange = { nuevoOperario = it.filter { ch -> ch.isDigit() } },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                label = { Text("Cedula del Operario") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val limpio = nuevoOperario.trim()
                    if (limpio.isNotEmpty()) {
                        operarios.add(limpio)
                        nuevoOperario = ""
                    }
                },
                enabled = enabled
            ) {
                Text("Agregar")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (operarios.isEmpty()) {
            Text(
                text = "Puedes guardar la marcación sin agregar operarios.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
            ) {
                items(operarios.size) { index ->
                    val nombre = operarios[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $nombre",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { operarios.removeAt(index) },
                            enabled = enabled
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar operario",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

fun saveStateChanges(
    context: Context,
    estado: String,
    hora: String,
    latitud: Double?,
    longitud: Double?,
    operarios: List<String>
) {
    try {
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastFileHadExit = lastTurnHadExit(context)

        val fileName = if ((estado == "En Turno" && lastFileHadExit) || lastFileHadExit) {
            "estado_${fechaHoy}_${System.currentTimeMillis()}.json"
        } else {
            getLatestJsonFileName(context) ?: "estado_${fechaHoy}_${System.currentTimeMillis()}.json"
        }

        val file = File(context.filesDir, fileName)

        val jsonData: MutableMap<String, Any> = if (file.exists()) {
            val existingJson = FileReader(file).use { it.readText() }
            try {
                Gson().fromJson(existingJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
                    ?: mutableMapOf()
            } catch (e: Exception) {
                FileLogger.e("JSON", "ERROR al parsear el JSON existente: ${e.message}")
                mutableMapOf()
            }
        } else {
            mutableMapOf(
                "usuario" to mapOf(
                    "Identificacion" to (loggedUserId?.toString() ?: ""),
                    "Nombre" to (loggedUserName ?: "No disponible"),
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
            "longitud" to (longitud ?: ""),
            "operarios" to operarios
        )

        estadosDelDia.add(registro)

        if (estado == "Fuera de Turno") {
            val fechaSalida = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            jsonData["FechaSalida"] = fechaSalida
        }

        jsonData[fechaHoy] = estadosDelDia
        val json = Gson().toJson(jsonData)
        FileWriter(file).use { it.write(json) }

        FileLogger.d("JSON", "JSON COMPLETO en archivo $fileName:\n$json")

        val prefs = context.getSharedPreferences("horas_extra_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("ultimoEstado", estado)
            .apply()

    } catch (e: Exception) {
        FileLogger.e("JSON", "ERROR al guardar el estado en JSON: ${e.message}")
    }
}

fun lastTurnHadExit(context: Context): Boolean {
    val latestFile = getLatestJsonFileName(context) ?: return false
    val file = File(context.filesDir, latestFile)

    if (!file.exists()) return false

    return try {
        val json = FileReader(file).use { it.readText() }
        val jsonData: MutableMap<String, Any> =
            Gson().fromJson(json, object : TypeToken<MutableMap<String, Any>>() {}.type) ?: mutableMapOf()

        val lastDate = jsonData.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.maxOrNull() ?: return false
        val estados = jsonData[lastDate] as? List<Map<String, String>> ?: return false

        estados.any { registro ->
            (registro["estado"] as? String) == "Fuera de Turno"
        }

    } catch (e: Exception) {
        FileLogger.e("JSON", "ERROR al verificar 'Fuera de Turno': ${e.message}")
        false
    }
}

fun getLatestJsonFileName(context: Context): String? {
    val files = context.filesDir.listFiles { _, name ->
        name.startsWith("estado_") && name.endsWith(".json")
    }
    return files?.maxByOrNull { it.lastModified() }?.name
}

fun getUltimosOperariosDesdeUltimoJson(context: Context): List<String> {
    val latestFile = getLatestJsonFileName(context) ?: return emptyList()
    val file = File(context.filesDir, latestFile)
    if (!file.exists()) return emptyList()

    return try {
        val json = FileReader(file).use { it.readText() }
        val jsonData: Map<String, Any> =
            Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type) ?: return emptyList()
        val lastDate = jsonData.keys
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .maxOrNull() ?: return emptyList()
        val estados = jsonData[lastDate] as? List<Map<String, Any>> ?: return emptyList()

        if (estados.isEmpty()) return emptyList()

        val lastRegistro = estados.last()
        val lastEstado = (lastRegistro["estado"] as? String)?.trim()

        if (lastEstado.equals("Fuera de Turno", ignoreCase = true)) {
            return emptyList()
        }

        val lastWithOps = estados.asReversed().firstOrNull { it["operarios"] != null } ?: return emptyList()

        when (val opsAny = lastWithOps["operarios"]) {
            is List<*> -> opsAny
                .mapNotNull { it?.toString()?.trim() }
                .filter { it.isNotEmpty() }
            else -> emptyList()
        }
    } catch (e: Exception) {
        FileLogger.e("JSON", "Error leyendo últimos operarios: ${e.message}")
        emptyList()
    }
}

fun normalizaNombre(s: String): String =
    s.trim().replace(Regex("\\s+"), " ").uppercase()