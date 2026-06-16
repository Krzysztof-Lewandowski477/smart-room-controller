package com.kris.smartroomcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LedControlScreen()
        }
    }
}

@Composable
fun LedControlScreen() {
    var ledOn by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(128f) }

    var red by remember { mutableStateOf(255f) }
    var green by remember { mutableStateOf(0f) }
    var blue by remember { mutableStateOf(0f) }

    val previewColor = Color(
        red = red.toInt(),
        green = green.toInt(),
        blue = blue.toInt()
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101010))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Smart Room Controller",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "MQTT: tryb testowy",
                color = Color.Yellow
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(previewColor, RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (ledOn) "LED: WŁĄCZONE" else "LED: WYŁĄCZONE",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = ledOn,
                    onCheckedChange = { ledOn = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Jasność: ${brightness.toInt()}", color = Color.White)
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = 0f..255f
            )

            Text("Czerwony: ${red.toInt()}", color = Color.White)
            Slider(
                value = red,
                onValueChange = { red = it },
                valueRange = 0f..255f
            )

            Text("Zielony: ${green.toInt()}", color = Color.White)
            Slider(
                value = green,
                onValueChange = { green = it },
                valueRange = 0f..255f
            )

            Text("Niebieski: ${blue.toInt()}", color = Color.White)
            Slider(
                value = blue,
                onValueChange = { blue = it },
                valueRange = 0f..255f
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    ledOn = true
                    brightness = 220f
                    red = 255f
                    green = 230f
                    blue = 180f
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scena: Praca")
            }

            Button(
                onClick = {
                    ledOn = true
                    brightness = 40f
                    red = 20f
                    green = 20f
                    blue = 120f
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scena: Noc")
            }

            Button(
                onClick = {
                    ledOn = true
                    brightness = 180f
                    red = 180f
                    green = 0f
                    blue = 255f
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Scena: Studio")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = """
                    {
                      "power": $ledOn,
                      "brightness": ${brightness.toInt()},
                      "r": ${red.toInt()},
                      "g": ${green.toInt()},
                      "b": ${blue.toInt()}
                    }
                """.trimIndent(),
                color = Color.LightGray
            )
        }
    }
}