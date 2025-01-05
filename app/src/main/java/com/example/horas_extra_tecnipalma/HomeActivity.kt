package com.example.horas_extra_tecnipalma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.horas_extra_tecnipalma.ui.theme.Horas_Extra_TecnipalmaTheme

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Horas_Extra_TecnipalmaTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to Home Screen!",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* TODO: Add navigation or action */ }) {
                    Text("Go Back")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Horas_Extra_TecnipalmaTheme {
        HomeScreen()
    }
}
