package com.example.horas_extra_tecnipalma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Horas_Extra_TecnipalmaTheme {
                MenuScreen()
            }
        }
    }
}

@Composable
fun MenuScreen() {
    var selectedOption by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Imagen en la parte superior
        Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = "Login Image",
            modifier = Modifier
                .size(250.dp)
                .padding(bottom = 32.dp)
        )

        // Botón desplegable
        Box {
            // Botón desplegable
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Seleccionar opción")
            }

            // Menú desplegable que se ancla justo debajo del botón
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("Opción 1") },
                    onClick = {
                        selectedOption = "Opción 1"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Opción 2") },
                    onClick = {
                        selectedOption = "Opción 2"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Opción 3") },
                    onClick = {
                        selectedOption = "Opción 3"
                        expanded = false
                    }
                )
            }
        }


        // Mostrar el texto solo si se ha seleccionado una opción
        if (selectedOption.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = " - $selectedOption",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Horas_Extra_TecnipalmaTheme {
        MenuScreen()
    }
}
