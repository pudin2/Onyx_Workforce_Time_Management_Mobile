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

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay




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
    var elapsedTimeMap by remember { mutableStateOf(mutableMapOf<String, Long>()) }
    var elapsedTime by remember { mutableStateOf(0L) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedOption) {
        if (selectedOption.isNotEmpty()) {
            // Retomar el tiempo del estado seleccionado o iniciar desde 0
            elapsedTime = elapsedTimeMap[selectedOption] ?: 0L

            // Inicia el cronómetro
            while (true) {
                delay(1000L)
                elapsedTime += 1
                elapsedTimeMap[selectedOption] = elapsedTime
            }
        }
    }


    // Función para convertir segundos a formato HH:mm:ss
    fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Selecciona el Estado")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("En Turno") },
                    onClick = {
                        selectedOption = "En Turno"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Pausa Activa") },
                    onClick = {
                        selectedOption = "Pausa Activa"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Almuerzo") },
                    onClick = {
                        selectedOption = "Almuerzo"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Descanso") },
                    onClick = {
                        selectedOption = "Descanso"
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Fuera de Turno") },
                    onClick = {
                        selectedOption = "Fuera de Turno"
                        expanded = false
                    }
                )
            }
        }

        // Mostrar el texto seleccionado
        if (selectedOption.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Estado: $selectedOption",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar el cronómetro en formato HH:mm:ss
            Text(
                text = "Tiempo: ${formatTime(elapsedTime)}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
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