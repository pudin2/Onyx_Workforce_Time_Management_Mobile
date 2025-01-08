package com.example.horas_extra_tecnipalma

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReporteScreen() {
    val records = listOf(
        ReportRecord("En Turno", "2025-01-08", "08:00", "12:00"),
        ReportRecord("Pausa Activa", "2025-01-08", "12:00", "12:30"),
        ReportRecord("Almuerzo", "2025-01-08", "12:30", "13:30"),
        ReportRecord("Descanso", "2025-01-08", "15:00", "15:15"),
        ReportRecord("En Turno", "2025-01-08", "08:00", "12:00"),
        ReportRecord("Pausa Activa", "2025-01-08", "12:00", "12:30"),
        ReportRecord("Almuerzo", "2025-01-08", "12:30", "13:30"),
        ReportRecord("Descanso", "2025-01-08", "15:00", "15:15")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        //Image(
        //       painter = painterResource(id = R.drawable.logor1),
        //     contentDescription = "Login Image",
        //     modifier = Modifier
        //        .fillMaxWidth()
        //        .size(250.dp)
        //       .padding(bottom = 16.dp)
        // )

        Text(
            text = "Reporte de Estados",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Encabezados de la tabla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1BA1BF)) // Color específico para el encabezado
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Estado", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(text = "Fecha", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(text = "Hora Inicio", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(text = "Hora Salida", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(records) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = record.estado, modifier = Modifier.weight(1f))
                    Text(text = record.fecha, modifier = Modifier.weight(1f))
                    Text(text = record.horaInicio, modifier = Modifier.weight(1f))
                    Text(text = record.horaSalida, modifier = Modifier.weight(1f))
                }
                Divider(color = Color.LightGray)
            }
        }
    }
}

data class ReportRecord(val estado: String, val fecha: String, val horaInicio: String, val horaSalida: String)
