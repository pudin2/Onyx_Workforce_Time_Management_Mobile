package com.example.horas_extra_tecnipalma

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme





class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Horas_Extra_TecnipalmaTheme {
                LoginScreen(this)
            }
        }
    }
}

@Composable
fun LoginScreen(context: Context) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }


    var isRegisterMode by remember { mutableStateOf(false) }
    var showTextField by remember { mutableStateOf(false) }
    var additionalInfo by remember { mutableStateOf("") }



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center, // Centra los elementos verticalmente
                horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos horizontalmente
            ) {

                Image(
                    painter = painterResource(id = R.drawable.logor1),
                    contentDescription = "Login Image",
                    modifier = Modifier
                        .size(250.dp) // Ajusta el tamaño de la imagen
                        .padding(bottom = 16.dp) // Espacio entre la imagen y los campos de texto
                )

                // Campo de entrada para el nombre de usuario
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Campo de entrada para la contraseña
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Botón interactivo para seleccionar la ubicación
                DropdownMenuExample(
                    selectedLocation = selectedLocation,
                    onLocationSelected = {
                        selectedLocation = it
                        showTextField = (it == "Montaje")
                    }
                )

                // Mostrar la caja de texto adicional si se selecciona "Montaje"
                if (showTextField) {
                    TextField(
                        value = additionalInfo,
                        onValueChange = { additionalInfo = it },
                        label = { Text("Número de OT") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }

                // Botón de login
                Button(
                    onClick = {
                        if (username == "admin" && password == "1234") {
                            // Navegar a HomeActivity si las credenciales son correctas
                            val intent = Intent(context, HomeActivity::class.java)
                            context.startActivity(intent)
                        } else {
                            // Mostrar mensaje de error si las credenciales son incorrectas
                            Toast.makeText(context, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar")
                }
            }
        }
    )
}

@Composable
fun DropdownMenuExample(selectedLocation: String, onLocationSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // Botón que despliega el menú
    Button(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1BA1BF), // Color personalizado (Hexadecimal)
            contentColor = Color.White // Texto blanco
        )
    ) {
        Text(text = "Ubicación: $selectedLocation")
    }

    // Menú desplegable con las opciones
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth()
    ) {
        DropdownMenuItem(
            text = { Text("Planta") },
            onClick = {
                onLocationSelected("Planta")
                expanded = false
            }
        )
        DropdownMenuItem(
            text = { Text("Montaje") },
            onClick = {
                onLocationSelected("Montaje")
                expanded = false
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Horas_Extra_TecnipalmaTheme {
        LoginScreen(context = MainActivity())
    }
}
