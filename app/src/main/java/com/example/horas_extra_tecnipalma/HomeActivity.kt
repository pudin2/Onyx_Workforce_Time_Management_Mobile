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
    // Crear un estado de la drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }



    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent {}
        },
        drawerState = drawerState
    ) {
        Scaffold(


            topBar = {
                TopAppBar(
                    title = { Text(text = " ") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                // Controlar la apertura/cierre del menú lateral
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

            Column(
                modifier = Modifier
                    .padding(innerPadding)
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
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Divider(color = Color.White.copy(alpha = 0.5f))

            Text(
                text = "Reporte",
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
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