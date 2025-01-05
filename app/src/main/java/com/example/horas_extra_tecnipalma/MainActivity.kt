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
    var isRegisterMode by remember { mutableStateOf(false) } // Modo de registro o login

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Campo de entrada para el nombre de usuario
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Campo de entrada para la contraseña
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Botón de acción (Registrar o Iniciar Sesión)
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            // Registrar el usuario
                            val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            sharedPreferences.edit().apply {
                                putString("username", username)
                                putString("password", password)
                                apply()
                            }
                            Toast.makeText(context, "User Registered!", Toast.LENGTH_SHORT).show()
                            isRegisterMode = false // Cambiar a modo login
                        } else {
                            // Validar las credenciales
                            val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            val savedUsername = sharedPreferences.getString("username", null)
                            val savedPassword = sharedPreferences.getString("password", null)

                            if (username == savedUsername && password == savedPassword) {
                                // Navegar a HomeActivity
                                val intent = Intent(context, HomeActivity::class.java)
                                context.startActivity(intent)
                            } else {
                                // Mostrar mensaje de error
                                Toast.makeText(context, "Invalid Username or Password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) "Register" else "Login")
                }

                // Botón para cambiar entre los modos de registro y login
                TextButton(
                    onClick = { isRegisterMode = !isRegisterMode }
                ) {
                    Text(if (isRegisterMode) "Already have an account? Login" else "Don't have an account? Register")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Horas_Extra_TecnipalmaTheme {
        LoginScreen(context = MainActivity())
    }
}
