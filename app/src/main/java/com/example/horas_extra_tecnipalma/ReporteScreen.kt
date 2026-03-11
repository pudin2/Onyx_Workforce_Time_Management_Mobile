package com.example.horas_extra_tecnipalma

import android.content.Context

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val formatoPantalla = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var todosLosRegistros by remember { mutableStateOf(emptyList<StateChangeRecord>()) }
    var registrosFiltrados by remember { mutableStateOf(emptyList<StateChangeRecord>()) }

    var fechaDesde by remember { mutableStateOf("") }
    var fechaHasta by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    var mostrarDatePickerDesde by remember { mutableStateOf(false) }
    var mostrarDatePickerHasta by remember { mutableStateOf(false) }

    fun recargarReporte() {
        val registros = cargarRegistrosUltimos30Dias(context)
        todosLosRegistros = registros
        registrosFiltrados = registros
    }

    fun aplicarFiltro() {
        mensajeError = null

        if (fechaDesde.isBlank() && fechaHasta.isBlank()) {
            registrosFiltrados = todosLosRegistros
            return
        }

        if (fechaDesde.isBlank() || fechaHasta.isBlank()) {
            mensajeError = "Debes seleccionar ambas fechas o dejar ambas vacías."
            return
        }

        try {
            val desde = formatoPantalla.parse(fechaDesde)
            val hasta = formatoPantalla.parse(fechaHasta)

            if (desde == null || hasta == null) {
                mensajeError = "No fue posible interpretar las fechas seleccionadas."
                return
            }

            if (desde.after(hasta)) {
                mensajeError = "La fecha desde no puede ser mayor que la fecha hasta."
                return
            }

            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            val hace30Dias = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            if (desde.before(hace30Dias) || hasta.after(hoy)) {
                mensajeError = "Solo puedes consultar fechas dentro de los últimos 30 días."
                return
            }

            registrosFiltrados = todosLosRegistros.filter { record ->
                try {
                    val fechaRegistro = formatoPantalla.parse(record.fecha)
                    fechaRegistro != null &&
                            !fechaRegistro.before(desde) &&
                            !fechaRegistro.after(hasta)
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            mensajeError = "Ocurrió un error al aplicar el filtro."
        }
    }

    LaunchedEffect(Unit) {
        recargarReporte()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                recargarReporte()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (mostrarDatePickerDesde) {
        val hoyMillis = remember { System.currentTimeMillis() }
        val hace30DiasMillis = remember {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = hoyMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in hace30DiasMillis..hoyMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDatePickerDesde = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            fechaDesde = formatoPantalla.format(Date(selectedMillis))
                        }
                        mostrarDatePickerDesde = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDatePickerDesde = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (mostrarDatePickerHasta) {
        val hoyMillis = remember { System.currentTimeMillis() }
        val hace30DiasMillis = remember {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = hoyMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in hace30DiasMillis..hoyMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { mostrarDatePickerHasta = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            fechaHasta = formatoPantalla.format(Date(selectedMillis))
                        }
                        mostrarDatePickerHasta = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDatePickerHasta = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Consulta de reportes",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        CampoFechaVisual(
            titulo = "Fecha desde",
            valor = fechaDesde,
            onClick = { mostrarDatePickerDesde = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        CampoFechaVisual(
            titulo = "Fecha hasta",
            valor = fechaHasta,
            onClick = { mostrarDatePickerHasta = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { aplicarFiltro() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Buscar")
            }

            OutlinedButton(
                onClick = {
                    fechaDesde = ""
                    fechaHasta = ""
                    mensajeError = null
                    registrosFiltrados = todosLosRegistros
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Limpiar")
            }
        }

        if (mensajeError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mensajeError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Fecha",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp
            )
            Text(
                text = "Estado",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp
            )
            Text(
                text = "Hora",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp
            )
        }

        if (registrosFiltrados.isEmpty()) {
            Text(
                text = "No hay registros para el rango consultado",
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            LazyColumn {
                items(registrosFiltrados) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = record.fecha,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = record.estado,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = record.hora,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}


fun cargarRegistrosUltimos30Dias(context: Context): List<StateChangeRecord> {
    val gson = Gson()
    val formatoJson = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val formatoPantalla = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val hoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val hace30Dias = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -30)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val archivos = context.filesDir.listFiles { file ->
        file.isFile && file.name.startsWith("estado_") && file.name.endsWith(".json")
    } ?: return emptyList()

    val registros = mutableListOf<StateChangeRecord>()

    archivos.forEach { file ->
        try {
            val json = FileReader(file).use { it.readText() }

            val tipo = object : TypeToken<Map<String, Any>>() {}.type
            val jsonData: Map<String, Any> = gson.fromJson(json, tipo) ?: emptyMap()

            val clavesFecha = jsonData.keys.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }

            clavesFecha.forEach { fechaKey ->
                try {
                    val fechaDate = formatoJson.parse(fechaKey) ?: return@forEach

                    if (fechaDate.before(hace30Dias.time) || fechaDate.after(hoy.time)) {
                        return@forEach
                    }

                    val listaEstados = jsonData[fechaKey] as? List<*> ?: return@forEach

                    listaEstados.forEach { item ->
                        val registroMap = item as? Map<*, *> ?: return@forEach

                        val estado = registroMap["estado"]?.toString() ?: ""
                        val hora = registroMap["hora"]?.toString() ?: ""

                        val latitud = (registroMap["latitud"] as? Number)?.toDouble()
                            ?: registroMap["latitud"]?.toString()?.toDoubleOrNull()

                        val longitud = (registroMap["longitud"] as? Number)?.toDouble()
                            ?: registroMap["longitud"]?.toString()?.toDoubleOrNull()

                        val operarios = when (val ops = registroMap["operarios"]) {
                            is List<*> -> ops.mapNotNull { it?.toString() }
                            else -> emptyList()
                        }

                        registros.add(
                            StateChangeRecord(
                                estado = estado,
                                hora = hora,
                                fecha = formatoPantalla.format(fechaDate),
                                latitud = latitud,
                                longitud = longitud,
                                operarios = operarios
                            )
                        )
                    }
                } catch (e: Exception) {
                    FileLogger.e("REPORTE", "Error procesando fecha $fechaKey en ${file.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            FileLogger.e("REPORTE", "Error leyendo archivo ${file.name}: ${e.message}")
        }
    }

    return registros.sortedWith(
        compareByDescending<StateChangeRecord> {
            try {
                formatoPantalla.parse(it.fecha)
            } catch (e: Exception) {
                Date(0)
            }
        }.thenByDescending { it.hora }
    )
}


@Composable
fun CampoFechaVisual(
    titulo: String,
    valor: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (valor.isBlank()) "Seleccionar fecha" else valor,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}