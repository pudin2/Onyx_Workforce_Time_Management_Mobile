package com.example.horas_extra_tecnipalma

import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun ReporteScreen(stateChanges: List<StateChangeRecord>) {
    // 📌 Obtener la fecha actual en formato "dd/MM/yyyy"
    val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    // 📌 Filtrar solo los registros que coincidan con la fecha actual
    val registrosDelDia = stateChanges.filter { it.fecha == fechaHoy }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 📌 Encabezado de la tabla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1BA1BF))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Estado", fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), fontSize = 18.sp)
            Text("Hora", fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), fontSize = 18.sp)
        }

        // 📌 Si no hay registros del día, mostrar un mensaje
        if (registrosDelDia.isEmpty()) {
            Text("No hay registros hoy", modifier = Modifier.padding(16.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        } else {
            LazyColumn {
                items(registrosDelDia) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(text = record.estado, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        Text(text = record.hora, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                    Divider(color = Color.LightGray)
                }
            }
        }
    }
}