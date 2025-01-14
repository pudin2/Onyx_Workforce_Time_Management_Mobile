package com.example.horas_extra_tecnipalma

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
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp


@Composable
fun ReporteScreen() {
    // Obtener la fecha de hoy en formato "dd/MM/yyyy"
    val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Encabezado de la tabla
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1BA1BF))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Estado",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp
            )
            Text(
                text = "Fecha",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp
            )
            Text(
                text = "Hora",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp
            )
        }

        // Tabla de historial
        LazyColumn {
            items(stateChanges) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()

                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = record.estado,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = fechaHoy,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    ) // Fecha de hoy

                    Text(text = record.hora,
                        textAlign = TextAlign.Center, 
                        modifier = Modifier.weight(1f)
                    )
                }
                Divider(color = Color.LightGray)
            }
        }
    }
}


data class StateChangeRecord(val estado: String, val hora: String)
