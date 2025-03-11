package com.example.horas_extra_tecnipalma

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

import java.util.Calendar

@Composable
fun ConfigScreen() {

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR).toString()
    val currentMonth = (calendar.get(Calendar.MONTH) + 1).toString()
    // Lista de detalles
    val detalles = listOf("Año", "Mes")
    // Lista mutable para los valores editables
    val valores = remember { mutableStateListOf(currentYear, currentMonth) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Detalle",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Tabla simulada
        detalles.forEachIndexed { index, detalle ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Columna de detalle (Texto estático)
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
                // Campo editable o deshabilitado si es "Año"
                if (index == 0 || index == 1) {
                    // Campo deshabilitado para "Año"
                    TextField(
                        value = valores[index],
                        onValueChange = {},
                        enabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                } else {
                    // Campos editables para los demás detalles
                    TextField(
                        value = valores[index],
                        onValueChange = { valores[index] = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        singleLine = true
                    )
                }
            }
            // Línea divisoria para separar cada fila
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}