package com.kris.smartroomcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartRoomControllerApp()
        }
    }
}

@Composable
fun SmartRoomControllerApp() {
    var mode by remember { mutableStateOf("manual") }
    var selectedZone by remember { mutableStateOf("main_led") }

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

    val jsonPayload = buildLedPayload(
        mode = mode,
        zone = selectedZone,
        power = ledOn,
        brightness = brightness.toInt(),
        red = red.toInt(),
        green = green.toInt(),
        blue = blue.toInt()
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101010))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Smart Room Controller",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "MQTT: tryb testowy / payload gotowy",
                color = Color.Yellow
            )

            ColorPreview(previewColor = previewColor)

            MainLedCard(
                ledOn = ledOn,
                brightness = brightness,
                red = red,
                green = green,
                blue = blue,
                onPowerChange = {
                    ledOn = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onBrightnessChange = {
                    brightness = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onRedChange = {
                    red = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onGreenChange = {
                    green = it
                    mode = "manual"
                    selectedZone = "main_led"
                },
                onBlueChange = {
                    blue = it
                    mode = "manual"
                    selectedZone = "main_led"
                }
            )

            SleepModeCard(
                onUnderBed = {
                    mode = "sleep_under_bed"
                    selectedZone = "under_bed"
                    ledOn = true
                    brightness = 8f
                    red = 255f
                    green = 35f
                    blue = 0f
                },
                onThreshold = {
                    mode = "sleep_threshold"
                    selectedZone = "threshold"
                    ledOn = true
                    brightness = 25f
                    red = 255f
                    green = 75f
                    blue = 0f
                },
                onReadingLamp = {
                    mode = "reading_lamp"
                    selectedZone = "reading_lamp"
                    ledOn = true
                    brightness = 70f
                    red = 255f
                    green = 110f
                    blue = 20f
                },
                onFullSleep = {
                    mode = "sleep_full"
                    selectedZone = "all_sleep_zones"
                    ledOn = true
                    brightness = 15f
                    red = 255f
                    green = 50f
                    blue = 0f
                },
                onAllOff = {
                    mode = "all_off"
                    selectedZone = "all"
                    ledOn = false
                    brightness = 0f
                    red = 0f
                    green = 0f
                    blue = 0f
                }
            )

            MorningStudioCard(
                onMorning = {
                    mode = "morning"
                    selectedZone = "studio_power_sequence"
                    ledOn = true
                    brightness = 180f
                    red = 255f
                    green = 220f
                    blue = 160f
                },
                onStudio = {
                    mode = "studio"
                    selectedZone = "studio_lights"
                    ledOn = true
                    brightness = 160f
                    red = 180f
                    green = 0f
                    blue = 255f
                },
                onNight = {
                    mode = "night"
                    selectedZone = "main_led"
                    ledOn = true
                    brightness = 35f
                    red = 255f
                    green = 55f
                    blue = 0f
                }
            )

            SensorsPreviewCard()

            JsonPreviewCard(jsonPayload = jsonPayload)
        }
    }
}

@Composable
fun ColorPreview(previewColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(previewColor, RoundedCornerShape(16.dp))
    )
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1D1D1D)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            content()
        }
    }
}

@Composable
fun MainLedCard(
    ledOn: Boolean,
    brightness: Float,
    red: Float,
    green: Float,
    blue: Float,
    onPowerChange: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onRedChange: (Float) -> Unit,
    onGreenChange: (Float) -> Unit,
    onBlueChange: (Float) -> Unit
) {
    SectionCard(title = "Ręczne sterowanie LED") {
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
                onCheckedChange = onPowerChange
            )
        }

        Text("Jasność: ${brightness.toInt()}", color = Color.White)
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..255f
        )

        Text("Czerwony: ${red.toInt()}", color = Color.White)
        Slider(
            value = red,
            onValueChange = onRedChange,
            valueRange = 0f..255f
        )

        Text("Zielony: ${green.toInt()}", color = Color.White)
        Slider(
            value = green,
            onValueChange = onGreenChange,
            valueRange = 0f..255f
        )

        Text("Niebieski: ${blue.toInt()}", color = Color.White)
        Slider(
            value = blue,
            onValueChange = onBlueChange,
            valueRange = 0f..255f
        )
    }
}

@Composable
fun SleepModeCard(
    onUnderBed: () -> Unit,
    onThreshold: () -> Unit,
    onReadingLamp: () -> Unit,
    onFullSleep: () -> Unit,
    onAllOff: () -> Unit
) {
    SectionCard(title = "Tryb Sen") {
        Text(
            text = "Ciepłe światło, niska jasność, bez telefonu przy łóżku.",
            color = Color.LightGray
        )

        Button(
            onClick = onUnderBed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pod łóżkiem — bardzo nisko")
        }

        Button(
            onClick = onThreshold,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Próg — trochę jaśniej")
        }

        Button(
            onClick = onReadingLamp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wysuwana lampka do czytania")
        }

        Button(
            onClick = onFullSleep,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tryb Sen — wszystkie strefy")
        }

        Button(
            onClick = onAllOff,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wszystko OFF")
        }
    }
}

@Composable
fun MorningStudioCard(
    onMorning: () -> Unit,
    onStudio: () -> Unit,
    onNight: () -> Unit
) {
    SectionCard(title = "Poranek / Studio") {
        Text(
            text = "Docelowo: Tuya, zasilacz, mikrofony, monitory i KRK w bezpiecznej kolejności.",
            color = Color.LightGray
        )

        Button(
            onClick = onMorning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scena Poranek")
        }

        Button(
            onClick = onStudio,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scena Studio")
        }

        Button(
            onClick = onNight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scena Noc")
        }
    }
}

@Composable
fun SensorsPreviewCard() {
    SectionCard(title = "Czujniki — podgląd planowany") {
        Text(
            text = "Pokój: 23.8°C / 48%",
            color = Color.LightGray
        )

        Text(
            text = "Serwer: 39.5°C",
            color = Color.LightGray
        )

        Text(
            text = "Carolina Reaper — gleba: 620",
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Później te dane przyjdą z MQTT z ESP32 / Arduino.",
            color = Color(0xFFFFCC66)
        )
    }
}

@Composable
fun JsonPreviewCard(jsonPayload: String) {
    SectionCard(title = "JSON payload do MQTT") {
        Text(
            text = jsonPayload,
            color = Color.LightGray
        )
    }
}

fun buildLedPayload(
    mode: String,
    zone: String,
    power: Boolean,
    brightness: Int,
    red: Int,
    green: Int,
    blue: Int
): String {
    return """
        {
          "mode": "$mode",
          "zone": "$zone",
          "power": $power,
          "brightness": $brightness,
          "r": $red,
          "g": $green,
          "b": $blue
        }
    """.trimIndent()
}
