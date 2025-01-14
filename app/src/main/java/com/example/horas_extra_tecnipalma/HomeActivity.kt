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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color


import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items



val stateChanges = mutableStateListOf<StateChangeRecord>()

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

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
                composable("home") { MenuContent() }
                composable("reporte") { ReporteScreen() }
                composable("config") { ConfigScreen() }
            }
        }
    }
}

@Composable
fun MenuContent() {
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }  // ✅ Declaración de selectedTime


    // Función para obtener la hora actual en formato HH:mm:ss
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
        // Imagen en la parte superior
        Image(
            painter = painterResource(id = R.drawable.logor1),
            contentDescription = "Login Image",
            modifier = Modifier
                .size(250.dp)
                .padding(bottom = 32.dp)
        )

        // Botón desplegable
        Box {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedOption.isNotEmpty()) {
                        "Estado: $selectedOption - $selectedTime"
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

                            // Actualizar o agregar el estado con la nueva hora
                            val existingRecord = stateChanges.find { it.estado == state }
                            if (existingRecord != null) {
                                stateChanges[stateChanges.indexOf(existingRecord)] =
                                    StateChangeRecord(state, selectedTime)
                            } else {
                                stateChanges.add(StateChangeRecord(state, selectedTime))
                            }

                            expanded = false
                        }
                    )
                }
            }
        }

        // Mostrar el texto del estado seleccionado
        if (selectedOption.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$selectedOption\n$selectedTime",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun DrawerContent(onItemSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1BA1BF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo2), // Reemplaza con tu imagen
                contentDescription = "Imagen de perfil",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Página Principal",
                color = Color.White,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { onItemSelected("home") }
            )

            Divider(color = Color.White.copy(alpha = 0.5f))

            Text(
                text = "Reporte",
                color = Color.White,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { onItemSelected("reporte") }
            )

            Divider(color = Color.White.copy(alpha = 0.5f))

            Text(
                text = "Detalle",
                color = Color.White,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .clickable { onItemSelected("config") }
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